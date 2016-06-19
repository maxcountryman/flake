(ns flake.core
  "A decentralized, k-ordered unique ID generator.

  This is a port of Boundary's eponymous Erlang unique ID service. The format
  of the IDs is as follows:

    64 bits - ts (i.e. Unix timestamp)
    48 bits - worker-id (i.e. MAC address)
    16 bits - seq-no (i.e. a counter)

  ts is the current Unix time with millisecond resolution, worker-id is the MAC
  address of the machine, and seq-no is a sequence of numbers usually
  initialized to the minimum value.

  Whenever an ID is requested within the same millisecond as a previous ID,
  seq-no is incremented otherwise it is reset to its minimum value. Since
  seq-no is a short this allows for 2^16-1 unique IDs per millisecond per
  machine.

  New IDs may be generated with the `generate!` function, however before doing
  so, `init!` should be executed. Using `init!` helps to ensure that duplicate
  IDs are not generated on a given machine by checking that the current Unix
  time exceeds the last timestamp written to disk.

  For example:

    => (require '[flake.core :as flake])
    => (flake/init!)
    => (flake/flake->bigint (take 10 (repeatedly flake/generate!)))
    (25981799066832176213716719468544N ...)

  Calling `generate!` will yield a ByteBuffer of 128 bits. This in turn can
  be converted to a BigInteger via `flake->bigint`. Because these numbers are
  long, it may be desirable to encode them in a shorter representation. To
  facilitate this, `flake.utils` provides a base62 encoder.

  For example:

    => (require '[flake.utils :as utils])
    => (->> (repeatedly flake/generate!)
            (take 3)
            flake/flake->bigint
            utils/base62-encode)
    (\"8n0RhygzZ84kHHLw1I\" \"8n0RhygzZ84kHHLw1J\" \"8n0RhyhLXoMKINWDZY\")"
  (:require [flake.timer     :as timer]
            [flake.utils     :as utils]
            [primitive-math  :as p])
  (:import [java.nio ByteBuffer]))


;; Simple container for all the bits necessary to assemble a flake ID.
;;
(deftype Flake [^long ts ^bytes worker-id ^short seq-no])

(defonce ^{:private true}
  default-worker-id (first (utils/get-hardware-addresses)))

(defonce ^{:private true}
  flake (atom (Flake. Long/MIN_VALUE default-worker-id Short/MIN_VALUE)))


;; Generator.
;;
(defn generate-flake!
  "Given an atom containing a Flake, a timestamp, and a worker ID, returns
  a Flake where the sequence has either been incremented or reset. An
  IllegalStateException will be thrown if the provided timestamp appears to be
  in the past--e.g. in multi-threaded contexts, where one thread has won a race
  to alter the state of the Flake."
  [f ts worker-id]
  (swap! f
    (fn update-flake [^Flake s]
      (cond
        (= ts (.ts s))
        (Flake. ts worker-id (p/inc (.seq-no s)))

        (> ts (.ts s))
        (Flake. ts worker-id Short/MIN_VALUE)

        :else (throw (IllegalStateException. "time cannot flow backwards."))))))

(defn generate!
  "Generate a new ByteBuffer from a Flake. An optional worker-id can be
  provided, otherwise the default uses a valid hardware interface. Returns the
  ByteBuffer which contains a fully formed Flake."
  ([]
   (generate! default-worker-id))
  ([worker-id]
   (let [bs (try
              (let [ts (utils/now)
                    ^Flake f (generate-flake! flake ts worker-id)]
                (doto (utils/byte-buffer 16)
                  (.putLong (.ts f))
                  (.put ^bytes (.worker-id f))
                  (.putShort (.seq-no f))))
              (catch IllegalStateException _ ::illegal-state))]
     (if (= bs ::illegal-state)
       (recur worker-id)
       bs))))

(defn flake->bigint
  "Converts a ByteBuffer containing a Flake to a BigInteger."
  [^ByteBuffer f]
  (bigint (.array f)))


;; Initializer.
;;
(defn init!
  "Ensures path contains a timestamp that is less than the current Unix time in
  milliseconds. This should be called before generating new IDs!"
  ([]
   (init! "/tmp/flake-timestamp-dets"))
  ([path]
   (assert (> (utils/now) (timer/read-timestamp path))
           "persisted time is in the future.")
   (timer/write-timestamp path flake)))
