(ns flake.core
  "A decentralized, k-ordered unique ID generator.

  This is a port of Boundary's eponymous Erlang unique ID service. The format
  of the IDs is as follows:

    64 bits - timestamp
    48 bits - id (i.e. MAC address)
    16 bits - sequence

  Timestamp is the current Unix time with millisecond resolution, id is the MAC
  address of the machine, and sequence is a sequence of numbers usually
  initialized to the minimum value.

  Whenever an ID is requested within the same millisecond as a previous ID,
  sequence is incremented otherwise it's reset to its minimum value. So this
  allows for 2^16-1 unique IDs per millisecond per machine.

  New IDs may be generated with the `generate` function, however before doing
  so, `init!` should be executed. Using `init!` helps to ensure that duplicate
  IDs are not generated on a given machine by checking that the current Unix
  time exceeds the last timestamp written to disk.

  For example:

    => (require '[flake.core :as flake])
    => (flake/init!)
    => (take 10 (repeatedly flake/generate))
    (25981799066832176213716719468544N ...)

  Calling `generate` will yeild a BigInteger of 128 bits. Because these numbers
  are long, it may be desirable to encode them in a shorter representation. To
  facilitate this, `flake.utils` provides a base62 encoder.

  For example:

    => (require '[flake.utils :as utils])
    => (map utils/base62-encode (take 3 (repeatedly flake/generate)))
    (\"8n0RhygzZ84kHHLw1I\" \"8n0RhygzZ84kHHLw1J\" \"8n0RhyhLXoMKINWDZY\")
  "
  (:require [clojure.java.io :as io]
            [flake.utils     :as utils]
            [primitive-math  :as p])
  (:import [java.net InetAddress NetworkInterface]))

;; n.b. this allows us to atomically set time whilst potentially.
;; incrementing sequence
(deftype PartialFlake [^long time ^short sequence])

(defonce ^{:private true}
  partial-flake
  (atom (PartialFlake. Long/MIN_VALUE Short/MIN_VALUE)))

;; TODO: Should throw an error if no network interface found.
(defonce ^{:private true}
  hardware-address
  (-> (InetAddress/getLocalHost)
      NetworkInterface/getByInetAddress
      .getHardwareAddress))

;; Persistent timer
(defn write-timestamp
  "Writes time contained by the PartialFlake f to path in a separate thread."
  [f path]
  (future
    (while true
      (with-open [w (io/writer path)]
        (.write w (str (.time ^PartialFlake @f))))

      ;; sleep for a second before writing the next timestamp
      (Thread/sleep 1e3))))

(defn read-timestamp
  "Reads a timestamp from path. If the path is not a file, returns 0."
  [path]
  (or (when (utils/file-exists? path)
        (read-string (slurp path)))
      0))

(defn evolve-flake
  "Evolves f relative to ts. If ts is greater than f's time, returns a new
  PartialFlake with time set to ts and sequence reset. If ts is equal to f's
  time, returns a new PartialFlake with time set to ts and sequence
  incremented. Otherwise time is flowing in the wrong direction and an
  IllegalStateExceptiion is thrown."
  [ts ^PartialFlake f]
  (cond
    ;; clock hasn't moved, increment sequence
    (= ts (.time f)) (PartialFlake. ts (p/inc (.sequence f)))

    ;; clock has progressed, reset sequence
    (> ts (.time f)) (PartialFlake. ts Short/MIN_VALUE)

    ;; illegal state, time is flowing the wrong way
    :else (throw
            (IllegalStateException. "time cannot flow backwards."))))

(defn next-seq-no!
  "Returns the next sequence number relative to ts."
  [ts]
  (.sequence
    ^PartialFlake (swap! partial-flake (partial evolve-flake ts))))

(defn flake-byte-buffer
  ^{:tag java.nio.ByteBuffer
    :doc "Generates a ByteBuffer of 16 bytes where the first 64 bits contain ts
         the next 48 id and the last 16 seq-no."}
  [ts ^bytes id seq-no]
  (doto (utils/byte-buffer 16)
        (.putLong ts)
        (.put id)
        (.putShort seq-no)))

;; Initializer
(defn init!
  "Ensures persistent-ts-path contains a timestamp that is less than the
  current Unix time in milliseconds. This should be called before generating
  new IDs!"
  [& [persistent-ts-path]]
  (let [path (or persistent-ts-path
                 "/tmp/flake-timestamp-dets")]

    ;; Ensure the current time is greater than the last recorded time to
    ;; prevent duplicate IDs from being generated.
    (assert (> (utils/now) (read-timestamp path))
            "persisted time is in the future.")

    ;; Write out the last timestamp in a separate thread
    (write-timestamp path partial-flake)))

;; Generator
(defn generate
  "Generates a unique, k-ordered ID. Returns a BigInteger of 128 bits."
  []
  (let [ts     (utils/now)
        seq-no (next-seq-no! ts)
        flake  (flake-byte-buffer ts hardware-address seq-no)]
    (bigint (.array flake))))
