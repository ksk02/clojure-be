(ns clojure-app.handler.main
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [clojure-app.util.response :as res]
            [clj-http.client :as client]
            [cheshire.core :as json]))

(defn home-view [req]
  "<h1>ホーム画面</h1>
   <a href=\"/todo\">TODO 一覧</a>")

(defn centroid-station-handler [{:keys [params]}]
  (let [stations (:stations params)
        stations (if (vector? stations) stations [stations])
        fetch-station (fn [name]
                        (let [url "https://express.heartrails.com/api/json"
                              resp (client/get url {:query-params {:method "getStations" :name name}
                                                    :as :json})
                              body (:body resp)
                              station-info (first (get-in body [:response :station]))] ;; 複数路線取得できる.
                          {:x (:x station-info) :y (:y station-info)}))
        fetched-locations (map fetch-station stations)
        existedStations (filter (fn [c] (and (:x c) (:y c))) fetched-locations)
        count (count existedStations)
        centroid (if (pos? count)
                   (let [sum-x (reduce + (map :x existedStations))
                         sum-y (reduce + (map :y existedStations))]
                     {:x (/ sum-x count) :y (/ sum-y count)})
                   nil)]
    (-> (res/response (json/generate-string {:centroid centroid}))
        (assoc-in [:headers "Content-Type"] "application/json; charset=utf-8"))))

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
