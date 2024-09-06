(ns p14n.malli-lacinia.core)

(declare malli-field->lacinia-field)

(defn var-to-kw [v]
  (-> v symbol name keyword))

(defn malli-field->lacinia-field-single [f]
  (if (var? f)
    {:type (var-to-kw f)}
    (case f
      :string {:type 'String})))

(defn object-options [opts]
  (let [imp (some->> opts :implements
                     (map malli-field->lacinia-field)
                     (map :type))]
    (if imp (assoc opts :implements imp)
        opts)))

(defn malli-field->lacinia-field-coll [f]
  (let [tp (first f)
        has-opts? (map? (second f))
        opts (if has-opts? (second f) nil)
        children (if has-opts? (drop 2 f) (drop 1 f))
        has-children? (seq children)]
    (if has-children?
      (case tp
        :ref (malli-field->lacinia-field (first children))
        :enum (merge opts {:values (vec children)})
        :vector {:type (list 'list (:type (malli-field->lacinia-field (first children))))}
        :map (merge (object-options opts)
                    {:fields (apply merge (map malli-field->lacinia-field children))})
        {tp (merge opts (malli-field->lacinia-field (first children)))})
      (malli-field->lacinia-field-single tp))))

(defn malli-field->lacinia-field [f]
  (if (coll? f)
    (malli-field->lacinia-field-coll f)
    (malli-field->lacinia-field-single f)))

(defn malli->lacinia [schema]
  (malli-field->lacinia-field schema))

