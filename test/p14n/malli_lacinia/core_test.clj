(ns p14n.malli-lacinia.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [p14n.malli-lacinia.core :refer [malli->lacinia]]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [malli.core :as m]))

(ns-unmap *ns* 'Character)

(def Episode [:enum {:description "The episodes of the original Star Wars trilogy."} :NEWHOPE :EMPIRE :JEDI])
(def Character [:map
                [:id :string]
                [:name {:optional true} :string]
                [:appearsIn [:vector #'Episode]]
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

(def all-malli
  [:map
   [:Episode Episode]
   [:Character Character]
   [:Droid Droid]
   [:Human Human]])

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

(def star-wars-schema-lacinia
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
                          :default-value "2001"}}}}}}})


(deftest create-schema
  (testing "all malli valid"
    (is (not (nil? (m/schema all-malli)))))
  (testing "lacinia example valid"
    (is (not (nil? (->  star-wars-schema-lacinia
                        (util/inject-resolvers {:Droid/friends (fn [_ _ _])})
                        (schema/compile))))))
  (testing "malli example valid"
    (is (not (nil? (->  star-wars-schema-lacinia-with-malli
                        (util/inject-resolvers {:Droid/friends (fn [_ _ _])})
                        (schema/compile))))))
  (testing "enum match"
    (is (= (:enums star-wars-schema-lacinia)
           (:enums star-wars-schema-lacinia-with-malli))))
  (testing "interface match"
    (is (= (:interfaces star-wars-schema-lacinia)
           (:interfaces star-wars-schema-lacinia-with-malli))))
  (testing "Droid object match"
    (is (= (-> star-wars-schema-lacinia :objects :Droid)
           (-> star-wars-schema-lacinia-with-malli :objects :Droid))))
  (testing "Human object match"
    (is (= (-> star-wars-schema-lacinia :objects :Human)
           (-> star-wars-schema-lacinia-with-malli :objects :Human))))
  (testing "Full schema match"
    (is (= star-wars-schema-lacinia
           star-wars-schema-lacinia-with-malli))))
