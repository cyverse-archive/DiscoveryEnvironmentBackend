(ns notification-agent.app-db
  (:use [korma.db]
        [korma.core]
        [notification-agent.config]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the app database."
  []
  {:classname   (app-db-driver-class)
   :subprotocol (app-db-subprotocol)
   :subname     (str "//" (app-db-host) ":" (app-db-port) "/" (app-db-name))
   :user        (app-db-user)
   :password    (app-db-password)})

(defn define-database
  "Defines the database connection to use for the app database."
  []
  (let [spec (create-db-spec)]
    (defonce app-db (create-db spec))))

(defn get-job-info
  "Gets information about a specific job from the app database."
  [id]
  (with-db app-db
    (first
     (select :jobs
             (fields [:job_name        :name
                      :job_description :description])
             (where {:external_id id})))))
