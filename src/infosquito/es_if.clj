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
  
  (scroll [_ scroll-id keep-alive-time]
    "This handles scrolling a response scroll

     Parameters:
     scroll-id - The scroll Id from a previous response.
     keep-alive-time - The time delta string indicating how long ES should way 
       before closing the response.
    
     Returns:
       It returns an ES response document formatted as a map with a :_scroll_id
       key indicating the next scroll id to use.")
  
  (search-all-types [_ index params]
    "Maps to clojurewerkz.elastisch.rest.document/search-all-types"))
