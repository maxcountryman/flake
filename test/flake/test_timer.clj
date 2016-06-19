(ns flake.test_timer
  (:require [clojure.java.io :as io]
            [clojure.test    :refer [deftest is]]
            [flake.test_core :refer [try-times]]
            [flake.timer     :as timer])
  (:import [flake.core Flake]))

(deftest test-write-timestamp
  (with-redefs [flake.utils/now (constantly 0)]
    (let [test-ts-path (java.io.File/createTempFile
                         "flake-test-timestamp-write" ".txt")
          flake-atom (atom (Flake. 1 0 0))
          writer (timer/write-timestamp test-ts-path flake-atom)]
      (try
        (try-times 3 1000
          (is (= (read-string (slurp test-ts-path)) 1)))
        (finally (future-cancel writer))))))

(deftest test-read-timestamp
  (let [test-ts-path (java.io.File/createTempFile
                       "flake-test-timestamp-read" ".txt")
        flake-atom (atom (Flake. 1 0 0))]
    (with-open [w (io/writer test-ts-path)]
      (.write w (str (.ts @flake-atom))))
    (is (= (timer/read-timestamp test-ts-path)) 1)))

(deftest test-read-timestamp-not-a-path
  (let [test-ts-path (gensym "not-a-path")]
    (is (= (timer/read-timestamp test-ts-path) 0))))
