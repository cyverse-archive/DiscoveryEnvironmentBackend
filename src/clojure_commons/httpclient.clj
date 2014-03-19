(ns clojure-commons.httpclient
  (:import org.apache.http.client.methods.HttpGet
           org.apache.http.impl.client.DefaultHttpClient
           org.apache.http.util.EntityUtils)
  (:require [clojure.string :as clj-str]))

(defn- binary-content? [http-headers]
  (and (contains? http-headers :content-type)
       (not= (first (clj-str/split (http-headers :content-type) #"\/")))))

(defn- http-response->map [http-response]
  (let [headers (reduce #(assoc %1
                           (keyword (clj-str/lower-case (.getName %2)))
                           (.getValue %2))
                        {}
                        (.getAllHeaders http-response))
        entity  (.getEntity http-response)]
    (list (merge headers {:status (.getStatusCode (.getStatusLine http-response))})
          (if (binary-content? headers)
            (EntityUtils/toByteArray entity)
            (EntityUtils/toString    entity)))))

(defn- http-client-get [uri]
  "Fetches a url.  Simple wrapper around HttpGet.
     uri     -> Uri to execute the get on."
  (let [http-client (DefaultHttpClient.)
        http-get    (HttpGet. uri)]
    (http-response->map (.execute http-client http-get))))

(defn http-client [request-type uri]
  "Executes an http request.
     request-type -> Type of request.  Should be :get, :post, ...
     uri          -> URI object or string to execute the request for"
  (case request-type
        :get  (http-client-get uri)
        (throw (RuntimeException. (str "Unknown request-type " request-type)))))
