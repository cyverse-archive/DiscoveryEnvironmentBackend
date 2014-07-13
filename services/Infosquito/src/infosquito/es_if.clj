(ns infosquito.es-if)


(defprotocol Indexes
  
  (delete [_ index type id]
    "Maps to clojurewerkz.elastisch.rest.document/delete")
  
  (exists? [_ index]
    "Maps to clojurewerkz.elastisch.rest.index/exists?")
    
  (put [_ index type id doc-map]
    "Maps to clojurewerkz.elastisch.rest.document/put")

  (put-bulk [_ index type docs]
    "Bulk indexes a bunch of documents")
  
  (search-all-types [_ index params]
    "Maps to clojurewerkz.elastisch.rest.document/search-all-types"))
