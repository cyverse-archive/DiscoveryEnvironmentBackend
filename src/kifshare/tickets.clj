(ns kifshare.tickets
  (:require [clj-jargon.jargon :as jargon]
            [clj-jargon.tickets :as jtickets]
            [clj-jargon.item-info :as jinfo]
            [clj-jargon.paging :as paging]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]
        [kifshare.errors]
        [kifshare.config :only [username]]
        [ring.util.response :only [status]]
        [clojure-commons.error-codes]))

(defn public-ticket?
  [cm user ticket-id]
  (log/debug "entered kifshare.tickets/public-ticket?")

  (let [tas    (jtickets/ticket-admin-service cm user)
        groups (.listAllGroupRestrictionsForSpecifiedTicket tas ticket-id 0)]
    (if (contains? (set groups) "public")
      true
      false)))

(defn check-ticket
  "Makes sure that the ticket actually exists, is not expired,
   and is not used up. Returns nil on success, throws an error
   on failure."
  [cm ticket-id]
  (log/debug "entered kifshare.tickets/check-ticket")

  (if-not (jtickets/ticket? cm (username) ticket-id)
    (throw+ {:error_code ERR_TICKET_NOT_FOUND
             :ticket-id ticket-id})

    (let [ticket-obj (jtickets/ticket-by-id cm (username) ticket-id)]
      (cond
        (jtickets/ticket-expired? ticket-obj)
        (throw+ {:error_code  ERR_TICKET_EXPIRED
                 :ticket-id ticket-id
                 :expired-date (str (.. ticket-obj getExpireTime getTime))})

        (jtickets/ticket-used-up? ticket-obj)
        (throw+ {:error_code ERR_TICKET_USED_UP
                 :ticket-id ticket-id
                 :num-uses (str (.getUsesLimit ticket-obj))})

        (not (public-ticket? cm (username) ticket-id))
        (throw+ {:error_code ERR_TICKET_NOT_PUBLIC
                 :ticket-id ticket-id})))))

(defn ticket-info
  [cm ticket-id]
  (log/debug "entered kifshare.tickets/ticket-info")

  (check-ticket cm ticket-id)

  (let [ticket-obj (jtickets/ticket-by-id cm (username) ticket-id)
        abs-path   (.getIrodsAbsolutePath ticket-obj)
        jfile      (jinfo/file cm abs-path)
        retval     (hash-map
                    :ticket-id ticket-id
                    :abspath   abs-path
                    :filename  (ft/basename abs-path)
                    :filesize  (str (.length jfile))
                    :lastmod   (str (.lastModified jfile))
                    :useslimit (str (.getUsesLimit ticket-obj))
                    :remaining (str (- (.getUsesLimit ticket-obj) (.getUsesCount ticket-obj))))]
    (log/debug "Ticket Info:\n" retval)
    retval))

(defn ticket-abs-path
  [cm ticket-id]
  (log/debug "entered kifshare.tickets/ticket-abs-path")
  (.getIrodsAbsolutePath (jtickets/ticket-by-id cm (username) ticket-id)))

(defn download
  "Calls (check-ticket) and returns a response map containing an
   input-stream to the file associated with the ticket."
  [cm ticket-id]
  (log/debug "entered kifshare.tickets/download")

  (check-ticket cm ticket-id)

  (let [ti (ticket-info cm ticket-id)]
    (log/warn "Dowloading file associated with ticket " ticket-id)
    (assoc-in
     {:status 200 :body (jtickets/ticket-proxy-input-stream cm (username) ticket-id)}
     [:headers "Content-Disposition"] (str "attachment; filename=\"" (:filename ti)  "\""))))

(defn download-byte-range
  "Calls (check-ticket and returns a response map containing a byte range from a file."
  [cm ticket-id start-byte end-byte]
  (log/debug "entered kifshare.tickets/download")

  (check-ticket cm ticket-id)

  (let [ti         (ticket-info cm ticket-id)
        abs-path   (:abspath ti)
        byte-range (paging/read-at-position cm abs-path start-byte (- end-byte start-byte) false)]
    (log/warn "Download file range " start-byte "-" end-byte "for ticket" ticket-id)
    (-> {:status 200
         :body   (java.io.ByteArrayInputStream. byte-range)}
        (assoc-in
         [:headers "Content-Range"]
         (str "Content-Range: bytes " start-byte "-" end-byte "/" (:filesize ti)))
        (assoc-in
         [:headers "Accept-Ranges"]
         (str "Accept-Ranges: bytes")))))
