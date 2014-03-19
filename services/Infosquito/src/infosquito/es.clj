(ns infosquito.es
  (:use infosquito.es-if)
  (:require [clojurewerkz.elastisch.rest :as cer]
            [clojurewerkz.elastisch.rest.bulk :as bulk]
            [clojurewerkz.elastisch.rest.document :as cerd]
            [clojurewerkz.elastisch.rest.index :as ceri]
            [cheshire.custom :as json]
            [clj-http.client :as http]
			[slingshot.slingshot :as ss])
  (:import [java.net ConnectException]))


;; FUNCTIONS THAT SHOULD BE IN clojurewerkz.elastisch.rest


(defn- cer_post-text
  [^String uri &{:keys [body] :as options}]
  (io! (json/decode (:body (http/post uri (merge options {:accept :json}))) 
                    true)))
        

(defn- cer_scroll-url
  []
  (cer/url-with-path "_search" "scroll"))


; This is how clojurewerkz.elastisch.rest.bulk/bulk-index should have been implemented
(defn- bulk-index
  "generates the content for a bulk insert operation, but allows an _id to be provided"
  ([documents]
    (let [operations (map bulk/index-operation documents)
          documents  (map #(dissoc % :_index :_type :_id) documents)]
      (interleave operations documents))))

;;


(defrecord ^{:private true} Indexer []
  Indexes
  
  (delete [_ index type id]
    (cerd/delete index type id))
  
  (exists? [_ index]
    (ceri/exists? index))
  
  (put [_ index type id doc-map]
    (cerd/put index type id doc-map))

  (put-bulk [_ index type docs]
    (bulk/bulk-with-index-and-type index type (bulk-index docs)))

  (scroll [_ scroll-id keep-alive-time]
    (cer_post-text (cer_scroll-url)
                   :query-params {:scroll keep-alive-time} 
                   :body         scroll-id))

  (search-all-types [_ index params]
    (apply cerd/search-all-types index (flatten (vec params)))))
  

(defn mk-indexer
  "Throws:
    :connection-refused - This is thrown if a connection cannot be established
      to Elastic Search"
  [es-url]
  (ss/try+ 
    (cer/connect! es-url)
    (->Indexer)
    (catch ConnectException e
      (ss/throw+ {:type :connection-refused 
                  :msg (str "Cannot connect to Elastic Search. " 
                            (if-let [m (.getMessage e)] m ""))}))))

