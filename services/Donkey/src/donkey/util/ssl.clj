(ns donkey.util.ssl
  [:import [java.net URL]]
  [:import [java.io
            InputStream
            IOException]]
  [:import [javax.net.ssl
            HostnameVerifier
            HttpsURLConnection
            SSLContext
            SSLSession
            SSLSocketFactory
            TrustManager
            X509TrustManager]]
  [:import [java.security
            GeneralSecurityException
            SecureRandom]]
  [:import [java.security.cert
            CertificateException
            X509Certificate]]
  [:import [org.apache.commons.net.ftp
            FTPClient
            FTPReply]])

(def trust-manager
  (proxy [X509TrustManager] []
    (getAcceptedIssuers [] nil)
    (checkClientTrusted [arg0, arg1])
    (checkServerTrusted [arg0, arg1])))

(def hostname-verifier
  (proxy [HostnameVerifier] []
    (verify [hostname, session] true)))

(def ssl-context
  (let [context (SSLContext/getInstance "SSL")]
    (do
      (. context init nil (into-array TrustManager [trust-manager]) (SecureRandom.))
      context)))

(defn- get-connection
  "Opens a connection to 'url' that doesn't care whether a cert is signed or not.
     url - Instance of java.net.URL
   Returns an open java.net.URLConnection"
  [url]
  (let [orig-socket-factory (HttpsURLConnection/getDefaultSSLSocketFactory)
        orig-hostname-verifier (HttpsURLConnection/getDefaultHostnameVerifier)]
    (try
      (do
        (HttpsURLConnection/setDefaultSSLSocketFactory (. ssl-context getSocketFactory))
        (HttpsURLConnection/setDefaultHostnameVerifier hostname-verifier)
        (.openConnection url))
      (catch GeneralSecurityException e
        (throw (IOException. "Unable to establish trusting SSL connection." e)))
      (finally
        (do
          (HttpsURLConnection/setDefaultSSLSocketFactory orig-socket-factory)
          (HttpsURLConnection/setDefaultHostnameVerifier orig-hostname-verifier))))))

(defn- ftp-connect
  [ftp url]
  (if (pos? (.getPort url))
    (.connect ftp (.getHost url) (.getPort url))
    (.connect ftp (.getHost url))))


(defn- urlize
  [url]
  (if (string? url)
    (java.net.URI. url)
    url))

(defn- ftp-login
  ([ftp username password]
     (.login ftp username password))
  ([ftp url]
     (if-let [user-info (.getUserInfo (urlize url))]
       (apply (partial ftp-login ftp) (clojure.string/split user-info #":"))
       (ftp-login ftp "anonymous" ""))))

(defn- get-ftp-input-stream
  ([ftp url]
     (ftp-connect ftp url)
     (.enterLocalPassiveMode ftp)
     (when-not (FTPReply/isPositiveCompletion (.getReplyCode ftp))
       (throw (IOException. "FTP server refused connection")))
     (when-not (ftp-login ftp url)
       (throw (IOException. "FTP server rejected credentials")))
     (.retrieveFileStream ftp (.getPath url)))
  ([url]
     (let [ftp (FTPClient.)
           in  (get-ftp-input-stream ftp url)]
       (proxy [InputStream] []
         (available []
           (.available in))
         (close []
           (.close in)
           (.logout ftp)
           (.disconnect ftp))
         (mark [read-limit]
           (.mark in read-limit))
         (markSupported []
           (.markSupported in))
         (read
           ([]
              (.read in))
           ([b]
              (.read in b))
           ([b off len]
              (.read in b off len)))
         (reset []
           (.reset in))
         (skip [n]
           (.skip in n))))))

(defn input-stream [url-string]
  (let [url (URL. url-string)]
    (if (= (.getProtocol url) "ftp")
      (get-ftp-input-stream url)
      (.getInputStream (get-connection url)))))
