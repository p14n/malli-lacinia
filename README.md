## malli-lacinia

Converts [malli](https://github.com/metosin/malli) schema defintions into [lacinia](https://github.com/walmartlabs/lacinia) compatible objects.  

Create malli definitions for your domain objects:

```clojure
(ns-unmap *ns* 'Character) ;Standard GQL example clashes with java.lang.Character

(def Episode [:enum {:description "The episodes of the original Star Wars trilogy."} :NEWHOPE :EMPIRE :JEDI])

(def Character [:map
                [:id :string]
                [:name {:optional true} :string]
                [:appearsIn [:vector #'Episode]] ;The use of vars leaves referenced objects as references
                [:friends [:vector [:ref #'Character]]]])
(def Droid [:map
            {:implements [#'Character]}
            [:id :string]
            [:name {:optional true} :string]
            [:appearsIn [:vector #'Episode]]
            [:friends
             [:vector #'Character]]
            [:primaryFunction {:optional true} [:vector :string]]])
(def Human [:map
            {:implements [#'Character]}
            [:id :string]
            [:name {:optional true} :string]
            [:isAlive :boolean]
            [:age :int]
            [:height :double]
            [:appearsIn [:vector #'Episode]]
            [:friends [:vector #'Character]]
            [:home_planet {:optional true} :string]])
```
Use these schema objects in a lacinia schema by converting with `malli->lacinia`

```clojure
(require '[p14n.malli-lacinia.core :refer [malli->lacinia]])

(def star-wars-schema-lacinia-with-malli
  {:enums
   {:Episode (malli->lacinia Episode)}
   :interfaces
   {:Character (malli->lacinia Character)}
   :objects
   {:Droid (malli->lacinia Droid)
    :Human (malli->lacinia Human)
    :Query
    {:fields
     {:hero {:type '(non-null :Character)
             :args {:episode {:type :Episode}}}

      :human {:type '(non-null :Human)
              :args {:id {:type 'String
                          :default-value "1001"}}}

      :droid {:type :Droid
              :args {:id {:type 'String
                          :default-value "2001"}}}}}}})
```
The resulting schema is valid for use in lacinia:

```clojure
  {:enums
   {:Episode
    {:description "The episodes of the original Star Wars trilogy."
     :values [:NEWHOPE :EMPIRE :JEDI]}}

   :interfaces
   {:Character
    {:fields {:id {:type 'ID}
              :name {:type 'String}
              :appearsIn {:type '(list :Episode)}
              :friends {:type '(list :Character)}}}}

   :objects
   {:Droid
    {:implements [:Character]
     :fields {:id {:type 'ID}
              :name {:type 'String}
              :appearsIn {:type '(list :Episode)}
              :friends {:type '(list :Character)}
              :primaryFunction {:type '(list String)}}}

    :Human
    {:implements [:Character]
     :fields {:id {:type 'ID}
              :name {:type 'String}
              :age {:type '(non-null Int)}
              :height {:type '(non-null Float)}
              :isAlive {:type '(non-null Boolean)}
              :appearsIn {:type '(list :Episode)}
              :friends {:type '(list :Character)}
              :home_planet {:type 'String}}}

    :Query
    {:fields
     {:hero {:type '(non-null :Character)
             :args {:episode {:type :Episode}}}

      :human {:type '(non-null :Human)
              :args {:id {:type 'String
                          :default-value "1001"}}}

      :droid {:type :Droid
              :args {:id {:type 'String
                          :default-value "2001"}}}}}}}
```
