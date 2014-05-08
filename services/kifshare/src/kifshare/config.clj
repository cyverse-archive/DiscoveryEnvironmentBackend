(ns kifshare.config
  (:require [clojure.string :as string]
            [clj-jargon.init :as jinit]
            [clojure-commons.props :as prps]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(def props (atom nil))

(def robots-txt (atom ""))

(defn robots-txt-path
  []
  (get @props "kifshare.app.robots-txt"))

(defn service-name
  []
  (get @props "kifshare.app.service-name"))

(defn service-version
  []
  (get @props "kifshare.app.service-version"))

(defn client-cache-scope
  []
  (get @props "kifshare.app.client-cache-scope"))

(defn client-cache-max-age
  []
  (get @props "kifshare.app.client-cache-max-age"))

(defn robots-txt-content
  []
  @robots-txt)

(defn local-init
  [local-config-path]
  (let [main-props (prps/read-properties local-config-path)]
    (reset! props main-props)))

(defn resources-root
  []
  (get @props "kifshare.app.resources-root"))

(defn js-dir
  []
  (get @props "kifshare.app.js-dir"))

(defn img-dir
  []
  (get @props "kifshare.app.images-dir"))

(defn css-dir
  []
  (get @props "kifshare.app.css-dir"))

(defn flash-dir
  []
  (get @props "kifshare.app.flash-dir"))

(defn de-url
  []
  (get @props "kifshare.app.de-url"))

(defn irods-url
  []
  (get @props "kifshare.app.irods-url"))

(defn logo-path
  []
  (get @props "kifshare.app.logo-path"))

(defn favicon-path
  []
  (get @props "kifshare.app.favicon-path"))

(defn de-import-flags
  []
  (get @props "kifshare.app.de-import-flags"))

(defn footer-text
  []
  (get @props "kifshare.app.footer-text"))

(defn curl-flags
  []
  (get @props "kifshare.app.curl-flags"))

(defn wget-flags
  []
  (get @props "kifshare.app.wget-flags"))

(defn iget-flags
  []
  (get @props "kifshare.app.iget-flags"))

(defn username
  []
  (or (get @props "kifshare.irods.user")
      "public"))

(def jgcfg (atom nil))

(defn jargon-config [] @jgcfg)

(defn jargon-init
  []
  (reset! jgcfg
          (jinit/init
           (get @props "kifshare.irods.host")
           (get @props "kifshare.irods.port")
           (get @props "kifshare.irods.user")
           (get @props "kifshare.irods.password")
           (get @props "kifshare.irods.home")
           (get @props "kifshare.irods.zone")
           (get @props "kifshare.irods.defaultResource"))))

(defn css-files
  []
  (mapv
    string/trim
    (string/split
      (get @props "kifshare.app.css-files")
      #",")))

(defn javascript-files
  []
  (mapv
    string/trim
    (string/split
      (get @props "kifshare.app.javascript-files")
      #",")))

(defn- exception-filters
  []
  (mapv #(re-pattern (str %))
        [(get @props "kifshare.irods.user")
         (get @props "kifshare.irods.password")]))

(defn register-exception-filters
  []
  (ce/register-filters (exception-filters)))

(defn log-config
  []
  (log/warn "Configuration:")
  (cc/log-config props :filters [#"irods\.user"]))
