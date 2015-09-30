(ns clj-jargon.tickets
  (:use [clj-jargon.validations]
        [clj-jargon.init :only [override-user-account proxy-input-stream]]
        [clj-jargon.cart :only [temp-password]]
        [clj-jargon.item-info :only [file is-dir?]]
        [clj-jargon.item-ops :only [input-stream]])
  (:require [clojure-commons.file-utils :as ft])
  (:import [org.irods.jargon.core.pub IRODSAccessObjectFactory]
           [org.irods.jargon.ticket.packinstr TicketInp] 
           [org.irods.jargon.ticket.packinstr TicketCreateModeEnum] 
           [org.irods.jargon.ticket TicketServiceFactoryImpl
                                    TicketAdminService
                                    TicketAdminServiceImpl
                                    TicketClientSupport
                                    Ticket]))

(defn ^TicketAdminService ticket-admin-service
  "Creates an instance of TicketAdminService, which provides
   access to utility methods for performing operations on tickets.
   Probably doesn't need to be called directly."
  [cm user]
  (let [tsf (TicketServiceFactoryImpl. (:accessObjectFactory cm))]
    (.instanceTicketAdminService tsf (override-user-account cm user (temp-password cm user)))))

(defn set-ticket-options
  "Sets the optional settings for a ticket, such as the expiration date
   and the uses limit."
  [ticket-id ^TicketAdminService tas
   {:keys [byte-write-limit expiry file-write-limit uses-limit]}]
  (when byte-write-limit
    (.setTicketByteWriteLimit tas ticket-id byte-write-limit))
  (when expiry
    (.setTicketExpiration tas ticket-id expiry))
  (when file-write-limit
    (.setTicketFileWriteLimit tas ticket-id file-write-limit))
  (when uses-limit
    (.setTicketUsesLimit tas ticket-id uses-limit)))

(defn create-ticket
  [cm user fpath ticket-id & {:as ticket-opts}]
  (validate-path-lengths fpath)
  (let [tas        (ticket-admin-service cm user)
        read-mode  TicketCreateModeEnum/READ
        new-ticket (.createTicket tas read-mode (file cm fpath) ticket-id)]
    (set-ticket-options ticket-id tas ticket-opts)
    new-ticket))

(defn modify-ticket
  [cm user ticket-id & {:as ticket-opts}]
  (set-ticket-options ticket-id (ticket-admin-service cm user) ticket-opts))

(defn delete-ticket
  "Deletes the ticket specified by ticket-id."
  [cm user ticket-id]
  (.deleteTicket (ticket-admin-service cm user) ticket-id))

(defn ticket?
  "Checks to see if ticket-id is already being used as a ticket
   identifier."
  [cm user ticket-id]
  (.isTicketInUse (ticket-admin-service cm user) ticket-id))

(defn ^Ticket ticket-by-id
  "Looks up the ticket by the provided ticket-id string and
   returns an instance of Ticket."
  [cm user ticket-id]
  (.getTicketForSpecifiedTicketString
    (ticket-admin-service cm user)
    ticket-id))

(defn ticket-obj->map
  [^Ticket ticket]
  {:ticket-id        (.getTicketString ticket)
   :path             (.getIrodsAbsolutePath ticket)
   :byte-write-limit (str (.getWriteByteLimit ticket))
   :byte-write-count (str (.getWriteByteCount ticket))
   :uses-limit       (str (.getUsesLimit ticket))
   :uses-count       (str (.getUsesCount ticket))
   :file-write-limit (str (.getWriteFileLimit ticket))
   :file-write-count (str (.getWriteFileCount ticket))
   :expiration       (or (.getExpireTime ticket) "")})

(defn ticket-map
  [cm user ticket-id]
  (ticket-obj->map (ticket-by-id cm user ticket-id)))

(defn ticket-ids-for-path
  [cm user path]
  (let [tas (ticket-admin-service cm user)]
    (if (is-dir? cm path)
      (mapv ticket-obj->map (.listAllTicketsForGivenCollection tas path 0))
      (mapv ticket-obj->map (.listAllTicketsForGivenDataObject tas path 0)))))

(defn ticket-expired?
  [^Ticket ticket-obj]
  (if (.getExpireTime ticket-obj)
    (.. (java.util.Date.) (after (.getExpireTime ticket-obj)))
    false))

(defn ticket-used-up?
  [^Ticket ticket-obj]
  (> (.getUsesCount ticket-obj) (.getUsesLimit ticket-obj)))

(defn init-ticket-session
  [{^IRODSAccessObjectFactory ao-factory    :accessObjectFactory
                              irods-account :irodsAccount} ticket-id]
  (.. ao-factory
    getIrodsSession
    (currentConnection irods-account)
    (irodsFunction
      (TicketInp/instanceForSetSessionWithTicket ticket-id))))

(defn ticket-input-stream
  [cm user ticket-id]
  (input-stream cm (.getIrodsAbsolutePath (ticket-by-id cm user ticket-id))))

(defn ticket-proxy-input-stream
  [cm user ticket-id]
  (proxy-input-stream cm (ticket-input-stream cm user ticket-id)))
