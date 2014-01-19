(defproject mobile-survey "0.1.0-SNAPSHOT"
  :description "This is program for analysis of text message"
  :url "http://ardfard.org/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.1.2"
  :source-paths ["src/clj"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/clojurescript "0.0-1913"]
                 [compojure "1.1.5"]
                 [domina "1.0.2"]
                 [enlive "1.1.4"]
                 [hiccups "0.2.0"]
                 [shoreleave/shoreleave-remote-ring "0.3.0"]
                 [shoreleave/shoreleave-remote "0.3.0"]
                 [com.cemerick/valip "0.3.2"]
                 [lib-noir "0.7.2"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [korma "0.3.0-RC6"]
                 [lobos "1.0.0-beta1"]
                 [postgresql "9.1-901.jdbc4"]
                 [ring-server "0.3.1"]
                 [clj-time "0.6.0"]
                 [dk.ative/docjure "1.6.0"]
                 [iron_mq_clojure "1.0.3"]
                 [cheshire "5.3.1"]
                 [environ "0.4.0"]
                 [clj-msgpack "0.2.0"]]
  :plugins [[lein-cljsbuild "0.3.3"]
            [lein-ring "0.8.7"]]
  :ring {:handler mobile-survey.handler/app :init mobile-survey.handler/init}

  :cljsbuild {:crossovers [valip.core valip.predicates mobile-survey.signin.validators]
              :builds
              [{
                :id "dev"
                :source-paths ["src/cljs"]
                :compiler {
                        :output-to "resources/public/js/mobile-survey.js"
                        :optimizations :whitespace
                        :pretty-print true }}]}

)
