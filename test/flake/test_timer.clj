(ns flake.test_timer
  (:require [clojure.java.io :as io]
            [clojure.test    :refer [deftest is]]
            [flake.timer     :as timer]
            [flake.utils     :as utils])
  (:import [flake.core Flake]))

(deftest test-write-timestamp
  (with-redefs [utils/now-from-epoch (constantly 0)]
    (let [test-ts-path (java.io.File/createTempFile
                         "flake-test-timestamp-write" ".txt")
          flake-atom (atom (Flake. 1 0 0))
          epoch 0
          writer (timer/write-timestamp test-ts-path flake-atom epoch)]
      (try
        (loop [ts-written? (> (.length test-ts-path) 0)]
          (if ts-written?
            (is (= (read-string (slurp test-ts-path)) 1))
            (recur (> (.length test-ts-path) 0))))
        (finally (future-cancel writer))))))

(deftest test-read-timestamp
  (let [test-ts-path (java.io.File/createTempFile
                       "flake-test-timestamp-read" ".txt")
        flake-atom (atom (Flake. 1 0 0))]
    (with-open [w (io/writer test-ts-path)]
      (.write w (str (.ts ^Flake @flake-atom))))
    (is (= (timer/read-timestamp test-ts-path)) 1)))

(deftest test-read-timestamp-not-a-path
  (let [test-ts-path (gensym "not-a-path")]
    (is (= (timer/read-timestamp test-ts-path) 0))))


(deftest test-read-empty-file
  (let [test-ts-path (java.io.File/createTempFile
                      "flake-test-timestamp-empty" ".txt")]
    (is (= (timer/read-timestamp test-ts-path) 0))))
