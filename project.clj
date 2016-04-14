(defproject flake "0.3.2"
  :description "Decentralized, k-ordered unique ID generator."
  :url "https://github.com/maxcountryman/flake"
  :license {:name "BSD 3-Clause license"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [primitive-math "0.1.3"]]
  :profiles {:dev {:dependencies [[criterium "0.4.3"]]}}
  :test-selectors {:default   (complement :benchmark)
                   :benchmark :benchmark
                   :all       (constantly true)}
  :global-vars {*warn-on-reflection* true})
