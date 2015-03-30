(ns facepalm.c199-2015053005
    (:use [korma.core]))

(def ^:private version
    "The destination database version."
    "1.9.9:20150530.05")

(defn- add-de-backwards-compatibility-image
  []
  (println "\t* Adding the backwards compatibility container to the database")
  (exec-raw
    "INSERT INTO container_images (id, \"name\", tag, url) VALUES
        ('fc210a84-f7cd-4067-939c-a68ec3e3bd2b',
         'discoenv/backwards-compat',
         'latest',
         'https://registry.hub.docker.com/u/discoenv/backwards-compat');")
  (exec-raw
    "INSERT INTO container_settings (tools_id)
      SELECT tools.id
        FROM tools
       WHERE tools.id NOT IN (
         SELECT container_settings.tools_id
           FROM container_settings
          WHERE container_settings.tools_id IS NOT NULL
       )
       AND tools.name != 'notreal';")
  (exec-raw
    "UPDATE ONLY tools
       SET container_images_id = 'fc210a84-f7cd-4067-939c-a68ec3e3bd2b'
     WHERE container_images_id IS NULL;"))

(defn convert
  []
  (println "Performing the conversion for" version)
  (add-de-backwards-compatibility-image))
