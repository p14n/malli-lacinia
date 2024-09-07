(ns p14n.malli-lacinia.gen-test
  (:require [clojure.test :refer [deftest testing is]]
            [malli.generator :as mg]
            [p14n.malli-lacinia.core-test :as t]
            [malli.core :as m]))

(def character-generator (mg/generator t/Character))

(deftest character-generator-test
  (testing "Character generator produces valid data"
    (let [generated-character (character-generator)]
      (is (m/validate t/Character generated-character)))))
