(ns clj-jargon.init
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure-commons.error-codes :refer [ERR_NOT_A_USER]])
  (:import [java.io InputStream]
           [java.net ConnectException]
           [org.irods.jargon.core.connection IRODSAccount]
           [org.irods.jargon.core.exception InvalidClientUserException]
           [org.irods.jargon.core.pub IRODSAccessObjectFactory  ; Hint for Cursive inspection
                                      IRODSFileSystem]))


; Debuging code.
(def with-jargon-index (ref 0))
(def ^:dynamic curr-with-jargon-index nil)

(defn clean-return
  [{^IRODSFileSystem cm-proxy :proxy} retval]
  (log/debug curr-with-jargon-index "- cleaning up and returning a plain value")
  (.close cm-proxy)
  retval)

(defn dirty-return
  [retval]
  (log/debug curr-with-jargon-index "- returning without cleaning up...")
  retval)

(defn proxy-input-stream
  [{^IRODSFileSystem cm-proxy :proxy} ^InputStream istream]
  (let [with-jargon-index curr-with-jargon-index]
    (proxy [InputStream] []
      (available [] (.available istream))
      (mark [readlimit] (.mark istream readlimit))
      (markSupported [] (.markSupported istream))
      (read
        ([] (.read istream))
        ([b] (.read istream b))
        ([b off len] (.read istream b off len)))
      (reset [] (.reset istream))
      (skip [n] (.skip istream n))
      (close []
        (log/debug with-jargon-index "- closing the proxy input stream")
        (.close istream)
        (.close cm-proxy)))))

(defn proxy-input-stream-return
  [cm retval]
  (log/debug curr-with-jargon-index "- returning a proxy input stream...")
  (proxy-input-stream cm retval))


(defn- account
  [cfg client-user]
  (IRODSAccount/instanceWithProxy (:host cfg)
                                  (Integer/parseInt (:port cfg))
                                  client-user
                                  (:password cfg)
                                  (:home cfg)
                                  (:zone cfg)
                                  (:defaultResource cfg)
                                  (:username cfg)
                                  (:zone cfg)))


(defn- context-map
  "Throws:
     org.irods.jargon.core.exception.JargonException - This is thrown when if fails to connect to iRODS
     ERR_NOT_A_USER                                  - If the client-user isn't a known iRODS user"
  [{^IRODSFileSystem cm-proxy :proxy :as cfg} client-user]
  (try+
    (let [acnt (account cfg client-user)
          aof  (.getIRODSAccessObjectFactory cm-proxy)]
      (assoc cfg
        :irodsAccount        acnt
        :accessObjectFactory aof
        :collectionAO        (.getCollectionAO aof acnt)
        :dataObjectAO        (.getDataObjectAO aof acnt)
        :userAO              (.getUserAO aof acnt)
        :userGroupAO         (.getUserGroupAO aof acnt)
        :fileFactory         (.getIRODSFileFactory cm-proxy acnt)
        :fileSystemAO        (.getIRODSFileSystemAO aof acnt)
        :lister              (.getCollectionAndDataObjectListAndSearchAO aof acnt)
        :quotaAO             (.getQuotaAO aof acnt)
        :executor            (.getIRODSGenQueryExecutor aof acnt)))
    (catch InvalidClientUserException _
      (throw+ {:error_code ERR_NOT_A_USER :user client-user}))))


(defn- log-value
  [msg value]
  (log/debug curr-with-jargon-index "-" msg value)
  value)


(defn- get-context
  "Throws:
     org.irods.jargon.core.exception.JargonException - This is thrown when if fails to connect to iRODS"
  [cfg client-user]
  (let [retval {:succeeded true :retval nil :exception nil :retry false}]
    (try
      (log-value "retval:" (assoc retval :retval (context-map cfg client-user)))
      (catch ConnectException e
        (log/debug curr-with-jargon-index "- caught a ConnectException:" e)
        (log/debug curr-with-jargon-index "- need to retry...")
        (assoc retval :exception e :succeeded false :retry true))
      (catch Exception e
        (log/debug curr-with-jargon-index "- got an Exception:" e)
        (log/debug curr-with-jargon-index "- shouldn't retry...")
        (assoc retval :exception e :succeeded false :retry false)))))


(defn create-jargon-context-map
  "Creates a map containing instances of commonly used Jargon objects.

   Throws:
     org.irods.jargon.core.exception.JargonException - This is thrown when if fails to connect to iRODS"
  [cfg client-user]
  (loop [num-tries 0]
    (let [retval (get-context cfg client-user)
          error? (not (:succeeded retval))
          retry? (:retry retval)]
      (cond
        (and error? retry? (< num-tries (:max-retries cfg)))
        (do (Thread/sleep (:retry-sleep cfg))
            (recur (inc num-tries)))

        error?
        (throw (:exception retval))

        :else (:retval retval)))))


(defmacro with-jargon
  "An iRODS connection is opened, binding the connection's context to the symbolic cm-sym value.
   Next it evaluates the body expressions. Finally, it closes the iRODS connection*. The body
   expressions should use the value of cm-sym to access the iRODS context.

   Calling:
   (with-jargon cfg [cm-sym] body)
   (with-jargon cfg :opt-k opt-v ... [cm-sym] body)

   Parameters:
     cfg      - The Jargon configuration used to connect to iRODS.
     [cm-sym] - Holds the name of the binding to the iRODS context map used by the body expressions.
     body     - Zero or more expressions to be evaluated while an iRODS connection is open.

   Options:
     :auto-close  - true if the connection should be closed automatically (default: true)
     :client-user - the user to operate as inside of iRODS (default: (:username cfg))

   Returns:
     It returns the result from evaluating the last expression in the body.*

    Throws:
      org.irods.jargon.core.exception.JargonException - This is thrown when if fails to connect to
                                                        iRODS.

    Example:
      (def config (init ...))

      (with-jargon config [ctx]
        (list-all ctx \"/zone/home/user/\"))

   * If an IRODSFileInputStream is the result of the last body expression, the iRODS connection is
     not closed. Instead, a special InputStream is returned that when closed, closes the iRODS
     connection as well. If the auto-close option is set to false (it's set to true by default)
     then the connection is not closed automatically. In that case, the caller must take steps to
     ensure that the connection will be closed (for example, by including a proxy input stream
     somewhere in the result and calling the close method on that proxy input stream later)."
  [cfg & params]
  (let [[opts-map [[cm-sym] & body]] (split-with #(not (vector? %)) params)
         opts                        (apply hash-map opts-map)]
    `(let [auto-close#  (if (nil? (:auto-close ~opts)) true (:auto-close ~opts))
           client-user# (if (:client-user ~opts) (:client-user ~opts) (:username ~cfg))]
       (binding [curr-with-jargon-index (dosync (alter with-jargon-index inc))]
         (log/debug "curr-with-jargon-index:" curr-with-jargon-index)
         (when-let [~cm-sym (create-jargon-context-map ~cfg client-user#)]
           (try+
             (let [retval# (do ~@body)]
               (cond
                 (instance? InputStream retval#) (proxy-input-stream-return ~cm-sym retval#)
                 auto-close#                     (clean-return ~cm-sym retval#)
                 :else                           (dirty-return retval#)))
             (catch Object o1#
               (try+
                 (let [^IRODSFileSystem proxy-cm# (:proxy ~cm-sym)] (.close proxy-cm#))
                 (catch Object o2#))
               (throw+))))))))


(defmacro log-stack-trace
  [msg]
  `(log/warn (Exception. "forcing a stack trace") ~msg))

(def ^IRODSFileSystem default-proxy-ctor
  "This is the default constructor for creating an iRODS proxy."
  #(IRODSFileSystem/instance))

(defn init
  "Creates the iRODS configuration map.

    Parameters:
      host - The IP address or FQDN of the iRODS server that will be used.
      port - The IP port the iRODS server listens to.
      username - The iRODS user name of the account that will be used while
        connected to iRODS.
      password - The password of user.
      home - The path to the user's home collection.
      zone - The zone to use
      defaultResource - The default resource to use.
      max-retries - The number of times to retry connecting to the server.  This
        defaults to 0.
      retry-sleep - The number of milliseconds to wait between connection
        retries.  This defaults to 0.
      use-trash - Indicates whether or to put deleted entries in the trash.
        This defaults to false.
      proxy-ctor - This is the constructor to use for creating the iRODS proxy.
        It takes no arguments, and the object its creates must implement have
        the following methods.
        ((close [_])
         (^IRODSAccessObjectFactory getIRODSAccessObjectFactory [_])
         (^IRODSFileFactory getIRODSFileFactory [_ ^IRODSAccount acnt]))
        These must be sematically equivalent to the corresponding methods in
        org.irods.jargon.core.pub.IRODSFileSystem.  This argument defaults to
        default-proxy-ctor.

    Returns:
      A map is returned with the provided parameters names and values forming
      the key-value pairs."
  [host port user pass home zone res
   & {:keys [max-retries retry-sleep use-trash proxy-ctor]
      :or   {max-retries 0
             retry-sleep 0
             use-trash   false
             proxy-ctor  default-proxy-ctor}}]
    {:host            host
     :port            port
     :username        user
     :password        pass
     :home            home
     :zone            zone
     :defaultResource res
     :max-retries     max-retries
     :retry-sleep     retry-sleep
     :use-trash       use-trash
     :proxy           (proxy-ctor)})

(defn override-user-account
  [cfg user pass]
  (IRODSAccount. (:host cfg)
    (Integer/parseInt (:port cfg))
    user
    pass
    (:home cfg)
    (:zone cfg)
    (:defaultResource cfg)))
