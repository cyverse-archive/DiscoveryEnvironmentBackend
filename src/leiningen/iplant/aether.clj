(ns leiningen.iplant.aether
  "Provides a way to resolve the immediate dependencies of a project without
   resolving transitive dependencies.  Much of this code was taken from
   cemerick.pomegranate.aether and modified to suit the needs of this plugin.
   Code was only duplicated in cases where it was necessary because functions
   or vars in cemeric.pomegranate.aether were defined as private."
  (:refer-clojure :exclude [type proxy])
  (:require [cemerick.pomegranate.aether :as aether]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [cemerick.pomegranate.aether PomegranateWagonProvider
            TransferListenerProxy]
           [java.util.regex Pattern]
           [org.apache.maven.repository.internal DefaultServiceLocator
            MavenRepositorySystemSession]
           [org.sonatype.aether RepositorySystem]
           [org.sonatype.aether.collection CollectRequest]
           [org.sonatype.aether.connector.file FileRepositoryConnectorFactory]
           [org.sonatype.aether.connector.wagon WagonProvider
            WagonRepositoryConnectorFactory]
           [org.sonatype.aether.graph Dependency Exclusion]
           [org.sonatype.aether.repository Authentication LocalRepository
            MirrorSelector Proxy RemoteRepository RepositoryPolicy]
           [org.sonatype.aether.resolution ArtifactRequest]
           [org.sonatype.aether.spi.connector RepositoryConnectorFactory]
           [org.sonatype.aether.transfer TransferListener]
           [org.sonatype.aether.util.artifact ArtifactProperties
            DefaultArtifact]
           [org.sonatype.aether.util.repository DefaultProxySelector]))

(def ^:private default-local-repo
  (io/file (System/getProperty "user.home") ".m2" "repository"))

(defn- repository-system
  "Generates a repository system object for use with Aether."
  []
  (.getService
   (doto (DefaultServiceLocator.)
     (.addService RepositoryConnectorFactory FileRepositoryConnectorFactory)
     (.addService RepositoryConnectorFactory WagonRepositoryConnectorFactory)
     (.addService WagonProvider PomegranateWagonProvider))
   RepositorySystem))

(defn- regex?
  "Determines if something is a regular expression."
  [v]
  (instance? Pattern v))

(defn- mirror-exception
  "Throws an exception indicating that multiple mirrors are configured to match
   a single repository."
  [name url mirrors]
  (throw
   (IllegalArgumentException.
    (str "Multiple mirrors configured to match repository " {name url} ": "
         (into {} (map #(update-in % [1] select-keys [:name :url]) mirrors))))))

(defn- mirror-selector-fn
  "The default mirror selector function.

   See cemerick.pomegranate.aether/mirror-selector-fn for more information."
  [mirrors {:keys [name url snapshots releases]}]
  (let [str-match       #(some (partial = %) [name url])
        re-match        #(some (partial re-matches %) [name url])
        mirror-matches? (fn [[matcher]]
                          (cond (string? matcher) (str-match matcher)
                                (regex? matcher)  (re-match matcher)))
        mirrors         (filter mirror-matches? mirrors)]
    (cond
     (empty? mirrors)                 nil
     (= 1 (count mirrors))            (-> mirrors first second)
     (some nil? (map second mirrors)) nil
     :else                            (mirror-exception name url mirrors))))

(defn- repo-spec-for
  "Creates a repository specification for a repository."
  [repo]
  {:name      (.getId repo)
   :url       (.getUrl repo)
   :snapshots (-> repo (.getPolicy true) .isEnabled)
   :releases  (-> repo (.getPolicy false) .isEnabled)})

(defn- policy
  "Returns a policy for a policy settings map."
  [policy-settings enabled?]
  (RepositoryPolicy.
   (boolean enabled?)
   (aether/update-policies (:update policy-settings :daily))
   (aether/checksum-policies (:checksum policy-settings :fail))))

(defn- set-policies
  "Sets the policies for a repsitory."
  [repo settings]
  (doto repo
    (.setPolicy true (policy settings (:snapshots settings true)))
    (.setPolicy false (policy settings (:releases settings true)))))

(defn- set-authentication
  "Calls the setAuthentication method on an object."
  [obj {:keys [username password passphrase private-key-file]}]
  (if (or username password private-key-file passphrase)
    (let [auth (Authentication. username password private-key-file passphrase)]
      (.setAuthentication obj auth))
    obj))

(defn- make-proxy-selector
  "Creates a proxy selector."
  [{:keys [type host port non-proxy-hosts] :as proxy}]
  (doto (DefaultProxySelector.)
    (.add (set-authentication (Proxy. type host port nil) proxy)
          non-proxy-hosts)))

(defn- set-proxy
  "Sets a proxy for a repository."
  [repo {:keys [type host port non-proxy-hosts]
         :or   {type "http"}
         :as   proxy}]
  (if (and repo host port)
    (let [prx-sel (make-proxy-selector proxy)
          prx     (.getProxy prx-sel repo)]
      (.setProxy repo prx))
    repo))

(defn- make-repository
  "Creates a repository."
  [[id settings] proxy]
  (let [settings-map (if (string? settings) {:url settings} settings)
        type         (:type settings-map "default")
        url          (str (:url settings-map))]
    (doto (RemoteRepository. id type url)
      (set-policies settings-map)
      (set-proxy proxy)
      (set-authentication settings-map))))

(defn- mirror-selector
  "Returns a MirrorSelector that delegates the matching of mirrors to the
   provided function."
  [mirror-selector-fn proxy]
  (reify MirrorSelector
    (getMirror [_ repo]
      (let [repo-spec    (repo-spec-for repo)
            mirror-spec  (mirror-selector-fn repo-spec)
            name         (:name mirror-spec)
            repo-manager (:repo-manager mirror-spec)
            content-type (:content-type mirror-spec)]
        (when-let [mirror (and mirror-spec
                               (make-repository [name mirror-spec] proxy))]
          (-> (.setMirroredRepositories mirror [repo])
              (.setRepositoryManager (boolean repo-manager))
              (.setContentType (or content-type "default"))))))))

(defn- local-repo-manager
  "Creates a local repository manager."
  [repo-system local-repo]
  (.newLocalRepositoryManager
   repo-system
   (-> (io/file (or local-repo default-local-repo))
       .getAbsolutePath
       LocalRepository.)))

(defn- transfer-msg
  "Prints a transfer message to standard output."
  [method name size repository]
  (print (case method :get "Retrieving" :put "Sending")
         name
         (if (neg? size)
           ""
           (format "(%sk)" (Math/round (double (max 1 (/ size 1024)))))))
  (when (< 60 (+ (count name) (count repository)))
    (println)
    (print "    "))
  (println (case method :get "from" :put "to") repository))

(defn- default-listener-fn
  "The default transfer listener function."
  [{:keys [type method transferred resource error]}]
  (let [{:keys [name size repository transfer-start-time]} resource]
    (case type
      :started   (transfer-msg method name size repository)
      :corrupted (when error (println (.getMessage error)))
      :failed    (when error (println (.getMessage error)))
      nil)))

(defn- transfer-event
  "Converts a transfer event to a map."
  [^org.sonatype.aether.transfer.TransferEvent e]
  (let [r (.getResource e)]
    {:type        (-> e .getType .name string/lower-case keyword)
     :method      (-> e .getRequestType string/lower-case keyword)
     :transferred (.getTransferredBytes e)
     :error       (.getException e)
     :data-buffer (.getDataBuffer e)
     :data-length (.getDataLength e)
     :resource    {:repository          (.getRepositoryUrl r)
                   :name                (.getResourceName r)
                   :file                (.getFile r)
                   :size                (.getContentLength r)
                   :transfer-start-time (.getTransferStartTime r)
                   :trace               (.getTrace r)}}))

(defn- construct-transfer-listener
  "Either returns the TransferListener instance that was passed in, constructs
   a default TransferListener if the keyword, :stdout, is passed in, constructs
   a TransferListener that delegates to a function if a function is passed in
   or constructs a TransferListener that does nothing."
  [transfer-listener]
  (cond
   (instance? TransferListener transfer-listener)
   transfer-listener

   (= transfer-listener :stdout)
   (aether/TransferListenerProxy. (comp default-listener-fn transfer-event))

   (fn? transfer-listener)
   (aether/TransferListenerProxy. (comp transfer-listener transfer-event))

   :else
   (aether/TransferListenerProxy. (fn [_]))))

(defn- repository-session
  "Creates a session for a repository."
  [repo-system local-repo offline? transfer-listener mirror-selector]
  (-> (MavenRepositorySystemSession.)
      (.setLocalRepositoryManager (local-repo-manager repo-system local-repo))
      (.setMirrorSelector mirror-selector)
      (.setOffline (boolean offline?))
      (.setTransferListener (construct-transfer-listener transfer-listener))))

(defn- group
  "Extracts the group portion of a leiningen style group/artifact spec."
  [group-artifact]
  (or (namespace group-artifact) (name group-artifact)))

(defn- coordinate-string
  "Produces a coordinate string in the format expected by Aether for a
   leiningen-style dependency specification."
  [[group-artifact version & {:keys [classifier extension]
                              :or   {extension "jar"}}]]
  (let [group-name    (group group-artifact)
        artifact-name (name group-artifact)]
    (->> [group-name artifact-name extension classifier version]
         (remove nil?)
         (interpose \:)
         (apply str))))

(defn- exclusion
  "Creates an exclusion from an exclusion specification."
  [[group-artifact & {:as opts}]]
  (Exclusion.
   (group group-artifact)
   (name group-artifact)
   (:classifier opts "*")
   (:extension opts "*")))

(defn- normalize-exclusion-spec
  "Normalizes an excusion specification, which may be an individual symbol
   or a vector."
  [spec]
  (if (symbol? spec) [spec] spec))

(defn- dependency
  "Creates a dependency for a coordinate."
  [[group-artifact version & {:keys [scope optional exclusions]
                              :or   {scope    "compile"
                                     optional false}}
    :as dep-spec]]
  (Dependency. (DefaultArtifact. (coordinate-string dep-spec))
               scope
               optional
               (map (comp exclusion normalize-exclusion-spec) exclusions)))

(defn- local-or-remote-dependency
  "Creates a dependency for a coordinate for which a local file may already
   be present."
  [files coordinate]
  (if-let [local-file (get files coordinate)]
    (.setArtifact (dependency coordinate)
                  (-> (dependency coordinate)
                      .getArtifact
                      (.setProperties {ArtifactProperties/LOCAL_PATH
                                       (.getPath (io/file local-file))})))
    (dependency coordinate)))

(defn mirror-or-repo
  "Obtains either a mirror or a repository for  a repository specification."
  [session proxy repo]
  (let [repo (make-repository repo proxy)]
    (-> session
        .getMirrorSelector
        (.getMirror repo)
        (or repo))))

(defn artifact-request
  "Obtains an artifact request for a list of repositories and a dependency."
  [repos dep]
  (ArtifactRequest. (.getArtifact dep) repos "runtime"))

(defn resolve-direct-dependencies
  "Resolves the direct dependencies of a project without attempting to resolve
   transitive dependencies."
  [& {:keys [repositories coordinates files local-repo transfer-listener
             offline? proxy mirrors]}]
  (let [repositories       (or repositories aether/maven-central)
        system             (repository-system)
        mirror-selector-fn (memoize (partial mirror-selector-fn mirrors))
        mirror-selector    (mirror-selector mirror-selector-fn proxy)
        session            (repository-session
                            system local-repo offline? transfer-listener
                            mirror-selector)
        dependency-for     (partial local-or-remote-dependency files)
        deps               (mapv dependency-for coordinates)
        repo-for           (partial mirror-or-repo session proxy)
        repos              (mapv repo-for repositories)
        request-for        (partial artifact-request repos)
        requests           (map request-for deps)]
    (->> (.resolveArtifacts system session requests)
         (map #(.getArtifact %))
         (remove nil?)
         (map #(.getFile %))
         (remove nil?))))
