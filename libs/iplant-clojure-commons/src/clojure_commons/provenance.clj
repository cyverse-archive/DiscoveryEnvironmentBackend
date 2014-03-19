(ns clojure-commons.provenance
  (:use cemerick.url)
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]))

(defn prov-map
  "Creates a map that can be passed to the provenance related functions.
   The proxy-user and data keys area optional and will be set to nil if
   they're not provided."
  [url id user service event category & [proxy-user data]]
  (hash-map
   :prov-url url
   :id id
   :user user
   :service service
   :event event
   :category category
   :proxy-user proxy-user
   :data data))

(defn dissoc-nils
  "Removes entries with a nil value from a map."
  [dmap]
  (into {} (filter (comp not nil? second) (seq dmap))))

(defn clean-prov-map
  "Removes the :prov-url value from the map, along with any keys with nil
   values."
  [prov-map]
  (-> prov-map
      (dissoc :prov-url)
      dissoc-nils))

(defn call-prov-log
  [prov-url lp-map]
  (client/put
   prov-url
   {:throw-exceptions false
    :content-type :json
    :body (cheshire/encode lp-map)}))

(defn call-add-object
  [prov-url ao-map & [parent-id]]
  (client/put
   (if parent-id
     (str (url prov-url :query {:parent parent-id}))
     prov-url)
   {:throw-exceptions false
    :content-type :json
    :body (cheshire/encode ao-map)}))

(defn call-lookup-object
  [prov-url]
  (client/get
   prov-url
   {:throw-exceptions false}))

(defn log-map
  [lmap line-header]
  (log/info (str line-header (cheshire/encode lmap)))
  lmap)

(defn log
  "Takes in a map created by (prov-map) and logs it to the provenance
   service."
  [{:keys [prov-url id user service event category proxy-user data]
    :as log-map}]
  (log/warn prov-url)
  (let [lp-map (clean-prov-map log-map)
        log-url (str (url prov-url "0.1" "log"))]
    (log/warn (str "Provenance URL: " log-url))
    (log/warn (str "Logging Provenance: " (cheshire/encode lp-map)))
    (log/warn (str "Logging Provenance Response: "
                   (call-prov-log log-url lp-map)))))

(defn register
  "Takes in an identifier string, an object name, and a optionally a string
   description. Returns a UUID."
  [prov-url obj-id name desc & [parent-uuid]]
  (let [robj    {:id obj-id :name name :desc desc}
        add-url (str (url prov-url "0.1" "object"))]
    (log/info (str "Register Provenance Object: " (cheshire/encode robj)))

    (-> (call-add-object add-url robj parent-uuid)
        :body
        (#(cheshire/decode % true))
        (log-map "Register Provenance Object Response: "))))

(defn lookup
  "Takes in an identifier and looks up the UUID."
  [prov-url obj-id]
  (let [lookup-url (str (url prov-url "0.1" "object"  (url-encode obj-id)))
        resp       (:body (call-lookup-object lookup-url))]
    (println resp)

    (if (contains? resp :UUID)
      (:UUID resp)
      nil)))

(defn exists?
  "Checks to see if an object is already registered or not."
  [prov-url obj-id]
  (let [exists-url (str (url prov-url "0.1" "object" (url-encode obj-id)))
        lookup-status (:status (call-lookup-object exists-url))]
    (cond
     (= lookup-status 404)      false
     (<= 200 lookup-status 299) true
     :else                      false)))
