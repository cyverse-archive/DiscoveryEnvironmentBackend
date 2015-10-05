(ns facepalm.c140-2012053001
  (:use [korma.core]
        [kameleon.core]
        [kameleon.queries :only [get-public-user-id]]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120530.01")

(defn- add-genome-reference-table
  "Adds the table used to track reference genomes."
  []
  (println "\t* adding the genome_reference table")
  (exec-raw "CREATE SEQUENCE genome_ref_id_seq")
  (exec-raw
   "CREATE TABLE genome_reference (
        id bigint DEFAULT nextval('genome_ref_id_seq'),
        uuid char(36) UNIQUE NOT NULL,
        name varchar(512) NOT NULL,
        path varchar(1024) NOT NULL,
        deleted boolean DEFAULT false NOT NULL,
        created_by bigint references users(id),
        created_on timestamp DEFAULT now() NOT NULL,
        last_modified_by bigint references users(id),
        last_modified_on timestamp DEFAULT now() NOT NULL,
        PRIMARY KEY(id)
    )"))

(defn- add-collaborators-table
  "Adds the table used to track collaborators."
  []
  (println "\t* adding the collaborators table")
  (exec-raw "CREATE SEQUENCE collaborators_id_seq")
  (exec-raw
   "CREATE TABLE collaborators (
        id bigint DEFAULT nextval('collaborators_id_seq'),
        user_id bigint NOT NULL references users(id),
        collaborator_id bigint NOT NULL references users(id),
        PRIMARY KEY(id)
    )"))

(def ^:private base-gr-path "/data2/collections/genomeservices/0.2/")

(def ^:private genome-reference-values
  [["A0EEBC99-9C0B-4EF8-BB6D-6BB9BD380A11"
    "Arabidopsis lyrata Araly1 (Ensembl 13)"
    (str base-gr-path "Arabidopsis_lyrata.Araly1/de_support/")]
   ["54DC0C16-226C-458A-B186-5E570228A130"
    "Arabidopsis thaliana TAIR10 (Ensembl 13)"
    (str base-gr-path "Arabidopsis_thaliana.TAIR10/de_support/")]
   ["61DC0C16-226C-458A-B186-5E570258A130"
    "Bos taurus UMD3.1 (Ensembl 66)"
    (str base-gr-path "Bos_taurus.UMD3.1/de_support/")]
   ["ABF7B800-21BE-44EB-84DB-0F87E7094B27"
    "Brachypodium distachyon Brachy1.0 (Ensembl 13)"
    (str base-gr-path "Brachypodium_distachyon.Brachy1.0/de_support/")]
   ["FCE82CED-6E9F-4909-B360-AA7FE0072DD7"
    "Brassica rapa IVFCAASv1 (Ensembl 13)"
    (str base-gr-path "Brassica_rapa.IVFCAASv1/de_support/")]
   ["806B6D5A-DF12-448B-BC14-206F1543F13F"
    "Caenorhabditis elegans WS220 (Ensembl 66)"
    (str base-gr-path "Caenorhabditis_elegans.WS220/de_support/")]
   ["B46C7DAE-9521-43DA-A7DA-8BDB76BE06FA"
    "Canis familiaris BROADD2 (Ensembl 66)"
    (str base-gr-path "Canis_familiaris.BROADD2/de_support/")]
   ["B7FE13D8-A9E0-408C-946F-5E3AFB922E7D"
    "Chlamydomonas reinhardtii ENA1 (Ensembl 13)"
    (str base-gr-path "Chlamydomonas_reinhardtii.ENA1/de_support/")]
   ["F1BE1410-9C90-41D1-84DD-04B674E488CF"
    "Cyanidioschyzon merolae ENA1 (Ensembl 13)"
    (str base-gr-path "Cyanidioschyzon_merolae.ENA1/de_support/")]
   ["2EF5D109-BF75-4497-AD82-701852D274A3"
    "Danio rerio Zv9 (Ensembl 66)"
    (str base-gr-path "Danio_rerio.Zv9/de_support/")]
   ["0CF5BA6C-9DE6-4D1D-A909-66FC69F6A4E8"
    "Drosophila melanogaster BDGP5 (Ensembl 66)"
    (str base-gr-path "Drosophila_melanogaster.BDGP5/de_support/")]
   ["F0F4D4F7-7782-41BF-8986-A161879CF3EC"
    "Felis catus CAT (Ensembl 66)"
    (str base-gr-path "Felis_catus.CAT/de_support/")]
   ["E4C89C48-84E4-487F-BE14-427562EFB5A4"
    "Glycine max 1.0 (Ensembl 13)"
    (str base-gr-path "Glycine_max.1.0/de_support/")]
   ["0D5FD2AC-69E7-4E5C-B0FE-4B7BD2EA9348"
    "Homo sapiens GRCh37 (Ensembl 66)"
    (str base-gr-path "Homo_sapiens.GRCh37/de_support/")]
   ["7FFE68A5-70BE-44D6-8A48-236D2E2BAF7A"
    "Mus musculus NCBIM37 (Ensembl 66)"
    (str base-gr-path "Mus_musculus.NCBIM37/de_support/")]
   ["D09C1C16-225C-459A-B187-5E570229A130"
    "Oryza glaberrima AGI1.1 (Ensembl 13)"
    (str base-gr-path "Oryza_glaberrima.AGI1.1/de_support/")]
   ["970E8ED8-B65D-46FA-899E-03D6344397DC"
    "Oryza indica Jan_2005 (Ensembl 13)"
    (str base-gr-path "Oryza_indica.Jan_2005/de_support/")]
   ["563DF22A-0B33-44EE-A30D-2B5033D8C71E"
    "Oryza sativa MSU6 (Ensembl 13)"
    (str base-gr-path "Oryza_sativa.MSU6/de_support/")]
   ["0F1BC02C-7741-4CC5-9936-D4692F412070"
    "Pan troglodytes CHIMP2.1.4 (Ensembl 66)"
    (str base-gr-path "Pan_troglodytes.CHIMP2.1.4/de_support/")]
   ["9B5AED20-7882-44AB-BEBC-8DD7B4C7E13F"
    "Physcomitrella patens Phypa1.1 (Ensembl 13)"
    (str base-gr-path "Physcomitrella_patens.Phypa1.1/de_support/")]
   ["710A06B3-FB13-4843-A607-80FD303B555D"
    "Populus trichocarpa JGI2.0 (Ensembl 13)"
    (str base-gr-path "Populus_trichocarpa.JGI2.0/de_support/")]
   ["080C3A4A-3566-4C62-9F4D-04F129915761"
    "Rattus norvegicus RGSC3.4 (Ensembl 66)"
    (str base-gr-path "Rattus_norvegicus.RGSC3.4/de_support/")]
   ["08D6F8D0-5B93-4607-9159-DC0BBDF3EC70"
    "Saccharomyces cerevisiae EF4 (Ensembl 66)"
    (str base-gr-path "Saccharomyces_cerevisiae.EF4/de_support/")]
   ["1CFEAAD6-F5F2-4B2A-BA85-A16A9E8EB35C"
    "Selaginella moellendorffii ENA1 (Ensembl 13)"
    (str base-gr-path "Selaginella_moellendorffii.ENA1/de_support/")]
   ["B533B529-8602-4FBD-B812-A2279CCEE452"
    "Sorghum bicolor Sbi1 (Ensembl 13)"
    (str base-gr-path "Sorghum_bicolor.Sbi1/de_support/")]
   ["803A7C3D-69A4-46EE-B884-72C2636A2B29"
    "Tursiops truncatus turTru1 (Ensembl 66)"
    (str base-gr-path "Tursiops_truncatus.turTru1/de_support/")]
   ["891AD871-8204-4F3E-B296-173511D4B0F3"
    "Vitis vinifera IGGP_12x (Ensembl 13)"
    (str base-gr-path "Vitis_vinifera.IGGP_12x/de_support/")]
   ["E47B2299-BA9C-47C5-9EAF-1CF7A90754BF"
    "Xenopus tropicalis JGI_4.2 (Ensembl 66)"
    (str base-gr-path "Xenopus_tropicalis.JGI_4.2/de_support/")]
   ["0699265D-6F90-4C46-B15C-1E294C76B3D4"
    "Zea mays AGPv2 (Ensembl 13)"
    (str base-gr-path "Zea_mays.AGPv2/de_support/")]])

(def ^:private genome-reference-fields)

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

(defn convert
  "Performs the conversion for database version 1.4.0:20120530.01."
  []
  (println "Performing conversion for" version)
  (add-genome-reference-table)
  (add-collaborators-table)
  (populate-genome-reference-table))
