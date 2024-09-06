(ns p14n.malli-lacinia.core)

(declare malli-field->lacinia-field)

(defn var-to-kw [v]
  (-> v symbol name keyword))

(defn sanitize-opts [opts]
  (select-keys opts [:name :description :default :enum :implements]))

(defn object-options [opts]
  (let [imp (some->> opts :implements
                     (map (partial malli-field->lacinia-field true))
                     (map :type))]
    (if imp (assoc opts :implements imp)
        opts)))

(defn force-optional [f]
  (if (coll? f)
    (let [[tp opts f] f
          nopts (if (and f (map? opts))
                  (assoc opts :optional true)
                  {:optional true})
          nchild [tp nopts (or f opts)]]
      nchild)
    f))

(defn list-type [children]
  {:type (list 'list (:type (malli-field->lacinia-field true (-> children first force-optional))))})

(defn merge-objects [obs]
  (reduce (fn [acc o]
            (merge acc o
                   (merge-with merge
                               (select-keys acc [:fields])
                               (select-keys o [:fields])))) obs))

(defn malli-field->lacinia-field-single [optional? f]
  (let [tp (if (var? f)
             (var-to-kw f)
             (case f
               :string 'String
               :keyword 'String
               :boolean 'Boolean
               :int 'Int
               :double 'Float
               :number 'Float
               (throw (ex-info (str "Unknown type: " f) {:type f}))))]
    (if optional?
      {:type tp}
      {:type (list 'non-null tp)})))

(defn malli-field->lacinia-field-coll [_ f]
  (let [tp (first f)
        has-opts? (map? (second f))
        opts (if has-opts? (second f) nil)
        sanitised-opts (sanitize-opts opts)
        children (if has-opts? (drop 2 f) (drop 1 f))
        has-children? (seq children)
        optional? (if (contains? opts :optional) (opts :optional) false)]
    (if has-children?
      (case tp
        :id (merge sanitised-opts {:id {:type 'ID}})
        :ref (malli-field->lacinia-field optional? (first children))
        :enum (merge sanitised-opts {:values (vec children)})
        :vector (list-type children)
        :set (list-type children)
        :merge (->> children
                    (map (partial malli-field->lacinia-field optional?))
                    (merge-objects))
        :map (merge (sanitize-opts (object-options opts))
                    {:fields (apply merge (map (partial malli-field->lacinia-field optional?) children))})
        {tp (merge sanitised-opts (malli-field->lacinia-field optional? (first children)))})
      (malli-field->lacinia-field-single tp optional?))))

(defn malli-field->lacinia-field [optional? f]
  (if (coll? f)
    (malli-field->lacinia-field-coll optional? f)
    (malli-field->lacinia-field-single optional? f)))

(defn malli->lacinia [schema]
  (malli-field->lacinia-field false schema))

