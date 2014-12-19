(ns notification-agent.db
  (:use [korma.db]
        [korma.core]
        [kameleon.notification-entities]
        [notification-agent.config]
        [notification-agent.common]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure-commons.error-codes :as ce]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [korma.sql.engine :as eng]
            [notification-agent.time :as time])
  (:import [java.sql Timestamp]
           [java.util UUID]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the database."
  []
  {:classname   (db-driver-class)
   :subprotocol (db-subprotocol)
   :subname     (str "//" (db-host) ":" (db-port) "/" (db-name))
   :user        (db-user)
   :password    (db-password)})

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (let [spec (create-db-spec)]
    (defonce notifications-db (create-db spec))
    (default-connection notifications-db)))

(defn- get-user-id
  "Gets the database primary key of the user with the given username.  If the
   user doesn't exist then a new row will be inserted into the database."
  [username]
  (or (:id (first (select users (where {:username username}))))
      (:id (insert users (values {:username username})))))

(defn- parse-date
  "Parses a date that is specified as a string representing the number of
   milliseconds since January 1, 1970."
  [millis & [param-name]]
  (try+
   (Timestamp. (Long/parseLong millis))
   (catch NumberFormatException _
     (if param-name
       (throw+ {:error_code  ce/ERR_BAD_QUERY_PARAMETER
                :param_name  param-name
                :param_value millis})
       (throw+ {:error_code  ce/ERR_BAD_OR_MISSING_FIELD
                :field_name  :timestamp
                :field_value millis})))))

(defn- parse-boolean
  "Parses a boolean field that is specified as a string."
  [value]
  (cond (nil? value)              value
        (instance? Boolean value) (.booleanValue value)
        :else                     (Boolean/parseBoolean value)))

(defn- unwrap-count
  [sql-result & {:keys [count-key] :or {count-key :count}}]
  (count-key (first sql-result)))

(defn- add-created-before-condition
  "Adds a condition specifying that only notifications older than a specified
   date should be returned."
  [query {:keys [created-before]}]
  (if created-before
    (assoc query :date_created [< (parse-date created-before :created-before)])
    query))

(defn- user-id-subselect
  "Builds an subselect query that can be used to get an internal user ID."
  [user]
  (subselect :users
             (fields :id)
             (where {:username user})))

(defn- build-where-clause
  "Builds an SQL where clause for a notification query from a set of query-string parameters.
   We're trying to remain compliant with ANSI SQL if possible, which only allows joins in
   SELECT statements, so a sub-query has to be used in this where clause to match notifications
   for the given username."
  [user {:keys [type subject seen] :as params}]
  (add-created-before-condition
   (into {} (remove (comp nil? val) {:type    (or type (:filter params))
                                     :user_id (user-id-subselect user)
                                     :subject subject
                                     :seen    (parse-boolean seen)
                                     :deleted false}))
   params))

(defn- normalize-notification-sort-field
  "Validates the sort field for a notification query."
  [sort-field]
  (let [sort-field (if (string? sort-field)
                     (keyword (string/lower-case sort-field))
                     (keyword sort-field))
        sort-field (if (= :timestamp sort-field)
                     :date_created
                     sort-field)]
    sort-field))

(defn- validate-sort-order
  "Validates the sort order for a query."
  [sort-dir]
  (when sort-dir
    (let [sort-dir (cond (string? sort-dir)  (keyword (string/upper-case sort-dir))
                         (keyword? sort-dir) (keyword (string/upper-case (name sort-dir)))
                         :else               nil)]
      (when-not (#{:ASC :DESC} sort-dir)
        (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
                 :arg_name   :sort-dir
                 :arg_value  sort-dir}))
      sort-dir)))

(defn- add-order-by-clause
  "Adds an order by clause to a notification query if a specific order is requested."
  [query {:keys [sort-field sort-dir]}]
  (if sort-field
    (order
     query
     (normalize-notification-sort-field sort-field)
     (validate-sort-order sort-dir))
    query))

(defn- validate-non-negative-int
  "Validates a non-negative integer argument."
  [arg-name arg-value]
  (if (number? arg-value)
    (int arg-value)
    (try+
     (Integer/parseInt arg-value)
     (catch NumberFormatException e
       (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
                :arg_name   arg-name
                :arg_value  arg-value
                :details    (.getMessage e)})))))

(defn- add-limit-clause
  "Adds a limit clause to a notifiation query if a limit is specified."
  [query {v :limit}]
  (if (and v (pos? v))
    (limit query (validate-non-negative-int :limit v))
    query))

(defn- add-offset-clause
  "Adds an offset clause to a notification query if an offset is specified."
  [query {v :offset}]
  (if (and v (pos? v))
    (offset query (validate-non-negative-int :order v))
    query))

(defn delete-notifications
  "Marks notifications with selected UUIDs as deleted if they belong to the
   specified user."
  [user uuids]
  (update notifications
          (set-fields {:deleted true})
          (where {:user_id (user-id-subselect user)
                  :uuid    [in (map parse-uuid uuids)]})))

(defn delete-matching-notifications
  "Deletes notifications matching a set of incoming parameters."
  [user params]
  (update notifications
          (set-fields {:deleted true})
          (where (build-where-clause user params))))

(defn mark-notifications-seen
  "Marks notifications with selected UUIDs as seen if they belong to the
   specified user."
  [user uuids]
  (update notifications
          (set-fields {:seen true})
          (where {:user_id (user-id-subselect user)
                  :uuid    [in (map parse-uuid uuids)]})))

(defn mark-matching-notifications-seen
  "Marks notifications matching a set of incoming parameters as seen."
  [user params]
  (update notifications
          (set-fields {:seen true})
          (where (build-where-clause user params))))

(defn count-matching-messages
  "Counts the number of messages matching a set of query-string parameters."
  [user params]
  (unwrap-count (select :notifications
                        (aggregate (count :*) :count)
                        (where (build-where-clause user params)))))

(defn find-matching-messages
  "Finds messages matching a set of query-string parameters."
  [user params]
  (-> (select* notifications)
      (fields :uuid :type [:users.username :username] :subject :seen :deleted :date_created
              :message)
      (with users)
      (where (build-where-clause user params))
      (add-order-by-clause params)
      (add-limit-clause params)
      (add-offset-clause params)
      (select)))

(defn insert-notification
  "Inserts a notification into the database."
  [type username subject created-date message]
  (let [uuid (UUID/randomUUID)]
    (insert notifications
            (values {:uuid         uuid
                     :type         type
                     :user_id      (get-user-id username)
                     :subject      subject
                     :message      message
                     :date_created (parse-date created-date)}))
    (string/upper-case (str uuid))))

(defn- notification-id-subselect
  "Creates a subselect statement to obtain the primary key for the notification
   with the given UUID."
  [uuid]
  (subselect notifications
             (fields :id)
             (where {:uuid (parse-uuid uuid)})))

(defn record-email-request
  "Inserts a record of an e-mail request into the database."
  [uuid template addr payload]
  (insert email_notification_messages
          (values {:notification_id (notification-id-subselect uuid)
                   :template        template
                   :address         addr
                   :payload         payload})))

(defn get-system-notification-type-id
  "Returns a system notification type id by looking it up by name."
  [sys-notif-type]
  (:id (first (select system_notification_types (where {:name sys-notif-type})))))

(defn get-system-notification-type
  "Returns a string containing the name of the system notification type
   associated with the ID 'type-id'."
  [type-id]
  (:name (first (select system_notification_types (where {:id type-id})))))

(defn get-system-notification-types
  "Returns a list of all of the names of the system notification types."
  []
  (map :name (select system_notification_types)))

(defn- xform-timestamp
  "Converts a PostgreSQL timestamp to a more useable timestamp format."
  [ts]
  (-> ts time/pg-timestamp->millis))

(defn system-map
  "Cleans up a map representing a system notification. Removed database specific
   information."
  [db-map]
  (let [notification-type (get-system-notification-type (:system_notification_type_id db-map))]
    (-> db-map
        (assoc :type              notification-type
               :activation_date   (xform-timestamp (:activation_date db-map))
               :deactivation_date (xform-timestamp (:deactivation_date db-map))
               :date_created      (xform-timestamp (:date_created db-map)))
        (dissoc :id :system_notification_type_id))))

(defn- ack-aware-system-map
  "This function converts a system notification record received from the database into the expected
   form for transmission.

   Unlike system-map, this version assumes that the :type of the notification is already in the db
   record. It also assumes that the ;date_acknowledged field is in the record and computes the
   :acknowledged flag from that field.

   Parameters:
     db-sys-note - the system notification record received from the database

   Returns:
     The system notification record prepared for transmission."
  [db-sys-note]
  (-> db-sys-note
      (assoc :activation_date   (xform-timestamp (:activation_date db-sys-note))
             :deactivation_date (xform-timestamp (:deactivation_date db-sys-note))
             :date_created      (xform-timestamp (:date_created db-sys-note))
             :acknowledged      (not= nil (:date_acknowledged db-sys-note)))
      (dissoc :date_acknowledged)))

(defn- system-listing-map
  "Cleans up a map representing a system notification. In this case, the system notification type
   name is already in the map and we don't have any acknowledgment information."
  [db-map]
  (assoc db-map
    :activation_date   (xform-timestamp (:activation_date db-map))
    :deactivation_date (xform-timestamp (:deactivation_date db-map))
    :date_created      (xform-timestamp (:date_created db-map))))

(defn insert-system-notification
  "Inserts a system notification into the database.

   Required Paramters
      type - The system notification type.
      deactivation_date - The date that the system notification is no longer valid.
          String containing the milliseconds since the epoch.
      message - The message that's displayed in the notification.

   Optional Parameters:
      :activation_date -  The date that the system notificaiton becomes valid.
          String containing the milliseconds since the epoch.
      :dismissible? - Boolean that tells whether a user can deactivate the notification.
      :logins_disabled? - Boolean"
  [type deactivation_date message
   & {:keys [activation_date
             dismissible
             logins_disabled]
      :or   {activation_date  (millis-since-epoch)
             dismissible     false
             logins_disabled false}}]
  (system-map
   (insert system_notifications
           (values {:uuid                         (UUID/randomUUID)
                    :system_notification_type_id  (get-system-notification-type-id type)
                    :activation_date              (parse-date activation_date)
                    :deactivation_date            (parse-date deactivation_date)
                    :message                      message
                    :dismissible                  dismissible
                    :logins_disabled              logins_disabled}))))

(defn get-system-notification-by-uuid
  "Selects system notifications that have a uuid of 'uuid'."
  [uuid]
  (-> (select system_notifications (where {:uuid (parse-uuid uuid)})) first system-map))

(defn- for-ack-state-above
  [query inf-state]
  (-> query
      (where (raw (str "(system_notification_acknowledgments.state IS NOT NULL
                       AND system_notification_acknowledgments.state > '" inf-state "')")))))

(defn- for-ack-state-below
  [query sup-state]
  (-> query
      (where (raw (str "(system_notification_acknowledgments.state IS NULL
                       OR system_notification_acknowledgments.state < '" sup-state "')")))))

(defn- for-sys-note
  [query sys-note-uuid]
  (-> query
      (where {:system_notifications.uuid (parse-uuid sys-note-uuid)})))

(defn- for-user
  [query user]
  (-> query (where {:users.username user})))

(defn- aggregate-count
  [query & {:keys [count-key] :or {count-key :count}}]
  (-> query (aggregate (count :*) count-key)))

(defn- sys-note-fields
  [query]
  (fields query
          [:system_notifications.uuid                             :uuid]
          [:system_notifications.date_created                     :date_created]
          [:system_notifications.activation_date                  :activation_date]
          [:system_notifications.deactivation_date                :deactivation_date]
          [:system_notifications.message                          :message]
          [:system_notifications.dismissible                      :dismissible]
          [:system_notifications.logins_disabled                  :logins_disabled]
          [:system_notification_types.name                        :type]
          [:system_notification_acknowledgments.date_acknowledged :date_acknowledged]))

(defn- sys-listing-fields
  [query]
  (fields query
          [:system_notifications.uuid              :uuid]
          [:system_notifications.date_created      :date_created]
          [:system_notifications.activation_date   :activation_date]
          [:system_notifications.deactivation_date :deactivation_date]
          [:system_notifications.message           :message]
          [:system_notifications.dismissible       :dismissible]
          [:system_notifications.logins_disabled   :logins_disabled]
          [:system_notification_types.name         :type]))

(defn- active-sys-note-ack-below-query
  [epoch sup-state user]
  (let [epoch-str (parse-date epoch)]
    (-> (select* system_notifications)
        (with system_notification_types)
        (join [(subselect system_notification_acknowledgments (with users (where {:username user})))
               :system_notification_acknowledgments]
              (= :id :system_notification_acknowledgments.system_notification_id))
        (where {:activation_date [<= epoch-str] :deactivation_date [> epoch-str]})
        (for-ack-state-below sup-state))))

(defn- sys-note-acks-query
  []
  (-> (select* system_notification_acknowledgments)
      (with users)
      (with system_notifications)))

(defn- add-active-condition
  "Adds a condition to a system notification query limiting the results to notifications that are
   currently active."
  [query]
  (let [epoch-str (parse-date (millis-since-epoch))]
    (where query {:activation_date   [<= epoch-str]
                  :deactivation_date [> epoch-str]})))

(defn- add-type-condition
  "Adds a condition to a system notification query limiting the results to notifications of a
   specific type."
  [query type]
  (where query {:system_notification_types.name type}))

(defn- system-notification-listing-query
  "Generates a query that can be used to list system notifications. The listing can be filtered
   based on the active state or system notification type."
  ([active-only type]
     (system-notification-listing-query active-only type nil nil))
  ([active-only type res-limit res-offset]
     (-> (select* system_notifications)
         (with system_notification_types)
         (#(if active-only (add-active-condition %) %))
         (#(if-not (nil? type) (add-type-condition % type) %))
         (#(if res-limit (limit % res-limit) %))
         (#(if res-offset (offset % res-offset) %)))))

;; NOT API
(defn count-results
  "This function takes a given query and executes it, counting the number of results."
  [query]
  (unwrap-count (select (-> query aggregate-count))))

;; NOT API
(def has-result?
  "This function takes a given query and executes it, checking if there were any results."
  (comp pos? count-results))

;; NOT API
(defn count-sys-note-ack-state-below
  "Given a user and an acknowledgment state, this function determines how many active system
   messages having a user acknowledgment state below the given acknowledgment state."
  [ack-state user]
  (count-results (active-sys-note-ack-below-query (millis-since-epoch) ack-state user)))

;; NOT API
(defn get-sys-note-ack-state-below
  "Retrieves the set of system notifications that currently have a user acknowledgment state below
   some provided state."
  [sup-state user]
  (let [now (millis-since-epoch)]
    (mapv ack-aware-system-map
          (select (-> (active-sys-note-ack-below-query now sup-state user) sys-note-fields)))))

(defn list-system-notifications
  "Lists system notifications."
  [active-only type res-limit res-offset]
  (mapv system-listing-map
        (select (system-notification-listing-query active-only type res-limit res-offset)
                (sys-listing-fields)
                (order :date_created))))

(defn count-system-notifications
  "Counts system notifications."
  [active-only type]
  ((comp :count first)
   (select (system-notification-listing-query active-only type)
           (aggregate (count :*) :count))))

(defn get-active-system-notifications
  "This function retrieves the set of active system notifications for a given user that have not
   been dismissed. It returns the transmission form of the system notification records.

   Parameters:
     user - The name of the user of interest

   Returns:
     The system notification records prepared for transmission."
  [user]
  (get-sys-note-ack-state-below "dismissed" user))

(defn get-new-system-notifications
  "Returns the active system notifications for a particular user that the user has not received
   yet.

   Parameters:
     user - The name of the user of interest

   Returns:
     The system notification records prepared for transmission."
  [user]
  (get-sys-note-ack-state-below "received" user))

(defn get-unseen-system-notifications
  "Returns the active system notifications for a particular user that have not been seen yet.

   Parameters:
     user - The name of the user of interest

   Returns:
     The system notification records prepared for transmission."
  [user]
  (get-sys-note-ack-state-below "acknowledged" user))

(defn count-new-system-notifications
  "Returns the number of system notifications that have not be received by a given user.

   Parameters:
     user - the name of the user of interest.

   Returns:
     The number of system notifications that are active by have not been received by a given user."
  [user]
  (count-sys-note-ack-state-below "received" user))

(defn count-unseen-system-notifications
  "Returns the count of the unseen system notifications for a user."
  [user]
  (count-sys-note-ack-state-below "acknowledged" user))

(defn count-active-system-notifications
  "Returns the number of active system notifications for a particular user."
  [user]
  (count-sys-note-ack-state-below "dismissed" user))

(defn- fix-date [a-date] (Timestamp. (-> a-date time/timestamp->millis)))

(defn- system-notification-update-map
  [{:keys [type deactivation_date activation_date dismissible logins_disabled message]}]
  (letfn [(get-val [f v] (when-not (nil? v) (f v)))]
    (->> {:system_notification_type_id (get-val get-system-notification-type-id type)
          :deactivation_date           (get-val fix-date deactivation_date)
          :activation_date             (get-val fix-date activation_date)
          :dismissible                 (get-val identity dismissible)
          :logins_disabled             (get-val identity logins_disabled)
          :message                     (get-val identity message)}
         (remove (fn [[_ v]] (nil? v)))
         (into {}))))

(defn update-system-notification
  "Updates a system notification.

   Required Parameters:
      uuid - The system notification uuid.

   Optional Parameters:
      :type - The system notification type.
      :deactivation_date - The date that the system notification is no longer valid.
          String containing the milliseconds since the epoch.
      :activation_date -  The date that the system notificaiton becomes valid.
          String containing the milliseconds since the epoch.
      :dismissible - Boolean that tells whether a user can deactivate the notification.
      :logins_disabled - Boolean
      :message - The message that's displayed in the notification."
  [uuid update-values]
  (system-map
   (update system_notifications
           (set-fields (system-notification-update-map update-values))
           (where {:uuid (parse-uuid uuid)}))))

(defn- system-notif-id
  [uuid]
  (:id (first (select system_notifications (where {:uuid (parse-uuid uuid)})))))

(defn delete-system-notification
  "Deletes a system notification.

   Required Parameters:
     uuid - The system notification uuid."
  [uuid]
  (delete system_notification_acknowledgments
          (where {:system_notification_id (system-notif-id uuid)})))

;; NOT API
(defn ack-exists?
  "Indicates whether or not an acknowledgment record exists for a given user and system
   notification."
  [user sys-note-uuid]
  (has-result? (-> (sys-note-acks-query)
                   (for-user user)
                   (for-sys-note sys-note-uuid))))

;; NOT API
(defn ack-state-above?
  "Indicates whether or not an anknowledgment state of a certain system notification and user pair
   is above the given acknowledgement state, inf-state."
  [inf-state user sys-note-uuid]
  (has-result? (-> (sys-note-acks-query)
                   (for-user user)
                   (for-sys-note sys-note-uuid)
                   (for-ack-state-above inf-state))))

;; NOT API
(def received? (partial ack-state-above? "unreceived"))
(def seen? (partial ack-state-above? "received"))
(def deleted? (partial ack-state-above? "acknowledged"))

;; NOT API
(defn insert-ack
  "Inserts a new system notification acknowledgment record for a given user and system notification
   and for a given acknowledgement state. For an 'acknowledged' state, use insert-seen-ack instead."
  [ack-state user sys-note-uuid]
  (exec-raw [(str "INSERT INTO system_notification_acknowledgments
                       (user_id, system_notification_id, state)
                     VALUES (?, ?, '" ack-state "')")
             [(get-user-id user) (system-notif-id sys-note-uuid)]]))

;; NOT API
(defn insert-seen-ack
  "Inserts a new system notification acknowledgment record for a given user and system notification.
   The state is set to acknowledged, and the given time used for the date acknowledged."
  [seen-date user sys-note-uuid]
  (exec-raw ["INSERT INTO system_notification_acknowledgments VALUES (?, ?, 'acknowledged', ?)"
             [(get-user-id user)
              (system-notif-id sys-note-uuid)
              (parse-date seen-date)]]))

;; NOT API
(defn update-ack
  "Updates an existing system notification acknowledgment record for a given user and system
   notification, setting the acknowledgement state to the given state. For an 'acknowledged' state,
   use update-seen-ack instead."
  [ack-state user sys-note-uuid]
  (exec-raw [(str "UPDATE system_notification_acknowledgments
                     SET state = '" ack-state "'
                     WHERE user_id = ? AND system_notification_id = ?")
             [(get-user-id user) (system-notif-id sys-note-uuid)]]))

;; NOT API
(defn update-ack-to-seen
  "Updates an existing system notification acknowledgment record for a given user and system
   notification, setting the acknowledgment state to 'acknowledged'. The given time used for the
   date acknowledged."
  [seen-date user sys-note-uuid]
  (exec-raw ["UPDATE system_notification_acknowledgments
               SET state = 'acknowledged', date_acknowledged = ?
               WHERE user_id = ? AND system_notification_id = ?"
             [(parse-date seen-date)
              (get-user-id user)
              (system-notif-id sys-note-uuid)]]))

;; NOT API
(defn upsert-ack
  "Upserts a system notification acknowledgment for a given user and notification. It checks to see
   if an acknowledgment record exists. If the record already exists, it uses the provided update
   function, otherwise it uses the provided insert function."
  [insert update user sys-note-uuid]
  (if (ack-exists? user sys-note-uuid)
    (update user sys-note-uuid)
    (insert user sys-note-uuid)))

;; NOT API
(defn received
  "Sets the acknowledgement record state to 'received' for a given user and system notification."
  [user sys-note-uuid]
  (upsert-ack (partial insert-ack "received")
              (partial update-ack "received")
              user
              sys-note-uuid))
;; NOT API
(defn seen
  "Sets the acknowledgement record state to 'acknowledged' for a given user and system
   notification. It also sets the acknowledgment time to the current time."
  [user sys-note-uuid]
  (let [now (millis-since-epoch)]
    (upsert-ack (partial insert-seen-ack now)
                (partial update-ack-to-seen now)
                user
                sys-note-uuid)))

;; NOT API
(defn delete-msg
  "Sets the acknowledgement record state to 'dismissed' for a given user and system notification."
  [user sys-note-uuid]
  (upsert-ack (partial insert-ack "dismissed")
              (partial update-ack "dismissed")
              user
              sys-note-uuid))

(defn dismissible?
  [uuid]
  (:dismissible (get-system-notification-by-uuid uuid)))

;; NOT API
(defn mark-sys-notes
  "For a given user and a list of system notifications, this function uses the provided exclude?
   function to filter the notifications. The provided mark function is used to modify the system
   notification states in some way."
  [mark exclude? user sys-note-uuids]
  (doseq [uuid (map str sys-note-uuids) :when (not (exclude? user uuid))]
    (mark user uuid)))

;; NOT API
(defn mark-selected-sys-notes
  "For a given user, this function uses the provided select function choose the notifications. The
   provided mark function is used to modify the system notification states in some way."
  [mark select user]
  (doseq [uuid (map (comp str :uuid) (select user))]
    (mark user uuid)))

(defn mark-system-notifications-received
  "Mark the provided set of system notications as received by the given user.

   Parameters:
     user - the name of the user of interest
     sys-note-uuids - the UUIDS of the system notifications to mark"
  [user sys-note-uuids]
  (mark-sys-notes received received? user sys-note-uuids))

(defn mark-all-system-notifications-received
  "Mark all of the system notification as received by the given user.

   Parameters:
     user - the name of the user of interest"
  [user]
  (mark-selected-sys-notes received get-new-system-notifications user))

(defn mark-system-notifications-seen
  [user sys-note-uuids]
  (mark-sys-notes seen seen? user sys-note-uuids))

(defn mark-all-system-notifications-seen
  [user]
  (mark-selected-sys-notes seen get-unseen-system-notifications user))

(defn soft-delete-system-notifications
  [user sys-note-uuids]
  (mark-sys-notes delete-msg
                  #(or (not (dismissible? %2)) (deleted? %1 %2))
                  user
                  sys-note-uuids))

(defn soft-delete-all-system-notifications
  [user]
  (mark-selected-sys-notes delete-msg
                           #(filter :dismissible (get-active-system-notifications %))
                           user))
