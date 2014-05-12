(defproject org.iplantc/clj-cas "3.1.3"
  :description "A CAS Client library written in Clojure."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/clj-cas.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/clj-cas.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/clj-cas.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.cemerick/url "0.1.0"]
                 [org.jasig.cas.client/cas-client-core "3.2.0"
                  :exclusions [javax.servlet/servlet-api]]]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
