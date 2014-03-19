(ns infosquito.actions
  (:require [infosquito.es-crawler :as esc]
            [infosquito.icat :as icat]
            [infosquito.props :as cfg]))

(defn- props->icat-cfg
  [p]
  {:icat-host        (cfg/get-icat-host p)
   :icat-port        (cfg/get-icat-port p)
   :icat-db          (cfg/get-icat-db p)
   :icat-user        (cfg/get-icat-user p)
   :icat-password    (cfg/get-icat-pass p)
   :collection-base  (cfg/get-base-collection p)
   :es-url           (cfg/get-es-url p)
   :notify?          (cfg/notify-enabled? p)
   :notify-count     (cfg/get-notify-count p)
   :index-batch-size (cfg/get-index-batch-size p)})

(defn reindex
  [props]
  (let [icat-cfg ((comp icat/init props->icat-cfg) props)]
    (icat/with-icat icat-cfg
      (esc/purge-index props)
      (icat/reindex icat-cfg))))
