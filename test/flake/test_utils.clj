(ns flake.test_utils
  (:require [clojure.test :refer [deftest is]]
            [flake.utils  :as utils]))

(deftest test-base62-encode
  (is (= "1c" (utils/base62-encode 100))))

(deftest test-now-from-epoch
  (let [epoch (utils/epoch-mean 10)]
    (dotimes [_ 100]
      (Thread/sleep 1)
      (is (>= 1 (- (System/currentTimeMillis)
                   (utils/now-from-epoch epoch)))))))

(deftest test-with-timeout
  (let [start (utils/now)
        timeout-ms 10]
    (is (thrown? java.util.concurrent.TimeoutException
                 (utils/with-timeout timeout-ms (while true))))
    (is (>= (- (utils/now) start) timeout-ms))
    (is (= (utils/with-timeout timeout-ms (identity :foo)) :foo))))
