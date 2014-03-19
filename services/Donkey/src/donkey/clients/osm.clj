(ns donkey.clients.osm
  (:use [donkey.auth.user-attributes :only [current-user]])
  (:require [clojure.string :as string]
            [clojure-commons.osm :as osm]
            [donkey.util.service :as service]
            [donkey.util.config :as config]))

(def ^:private osm-client
  (memoize (fn [bucket] (osm/create (config/osm-base-url) bucket))))

(defn- osm-jobs-client
  []
  (osm-client (config/osm-jobs-bucket)))

(defn list-jobs
  []
  (map :state
       ((comp :objects service/decode-json)
        (osm/query (osm-jobs-client)
                   {:state.user        (:shortUsername current-user)
                    :state.analysis_id {"$exists" true}}))))

(defn get-jobs
  [ids]
  (map :state
       ((comp :objects service/decode-json)
        (osm/query (osm-jobs-client) {:state.uuid {"$in" ids}}))))
