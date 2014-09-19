(ns metadactyl.routes.domain.app.category
  (:use [metadactyl.routes.domain.app]
        [metadactyl.routes.params]
        [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema optional-key Any]])
  (:import [java.util UUID]))

(defschema AppCategory
  {:id
   AppCategoryIdPathParam

   :name
   (describe String "The App Category's name")

   (optional-key :description)
   (describe String "The App Category's description")

   :app_count
   (describe Long "The number of Apps under this Category and all of its children")

   :workspace_id
   (describe UUID "The ID of this App Category's Workspace")

   :is_public
   (describe Boolean
     "Whether this App Category is viewable to all users or private to only the user that owns its
      Workspace")

   ;; KLUDGE
   :categories
   (describe [Any]
     "A listing of child App Categories under this App Category.
      <b>Note</b>: This will be a list of more categories like this one, but the documentation
      library does not currently support recursive model schema definitions")})

(defschema AppCategoryListing
  {:categories (describe [AppCategory] "A listing of App Categories visisble to the requesting user")})

(defschema AppCategoryAppListing
  (merge (dissoc AppCategory :workspace_id :categories)
         {:apps (describe [AppListingDetail] "A listing of Apps under this Category")}))

(defschema AppCategoryPath
  {:username (describe String "A specific username or '&lt;public&gt;' for public Apps")
   :path (describe [String] "The Category path split into a list, starting with the root")})

(defschema AppCategorizationAppInfo
  {:id AppIdParam})

(defschema AppCategorization
  {:category_path (describe AppCategoryPath "")
   :app (describe AppCategorizationAppInfo "The App to be Categorized")})

(defschema AppCategorizationRequest
  {:categories (describe [AppCategorization] "Apps and the Categories they should be listed under")})
