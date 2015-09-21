(ns metadata.routes.domain.tags
  (:use [common-swagger-api.schema :only [->optional-param
                                          describe
                                          NonBlankString
                                          StandardUserQueryParams]]
        [metadata.routes.domain.common])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def TagIdPathParam (describe UUID "The tag's UUID"))
(def TagValueString (s/both NonBlankString (s/pred #(<= (count %) 255) 'valid-tag-value-size?)))
(def TagSuggestLimit (s/both Long (s/pred (partial < 0) 'valid-tag-suggest-limit?)))

(s/defschema UpdateAttachedTagsQueryParams
  (merge StandardDataItemQueryParams
    {:type
     (describe (s/enum "attach" "detach")
       "Whether to attach or detach the provided set of tags to the file/folder")}))

(s/defschema TagSuggestQueryParams
  (merge StandardUserQueryParams
    {:contains
     (describe String "The value fragment")

     (s/optional-key :limit)
     (describe TagSuggestLimit
       "The maximum number of suggestions to return. No limit means return all")}))

(s/defschema Tag
  {:id
   (describe UUID "The service-provided UUID associated with the tag")

   :value
   (describe TagValueString "The value used to identify the tag, at most 255 characters in length")

   (s/optional-key :description)
   (describe String "The description of the purpose of the tag")})

(s/defschema TagRequest
  (dissoc Tag :id))

(s/defschema TagUpdateRequest
  (->optional-param TagRequest :value))

(s/defschema TagList
  {:tags (describe [Tag] "A list of Tags")})

(s/defschema TagIdList
  {:tags (describe [UUID] "A list of Tag UUIDs")})

(s/defschema AttachedTagTarget
  {:id   (describe UUID "The target's UUID")
   :type (describe DataTypeEnum "The target's data type")})

(s/defschema TagDetails
  (merge Tag
    {:owner_id    (describe String "The owner of the tag")
     :public      (describe Boolean "Whether the tag is publicly accessible")
     :created_on  (describe Long "The date the tag was created in ms since the POSIX epoch")
     :modified_on (describe Long "The date the tag was last modified in ms since the POSIX epoch")}))

(s/defschema AttachedTagDetails
  (merge TagDetails
    {:targets (describe [AttachedTagTarget] "A list of targets attached to the tag")}))

(s/defschema UpdateAttachedTagsResponse
  {:tags (describe [AttachedTagDetails] "A list of tags and their attached targets")})
