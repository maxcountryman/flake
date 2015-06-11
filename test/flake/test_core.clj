(ns flake.test_core
  (:require [clojure.test    :refer [deftest is]]
            [criterium.core  :refer [quick-bench]]
            [flake.core      :as flake]
            [primitive-math  :as p])
  (:import [flake.core PartialFlake]))

(deftest test-evolve-flake
  (let [f (PartialFlake. 1 (short 0))]
    ;; Ensure a new PartialFlake is returned with ts
    (is (= 2
           (.time ^PartialFlake (flake/evolve-flake 2 f))))

    ;; Ensure a new millisecond resets the sequence
    (is (= Short/MIN_VALUE
           (.sequence ^PartialFlake (flake/evolve-flake 2 f))))

    ;; Ensure the same millisecond incs the sequence
    (is (= (p/inc (.sequence f))
           (.sequence ^PartialFlake (flake/evolve-flake 1 f))))

    ;; Ensure we can't time travel
    (is (thrown? java.lang.IllegalStateException
                 (flake/evolve-flake 0 f)))))

(deftest test-init!
  (with-redefs [flake.utils/now (constantly 0)]
    (let [test-ts-path "/tmp/flake-test-timestamp"]
      ;; Write a time we know is in the future
      (spit test-ts-path "1")

      ;; Init should fail
      (is (thrown? java.lang.AssertionError (flake/init! test-ts-path))))))

(deftest test-generate
  (let [ids (->> (repeatedly flake/generate)
                 (take 1e6))]

    ;; IDs are lexicographically ordered and unique
    (is (= ids (sort ids)))
    (is (= ids (distinct ids)))))

(deftest ^:benchmark test-performance
  (quick-bench (flake/generate)))
