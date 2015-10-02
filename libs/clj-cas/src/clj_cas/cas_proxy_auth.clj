(ns clj-cas.cas-proxy-auth
  (:use [clojure.string :only (blank? split)])
  (:require [cemerick.url :as curl]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [org.jasig.cas.client.proxy ProxyGrantingTicketStorage ProxyGrantingTicketStorageImpl]
           [org.jasig.cas.client.validation Cas20ProxyTicketValidator TicketValidationException Assertion]
           [org.jasig.cas.client.authentication AttributePrincipal]))

(defn- ^ProxyGrantingTicketStorage build-pgt-storage
  "Builds the object used to store proxy granting tickets."
  [callback-url]
  (when callback-url
    (ProxyGrantingTicketStorageImpl.)))

(defn- ^Cas20ProxyTicketValidator build-validator
  "Builds the service ticket validator."
  [cas-server callback-url pgt-storage]
  (if (and callback-url pgt-storage)
    (doto (Cas20ProxyTicketValidator. cas-server)
      (.setAcceptAnyProxy true)
      (.setProxyCallbackUrl callback-url)
      (.setProxyGrantingTicketStorage pgt-storage))
    (doto (Cas20ProxyTicketValidator. cas-server)
      (.setAcceptAnyProxy true))))

(defn- ^Assertion get-assertion
  "Gets a security assertion from the CAS server."
  [proxy-ticket ^Cas20ProxyTicketValidator validator server-name]
  (when-not (blank? proxy-ticket)
    (try (.validate validator proxy-ticket server-name)
         (catch TicketValidationException e
           (do (log/error e "proxy ticket validation failed") nil)))))

(defn- build-attr-map
  "Builds a map containing the user's attributes"
  [^AttributePrincipal principal]
  (assoc
    (into {} (.getAttributes principal))
    "uid"       (.getName principal)
    "principal" principal))

(defn- assoc-attrs
  "Associates user attributes from an assertion principal with a request."
  [request principal]
  (let [m (build-attr-map principal)]
    (log/debug "User Attributes:" m)
    (assoc request :user-attributes m)))

(defn- string->vector
  "Converts a string representation of a Java string array to a vector."
  [string]
  (let [groups (re-find #"\A\[([^\]]*)\]\z" string)]
    (if (or (nil? groups) (blank? (second groups)))
      []
      (split (second groups) #",\s*"))))

(defn validate-group-membership
  "Verifies that a the user belongs to at least one group that is permitted to
   access a resource."
  [handler allowed-groups-fn]
  (fn [request]
    (let [allowed-groups (allowed-groups-fn)
          actual-groups (:user-groups request)]
      (log/debug "allowed groups:" allowed-groups)
      (log/debug "actual groups:" actual-groups)
      (if (some #(contains? (set allowed-groups) %) actual-groups)
        (handler request)
        {:status 401}))))

(defn extract-groups-from-user-attributes
  "Extracts group membership information from the user's attributes.  The
   group membership information is assumed to be in a format that resembles
   the list representation of an array of strings in Java."
  [handler attr-name-fn]
  (fn [request]
    (let [attr-value (get-in request [:user-attributes (attr-name-fn)])]
      (log/debug "group membership attribute value: " attr-value)
      (handler (assoc request :user-groups (string->vector attr-value))))))

(def ^:private proxy-success-response
  (str "<?xml version=\"1.0\"?>"
       "<casClient:proxySuccess xmlns:casClient=\"http://www.yale.edu/tp/casClient\" />"))

(defn- store-pgt
  [^ProxyGrantingTicketStorage pgt-storage pgt-iou pgt-id]
  (.save pgt-storage pgt-iou pgt-id)
  {:status       200
   :content-type "application/xml"
   :body         proxy-success-response})

(defn- handle-proxy-callback
  [pgt-storage request]
  (let [query-params (:query-params request)
        pgt-iou      (query-params "pgtIou")
        pgt-id       (query-params "pgtId")]
    (if (and pgt-iou pgt-id)
      (store-pgt pgt-storage pgt-iou pgt-id)
      {:status 200})))

(defn- handle-authentication
  "Handles the authentication for a request."
  [handler ticket-fn validator server-name request]
  (let [ticket (ticket-fn request)]
    (log/debug (str "validating proxy ticket: " ticket))
    (let [assertion (get-assertion ticket validator server-name)]
      (if (nil? assertion)
        {:status 401}
        (handler (assoc-attrs request (.getPrincipal assertion)))))))

(defn- build-url
  [& components]
  (when-not (some nil? components)
    (str (apply curl/url (map #(string/replace % #"^/|/$" "") components)))))

(defn validate-cas-proxy-ticket
  "Authenticates a CAS proxy ticket. If the proxy ticket can be validated then the request
   is passed to the handler. Otherwise, the handler responds with HTTP status code 401."
  [handler ticket-fn cas-server-fn server-name-fn
   & [proxy-callback-base-fn proxy-callback-path-fn]]
  (let [ticket-fn        (or ticket-fn (constantly nil))
        callback-base-fn (or proxy-callback-base-fn (constantly nil))
        callback-path-fn (or proxy-callback-path-fn (constantly nil))
        callback-url-fn  #(build-url (callback-base-fn) (callback-path-fn))
        pgt-storage      (delay (build-pgt-storage (callback-url-fn)))
        validator        (delay (build-validator (cas-server-fn) (callback-url-fn) @pgt-storage))]
    (fn [request]
      (if (= (callback-path-fn) (:uri request))
        (handle-proxy-callback @pgt-storage request)
        (handle-authentication handler ticket-fn @validator (server-name-fn) request)))))

(defn get-proxy-ticket
  "Obtains a proxy ticket that can be used to authenticate to other CAS-secured services."
  [^AttributePrincipal principal ^String url]
  (log/warn "obtaining a proxy ticket for " principal " for service " url)
  (when (and principal url)
    (.getProxyTicketFor principal url)))
