(ns clojure-app.handler.main
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [clojure-app.util.response :as res]))

(defn home-view [req]
  "<h1>ホーム画面</h1>
   <a href=\"/todo\">TODO 一覧</a>")

(defn centroid-station-handler [{:keys [params]}]
  (let [stations (:stations params)
        stations (if (vector? stations) stations [stations])
        result (apply str stations)]
    (-> (res/response result)
        (assoc-in [:headers "Content-Type"] "text/plain; charset=utf-8"))))

(defn home [req]
  (-> (home-view req)
      res/response
      res/html))

;; (defn home [req]
;;   (throw (Exception. "Test Exception!!"))
;;   #_(-> (home-view req)
;;         res/response
;;         res/html))

(defroutes main-routes
  (GET "/centroid-station" req (centroid-station-handler req))
  (GET "/" _ home)
  (route/not-found "<h1>Not found</h1>"))
