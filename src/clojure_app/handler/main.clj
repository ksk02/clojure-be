(ns clojure-app.handler.main
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [clojure-app.util.response :as res]
            [clj-http.client :as client]
            [cheshire.core :as json]))

(defn home-view [req]
  "<!DOCTYPE html>
   <html>
   <head>
     <meta charset=\"UTF-8\">
     <title>集合駅候補検索</title>
     <style>
       body { font-family: sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
       h1 { color: #333; }
       .station-input { margin: 10px 0; }
       .station-input input { padding: 8px; font-size: 14px; width: 300px; }
       .station-input button { padding: 8px 12px; margin-left: 10px; cursor: pointer; }
       .add-btn { background-color: #4CAF50; color: white; border: none; padding: 10px 20px; margin: 10px 0; cursor: pointer; }
       .submit-btn { background-color: #008CBA; color: white; border: none; padding: 10px 30px; margin: 10px 0; cursor: pointer; font-size: 16px; }
       .remove-btn { background-color: #f44336; color: white; border: none; }
       #results { margin-top: 30px; }
       .station-card { border: 1px solid #ddd; padding: 15px; margin: 10px 0; border-radius: 5px; }
       .station-name { font-size: 18px; font-weight: bold; color: #333; }
       .lines { margin-top: 10px; }
       .line-tag { display: inline-block; background-color: #e3f2fd; padding: 5px 10px; margin: 3px; border-radius: 3px; font-size: 14px; }
     </style>
   </head>
   <body>
     <h1>集合駅候補検索</h1>
     <div id=\"station-inputs\">
       <div class=\"station-input\">
         <input type=\"text\" placeholder=\"駅名を入力\" class=\"station-name-input\">
       </div>
     </div>
     <button class=\"add-btn\" onclick=\"addStationInput()\">+ 駅を追加</button>
     <br>
     <button class=\"submit-btn\" onclick=\"searchCentroid()\">検索</button>
     <div id=\"results\"></div>
     <script>
       function addStationInput() {
         const container = document.getElementById('station-inputs');
         const div = document.createElement('div');
         div.className = 'station-input';
         div.innerHTML = `
           <input type=\"text\" placeholder=\"駅名を入力\" class=\"station-name-input\">
           <button class=\"remove-btn\" onclick=\"this.parentElement.remove()\">削除</button>
         `;
         container.appendChild(div);
       }
       
       async function searchCentroid() {
         const inputs = document.querySelectorAll('.station-name-input');
         const stations = Array.from(inputs)
           .map(input => input.value.trim())
           .filter(val => val !== '');
         
         if (stations.length === 0) {
           alert('少なくとも1つの駅名を入力してください');
           return;
         }
         
         const params = new URLSearchParams();
         stations.forEach(station => params.append('stations', station));
         
         try {
           const response = await fetch('/centroid-station?' + params.toString());
           const data = await response.json();
           displayResults(data);
         } catch (error) {
           document.getElementById('results').innerHTML = '<p style=\"color: red;\">エラーが発生しました: ' + error.message + '</p>';
         }
       }
       
       function displayResults(data) {
         const resultsDiv = document.getElementById('results');
         if (data.error) {
           resultsDiv.innerHTML = '<p style=\"color: red;\">' + data.error + '</p>';
           return;
         }
         
         let html = '<h2>検索結果</h2>';
         if (data.stations && data.stations.length > 0) {
           data.stations.forEach(station => {
             html += `
               <div class=\"station-card\">
                 <div class=\"station-name\">${station.name}</div>
                 <div class=\"lines\">
                   ${station.lines.map(line => `<span class=\"line-tag\">${line}</span>`).join('')}
                 </div>
               </div>
             `;
           });
         } else {
           html += '<p>結果が見つかりませんでした</p>';
         }
         resultsDiv.innerHTML = html;
       }
     </script>
   </body>
   </html>")

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
    (if centroid
      (let [round-2 (fn [n] (Double/parseDouble (format "%.2f" n)))
            x (round-2 (:x centroid))
            y (round-2 (:y centroid))
            url "https://express.heartrails.com/api/json"
            resp (client/get url {:query-params {:method "getStations" :x x :y y}
                                  :as :json})
            station-list (get-in resp [:body :response :station])
            grouped (->> station-list
                         (group-by :name)
                         (mapv (fn [[name entries]]
                                {:name name
                                 :lines (mapv :line entries)})))]
        (-> (res/response (json/generate-string {:stations grouped}))
            (assoc-in [:headers "Content-Type"] "application/json; charset=utf-8")))
      (-> (res/response (json/generate-string {:error "No valid stations found to calculate centroid."}))
          (assoc-in [:headers "Content-Type"] "application/json; charset=utf-8")))))

(defn home [req]
  (-> (home-view req)
      res/response
      res/html))

(defroutes main-routes
  (GET "/centroid-station" req (centroid-station-handler req))
  (GET "/" _ home)
  (route/not-found "<h1>Not found</h1>"))
