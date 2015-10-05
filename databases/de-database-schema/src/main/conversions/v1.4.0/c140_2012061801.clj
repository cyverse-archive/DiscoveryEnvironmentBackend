(ns facepalm.c140-2012061801
  (:use [korma.core]
        [kameleon.core]
        [kameleon.queries :only [get-public-user-id]]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120618.01")

(def ^:private base-gr-path "/data2/collections/genomeservices/0.2.1/")

(def ^:private genome-reference-values
  [["4bb9856a-43da-4f67-bdf9-f90916b4c11f"
    "Arabidopsis lyrata (Ensembl 14)"
    (str base-gr-path "Arabidopsis_lyrata.1.0/de_support/")]
   ["18027404-d09f-41bf-99a4-74197ce0e7bf"
    "Rattus norvegicus [Rat] (Ensembl 67)"
    (str base-gr-path "Rattus_norvegicus.RGSC3.4/de_support/")]
   ["2b1154f3-6c10-4707-a5ea-50d6eb890582"
    "Zea mays [Maize] (Ensembl 14)"
    (str base-gr-path "Zea_mays.AGPv2/de_support/")]
   ["e38b6fae-2e4b-4217-8c1f-6badea3ff7fc"
    "Canis familiaris [Dog] (Ensembl 67)"
    (str base-gr-path "Canis_familiaris.BROAD2/de_support/")]
   ["72facaa7-ba29-49ee-b184-42ba3c015ca4"
    "Equus caballus [Horse] (Ensembl 67)"
    (str base-gr-path "Equus_caballus.EquCab2/de_support/")]
   ["e21e71f2-219f-4704-a8a6-9ab487a759a6"
    "Oryza brachyantha (Ensembl 14)"
    (str base-gr-path "Oryza_brachyantha.v1.4b/de_support/")]
   ["9875f6cc-0503-418b-b5cc-8cb8dd44d56d"
    "Setaria italica [Foxtail millet] (Ensembl 14)"
    (str base-gr-path "Setaria_italica.JGIv2.0/de_support/")]
   ["46f9d53d-36b6-4bd9-b4f2-ff952833103f"
    "Oryza indica (Ensembl 14)"
    (str base-gr-path "Oryza_indica.ASM465v1/de_support/")]
   ["2d748e14-47f5-4a91-bc67-214787ad0843"
    "Chlamydomonas reinhardtii (Ensembl 14)"
    (str base-gr-path "Chlamydomonas_reinhardtii.v3.0/de_support/")]
   ["ef269f1a-e561-4f0c-92b7-3d9d8e7362f3"
    "Drosophila melanogaster [Fruitfly] (Ensembl 67)"
    (str base-gr-path "Drosophila_melanogaster.BGDP5/de_support/")]
   ["8af62f2b-15fc-4f36-ae04-c6b801d98c1b"
    "Vitis vinifera [Grape] (Ensembl 14)"
    (str base-gr-path "Vitis_vinifera.IGPP_12x/de_support/")]
   ["2c967e76-9b8a-4a3b-aa30-2e7de3a0a952"
    "Sorghum bicolor [Sorghum] (Ensembl 14)"
    (str base-gr-path "Sorghum_bicolor.Sorbi1/de_support/")]
   ["58a84f5e-3922-43dc-8414-e42b1513be78"
    "Physcomitrella patens (Ensembl 14)"
    (str base-gr-path "Physcomitrella_patens.AMS242v1/de_support/")]
   ["c4dadc23-e0d2-481c-a3d1-1f5067e6528e"
    "Gallus gallus [Chicken] (Ensembl 67)"
    (str base-gr-path "Gallus_gallus.WASHUC2/de_support/")]
   ["4fce9ee9-0471-436b-938d-2e1820a71e6c"
    "Homo sapiens [Human] (Ensembl 67)"
    (str base-gr-path "Homo_sapiens.GRCh37/de_support/")]
   ["f772929d-0ba3-4432-8623-7a74bf2720aa"
    "Meleagris gallopavo [Turkey] (Ensembl 67)"
    (str base-gr-path "Meleagris_gallopavo.UMD2/de_support/")]
   ["ba3d662f-0f71-45fa-83a3-7a80b9bb2b3f"
    "Xenopus tropicalis [Xenopus] (Ensembl 67)"
    (str base-gr-path "Xenopus_tropicalis.JGI_4.2/de_support/")]
   ["41149e71-4328-4391-b1d2-25fdbdca5a54"
    "Felis catus [Cat] (Ensembl 67)"
    (str base-gr-path "Felis_catus.CAT/de_support/")]
   ["bb5317ce-ad00-466b-8109-432c117c0781"
    "Sus scrofa [Pig] (Ensembl 67)"
    (str base-gr-path "Sus_scrofa.Sscrofa10.2/de_support/")]
   ["eb059ac7-ee82-421a-bbc1-12f117366c4a"
    "Danio rerio [Zebrafish] (Ensembl 67)"
    (str base-gr-path "Danio_rerio.Zv9/de_support/")]
   ["a55701bc-44e6-4661-bc3a-888ca1febaed"
    "Pan troglodytes [Chimp] (Ensembl 67)"
    (str base-gr-path "Pan_troglodytes.CHIMP2.1.4/de_support/")]
   ["6149be1b-7aaa-43b4-84df-de2567ab9489"
    "Mus musculus [Mouse] (Ensembl 67)"
    (str base-gr-path "Mus_musculus.NCBIM37/de_support/")]
   ["ca94864b-b5a3-49a7-9638-0d57715a301d"
    "Brassica rapa (Ensembl 14)"
    (str base-gr-path "Brassica_rapa.IVFCAASv1/de_support/")]
   ["826f0934-69a5-401d-8b5f-36da33fc520e"
    "Glycine max [Soybean] (Ensembl 14)"
    (str base-gr-path "Glycine_max.V1.0/de_support/")]
   ["80aa0d1a-f32c-439a-940d-c9a6d629ed43"
    "Populus trichocarpa [Poplar] (Ensembl 14)"
    (str base-gr-path "Populus_trichocarpa.JGI2.0/de_support/")]
   ["756adb31-72f4-487f-ba95-c5bcca7b13b5"
    "Caenorhabditis elegans [C. elegans] (Ensembl 67)"
    (str base-gr-path "Caenorhabditis_elegans.WBcel215/de_support/")]
   ["7f66a989-9bb6-42c4-9db3-0e316304c93e"
    "Arabidopsis thaliana (Ensembl 14)"
    (str base-gr-path "Arabidopsis_thaliana.TAIR10/de_support/")]
   ["999a1d22-d2d8-4845-b685-da6403e9016e"
    "Cyanidioschyzon merolae (Ensembl 14)"
    (str base-gr-path "Cyanidioschyzon_merolae.ASM9120v1/de_support/")]
   ["8683bbe8-c577-42f8-8d9b-1bdd861122ae"
    "Brachypodium distachyon (Ensembl 14)"
    (str base-gr-path "Brachypodium_distachyon.1.0/de_support/")]
   ["e4785abc-f1e7-4d71-9ae6-bff4f2b4613b"
    "Oreochromis niloticus [Tilapia] (Ensembl 67)"
    (str base-gr-path "Oreochromis_niloticus.Orenil1.0/de_support/")]
   ["72de2532-bdf6-46b3-bffa-6c4860d63813"
    "Bos taurus [Cow] (Ensembl 67)"
    (str base-gr-path "Bos_taurus.UMD3.1/de_support/")]
   ["f3197615-747d-44c6-bd5f-293cbde95bab"
    "Gadus morhua [Cod] (Ensembl 67)"
    (str base-gr-path "Gadus_morhua.gadMor1/de_support/")]
   ["bdc96014-9b89-4dbc-9376-bc4805d9c1dd"
    "Selaginella moellendorffii (Ensembl 14)"
    (str base-gr-path "Selaginella_moellendorffii.v1.0/de_support/")]
   ["1e1c62e5-bd56-4cfa-b3ab-aa6a1496d3e5"
    "Solanum lycopersicum [Tomato] (Ensembl 14)"
    (str base-gr-path "Solanum_lycopersicum.SL2.40/de_support/")]
   ["0876c503-9634-488b-9584-ac6c0d565b8d"
    "Oryza sativa [Rice] (Ensembl 14)"
    (str base-gr-path "Oryza_sativa.MSU6/de_support/")]
   ["ea2e3413-924e-4de3-b012-05d906dd5d4a"
    "Caenorhabditis elegans [C. elegans] (Ensembl 66)"
    (str base-gr-path "Caenorhabditis_elegans.WS220/de_support/")]
   ["443befdd-c7ed-4b33-ac67-56a6748d7a48"
    "Tursiops truncatus [Dolphin] (Ensembl 67)"
    (str base-gr-path "Tursiops_truncatus.turTru1/de_support/")]
   ["70a34bd3-a7a4-4c7e-8ff5-36335b3f9b57"
    "Saccharomyces cerevisiae [Yeast] (Ensembl 67)"
    (str base-gr-path "Saccharomyces_cerevisiae.EF4/de_support/")]
   ["7e5eff7b-35fa-4635-806c-06ef5ef50db4"
    "Oryza glaberrima (Ensembl 14)"
    (str base-gr-path "Oryza_glaberrima.AGI1.1/de_support/")]])

(defn populate-genome-reference-table
  "Populates the genome-reference table with genomic metadata."
  []
  (println "\t* repopulating genome_references with updated data")
  (exec-raw "TRUNCATE genome_reference;")
  (let [public-user-id (get-public-user-id)
        field-names    [:uuid :name :path :created_by :last_modified_by]]
    (insert :genome_reference
            (values (->> genome-reference-values
                         (map #(conj % public-user-id public-user-id))
                         (map #(zipmap field-names %)))))))

(defn- dedup-integration-data-for-deployed-components
  "Ensures that all deployed components that point to integration data elements
   with the same name and e-mail address actually point to the same integration
   data element.  I didn't see an obvious way to use Korma for this update, so
   I decided to go with raw SQL."
  []
  (println "\t* deduplicating integration data for deployed components")
  (exec-raw
   "UPDATE deployed_components dc
    SET integration_data_id = (
        SELECT id2.id
        FROM integration_data id1
        JOIN integration_data id2
            ON id1.integrator_name = id2.integrator_name
            AND id1.integrator_email = id2.integrator_email
        WHERE id1.id = dc.integration_data_id
        ORDER BY id2.id
        LIMIT 1)"))

(defn- dedup-integration-data-for-analyses
  "Ensures that all analyses that point to integration data elements with the
   same name ande e-mail address actually point to the same integration data
   element.  I didn't see an obvious way to use Korma for this update, so I
   decided to go with raw SQL."
  []
  (println "\t* deduplicating integration data for analyses")
  (exec-raw
   "UPDATE transformation_activity a
    SET integration_data_id = (
        SELECT id2.id
        FROM integration_data id1
        JOIN integration_data id2
            ON id1.integrator_name = id2.integrator_name
            AND id1.integrator_email = id2.integrator_email
        WHERE id1.id = a.integration_data_id
        ORDER BY id2.id
        LIMIT 1)"))

(defn- remove-unreferenced-integration-data-elements
  "Removes any integration data elements that are no longer referenced."
  []
  (println "\t* removing unreferenced integration data elements")
  (exec-raw
   "DELETE FROM integration_data id
    WHERE NOT EXISTS (
        SELECT * FROM transformation_activity a
        WHERE a.integration_data_id = id.id)
    AND NOT EXISTS (
        SELECT * FROM deployed_components dc
        WHERE dc.integration_data_id = id.id)"))

(defn- add-integration-data-uniqueness-constraint
  "Adds the uniqueness constraint to the integration_data table."
  []
  (println "\t* adding a uniqueness constraint to the integration_data table.")
  (exec-raw
   "ALTER TABLE ONLY integration_data
    ADD CONSTRAINT integration_data_name_email_unique
    UNIQUE (integrator_name, integrator_email);"))

(defn convert
  "Performs the conversions for database version 1.40:20120618.01."
  []
  (println "Performing conversion for" version)
  (dedup-integration-data-for-deployed-components)
  (populate-genome-reference-table)
  (dedup-integration-data-for-analyses)
  (remove-unreferenced-integration-data-elements)
  (add-integration-data-uniqueness-constraint))
