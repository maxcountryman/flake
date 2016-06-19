(ns flake.test_core
  (:require [clojure.test    :refer [deftest is testing]]
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
  `(try-times* ~n ~sleep-ms (fn [] ~@body)))


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

(deftest test-multi-threaded-generate!
  (let [t1 (future (doall (take 1e6 (repeatedly flake/generate!))))
        t2 (future (doall (take 1e6 (repeatedly flake/generate!))))]
    (try
      (is (apply distinct? (map flake/flake->bigint (concat @t1 @t2))))
      (finally (future-cancel t1) (future-cancel t2)))))

(deftest test-init!
  (let [start (utils/now)
        test-ts-path (java.io.File/createTempFile
                       "flake-test-timestamp-init" ".txt")
        writer (flake/init! test-ts-path)]
    (try
      (loop [ts-written? (> (.length test-ts-path) 0)]
        (if ts-written?
          (is (>= (read-string (slurp test-ts-path)) start))
          (recur (> (.length test-ts-path) 0))))
      (finally (future-cancel writer)))))

(deftest test-bad-init!
  (with-redefs [flake.core/default-epoch 0]
    (let [test-ts-path "/tmp/flake-test-timestamp"]
      ;; Write a time we know is in the future.
      (spit test-ts-path "1")

      ;; Init should fail.
      (is (thrown? java.lang.AssertionError (flake/init! test-ts-path))))))

(deftest ^:benchmark test-performance
  (prn "perf test")
  (testing "Generation performance."
    (quick-bench (flake/generate!))))


;; Simple comparison of Java's random UUIDs to Flakes.
;;
(deftest ^:benchmark test-comp-perf
  (let [flakes (take 1e6 (map flake/flake->bigint (repeatedly flake/generate!)))
        uuids (take 1e6 (repeatedly #(java.util.UUID/randomUUID)))]
    (prn "comparison")
    (testing "Comparison of flakes and UUIDs."
      (quick-bench (= (rand-nth flakes) (rand-nth flakes)))
      (quick-bench (= (rand-nth uuids) (rand-nth uuids))))))

(deftest ^:benchmark test-gen-perf
  (prn "generation")
  (testing "Generation of flakes and UUIDs."
    (quick-bench (flake/generate!))
    (quick-bench #(java.util.UUID/randomUUID))))
