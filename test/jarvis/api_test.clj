(ns jarvis.api-test
  (:require [clojure.test :refer :all]
            [jarvis.api :refer :all]))

(deftest ref-test
  (testing "ref specs - supported creation"
    (is
      (= (->Ref :task "az") (new-ref :task "az"))))
  (testing "ref specs - invalid creation"
    (is
      (thrown? AssertionError (new-ref :task :az)))))
