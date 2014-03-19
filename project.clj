(defproject org.iplantc/porklock "1.2.2"
  :description "A command-line tool for interacting with iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/filetool.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/filetool.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/filetool.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :main porklock.core
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.iplantc/clojure-commons "1.4.8"]
                 [org.clojure/tools.cli "0.2.1"]
                 [commons-io/commons-io "2.2"]
                 [slingshot "0.10.3"]
                 [com.cemerick/url "0.0.7"]
                 [com.novemberain/welle "1.4.0"]
                 [cheshire "5.1.1"]
                 [org.iplantc/clj-jargon "0.4.2"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.3"]]
  :iplant-rpm {:summary "Porklock"
               :type :command
               :exe-files ["curl_wrapper.pl"]}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
