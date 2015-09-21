(ns metadactyl.routes.domain.app.category
  (:use [common-swagger-api.schema :only [->optional-param
                                          ->required-key
                                          describe
                                          PagingParams
                                          SortFieldDocs
                                          SortFieldOptionalKey]]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.params]
        [schema.core :only [defschema optional-key recursive enum]])
  (:require [clojure.set :as sets])
  (:import [java.util UUID]))

(def AppCategoryNameParam (describe String "The App Category's name"))

(defschema CategoryListingParams
  (merge SecuredQueryParamsEmailRequired
    {(optional-key :public)
     (describe Boolean
       "If set to 'true', then only app categories that are in a workspace that is marked as
        public in the database are returned. If set to 'false', then only app categories that
        are in the user's workspace are returned. If not set, then both public and the user's
        private categories are returned.")}))

(def AppListingValidSortFields
  (-> (map ->required-key (keys AppListingDetail))
      (conj :average_rating :user_rating)
      set
      (sets/difference #{:app_type
                         :can_favor
                         :can_rate
                         :can_run
                         :pipeline_eligibility
                         :rating})))

(defschema AppListingPagingParams
  (merge SecuredQueryParamsEmailRequired
    (assoc PagingParams
      SortFieldOptionalKey
      (describe (apply enum AppListingValidSortFields) SortFieldDocs))))

(defschema AppCategory
  {:id
   AppCategoryIdPathParam

   :name
   AppCategoryNameParam

   :app_count
   (describe Long "The number of Apps under this Category and all of its children")

   :is_public
   (describe Boolean
     "Whether this App Category is viewable to all users or private to only the user that owns its
      Workspace")

   (optional-key :categories)
   (describe [(recursive #'AppCategory)]
     "A listing of child App Categories under this App Category")})

(defschema AppCategoryListing
  {:categories (describe [AppCategory] "A listing of App Categories visisble to the requesting user")})

(defschema AppCategoryIdList
  {:category_ids (describe [UUID] "A List of UUIDs used to identify App Categories")})

(defschema AppCategoryAppListing
  (merge (dissoc AppCategory :categories)
         {:apps (describe [AppListingDetail] "A listing of Apps under this Category")}))

(defschema AppCategorization
  (merge AppCategoryIdList
    {:app_id (describe UUID "The UUID of the App to be Categorized")}))

(defschema AppCategorizationRequest
  {:categories (describe [AppCategorization] "Apps and the Categories they should be listed under")})

(defschema AppCategoryRequest
  {:name      AppCategoryNameParam
   :parent_id (describe UUID "The UUID of the App Category's parent Category.")})

(defschema AppCategoryPatchRequest
  (-> AppCategoryRequest
      (->optional-param :name)
      (->optional-param :parent_id)))
