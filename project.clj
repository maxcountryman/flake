(defproject flake "0.4.4"
  :description "Decentralized, k-ordered unique ID generator."
  :url "https://github.com/maxcountryman/flake"
  :license {:name "BSD 3-Clause license"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [primitive-math "0.1.5"]]
  :java-source-paths ["src/flake"]
  :profiles {:dev {:dependencies [[criterium "0.4.4"]]}}
  :test-selectors {:default   (complement :benchmark)
                   :benchmark :benchmark
                   :all       (constantly true)}
  :global-vars {*warn-on-reflection* true})
