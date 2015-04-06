(ns metadactyl.service.apps.combined
  "This namespace contains an implementation of metadactyl.protocols.Apps that interacts with one
  or more other implementations. This implementation expects at most one the implementations that
  it interacts with to allow users to add new apps and edit existing ones. If this is not the case
  then the first app in the list that is capable of adding or editing apps wins."
  (:use [metadactyl.service.apps.combined.util :as util]
        [metadactyl.service.apps.job-listings :as job-listings]
        [metadactyl.util.assertions :only [assert-not-nil]])
  (:require [metadactyl.service.apps.combined.job-view :as job-view]))

(deftype CombinedApps [clients user]
  metadactyl.protocols.Apps

  (getClientName [_]
    "combined")

  (getJobTypes [_]
    (apply concat (map #(.getJobTypes %) clients)))

  (listAppCategories [_ params]
    (apply concat (map #(.listAppCategories % params) clients)))

  (hasCategory [_ category-id]
    (some #(.hasCategory % category-id) clients))

  (listAppsInCategory [_ category-id params]
    (assert-not-nil
     [:category-id category-id]
     (when-let [client (first (filter #(.hasCategory % category-id) clients))]
       (.listAppsInCategory client category-id params))))

  (searchApps [_ search-term params]
    (->> (map #(.searchApps % search-term (select-keys params [:search])) clients)
         (remove nil?)
         (combine-app-search-results params)))

  (canEditApps [_]
    (some #(.canEditApps %) clients))

  (addApp [_ app]
    (.addApp (util/get-apps-client clients) app))

  (previewCommandLine [_ app]
    (.previewCommandLine (util/get-apps-client clients) app))

  (listAppIds [_]
    (apply merge-with concat (map #(.listAppIds %) clients)))

  (deleteApps [_ deletion-request]
    (.deleteApps (util/get-apps-client clients) deletion-request))

  (getAppJobView [_ app-id]
    (job-view/get-app app-id clients))

  (deleteApp [_ app-id]
    (.deleteApp (util/get-apps-client clients) app-id))

  (relabelApp [_ app]
    (.relabelApp (util/get-apps-client clients) app))

  (updateApp [_ app]
    (.updateApp (util/get-apps-client clients) app))

  (copyApp [_ app-id]
    (.copyApp (util/get-apps-client clients) app-id))

  (getAppDescription [_ app-id]
    (->> (map #(.getAppDescription % app-id) clients)
         (remove nil?)
         (first)))

  (getAppDetails [_ app-id]
    (->> (map #(.getAppDetails % app-id) clients)
         (remove nil?)
         (first)))

  (removeAppFavorite [_ app-id]
    (.removeAppFavorite (util/get-apps-client clients) app-id))

  (addAppFavorite [_ app-id]
    (.addAppFavorite (util/get-apps-client clients) app-id))

  (isAppPublishable [_ app-id]
    (.isAppPublishable (util/get-apps-client clients) app-id))

  (makeAppPublic [_ app]
    (.makeAppPublic (util/get-apps-client clients) app))

  (deleteAppRating [_ app-id]
    (.deleteAppRating (util/get-apps-client clients) app-id))

  (rateApp [_ app-id rating]
    (.rateApp (util/get-apps-client clients) app-id rating))

  (getAppTaskListing [_ app-id]
    (->> (map #(.getAppTaskListing % app-id) clients)
         (remove nil?)
         (first)))

  (getAppToolListing [_ app-id]
    (->> (map #(.getAppToolListing % app-id) clients)
         (remove nil?)
         (first)))

  (getAppUi [_ app-id]
    (.getAppUi (util/get-apps-client clients) app-id))

  (addPipeline [self pipeline]
    (.formatPipelineTasks self (.addPipeline (util/get-apps-client clients) pipeline)))

  (formatPipelineTasks [_ pipeline]
    (reduce (fn [acc client] (.formatPipelineTasks client acc)) pipeline clients))

  (updatePipeline [self pipeline]
    (.formatPipelineTasks self (.updatePipeline (util/get-apps-client clients) pipeline)))

  (copyPipeline [self app-id]
    (.formatPipelineTasks self (.copyPipeline (util/get-apps-client clients) app-id)))

  (editPipeline [self app-id]
    (.formatPipelineTasks self (.editPipeline (util/get-apps-client clients) app-id)))

  (listJobs [self params]
    (job-listings/list-jobs self user params))

  (loadAppTables [_ app-ids]
    (apply concat (map  #(.loadAppTables % app-ids) clients)))

  (submitJob [_ submission]
    (.submitJob (util/get-apps-client clients) submission)))
