(ns donkey.services.filesystem.metadata-template-avus
  (:use [clj-jargon.init :only [with-jargon]]
        [donkey.services.filesystem.common-paths]
        [kameleon.uuids :only [uuidify]])
  (:require [clojure-commons.validators :as common-validators]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.clients.metadata.raw :as metadata]
            [donkey.services.filesystem.icat :as icat]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.services.filesystem.validators :as validators]))

(defn do-set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [{username :user} data-id template-id body]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm username)
    (let [data-id (uuidify data-id)
          template-id (uuidify template-id)
          {:keys [path type]} (uuids/path-for-uuid cm username data-id)
          data-type (metadata/resolve-data-type type)]
      (validators/path-writeable cm username path)
      (metadata/set-metadata-template-avus data-id data-type template-id body))))

(with-pre-hook! #'do-set-metadata-template-avus
  (fn [params data-id template-id body]
    (log-call "do-set-metadata-template-avus" params data-id template-id body)
    (common-validators/validate-map params {:user string?})))

(with-post-hook! #'do-set-metadata-template-avus (log-func "do-set-metadata-template-avus"))

(defn do-remove-metadata-template-avus
  "Removes AVUs associated with a Metadata Template for the given user's data item."
  ([params data-id template-id]
   (metadata/remove-metadata-template-avus (uuidify data-id) (uuidify template-id)))

  ([params data-id template-id avu-id]
   (metadata/remove-metadata-template-avu (uuidify data-id) (uuidify template-id) (uuidify avu-id))))

(with-pre-hook! #'do-remove-metadata-template-avus
  (fn [{user :user :as params} data-id template-id & [avu-id]]
    (log-call "do-remove-metadata-template-avus" params data-id template-id avu-id)
    (common-validators/validate-map params {:user string?})
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (let [path (:path (uuids/path-for-uuid cm user data-id))]
        (validators/path-writeable cm user path)))))

(with-post-hook! #'do-remove-metadata-template-avus (log-func "do-remove-metadata-template-avus"))
