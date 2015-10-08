(defproject org.iplantc/clj-cas "5.0.0"
  :description "A CAS Client library written in Clojure."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.cemerick/url "0.1.0"]
                 [org.jasig.cas.client/cas-client-core "3.2.0"
                  :exclusions [javax.servlet/servlet-api]]])
