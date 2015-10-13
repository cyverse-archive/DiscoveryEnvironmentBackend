(ns donkey.clients.data-info
  (:use [donkey.auth.user-attributes :only [current-user]]
        [clj-jargon.init :only [with-jargon]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [me.raynes.fs :as fs]
            [clj-icat-direct.icat :as db]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.assertions :as assertions]
            [donkey.services.filesystem.common-paths :as cp]
            [donkey.services.filesystem.create :as cr]
            [donkey.services.filesystem.exists :as e]
            [donkey.services.filesystem.icat :as icat]
            [donkey.services.filesystem.metadata :as mt]
            [donkey.services.filesystem.sharing :as sharing]
            [donkey.services.filesystem.stat :as st]
            [donkey.services.filesystem.status :as status]
            [donkey.services.filesystem.users :as users]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.util.config :as cfg]
            [donkey.util.service :as svc])
  (:import [clojure.lang IPersistentMap ISeq Keyword]
           [java.util UUID]))


(defn ^Boolean irods-running?
  "Determines whether or not iRODS is running."
  []
  (status/irods-running?))


(defn ^String user-home-folder
  "Determines the home folder for the given user.

   Parameters:
     user - the user of the home folder.

   Returns:
     It returns the absolute path to the home folder."
  [^String user]
  (cp/user-home-dir user))


(defn ^String base-trash-folder
  "It returns the root directory for all users' trash folders."
  []
  (cp/base-trash-path))


(defn ^String user-trash-folder
  "Determines the top-level trash folder for the given user.

   Parameters:
     user - the user of the trash folder.

   Returns:
     It returns the absolute path to the trash folder."
  [^String user]
  (cp/user-trash-path user))

(defn- uuid-for-path
  [^String user ^String path]
    (with-jargon (icat/jargon-cfg) [cm]
      (:id (uuids/uuid-for-path cm user path))))

(defn ensure-dir-created
  "If a folder doesn't exist, it creates the folder and makes the given user an owner of it.

   Parameters:
     user - the username of the user to become an owner of the new folder
     dir  - the absolute path to the folder"
  [^String user ^String dir]
  (cr/ensure-created user dir))

(defn read-chunk
  "Uses the data-info read-chunk endpoint."
  [params body]
  (let [path-uuid (uuid-for-path (:user params) (:path body))
        url (url/url (cfg/data-info-base-url) "data" path-uuid "chunks")
        req-map {:query-params (assoc
                                 (select-keys params [:user])
                                 :position (:position body)
                                 :size (:chunk-size body))
                 :content-type :json}]
    (http/get (str url) req-map)))

(defn read-tabular-chunk
  "Uses the data-info read-tabular-chunk endpoint."
  [params body]
  (let [path-uuid (uuid-for-path (:user params) (:path body))
        url (url/url (cfg/data-info-base-url) "data" path-uuid "chunks-tabular")
        req-map {:query-params (assoc
                                 (select-keys params [:user])
                                 :separator (:separator body)
                                 :page (:page body)
                                 :size (:chunk-size body))
                 :content-type :json}]
    (http/get (str url) req-map)))

(defn create-dirs
  [params body]
  (let [url     (url/url (cfg/data-info-base-url) "data" "directories")
        req-map {:query-params (select-keys params [:user])
                 :content-type :json
                 :body         (json/encode body)}]
    (http/post (str url) req-map)))

(defn create-dir
  [params {:keys [path]}]
  (let [paths-request {:paths [path]}]
    (create-dirs params paths-request)
    (-> (st/do-stat params paths-request)
        :paths
        (get path))))

(defn get-or-create-dir
  "Returns the path argument if the path exists and refers to a directory.  If
   the path exists and refers to a regular file then nil is returned.
   Otherwise, a new directory is created and the path is returned."
  [path]
  (log/debug "getting or creating dir: path =" path)
  (cond
   (not (e/path-exists? path))
    (create-dir {:user (:shortUsername current-user)} {:path path})

   (and (e/path-exists? path) (st/path-is-dir? path))
   path

   (and (e/path-exists? path) (not (st/path-is-dir? path)))
   nil

   :else
   nil))

(defn can-create-dir?
  "Determines if a directory exists or can be created."
  [user path]
  (log/warn "checking to see if" path "can be created")
  (st/can-create-dir? user path))

(defn rename
  "Uses the data-info set-name endpoint to rename a file within the same directory."
  [params body]
  (assertions/assert-valid (= (ft/dirname (:dest body)) (ft/dirname (:source body)))
      "The directory names of the source and destination must match for this endpoint.")
  (let [path-uuid (uuid-for-path (:user params) (:source body))
        url (url/url (cfg/data-info-base-url) "data" path-uuid "name")
        req-map {:query-params (select-keys params [:user])
                 :content-type :json
                 :body         (json/encode {:filename (ft/basename (:dest body))})}]
    (http/put (str url) req-map)))

(defn- move-single
  "Uses the data-info single-item directory change endpoint to move an item to a different directory."
  [user source dest]
  (let [path-uuid (uuid-for-path user source)
        url (url/url (cfg/data-info-base-url) "data" path-uuid "dir")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:dirname dest})}]
    (log/info "using " (str url) " to move data item " source " (" path-uuid ") to " dest)
    (http/put (str url) req-map)))

(defn- move-multi
  "Uses the data-info bulk mover endpoint to move a number of items to a different directory."
  [user sources dest]
  (let [url (url/url (cfg/data-info-base-url) "mover")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:sources sources :dest dest})}]
    (log/info "using " (str url) " to move several data items to " dest)
    (http/post (str url) req-map)))

(defn move
  "Uses the data-info single and bulk mover endpoints to move an item or many items into a new directory."
  [params body]
  (if (= 1 (count (:sources body)))
    (move-single (:user params) (first (:sources body)) (:dest body))
    (move-multi (:user params) (:sources body) (:dest body))))

(defn move-contents
  "Uses the data-info set-children-directory-name endpoint to move the contents of one directory
   into another directory."
  [params body]
  (let [path-uuid (uuid-for-path (:user params) (:source body))
        url (url/url (cfg/data-info-base-url) "data" path-uuid "children" "dir")
        req-map {:query-params (select-keys params [:user])
                 :content-type :json
                 :body         (json/encode {:dirname (:dest body)})}]
    (http/put (str url) req-map)))

(defn delete-paths
    "Uses the data-info deleter endpoint to delete many paths."
    [params body]
    (let [url (url/url (cfg/data-info-base-url) "deleter")
          req-map {:query-params (select-keys params [:user])
                   :content-type :json
                   :body (json/encode body)}]
      (http/post (str url) req-map)))

(defn delete-contents
    "Uses the data-info delete-children endpoint to delete the contents of a directory."
    [params body]
    (let [path-uuid (uuid-for-path (:user params) (:path body))
          url (url/url (cfg/data-info-base-url) "data" path-uuid "children")
          req-map {:query-params (select-keys params [:user])
                   :content-type :json}]
      (http/delete (str url) req-map)))

(defn delete-trash
    "Uses the data-info trash endpoint to empty the trash of a user."
    [params]
    (let [url (url/url (cfg/data-info-base-url) "/trash")
          req-map {:query-params (select-keys params [:user])
                   :content-type :json}]
      (http/delete (str url) req-map)))

(defn restore-files
    "Uses the data-info restorer endpoint to restore many or all paths."
    ([params]
     (restore-files params {}))
    ([params body]
     (let [url (url/url (cfg/data-info-base-url) "/restorer")
           req-map {:query-params (select-keys params [:user])
                    :content-type :json
                    :body         (json/encode body)}]
       (http/post (str url) req-map))))

(defn collect-permissions
    "Uses the data-info permissions-gatherer endpoint to query user permissions for a set of files/folders."
    [params body]
    (let [url (url/url (cfg/data-info-base-url) "permissions-gatherer")
          req-map {:query-params (select-keys params [:user])
                   :content-type :json
                   :body (json/encode body)}]
      (http/post (str url) req-map)))

(defn get-type-list
    "Uses the data-info file-types endpoint to produce a list of acceptable types."
    []
    (let [url (url/url (cfg/data-info-base-url) "file-types")
          req-map {:content-type :json}]
      (http/get (str url) req-map)))

(defn set-file-type
    "Uses the data-info set-type endpoint to change the type of a file."
    [params body]
    (let [path-uuid (uuid-for-path (:user params) (:path body))
          url (url/url (cfg/data-info-base-url) "data" path-uuid "type")
          req-map {:query-params (select-keys params [:user])
                   :content-type :json
                   :body (json/encode (select-keys body [:type]))}]
      (http/put (str url) req-map)))

(defn gen-output-dir
  "Either obtains or creates a default output directory using a specified base name."
  [base]
  (first
   (remove #(nil? (get-or-create-dir %))
           (cons base (map #(str base "-" %) (iterate inc 1))))))

(defn build-path
  "Builds a path from a base path and a list of components."
  [path & components]
  (string/join
   "/"
   (cons (string/replace path #"/+$" "")
         (map #(string/replace % #"^/+|/+$" "") components))))


(defn get-tree-metaurl
  "Gets the URL used to get saved tree URLs."
  [user path]
  (->> (mt/metadata-get user path)
    (:metadata)
    (filter #(= (:attr %) "tree-urls"))
    (first)
    (:value)))


(defn save-tree-metaurl
  "Saves the URL used to get saved tree URLs. The metaurl argument should contain the URL used to
   obtain the tree URLs."
  [path metaurl]
  (mt/admin-metadata-set path {:attr "tree-urls" :value metaurl :unit ""}))


(defn ^ISeq list-user-groups
  "retrieves a list of groups names a given user belongs to

   Params:
     user - the username of the user of interest

   Returns:
     It returns the list of group names."
  [^String user]
  (users/list-user-groups user))


(defn ^IPersistentMap path-stat
  "retrieves the stat info for an entity with a given path

   Params:
     user - the username of the user making the request
     path - the absolute path to the entity

   Returns:
     It returns the stat info formatted for the HTTP response."
  [^String user ^String path]
  (st/path-stat user path))


(defn ^IPersistentMap stat-by-uuid
  "Resolves a stat info for the entity with a given UUID.

   Params:
     user - the user requesting the info
     uuid - the UUID

   Returns:
     It returns a path-stat map containing an additional UUID field."
  [^String user ^UUID uuid]
  (uuids/path-for-uuid user uuid))


(defn ^ISeq stats-by-uuids-paged
  "Resolves the stat info for the entities with the given UUIDs. The results are paged.

   Params:
     user       - the user requesting the info
     sort-field - the stat field to sort on
     sort-order - the direction of the sort (asc|desc)
     limit      - the maximum number of results to return
     offset     - the number of results to skip before returning some
     uuids      - the UUIDS of interest
     info-types - This is info types to of the files to return. It may be nil, meaning return all
                  info types, a string containing a single info type, or a sequence containing a set
                  of info types.

   Returns:
     It returns a page of stat info maps."
  [^String  user
   ^String  sort-field
   ^String  sort-order
   ^Integer limit
   ^Integer offset
   ^ISeq    uuids
            info-types]
  (let [info-types (if (string? info-types) [info-types] info-types)
        page       (uuids/paths-for-uuids-paged user
                                                sort-field
                                                sort-order
                                                limit
                                                offset
                                                uuids
                                                info-types)]
    {:files   (filter #(= (:type %) :file) page)
     :folders (filter #(= (:type %) :dir) page)
     :total   (db/number-of-uuids-in-folder user (cfg/irods-zone) uuids info-types)}))


(defn ^Boolean uuid-accessible?
  "Indicates if a data item is readable by a given user.

   Parameters:
     user     - the authenticated name of the user
     data-id  - the UUID of the data item

   Returns:
     It returns true if the user can access the data item, otherwise false"
  [^String user ^UUID data-id]
  (uuids/uuid-accessible? user data-id))


(defn validate-uuid-accessible
  "Throws an exception if the given data item is not accessible to the given user.

   Parameters:
     user     - the authenticated name of the user
     data-id  - the UUID of the data item"
  [^String user ^UUID data-id]
  (when-not (uuid-accessible? user data-id)
    (throw+ {:error_code error/ERR_NOT_FOUND :uuid data-id})))


(defn ^Boolean owns?
  "Indicates if a file or folder is owned by a given user.

   Parameters:
     user       - the username of the user
     data-path - The absolute path to the file or folder

   Returns:
     It returns true if the user own the data item, otherwise false."
  [^String user ^String data-path]
  (users/owns? user data-path))


(defn ^String resolve-data-type
  "Given filesystem id, it returns the type of data item it is, file or folder.

   Parameters:
     data-id - The UUID of the data item to inspect

   Returns:
     The type of the data item, `file` or `folder`"
  [^UUID data-id]
  (icat/resolve-data-type data-id))


  (defn ^IPersistentMap share
  "grants access to a list of data entities for a list of users by a user

   Params:
     user        - the username of the sharing user
     share-withs - the list of usernames receiving access
     fpaths      - the list of absolute paths to the data entities being shared
     perm        - the permission being granted to the user users (read|write|own)

   Returns:
     It returns a map with the following fields:

       :user    - the list of users who actually received access
       :path    - the list of paths actually shared
       :skipped - the list of records for the things skipped, each record has the following fields:
                    :user   - the user who didn't get access
                    :path   - the path the user didn't get access to
                    :reason - the reason access wasn't granted
       :perm    - the permission that was granted"
  [^String user ^ISeq share-withs ^ISeq fpaths ^String perm]
  (sharing/share user share-withs fpaths perm))


(defn unshare
  "Params:
     user          - the username of the user removing access
     unshare-withs - the list of usernames having access removed
     fpaths        - the list of absolute paths ot the data entities losing accessibility

   Returns:
     It returns a map with the following fields:

       :user    - the list of users who lost access
       :path    - the list of paths that lost accessibility
       :skipped - a list of records for the things skipped, each record has the following fields:
                    :user   - the user who kept access
                    :path   - the path the user still can access
                    :reason - the reason access wasn't removed"
  [^String user ^ISeq unshare-withs ^ISeq fpaths]
  (sharing/unshare user unshare-withs fpaths))


(defn- fmt-method
  [method]
  (string/upper-case (name method)))


(defn- handle-service-error
  [method url msg]
  (let [full-msg (str method " " url " had a service error: " msg)]
    (log/error full-msg)
    (svc/request-failure full-msg)))


(defn- handle-client-error
  [method url err msg]
  (let [full-msg (str "interal error related to usage of " method " " url ": " msg)]
    (log/error err full-msg)
    (svc/request-failure full-msg)))


(defn ^String mk-data-path-url-path
  "This function constructs the url path to the resource backing a given data item.

   Parameters:
     path - the absolute iRODS path to the data item

   Returns:
     It returns the data-info URL path to the corresponding resource"
  [^String path]
  (let [nodes (fs/split path)
        nodes (if (= "/" (first nodes)) (next nodes) nodes)]
    (str "data/path/" (string/join "/" (map url/url-encode nodes)))))


(defn respond-with-default-error
  "This function generates the default responses for errors returned from a data-info request.

   Parameters:
     status - the data-info HTTP response code
     method - the HTTP method called
     url    - the URL called
     err    - the clj-http stone wrapping the error

   Throws:
     It always throws a slingshot stone with the following fields.

       :error_code - ERR_REQUEST_FAILED
       :message    - a message describing the error"
  [^Integer status method url ^IPersistentMap err]
  (let [method (fmt-method method)
        url    (str url)]
    (case status
       400 (handle-client-error method url err "bad request")
       403 (handle-client-error method url err "user not allowed")
       404 (handle-client-error method url err "URL not found")
       405 (handle-client-error method url err "method not supported")
       406 (handle-client-error method url err "doesn't support requested content type")
       409 (handle-client-error method url err "request would conflict")
       410 (handle-client-error method url err "no longer exists")
       412 (handle-client-error method url err "provided precondition failed")
       413 (handle-client-error method url err "request body too large")
       414 (handle-client-error method url err "URL too long")
       415 (handle-client-error method url err "doesn't support request body's content type")
       422 (handle-client-error method url err "the request was not processable")
       500 (handle-service-error method url "internal error")
       501 (handle-service-error method url "not implemented")
       503 (handle-service-error method url "temporarily unavailable")
           (handle-client-error method url err "unexpected response code"))))


(defn- handle-error
  [method url err handlers]
  (let [status (:status err)]
    (if-let [handler ((keyword (str status)) handlers)]
      (handler method url err)
      (respond-with-default-error status method url err))))


(defn- resolve-http-call
  [method]
  (case method
    :delete   http/delete
    :get      http/get
    :head     http/head
    :options  http/options
    :patch    http/patch
    :post     http/post
    :put      http/put))


(defn request
  "This function makes an HTTP request to the data-info service. It uses clj-http to make the
   request. It traps any errors and provides a response to it. A custom error handler may be
   provided for each type of error.

   The handler needs to be a function with the following signature.

     (fn [^Keyword method ^String url ^IPersistentMap err])

     method - is the unaltered method parameter passed in the the request function.
     url    - is the URL of the data-info resource.
     err    - is the clj-http stone wrapping the error response.

   Parameters:
     method         - The HTTP method (:delete|:get|:head|:options|:patch|:post|:put)
     url-path       - The path to the data info resource being accessed
     req-map        - The ring request
     error-handlers - (OPTIONAL) zero or more handlers for different error responses. They are
                      provided as named parameters where the name is a keyword based on the error
                      code. For example, :404 handle-404 would define the function handle-404 for
                      handling 404 error responses."
  [^Keyword method ^String url-path ^IPersistentMap req-map & {:as error-handlers}]
  (let [url (url/url (cfg/data-info-base) url-path)]
    (try+
      ((resolve-http-call method) (str url) req-map)
      (catch #(not (nil? (:status %))) err
        (handle-error method url err error-handlers)))))
