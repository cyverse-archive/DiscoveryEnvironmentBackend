(ns donkey.util.tree-url)


(defn format-tree-url
  "Creates a tree URL element."
  [label url]
  {:label label
   :url   url})

(defn format-tree-urls
  "Formats the tree URLs for storage in the file metadata.  The urls argument
   should contain a sequence of elements as returned by format-tree-url."
  [urls]
  {:tree-urls urls})
