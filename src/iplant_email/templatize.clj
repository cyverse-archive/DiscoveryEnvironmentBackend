(ns iplant-email.templatize
  (:import [org.antlr.stringtemplate StringTemplateGroup StringTemplate]))

(defn template
  "Gets and instantiates the named template from the classpath."
  [template-name]
  (. (StringTemplateGroup. "default") getInstanceOf template-name))

(defn set-attrs
  [tmpl attr-map]
  (doseq [key-value attr-map]
    (. tmpl setAttribute (name (key key-value)) (val key-value)))
  tmpl)

(defn render-template
  [tmpl]
  (. tmpl toString))

(defn create-email
  [template-name value-map]
  (let [tmpl        (template template-name)
        filled-tmpl (set-attrs tmpl value-map)]
    (render-template filled-tmpl)))

