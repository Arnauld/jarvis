(defproject jarvis "0.1.0-SNAPSHOT"
  :description "JARVIS - my personal assistant"
  :url "http://technbolts.org/jarvis"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ; --- SQL
                 [org.clojure/java.jdbc "0.3.0"]
                 [java-jdbc/dsl "0.1.0"]
                 [org.postgresql/postgresql "9.4.1208"]])
