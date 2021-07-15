(ns local-server
  (:require [ring.adapter.jetty :as jetty]
            [reitit.ring :as reitit]))

(defonce server (atom nil))

(defn start!
  [opts]
  (reset! server (jetty/run-jetty (reitit/ring-handler (reitit/router (:routes opts))
                                                       (constantly {:status 200
                                                                    :body   "local native server: route not matched"}))
                                  (merge {:port  3001
                                          :join? false}
                                         opts))))

(defn stop!
  []
  (when @server
    (println (.stop @server))
    (reset! server nil)))

(defn restart!
  [opts]
  (stop!)
  (start! opts))


