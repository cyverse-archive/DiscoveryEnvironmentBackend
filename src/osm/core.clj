(ns osm.core
  (:gen-class)
  (:use [compojure.core])
  (:require [osm.mongo :as mongo]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure-commons.json :as cc-json]
            [clojure-commons.props :as cc-props]
            [clojure-commons.clavin-client :as cl]
            [ring.adapter.jetty :as jetty]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log])
  (:use [ring.middleware keyword-params nested-params]
        [slingshot.slingshot :only [throw+ try+]]))

(defn resp
  [status msg]
  {:status status
   :body msg})

(def props (atom nil))

(defn max-retries [] (Integer/parseInt (get @props "osm.app.max-retries")))
(defn retry-delay [] (Integer/parseInt (get @props "osm.app.retry-delay")))
(defn listen-port [] (Integer/parseInt (get @props "osm.app.listen-port")))

(defn format-exception
  "Formats a raised exception as a JSON object. Returns a response map."
  [exception]
  (log/debug "format-exception")
  (let [string-writer (java.io.StringWriter.)
        print-writer  (java.io.PrintWriter. string-writer)]
    (. exception printStackTrace print-writer)
    (let [localized-message (. exception getLocalizedMessage)
          stack-trace       (. string-writer toString)]
      (log/warn (str localized-message stack-trace))
      {:status 500
       :body (cheshire/encode {:message     (. exception getLocalizedMessage)
                               :stack-trace (. string-writer toString)})})))

(defn- do-apply
  [func & args]
  (let [retval {:succeeded true :retval nil :exception nil}]
    (try
      (assoc retval :succeeded true :retval (apply func args))
      (catch java.io.IOException c
        (assoc retval :succeeded false :exception c))
      (catch java.lang.Exception e
        (assoc retval :succeeded false :exception e)))))

(defn reconn
  "Uses apply to call func with args. The call is wrapped with
   logic that will attempt to reconnect with MongoDB if an
   exception is raised."
  [func & args]
  (log/debug "reconn")
  (loop [num-retries 0]
    (let [retval (apply do-apply (concat [func] args))]
      (if (and (not (:succeeded retval)) (< num-retries (max-retries)))
        (do (Thread/sleep (retry-delay))
          (log/warn (str "Number of retries " num-retries))
          (recur (+ num-retries 1)))
        (if (:succeeded retval)
          (:retval retval)
          (throw (:exception retval)))))))

(defn controller-delete-callbacks
  [collection uuid body]
  (try+
    (resp 200 (cheshire/encode (mongo/remove-callbacks collection uuid body)))
    (catch [:status :osm.mongo/error] {:keys [return]}
      (log/warn (str "remove-callbacks failed: " return))
      (resp 500 return))))

(defn controller-add-callbacks
  [collection uuid body]
  (try+
    (resp 200 (cheshire/encode (mongo/add-callbacks collection uuid body)))
    (catch [:status :osm.mongo/error] {:keys [return]}
      (log/warn (str "add-callbacks failed: " return))
      (resp 500 return))))

(defn controller-get-callbacks
  [collection query]
  (try+
    (resp 200 (cheshire/encode {:callbacks (:callbacks (first (mongo/query collection query)))}))
    (catch [:status :osm.mongo/error] {:keys [return]}
      (log/warn (str "get-callbacks failed: " return))
      (resp 500 return))))

(defn controller-get-object
  [collection uuid]
  (try+
    (resp 200 (cheshire/encode (mongo/get-object collection uuid)))
    (catch [:status :osm.mongo/error] {:keys [return]}
      (log/warn (str "get-object failed: " return))
      (resp 500 return))))

(defn controller-post-object
  [collection uuid new-obj]
  (try+
    (resp 200 (cheshire/encode (mongo/update collection uuid new-obj)))
    (catch [:status :osm.mongo/error] {:keys [return]}
      (log/warn (str "update failed: " return))
      (resp 500 return))))

(defn controller-insert-object
  [collection new-obj]
  (try+
    (resp 200 (mongo/insert collection new-obj))
    (catch [:status :osm.mongo/error] {:keys [return]}
      (log/warn (str "insert failed: " return))
      (resp 500 return))))

(defn controller-query
  [collection query]
  (try+
   (resp 200 (cheshire/encode {:objects (mongo/query collection query)}))
   (catch [:status :osm.mongo/error] {:keys [return]}
     (log/warn (str "query failed: " return))
     (resp 500 return))))

(defn controller-count
  [collection query]
  (try+
   (resp 200 (cheshire/encode {:count (mongo/count-documents collection query)}))
   (catch [:status :osm.mongo/error] {:keys [return]}
     (log/warn (str "count failed: " return))
     (resp 500 return))))

(defroutes osm-routes
  (GET "/" [] "Welcome to the OSM.")

  (POST "/:collection/:uuid/callbacks/delete"
        [collection uuid :as {body :body}]
        (let [query  {:object_persistence_uuid uuid}
              filter {:_id 0 :callbacks 1}
              body   (cc-json/body->json body false)]
          (try
            (reconn controller-delete-callbacks collection uuid body)
            (catch java.lang.Exception e
              (format-exception e)))))

  (POST "/:collection/:uuid/callbacks"
        [collection uuid :as {body :body}]
        (try
          (reconn controller-add-callbacks collection uuid (cc-json/body->json body false))
          (catch java.lang.Exception e
            (format-exception e))))

  (GET "/:collection/:uuid/callbacks"
       [collection uuid :as {body :body}]
       (let [query {:object_persistence_uuid uuid}]
         (try
           (reconn controller-get-callbacks collection query)
           (catch java.lang.Exception e
             (format-exception e)))))

  (POST "/:collection/query"
        [collection :as {body :body}]
        (try
          (let [query (cc-json/body->json body false)]
            (reconn controller-query collection query))
          (catch java.lang.Exception e
            (format-exception e))))

  (GET "/:collection/query"
       [collection]
       {:status 404 :body "Not Found"})

  (POST "/:collection/count"
        [collection :as {body :body}]
        (try
          (let [query (cc-json/body->json body false)]
            (reconn controller-count collection query))
          (catch java.lang.Exception e
            (format-exception e))))

  (GET "/:collection/count"
       [collection]
       {:status 404 :body "Not Found"})

  (GET "/:collection/:uuid"
       [collection uuid]
       (try
         (log/debug "GET OBJECT: controller-get-object")
         (reconn controller-get-object collection uuid)
         (catch java.lang.Exception e
           (format-exception e))))

  (POST "/:collection/:uuid"
        [collection uuid :as {body :body}]
        (try
          (let [new-obj (cc-json/body->json body true)]
            (reconn controller-post-object collection uuid new-obj))
          (catch java.lang.Exception e
            (format-exception e))))

  (POST "/:collection"
        [collection :as {body :body}]
        (try
          (let [new-obj (cc-json/body->json body false)]
            (reconn controller-insert-object collection new-obj))
          (catch java.lang.Exception e
            (format-exception e)))))

(defn site-handler [routes]
  (-> routes
    wrap-keyword-params
    wrap-nested-params))

(defn -main
  [& args]
  (def zkprops (cc-props/parse-properties "zkhosts.properties"))
  (def zkurl (get zkprops "zookeeper"))

  (log/warn "ZKURL: " zkurl)

  (cl/with-zk
    zkurl
    (when (not (cl/can-run?))
      (log/warn "THIS APPLICATION CANNOT RUN ON THIS MACHINE. SO SAYETH ZOOKEEPER.")
      (log/warn "THIS APPLICATION WILL NOT EXECUTE CORRECTLY.")
      (System/exit 1))

    (reset! props (cl/properties "osm")))

  (log/warn @props)
  (mongo/set-mongo-props @props)
  (log/warn (str "Listening on " (listen-port)))
  (jetty/run-jetty (site-handler osm-routes) {:port (listen-port)}))
