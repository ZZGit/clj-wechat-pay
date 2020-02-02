(defproject clj-wechat-pay "0.1.0"
  :description "clojure wechat pay"
  :url "https://github.com/ZZGit/clj-wechat-pay"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.json "0.2.7"]
                 [org.clojure/tools.logging "0.4.1"]
                 [clj-http "3.9.1"]]
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :sign-releases false}]]
  :repl-options {:init-ns clj-wechat-pay.core})
