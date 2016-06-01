(ns flake.utils
  (:require [clojure.java.io :as io])
  (:import [java.net NetworkInterface]
           [java.nio ByteBuffer]
           [java.security SecureRandom]))

(def ^{:const true :private true}
  base62-alphabet
  "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")

(defn encode
  "Encodes n in a new base of ks."
  [ks n]
  (let [base (count ks)]
    (->> (iterate #(quot % base) n)
         (take-while pos?)
         (reduce (fn [acc n]
                   (conj acc (nth ks (mod n base))))
                 nil)
         (apply str))))

(def ^{:doc "Encodes a given value into a base62 representation."}
  base62-encode
  (partial encode base62-alphabet))

(defn byte-buffer
  ^{:tag java.nio.ByteBuffer
    :doc "Returns a ByteBuffer allocated to be size."}
  [size]
  (ByteBuffer/allocate size))

(defn now
  "Returns the current Unix time in milliseconds."
  []
  (System/currentTimeMillis))

(defn file-exists?
  "Returns true if path is a file, otherwise false."
  [path]
  (.exists (io/as-file path)))

(defn rand-bytes
  "Return `n` random bytes in an array."
  [n]
  (let [bs (byte-array n)]
    (.nextBytes (SecureRandom.) bs)
    bs))

(defn print-stderr
  "Prints an `error` to stderr."
  [& error]
  (binding [*out* *err*]
    (apply println error)))

(defn get-hardware-addresses
  "Returns a sequence of hardware addresses (generally MACs) formatted as byte
  arrays which have been filtered such that they do not contain nil or
  `0.0.0.0.0.0`."
  []
  (->> (NetworkInterface/getNetworkInterfaces)
       enumeration-seq
       (map (fn [^NetworkInterface ni] (.getHardwareAddress ni)))
       (filter identity)
       (remove #(every? zero? %))))
