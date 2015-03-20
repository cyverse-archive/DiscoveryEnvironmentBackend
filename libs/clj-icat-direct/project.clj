(defproject org.iplantc/clj-icat-direct "4.2.2"
  :description "A Clojure library for accessing the iRODS ICAT database directly."
  :url "http://github.com/iPlantCollaborativeOpenSource/clj-icat-direct/"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [korma "0.4.0"]
                 [org.postgresql/postgresql "9.2-1002-jdbc4"]])
