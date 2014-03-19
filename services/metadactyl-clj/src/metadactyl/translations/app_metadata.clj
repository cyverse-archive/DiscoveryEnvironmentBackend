(ns metadactyl.translations.app-metadata
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as cheshire]
            [metadactyl.translations.app-metadata.external-to-internal :as e2i]
            [metadactyl.translations.app-metadata.external-to-preview :as e2p]
            [metadactyl.translations.app-metadata.internal-to-external :as i2e]))

(defn- log-as-json
  [msg obj]
  (log/trace msg (cheshire/encode obj {:pretty true})))

(defn template-external-to-internal
  "Translates the external template format to the internal template format."
  [external]
  (log-as-json "template-external-to-internal - before:" external)
  (let [internal (e2i/translate-template external)]
    (log-as-json "template-external-to-internal - after:" internal)
    internal))

(defn template-internal-to-external
  [internal]
  (log-as-json "template-internal-to-external - before:" internal)
  (let [external (i2e/translate-template internal)]
    (log-as-json "template-internal-to-external - after:" external)
    external))

(defn template-cli-preview-req
  [external]
  (log-as-json "template-cli-preview-req - before:" external)
  (let [internal (e2p/translate-template external)]
    (log-as-json "template-cli-preview-req - after:" internal)
    internal))
