(defproject clojure-app "0.1.0-SNAPSHOT"
  :description "handson for my study clojure"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"] [clj-http "2.0.0"]
                 [cheshire "5.10.0"]
                 [ring "1.4.0"]
                 [compojure "1.4.0"]
                 [environ "1.0.1"]]
  :plugins [[lein-environ "1.0.1"]]
  :main ^:skip-aot clojure-app.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[prone "0.8.2"]]
                   :env {:dev true}}})
