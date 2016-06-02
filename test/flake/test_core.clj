(ns flake.test_core
  (:require [clojure.test    :refer [deftest is]]
            [criterium.core  :refer [quick-bench]]
            [flake.core      :as flake]
            [flake.utils     :as utils]
            [primitive-math  :as p])
  (:import [flake.core Flake]))


;; Test helper.
;;
(defn try-times*
  [n sleep-ms thunk]
  (loop [n n]
    (if-let [ret (try
                   [(thunk)]
                   (catch Exception e
                     (when (zero? n)
                       (throw e))
                     (Thread/sleep sleep-ms)))]
      (ret 0)
      (recur (dec n)))))

(defmacro try-times
  [n sleep-ms & body]
  `(try-times* ~n ~sleep-ms (constantly ~@body)))


(deftest test-generate-flake!
  (let [f (atom (Flake. 0 0 Short/MIN_VALUE))]
    (let [ret (flake/generate-flake! f 0 0)]
      (is (= (.ts ret) 0))
      (is (= (.seq-no ret) (p/inc Short/MIN_VALUE))))
    (let [ret (flake/generate-flake! f 1 0)]
      (is (= (.ts ret) 1))
      (is (= (.seq-no ret) Short/MIN_VALUE)))
    (is (thrown? java.lang.IllegalStateException
                 (flake/generate-flake! f -1 0)))))

(deftest test-generate!
  (let [ids (->> (repeatedly flake/generate!)
                 (take 1e6)
                 (map flake/flake->bigint))]

    ;; IDs are lexicographically ordered and unique.
    (is (= ids (sort ids)))
    (is (= ids (distinct ids)))))

(deftest test-init!
  (let [start (utils/now)
        test-ts-path (java.io.File/createTempFile
                       "flake-test-timestamp-init" ".txt")
        writer (flake/init! test-ts-path)]
    (try
      (try-times 3 1e3
        (is (>= (read-string (slurp test-ts-path)) start)))
      (finally (future-cancel writer)))))

(deftest test-bad-init!
  (with-redefs [flake.utils/now (constantly 0)]
    (let [test-ts-path "/tmp/flake-test-timestamp"]
      ;; Write a time we know is in the future.
      (spit test-ts-path "1")

      ;; Init should fail.
      (is (thrown? java.lang.AssertionError (flake/init! test-ts-path))))))

(deftest ^:benchmark test-performance
  (quick-bench (take 1e6 (repeatedly flake/generate!))))
