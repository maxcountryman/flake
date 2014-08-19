(ns flake.test_utils
  (:require [clojure.test :refer [deftest is]]
            [flake.utils  :as utils]))

(deftest test-base62-encode
  (is (= "1C" (utils/base62-encode 100))))
