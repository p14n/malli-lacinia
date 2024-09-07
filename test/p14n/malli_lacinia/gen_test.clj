(ns p14n.malli-lacinia.gen-test
  (:require [clojure.test :refer [deftest testing is]]
            [malli.generator :as mg]
            [p14n.malli-lacinia.core-test :as t]
            [malli.core :as m]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia :refer [execute]]
            [malli.error :as me]))

(def characters-generator #(mg/generate [:vector t/Character]))
(def character-generator #(mg/generate t/Character))
(def droid-generator #(mg/generate t/Droid))
(def human-generator #(mg/generate t/Human))

(deftest character-generator-test
  (testing "Character generator produces valid data"
    (let [generated-character (characters-generator)]
      (is (m/validate [:vector t/Character] generated-character)))))
(defn pr> [x] (println x) x)
(def schema-with-generators
  (->  t/star-wars-schema-lacinia-with-malli
       (util/inject-resolvers {:Droid/friends (fn [_ _ _] (characters-generator))
                               :Human/friends (fn [_ _ _] (characters-generator))
                               :Query/human (fn [_ _ _] (human-generator))
                               :Query/droid (fn [_ _ _] (droid-generator))
                               :Query/hero (fn [_ _ _] (schema/tag-with-type (character-generator) :Human))})
       (schema/compile)))

(defn exec-query [q]
  (execute schema-with-generators q nil nil))

(defn humanised-error [s r]
  (->> (m/explain s r)
       (me/humanize)))

(deftest run-queries
  (testing "Find a hero"
    (is (nil? (->> (exec-query "{ hero(episode: NEWHOPE) { id appearsIn } }")
                   :data :hero
                   (humanised-error t/Character)))))
  (testing "Find a droid"
    (is (nil? (->> (exec-query "{ droid { id appearsIn } }")
                   :data :droid
                   (humanised-error t/Droid)))))
  (testing "Find a human"
    (is (nil? (->> (exec-query "{ human { id appearsIn age height isAlive } }")
                   :data :human
                   (humanised-error t/Human))))))