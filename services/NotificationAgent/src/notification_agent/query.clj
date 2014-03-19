(ns notification-agent.query
 (:use [notification-agent.common]
       [notification-agent.messages :only [reformat-message]]
       [clojure.string :only [blank? lower-case upper-case]]
       [slingshot.slingshot :only [throw+]])
 (:require [cheshire.core :as cheshire]
           [clojure.string :as string]
           [clojure.tools.logging :as log]
           [notification-agent.db :as db]))

(defn- reformat
  "Reformats a message corresponding to a notification that was retrieved from
   the database."
  [{:keys [uuid message seen deleted]}]
  (reformat-message (upper-case (str uuid))
                    (cheshire/decode message true)
                    :seen seen
                    :deleted deleted))

(defn- count-messages*
  "Counts the number of matching messages. The user messages are filtered with the provided query."
  [user user-query]
  (let [user-total       (db/count-matching-messages user user-query)
        sys-total        (db/count-active-system-notifications user)
        new-sys-total    (db/count-new-system-notifications user)
        unseen-sys-total (db/count-unseen-system-notifications user)]
    (json-resp 200 (cheshire/encode {:user-total          user-total
                                     :system-total        sys-total
                                     :system-total-new    new-sys-total
                                     :system-total-unseen unseen-sys-total}))))

(defn- get-messages*
  "Retrieves notification messages."
  [user query]
  (let [body {:total    (str (db/count-matching-messages user query))
              :messages (map reformat (db/find-matching-messages user query))
              :system-messages (db/get-active-system-notifications user)}]
    (json-resp 200 (cheshire/encode body))))

(defn- required-string
  "Extracts a required string argument from the query-string map."
  [k m]
  (let [v (m k)]
    (when (blank? v)
      (throw+ {:type  :illegal-argument
               :code  ::missing-or-empty-param
               :param (name k)}))
    v))

(defn- optional-long
  "Extracts an optional long argument from the query-string map, using a default
   value if the argument wasn't provided."
  [k m d]
  (let [v (k m)]
    (if-not (nil? v)
      (string->long v ::invalid-long-integer-param {:param (name k)
                                                    :value v})
      d)))

(defn- optional-boolean
  "Extracts an optional Boolean argument from the query-string map."
  ([k m]
     (optional-boolean k m nil))
  ([k m d]
     (let [v (k m)]
       (if (nil? v) d (Boolean/valueOf v)))))

(defn- as-keyword
  "Converts a string to a lower-case keyword."
  [s]
  (keyword (lower-case s)))

(defn- mangle-filter
  "Converts a filter to lower case with underscores replacing spaces."
  [filt]
  (when-not (nil? filt)
    (string/lower-case (string/replace filt #" " "_"))))

(defn- get-seen-flag
  "Gets the seen flag from the query parameters."
  [query-params]
  (let [seen (optional-boolean :seen query-params)
        filt (mangle-filter (:filter query-params))]
    (if (= filt "new") false seen)))

(defn- get-filter
  "Gets the filter from the query parameters."
  [query-params]
  (let [filt (mangle-filter (:filter query-params))]
    (when-not (or (nil? filt) (= filt "new"))
      filt)))

(defn get-unseen-messages
  "Looks up all messages in the that have not been seen yet for a specified user."
  [query-params]
  (let [user  (required-string :user query-params)]
    (log/debug "retrieving unseen messages for" user)
    (get-messages* user {:limit      0
                         :offset     0
                         :seen       false
                         :sort-field :timestamp
                         :sort-dir   :asc})))

(defn get-paginated-messages
  "Provides a paginated view for notification messages.  This endpoint takes
   several query-string parameters:

       user      - the name of the user to get notifications for
       limit     - the maximum number of messages to return or zero if there is
                   no limit - optional (0)
       offset    - the number of leading messages to skip - optional (0)
       seen      - specify 'true' for only seen messages or 'false' for only
                   unseen messages - optional (defaults to displaying both seen
                   and unseen messages)
       sortField - the field to use when sorting the messages - optional
                   (currently, only 'timestamp' can be used)
       sortDir   - the sort direction, 'asc' or 'desc' - optional (desc)
       filter    - filter by message type ('data', 'analysis', etc.)"
  [query-params]
  (let [user  (required-string :user query-params)
        query {:limit      (optional-long :limit query-params 0)
               :offset     (optional-long :offset query-params 0)
               :seen       (get-seen-flag query-params)
               :sort-field (as-keyword (:sortfield query-params "timestamp"))
               :sort-dir   (as-keyword (:sortdir query-params "desc"))
               :filter     (get-filter query-params)}]
    (get-messages* user query)))

(defn count-messages
  "Provides a way to retrieve the system message counts along with the number of user messages that
   match a set of criteria. This endpoint takes several query-string parameters:

       user   - the name of the user to count messages for
       seen   - specify 'true' for only seen user messages or 'false' for only unseen user messages
                - optional (defaults to counting both seen and unseen user messages)
       filter - filter user messages by message type ('data', 'analysis', etc.)"
  [query-params]
  (let [user       (required-string :user query-params)
        user-query {:seen   (optional-boolean :seen query-params)
                    :filter (mangle-filter (:filter query-params))}]
    (count-messages* user user-query)))

(defn last-ten-messages
  "Obtains the ten most recent notifications for the user in ascending order."
  [query-params]
  (let [user    (required-string :user query-params)
        query   {:limit      10
                 :offset     0
                 :sort-field :timestamp
                 :sort-dir   :desc}
        total   (db/count-matching-messages user query)
        results (->> (db/find-matching-messages user query)
                     (map reformat)
                     (sort-by #(get-in % [:message :timestamp])))]
    (json-resp 200 (cheshire/encode {:total    (str total)
                                     :messages results}))))

(defn- get-sys-msgs-with
  [db-get query-params]
  (let [user    (required-string :user query-params)
        results (db-get user)]
    (json-resp 200 (cheshire/encode {:system-messages results}))))

(defn get-system-messages
  "Obtains the system messages that apply to a user."
  [query-params]
  (get-sys-msgs-with db/get-active-system-notifications query-params))

(defn get-new-system-messages
  "Obtains the active system messages for a given user that have not been marked as retrieved.

   Parameters:
     query-params - The query-params as provided by ring.

   Return:
     It returns the list of new system messages in a map that ring can understand."
  [query-params]
  (get-sys-msgs-with db/get-new-system-notifications query-params))

(defn get-unseen-system-messages
  [query-params]
  (get-sys-msgs-with db/get-unseen-system-notifications query-params))
