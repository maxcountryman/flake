(ns flake.timer
  (:require [clojure.java.io :as io]
            [flake.utils     :as utils]))

(defn write-timestamp
  "Periodically writes the most recent timestamp to path."
  [path f epoch]
  (future
    (loop [next-update (+ 1e3 (utils/now))]
      (with-open [w (io/writer path)]
        (let [now (utils/now-from-epoch epoch)
              ts  (.ts @f)]
          (.write w (str (if (> ts now) ts now)))))

      ;; Sleep for the difference between 1000ms and time spent.
      (Thread/sleep (- next-update (utils/now)))
      (recur (+ 1e3 next-update)))))

(defn read-timestamp
  "Reads a timestamp from path. If the path is not a file or the file is empty, returns 0."
  [path]
  (try
    (Integer/parseInt (slurp path))
    (catch java.lang.NumberFormatException _ 0)
    (catch java.io.IOException _ 0)))
