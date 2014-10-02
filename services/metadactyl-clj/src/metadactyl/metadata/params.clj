(ns metadactyl.metadata.params
  (:use [korma.core]
        [kameleon.core]
        [kameleon.entities]))

(defn- selection-param?
  [param-type]
  (re-find #"Selection$" param-type))

(defn- tree-selection-param?
  [param-type]
  (= "TreeSelection" param-type))

(defn- format-tree-root
  [root]
  {:id               (:id root)
   :selectionCascade (:name root)
   :isSingleSelect   (:isDefault root)})

(defn- format-param-value
  [param-value]
  (dissoc param-value :parent_id))

(defn- format-tree-node
  [values-map node children]
  (if (seq children)
    (let [is-group? (comp values-map :id)]
      (assoc node
        :groups    (filter is-group? children)
        :arguments (remove is-group? children)))
    node))

(defn- format-tree-values
  ([param-values]
     (let [values-map (group-by :parent_id param-values)
           root       (format-tree-root (first (values-map nil)))]
       (format-tree-values values-map root)))
  ([values-map {:keys [id] :as node}]
     (->> (values-map id)
          (mapv (comp format-param-value (partial format-tree-values values-map)))
          (format-tree-node values-map node))))

(defn format-param-values
  [type param-values]
  (cond (tree-selection-param? type) [(format-tree-values param-values)]
        (selection-param? type)      (mapv format-param-value param-values)
        :else                        []))

(defn get-param-values
  [id]
  (-> (select* :parameter_values)
      (fields :id :parent_id :name :value [:label :display] :description [:is_default :isDefault])
      (where {:parameter_id id})
      (order [:parent_id :display_order])
      (select)))

(defn params-base-query
  []
  (-> (select* [:parameters :p])
      (join :inner [:parameter_types :t] {:p.parameter_type :t.id})
      (fields :p.description :p.id :p.name :p.label :p.is_visible [:t.name :type])))
