(ns monkey.actions
  "This namespace contains the top-level business logic for what monkey does.")


;; TODO implement
(defn reindex
  "forces a reindexing of metadata database

   Parameters:
     props - the configuration values"
  [props])


;; TODO implement
(defn listen
  "monitors the AMQP message queue for messages related reindexing tags metadata. When it receives
   one, it will trigger a reindexing action.

   Parameters:
     props - the configuration values"
  [props])
