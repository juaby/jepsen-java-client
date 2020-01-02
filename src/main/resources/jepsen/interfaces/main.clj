(ns jepsen.interfaces
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :refer :all]
            [knossos.model :as model]
            [knossos.op :as op]
            [jepsen.db :as db]
            [jepsen.cli :as cli]
            [jepsen.checker :as checker]
            [jepsen.client :as client]
            [jepsen.control :as control]
            [jepsen.generator :as gen]
            [jepsen.nemesis :as nemesis]
            [jepsen.tests :as tests]
            [jepsen.os.centos :as centos]
            [jepsen.control.util :as control-util]
            [jepsen.client :as client])
  (:import [user.jepsen Client]
	   [user.jepsen CheckerCallback]
	   [java.util HashMap])
)

(def userClient (atom {:client nil}))
(def checkers (atom {}))

(defn java-client "Method which returns client based on protocol"
  [args]
  (reify client/Client
	 (setup! [_ _] )
	 (open! [_ test node] (java-client (-> (:client @userClient) (.openClient node))))
	 (invoke! [client test op] 
	     (let [result (-> (:client @userClient) (.invokeClient args (:f op) (:value op)))]
	         (if (nil? result)
			(assoc op :type :fail :value 0)
			(assoc op :type :ok :value result) 
		))
	 )
         (teardown! [_ test]
             (-> (:client @userClient) (.teardownClient args))
         )))

(defn db [args] "Helps setup and teardown cluster of database being tested"
  (reify db/DB
         (setup! [_ test node]
                (-> (:client @userClient) (.setUpDatabase node)))
         (teardown! [_ test node]
                (-> (:client @userClient) (.teardownDatabase node)))))

(defn clientOp [_ _] 
    (let [op (-> (:client @userClient) (.generateOp))]
        {:type :invoke, :f op, :value (-> (:client @userClient) (.getValue op))})
)

(defn determineNemesis [nemesisName] 
      (case nemesisName
        "partition-majorities-ring" (nemesis/partition-majorities-ring)
        "partition-random-halves"  (nemesis/partition-random-halves)
      ))

(defn checkerBase [checkerCallback]
  (reify checker/Checker
     (check [checker test history opts]
       (-> checkerCallback (.check checker test history opts))
       {:valid? true}
     )))

(defn getChecker [checkerName]
  (case checkerName
      "perf" (checker/perf)
      nil
  )
)

(defn java-client-test [opts] "Test to be run"
   (merge tests/noop-test
         opts
         {:name "shiva"
          :client (java-client nil)
          :db (db nil)
	  ;:nemesis (determineNemesis (-> (:client @userClient) (.getNemesis))) 
          :generator (->> (gen/mix [clientOp]) ; this operation is just as the name suggests, chosen by the client
					       ; we will leave the operation selection to the user
                          (gen/stagger 1)
                          (gen/nemesis (gen/seq (cycle [(gen/sleep 30)
                                                        {:type :info, :f :start}
                                                        (gen/sleep 30)
                                                        {:type :info, :f :stop}])))
                          (gen/time-limit (:time-limit opts)))
          :checker (checker/compose @checkers)})
)

(defn main [args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn java-client-test})
                   (cli/serve-cmd)) args)
)

(defn setClient [localClient]
  (swap! userClient assoc :client localClient)
)

(defn setCheckerCallbacks [callbacks]
  (if (nil? callbacks) (info "Not setting callbacks since provided argument is nil.")
  (let [iter (-> callbacks .keySet .iterator)]
     (while (.hasNext iter)
	(let [k (.next iter)
	      v (-> callbacks (.get k))
	      preImplementedChecker (getChecker k)]
	   (if (nil? preImplementedChecker)
                (swap! checkers assoc k (checkerBase v))
                (swap! checkers assoc k preImplementedChecker)
            )))
)))

(defn -main [& args] "Main method from which test is launched and also place from which Java will call this function." 
  (main args)
)
