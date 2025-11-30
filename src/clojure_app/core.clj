(ns clojure-app.core
  (:require [compojure.core :refer [routes]]
            [ring.adapter.jetty :as server]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure-app.handler.main :refer [main-routes]]
            [clojure-app.handler.todo :refer [todo-routes]]
            [clojure-app.middleware :refer [wrap-dev]]
            [environ.core :refer [env]]))

(defonce server (atom nil))

(defn- wrap [handler middleware opt]
  (if (true? opt)
    (middleware handler)
    (if opt
      (middleware handler opt)
      handler)))

(def app
  (-> (routes
       todo-routes
       main-routes)
      (wrap wrap-keyword-params true)
      (wrap wrap-params {:make-vectors true})
      (wrap wrap-dev (:dev env))))

(defn start-server []
  (when-not @server
    (reset! server (server/run-jetty #'app {:port 3000 :join? false}))))

(defn stop-server []
  (when @server
    (.stop @server)
    (reset! server nil)))

(defn restart-server []
  (when @server
    (stop-server)
    (start-server)))

(defn -main [& args]
  (start-server)
  (.join @server))
