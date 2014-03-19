(ns metadactyl.app-categorization
  (:use [korma.core]
        [korma.db]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.app-groups]
        [kameleon.queries :only
         [get-or-create-user get-or-create-workspace-for-user]]
        [metadactyl.util.config]
        [metadactyl.json :only [to-json from-json]]
        [metadactyl.util.service :only [success-response]]
        [metadactyl.validation]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string])
  (:import [java.util UUID]))

(defn- uuid
  "Generates a random UUID."
  []
  (string/upper-case (str (UUID/randomUUID))))

(defn- build-hierarchy
  "Builds an app group hierarchy from the result of loading the app group
   group hierarchy from the database."
  [group groups]
  (let [subgroups    (filter #(= (:hid group) (:parent_hid %)) groups)
        subgroups    (map #(build-hierarchy % groups) subgroups)
        group        (assoc group :subgroups subgroups)]
    (dissoc group :description :is_public :app_count)))

(defn- load-app-group-hierarchy
  "Loads an app group hierarchy from the database and returns it in
   hierarchical format."
  [root-id]
  (let [app-groups (get-app-group-hierarchy root-id)
        root       (first (filter #(= (:hid %) root-id) app-groups))]
    (build-hierarchy root app-groups)))

(defn- load-app-group-hierarchies-from-database
  "Loads all of the existing app group hierarchies from the database.  And
   returns them in a map that is indexed by username."
  []
  (into {}
        (map #(vector (:username %) (load-app-group-hierarchy (:app_group_id %)))
             (load-root-app-groups-for-all-users))))

(defn- resolve-workspace
  "Resolves a workspace in the map of app group hierarchies.  The root group
   name that is provided needs to match the actual root group name."
  [hierarchy username group-name]
  (when-let [workspace (hierarchy username)]
    (if-not (= group-name (:name workspace))
      (throw+ {:type                 ::inconsistent_root_app_group_name
               :username             username
               :actual_group_name    (:name workspace)
               :requested_group_name group-name}))
    workspace))

(defn- find-by-name
  "Finds a map with a :name key equal to a specified name in a sequence of
   maps."
  [s name]
  (first (filter #(= name (:name %)) s)))

(defn- resolve-category
  "Recursively resolves an app category in an app category tree.  The parent
   argument contains the current node in the tree.  The second argument
   contains a sequence of category names representing the path to the desired
   app category."
  [parent [name & names]]
  (if (nil? name)
    parent
    (when-let [child (find-by-name (:subgroups parent) name)]
      (recur child names))))

(defn- create-category
  "Creates a new app category with the given name and a randomly generated
   UUID."
  [group-name]
  {:name      group-name
   :id        (uuid)
   :subgroups []})

(defn- add-subgroups
  "Adds subgroups a parent app group.  The parent might have existing subgroups
   already."
  [parent children]
  (let [children (concat (:subgroups parent) children)]
    (assoc parent :subgroups children)))

(defn- create-workspace-root-category
  "Creates the root category and the default subcategories for a new
   workspace."
  []
  (let [root-name (workspace-root-app-group)
        root      (create-category root-name)
        sub-names (get-default-app-groups)
        subs      (map create-category sub-names)]
    (add-subgroups root subs)))

(defn- add-workspace
  "Adds a new workspace to the hierarchy."
  [hierarchy username]
  (assoc hierarchy
    username (create-workspace-root-category)))

(defn- add-category
  "Adds a new category, along with of its parents that don't exist yet, to an
   app category tree.  The first argument contains the current node in the tree.
   The second argument contains a list of app category names representing the
   path to the node we want to insert."
  [parent [name & names]]
  (if (nil? name)
    parent
    (let [subgroups (:subgroups parent)]
      (assoc parent
        :subgroups
        (if (find-by-name subgroups name)
          (map #(if (= name (:name %)) (add-category % names) %) subgroups)
          (conj (vec subgroups)
                (add-category (create-category name) names)))))))

(defn- add-workspace-and-categories
  "Adds a workspace and subcategories to the hierarchy."
  [hierarchy username root path]
  (let [hierarchy (add-workspace hierarchy username)
        workspace (resolve-workspace hierarchy username root)]
    (assoc hierarchy
      username (add-category workspace path))))

(defn- add-missing-category-to-workspace
  "Adds an app category to a workspace if the category doesn't exist in the
   workspace yet."
  [hierarchy username workspace path]
  (if-not (resolve-category workspace path)
    (assoc hierarchy username (add-category workspace path))
    hierarchy))

(defn- add-missing-category
  "Adds an app category and its associated workspace, if necessary, to the
   hierarchy if it doesn't exist already."
  [hierarchy category]
  (let [{:keys [username path]} (:category_path category)
        [root & path]           path
        workspace               (resolve-workspace hierarchy username root)]
    (if (nil? workspace)
      (add-workspace-and-categories hierarchy username root path)
      (add-missing-category-to-workspace hierarchy username workspace path))))

(defn- associate-subcategory
  "Associates a subcategory with its parent category in the database."
  [{parent-group-id :hid} {subgroup-id :hid} index]
  (when-not (is-subgroup? parent-group-id subgroup-id)
    (add-subgroup parent-group-id index subgroup-id)))

(defn- insert-category-if-missing
  "Inserts a category into the database if it doesn't exist already."
  [workspace-id category]
  (let [category (if-not (:hid category)
                   (assoc category
                     :hid (:hid (create-app-group workspace-id category)))
                   category)
        category (assoc category
                   :subgroups (map #(insert-category-if-missing workspace-id %)
                                   (:subgroups category)))]
    (dorun (map #(associate-subcategory category % %2)
                (:subgroups category) (range)))
    category))

(defn- insert-workspace-if-missing
  "Inserts a workspace into the database if it doesn't exist already."
  [username root-group]
  (if-not (:workspace_id root-group)
    (assoc root-group :workspace_id
           (:id (get-or-create-workspace-for-user username)))
    root-group))

(defn- insert-workspace-and-categories
  "Inserts a workspace and categories into the database if they don't exist
   already."
  [[username root-group]]
  (let [root-group   (insert-workspace-if-missing username root-group)
        workspace-id (:workspace_id root-group)
        root-group   (insert-category-if-missing workspace-id root-group)]
    (set-root-app-group workspace-id (:hid root-group))
    [username root-group]))

(defn- insert-workspaces-and-categories
  "Inserts all workspaces and categories into the database if they don't exist
   already."
  [hierarchy]
  (into {} (map insert-workspace-and-categories hierarchy)))

(defn- get-app-hid
  "Gets the internal identifier of an app, throwing an exception if the app
   doesn't exist."
  [id]
  (let [hid (:hid (get-app-by-id id))]
    (when (nil? hid)
      (throw+ {:type   ::app_not_found
               :app-id id}))
    hid))

(defn- categorize-app
  "Associates an app with an app category."
  [hierarchy {app :analysis {:keys [username path]} :category_path}]
  (let [[root & path] path
        workspace     (resolve-workspace hierarchy username root)
        category      (resolve-category workspace path)
        cat-hid       (:hid category)
        app-hid       (get-app-hid (:id app))]
    (add-app-to-group cat-hid app-hid)))

(defn- extract-app-id
  "Extracts an app ID from a category definition."
  [{{app-id :id} :analysis}]
  app-id)

(defn- do-categorization
  "Categorizes one or more apps in the database."
  [{:keys [categories]}]
  (let [hierarchy (load-app-group-hierarchies-from-database)
        hierarchy (reduce add-missing-category hierarchy categories)
        hierarchy (insert-workspaces-and-categories hierarchy)]
    (dorun (map #(decategorize-app (extract-app-id %)) categories))
    (dorun (map #(categorize-app hierarchy %) categories))))

(defn- validate-app-info
  "Validates the app information in a categorized app.  At this time, we only
   require the identifier field."
  [app-info path]
  (validate-required-json-string-field app-info :id path))

(defn- validate-path
  "Validates an app category path, which must contain a username and path, which
   is an array of strings."
  [category-path path]
  (validate-required-json-string-field category-path :username path)
  (validate-json-array-field category-path :path path)
  (validate-value #(> (count %) 0) (:path category-path)
                  #(hash-map :type ::empty_category_path)))

(defn- validate-category
  "Validates each categorized app in the request."
  [category path]
  (validate-json-object-field category :analysis path validate-app-info)
  (validate-json-object-field category :category_path path validate-path))

(defn- parse-and-validate-body
  "Parses and validates the request body."
  [body]
  (doto (from-json body)
    (validate-json-object ""
                          #(validate-json-object-array-field
                            % :categories %2 validate-category))))

(defn categorize-apps
  "A service that categorizes one or more apps in the database."
  [body]
  (let [m (parse-and-validate-body (slurp body))]
    (transaction (do-categorization m))
    (success-response)))
