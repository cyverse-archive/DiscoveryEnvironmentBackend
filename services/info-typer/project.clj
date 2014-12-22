(defproject info-typer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/tools.logging "0.3.0"]
                 [com.novemberain/langohr "2.11.0"]
                 [me.raynes/fs "1.4.6"]
                 [org.iplantc/clj-jargon "4.1.0"]
                 [org.iplantc/clojure-commons "4.1.0"]
                 [org.iplantc/common-cli "4.1.0"]
                 [org.iplantc/heuristomancer "4.1.0"]]
  :main ^:skip-aot info-typer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
