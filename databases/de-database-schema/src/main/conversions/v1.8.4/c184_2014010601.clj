(ns facepalm.c184-2014010601
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.4:2014010601")

(defn- add-metadata-attribute-id-column
  "Adds the id column to the metadata_attributes table."
  []
  (println "\t* adding the id column to the metadata attributes table")
  (exec-raw "ALTER TABLE metadata_attributes ADD id uuid"))

(def ^:private attribute-ids
  [["33e3e3d8-cd48-4572-8b16-89207b1609ec" "59bd3d26-34d5-4e75-99f5-840a20089caf" 0]
   ["8e98fdc8-3e07-413e-91d4-f31631fa232d" "59bd3d26-34d5-4e75-99f5-840a20089caf" 1]
   ["1a118b71-5165-41b2-adc6-bffdeb42e628" "59bd3d26-34d5-4e75-99f5-840a20089caf" 2]
   ["98fdaf79-478a-4931-a83c-a26a2988fccf" "59bd3d26-34d5-4e75-99f5-840a20089caf" 3]
   ["fbfe1694-775e-4537-b40b-8b39c0c02d9f" "59bd3d26-34d5-4e75-99f5-840a20089caf" 4]
   ["06109a1e-f487-4c88-be20-2be0695a31de" "59bd3d26-34d5-4e75-99f5-840a20089caf" 5]
   ["8ee535d2-9cef-405d-975c-c17f75f9b76f" "59bd3d26-34d5-4e75-99f5-840a20089caf" 6]
   ["cb65328e-82aa-4b0b-928d-d3e30714446d" "59bd3d26-34d5-4e75-99f5-840a20089caf" 7]
   ["717014c4-1621-4d0a-8f74-35532373fd48" "59bd3d26-34d5-4e75-99f5-840a20089caf" 8]
   ["d84dc7c6-ff03-4503-aca4-4b4ec3b2eff9" "59bd3d26-34d5-4e75-99f5-840a20089caf" 9]
   ["9dc16c9e-7dba-47a2-b282-db8dcce3fa13" "59bd3d26-34d5-4e75-99f5-840a20089caf" 10]
   ["a4172696-52b4-49c1-b5d2-fbfc2cd88992" "59bd3d26-34d5-4e75-99f5-840a20089caf" 11]
   ["e72b7053-fb55-4211-a068-d80409066cd1" "59bd3d26-34d5-4e75-99f5-840a20089caf" 12]
   ["7fffb575-e6ee-4516-89d8-8c2cd30ab971" "59bd3d26-34d5-4e75-99f5-840a20089caf" 13]
   ["dec8e1bf-5bd4-4392-a218-425ae7f26f89" "59bd3d26-34d5-4e75-99f5-840a20089caf" 14]
   ["1b158a4d-0776-4137-b944-98f499e16fbe" "59bd3d26-34d5-4e75-99f5-840a20089caf" 15]
   ["07360894-3c19-428b-a2f8-2028e5d3f866" "59bd3d26-34d5-4e75-99f5-840a20089caf" 16]
   ["693c4508-61fa-4f0c-b66d-6b495b128ef1" "59bd3d26-34d5-4e75-99f5-840a20089caf" 17]
   ["5badc3b1-88da-4667-976a-bc5ad31645e7" "59bd3d26-34d5-4e75-99f5-840a20089caf" 18]
   ["e6f1b863-884b-4430-afc6-6b77544a595f" "59bd3d26-34d5-4e75-99f5-840a20089caf" 19]
   ["704c67d9-3057-4e99-8a1a-4b28256bdeab" "59bd3d26-34d5-4e75-99f5-840a20089caf" 20]
   ["53f8d83d-168e-4123-a97c-720738e67f5b" "59bd3d26-34d5-4e75-99f5-840a20089caf" 21]
   ["a53aea22-a48a-4a0f-9f05-02d29baa956a" "59bd3d26-34d5-4e75-99f5-840a20089caf" 22]
   ["7e21f9b9-2b12-4b19-9321-fdbdeb6815e2" "59bd3d26-34d5-4e75-99f5-840a20089caf" 23]
   ["cf866192-f155-43bf-ad88-26ffecf2c084" "59bd3d26-34d5-4e75-99f5-840a20089caf" 24]
   ["12177188-5090-4baa-8361-51b249a06778" "59bd3d26-34d5-4e75-99f5-840a20089caf" 25]
   ["4902708e-8790-4033-a10d-76831bcd246e" "59bd3d26-34d5-4e75-99f5-840a20089caf" 26]
   ["13e24b17-a9c5-409e-b147-8c6ac8f83a2b" "59bd3d26-34d5-4e75-99f5-840a20089caf" 27]
   ["82ef8400-4737-4bd3-9a8f-72cfca9ae371" "59bd3d26-34d5-4e75-99f5-840a20089caf" 28]
   ["5616479d-1730-436d-b0d9-6889052a5cfb" "59bd3d26-34d5-4e75-99f5-840a20089caf" 29]
   ["f840acee-5f55-4ef8-96b5-5e15dc5e3eed" "59bd3d26-34d5-4e75-99f5-840a20089caf" 30]
   ["6fbd6a0f-36ed-4b68-bd51-cc96dce8e285" "59bd3d26-34d5-4e75-99f5-840a20089caf" 31]
   ["2906c6dc-a1da-4610-8d61-baf773aab4d7" "59bd3d26-34d5-4e75-99f5-840a20089caf" 32]
   ["5cd49a1c-3787-4d14-985f-59e7e49249d9" "59bd3d26-34d5-4e75-99f5-840a20089caf" 33]
   ["54f6f54a-8e99-4615-8525-5bd986a11137" "59bd3d26-34d5-4e75-99f5-840a20089caf" 34]])

(defn- add-metadata-attribute-id
  "Adds an identifier to a metadata attribute."
  [[id template-id display-order]]
  (update :metadata_attributes
          (set-fields {:id (UUID/fromString id)})
          (where {:template_id   (UUID/fromString template-id)
                  :display_order display-order})))

(defn- populate-metadata-attribute-id-column
  "Populates the id column of the metadata_attributes table."
  []
  (println "\t* populating the id column of the metadata_attributes table")
  (dorun (map add-metadata-attribute-id attribute-ids)))

(defn- make-metadata-attribute-id-column-required
  "Adds the not-null constraint to the id column of the metadata_attributes table."
  []
  (println "\t* making the id column of the metadata_attributes table required")
  (exec-raw "ALTER TABLE metadata_attributes ALTER COLUMN id SET NOT NULL")
  (exec-raw
   "ALTER TABLE metadata_attributes
    ADD CONSTRAINT metadata_attributes_pkey
    PRIMARY KEY (id)"))

(defn convert
  "Performs the conversion for database version 1.8.4:2014010601."
  []
  (println "Performing conversion for" version)
  (add-metadata-attribute-id-column)
  (populate-metadata-attribute-id-column)
  (make-metadata-attribute-id-column-required))
