(ns clavin.loader
  (:require [clavin.generator :as gen]
            [clavin.properties :as props]
            [clavin.zk :as zk]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.props :as ccprops]
            [clojure.string :as string]))

(def prop-hosts
  #(vec (map string/trim (string/split (get %1 %2) #","))))

(defn- set-of-hosts
  [acl-props]
  (set
   (flatten
    (vec (for [ak (keys acl-props)]
           (prop-hosts acl-props ak))))))

(defn- list-of-host-deployments
  [host acl-props]
  (vec (filter
        #(contains? (set (prop-hosts acl-props %1)) host)
        (keys acl-props))))

(defn- host-map
  [host acl-props]
  {host (list-of-host-deployments host acl-props)})

(defn all-host-maps
  [acl-props]
  (let [hosts      (set-of-hosts acl-props)]
    (apply merge (for [h hosts] (host-map h acl-props)))))

(defn can-run?
  ([acl-props]
    (let [local-host (java.net.InetAddress/getLocalHost)
          local-ip   (.getHostAddress local-host)]
      (can-run? acl-props local-ip)))
  ([acl-props local-ip]
    (contains? (set (prop-hosts acl-props "admin")) local-ip)))

(defn is-localhost-and-admin?
  [zhost acl-props]
  (let [local-host (java.net.InetAddress/getLocalHost)
        local-ip   (.getHostAddress local-host)]
    (and (can-run? acl-props "127.0.0.1") (or (= zhost local-ip) (= zhost "127.0.0.1")))))

(defn load-hosts
  [connection-str acl-props]
  (let [hms         (all-host-maps acl-props)
        admin-hosts (prop-hosts acl-props "admin")
        admin-acls  (map zk/admin-host-acl admin-hosts)
        hosts-root  "/hosts"
        all-hosts   (keys hms)]
    (zk/with-zk connection-str
      (when-not (zk/exists? hosts-root)
        (println (str "Creating " hosts-root))
        (zk/create hosts-root))

      (println (str "Setting ACLs for " hosts-root))
      (zk/set-acl hosts-root admin-acls)

      (doseq [[host host-deps] hms]
        (let [host-acls [(zk/env-host-acl host)]
              host-path (ft/path-join hosts-root host)]
          ;;Let the host read and write to /hosts
          (zk/add-acl hosts-root host-acls)

          ;Nuke old settings
          (when (zk/exists? host-path)
            (println (str "Deleting " host-path))
            (zk/delete-all host-path))

          ;Recreate host node.
          (println (str "Creating " host-path))
          (zk/create host-path)

          (println (str "Setting ACLs for " host-path))
          (zk/set-acl host-path admin-acls)
          (zk/add-acl host-path host-acls)

          (doseq [dep host-deps]
            (let [dep-path (ft/path-join host-path dep)]
              (when-not (zk/exists? dep-path)
                (println (str "Creating " dep-path))
                (zk/create dep-path))

              (println (str "Setting ACLs for ") dep-path)
              (zk/set-acl dep-path admin-acls)
              (zk/add-acl dep-path host-acls))))))))

(defn load-acls
  [app env dpmt acl-props]
  (let [full-env        (if (not= env "admin") (str app "." env "." dpmt) env)
        env-hosts       (prop-hosts acl-props full-env)
        admin-hosts     (prop-hosts acl-props "admin")
        env-host-acls   (map zk/env-host-acl env-hosts)
        admin-host-acls (map zk/admin-host-acl admin-hosts)]
    (concat env-host-acls admin-host-acls)))

(defn load-service
  [connection-str app env dpmt svc svc-settings acls]
  (zk/with-zk connection-str
    (let [app-path  (ft/path-join "/" app)
          env-path  (ft/path-join app-path env)
          dpmt-path (ft/path-join env-path dpmt)
          svc-path  (ft/path-join dpmt-path svc)]
      ;;Create the app path and set its ACLs.
      (when-not (zk/exists? app-path)
        (println (str "Creating " app-path))
        (zk/create app-path))

      (println (str "Setting ACLs for " app-path))
      (zk/create app-path)

      ;;Create the environment path and set its ACLs.
      (when-not (zk/exists? env-path)
        (println (str "Creating " env-path))
        (zk/create env-path))

      (println (str "Setting ACLs for " env-path))
      (zk/set-acl env-path acls)

      ;;Create the deployment path and set its ACLs.
      (when-not (zk/exists? dpmt-path)
        (println (str "Creating " dpmt-path))
        (zk/create dpmt-path))

      (println (str "Setting ACLs for " dpmt-path))
      (zk/set-acl dpmt-path acls)

      ;;Nuke the service settings from orbit first before
      ;;reloading them.
      (when (zk/exists? svc-path)
        (println (str "Deleting " svc-path))
        (zk/delete-all svc-path))

      ;;Create the service path and set its ACLs.
      (println (str "Creating " svc-path))
      (zk/create svc-path)

      (println (str "Setting ACLs for " svc-path))
      (zk/set-acl svc-path acls)

      (doseq [[svc-key node-data] svc-settings]
        (let [node-path (ft/path-join svc-path svc-key)]
          (when-not (zk/exists? node-path)
            (println (str "Creating node at " node-path))
            (zk/create node-path))

          (println (str "Setting ACLs for " node-path))
          (zk/set-acl node-path acls)

          (println (str "Setting " node-path " to " (get svc-settings svc-key)))
          (zk/set-value node-path node-data))))))

(defn load-settings
  [connection-str app env-name dpmt template-dir templates acls env]
  (dorun
   (map (fn [[svc settings]]
          (load-service connection-str app env-name dpmt svc settings acls))
        (gen/generate-all-props env template-dir templates))))
