(ns metadactyl.routes.apps
  (:use [metadactyl.app-listings :only [get-app-groups list-apps-in-group search-apps]]
        [metadactyl.app-validation :only [app-publishable?]]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.params]
        [metadactyl.zoidberg :only [edit-app copy-app edit-workflow]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.service.app-metadata :as app-metadata]
            [metadactyl.util.service :as service]
            [compojure.route :as route]
            [ring.swagger.schema :as ss]
            [schema.core :as s]))

(defroutes* apps
  (GET* "/" []
        :query [params AppSearchParams]
        :summary "Search Apps"
        :notes "This service allows users to search for Apps based on a part of the App name or
        description. The response body contains an `apps` array that is in the same format as
        the `apps` array in the /apps/categories/:category-id endpoint response."
        (service/trap #(search-apps params)))

  (GET* "/:app-id/ui" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :return App
        :summary "Make an App Available for Editing"
        :notes "The app integration utility in the DE uses this service to obtain the App
        description JSON so that it can be edited. The App must have been integrated by the
        requesting user, and it must not already be public."
        (service/trap #(edit-app app-id)))

  (POST* "/:app-id/copy" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParams]
         :summary "Make a Copy of an App Available for Editing"
         :notes "This service can be used to make a copy of an App in the user's workspace."
         (service/trap #(copy-app app-id)))

  (GET* "/:app-id/pipeline-ui" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :return Pipeline
        :summary "Make a Pipeline Available for Editing"
        :notes "The DE uses this service to obtain a JSON representation of a Pipeline for editing.
        The Pipeline must have been integrated by the requesting user, and it must not already be
        public."
        (service/trap #(edit-workflow app-id)))

  (POST* "/:app-id/copy-pipeline" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParams]
         :summary "Make a Copy of a Pipeline Available for Editing"
         :notes "This service can be used to make a copy of a Pipeline in the user's workspace."
         (service/trap #(copy-app app-id)))

  (GET* "/:app-id/is-publishable" [app-id]
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :summary "Determine if an App Can be Made Public"
        :notes "A multi-step App can't be made public if any of the Tasks that are included in it
        are not public. This endpoint returns a true flag if the App is a single-step App or it's a
        multistep App in which all of the Tasks included in the pipeline are public."
        (ce/trap "is-publishable" #(hash-map :publishable (first (app-publishable? app-id)))))

  (DELETE* "/:app-id" []
           :path-params [app-id :- AppIdPathParam]
           :query [params SecuredQueryParams]
           :summary "Logically Deleting an App"
           :notes "An app can be marked as deleted in the DE without being completely removed from
           the database using this service. <b>Note</b>: an attempt to delete an App that is already
           marked as deleted is treated as a no-op rather than an error condition. If the App
           doesn't exist in the database at all, however, then that is treated as an error condition."
           (ce/trap "delete-app" #(app-metadata/delete-app app-id)))

  (POST* "/shredder" []
         :query [params SecuredQueryParams]
         :body [body (describe AppDeletionRequest "List of App IDs to delete.")]
         :summary "Logically Deleting Apps"
         :notes "One or more Apps can be marked as deleted in the DE without being completely
         removed from the database using this service. <b>Note</b>: an attempt to delete an app that
         is already marked as deleted is treated as a no-op rather than an error condition. If the
         App doesn't exist in the database at all, however, then that is treated as an error
         condition."
         (ce/trap "apps-shredder" #(app-metadata/delete-apps body)))

  (route/not-found (service/unrecognized-path-response)))

(defroutes* app-categories
   (GET* "/" []
         :query [params CategoryListingParams]
         :summary "List App Categories"
         :notes "This service is used by the DE to obtain the list of app categories that
         are visible to the user."
         (service/trap #(get-app-groups params)))

   (GET* "/:category-id" []
         :path-params [category-id :- AppCategoryIdPathParam]
         :query [params AppListingParams]
         :summary "List Apps in a Category"
         :notes "This service lists all of the apps within an app category or any of its
         descendents. The DE uses this service to obtain the list of apps when a user
         clicks on a category in the _Apps_ window.
         This endpoint accepts optional URL query parameters to limit and sort Apps,
         which will allow pagination of results.
         The `can_run` flag is calculated by comparing the number of steps in the app to
         the number of steps that have a tool associated with them. If the numbers are
         different then this flag is set to `false`. The idea is that every step in the
         analysis has to have, at the very least, a tool associated with it in order to run
         successfully."
         (service/trap #(list-apps-in-group category-id params)))

  (route/not-found (service/unrecognized-path-response)))
