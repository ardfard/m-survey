(ns mobile-survey.core
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [hiccup.core :refer [html]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.io :as io]
            [noir.util.route :refer [def-restricted-routes]]
            [mobile-survey.models.db :as models]
            [mobile-survey.templates :as t]
            [mobile-survey.sms :as sms]
            [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clj-time.coerce :refer [from-date]]
            [dk.ative.docjure.spreadsheet :as xls]
            [clojure.string :refer [join]]))


(defn create-js-script-for [view]
    (cond
        (= view :create) '({}) #_( {:tag :script, :attrs {:src "js/mobile-survey.js"}}
                            {:tag :script, :content ("mobile_survey.create_survey.init()")})
        (= view :surveys) '({:tag :script, :attrs {:src "js/mobile-survey.js"}})
        (= view :detail)  '({})))

(def status_code {0 "On progress", 1 "Complete"})

(defn get-numbers-from-file [excel-file]
  (let [wb (xls/load-workbook excel-file)
        sheet (first (xls/sheet-seq wb))]
    (map xls/read-cell (xls/cell-seq sheet))))

(defn get-duration [since]
  (let [interval (time-core/interval since (time-core/now))]
    (cond (not= (time-core/in-years interval) 0) (str (time-core/in-years interval) " year(s)")
          (not= (time-core/in-days interval) 0) (str (time-core/in-days interval) " day(s)")
          (not= (time-core/in-hours interval) 0) (str (time-core/in-hours interval) " hour(s)")
          (not= (time-core/in-minutes interval) 0) (str (time-core/in-minutes interval) " minute(s)"))))

(defn surveys [user-id]
    (let [survey-list (models/get-surveys user-id)]
      (t/base (t/list-survey survey-list) (create-js-script-for :surveys))))

(defn create-survey-form []
    (t/base (t/create-snippet) (:content (first (t/create-script)))))

(defn create-survey-helper [values]
  (println values)
  #_(models/create-survey! values)
  #_(models/get-survey-id (:user_id values) (:name values)))

(defn handle-file-number [file-number]
    (let [file-num (io/upload-file "/temp/" file-number)]
      (get-numbers-from-file (str "resources/public/temp/" (file-number :filename)))))

(defn create-survey [{:keys [user-id name descriptions content selectNumber
                             incentiveOption numbersText numbersFile] :as params}]
  (let [options (map val (dissoc params :user-id :name :descriptions :content :selectNumber
                               :incentiveOption :numbersText :numbersFile))
        option-letters (map #(format "(%s) " %) "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        options (->> options
                     (map list option-letters)
                     (map #(apply str %))
                     (join \newline))
        content (str content \newline options)
        survey-id (create-survey-helper {:name name
                          :description descriptions
                          :content content
                          :incentive 0
                          :user_id user-id
                          })
        numbers (if (= selectNumber "from-text")
                    (clojure.string/split (clojure.string/triml numbersText) #"\s+")
                    (handle-file-number numbersFile))]
      (doseq [number numbers]
        #_(models/create-number! {:survey_id survey-id
                               :number number
                               :reply ""}))
      #_(sms/publish-survey! {:id survey-id :content content} numbers)
      #_(redirect "/surveys")))

(defn detail-survey [id]
    (let [{:keys [name description created_on content status]} (models/get-survey-with-id id)
          duration (get-duration (from-date created_on))
          numbers (models/get-numbers-for-survey id)
          replied-cnt (count (for [x numbers :when (not= x "")] x))]
        (t/base (t/detail-snippet id name description content duration (status_code status) (count numbers) replied-cnt)
                (create-js-script-for :detail))))

(defn delete-survey [id]
  (models/delete-survey! id)
  (redirect "/surveys"))

(defn view-number [id]
  (let [numbers (models/get-numbers-for-survey id)]
    (t/base (t/numbers-snippet numbers) '({}))))

(defn create-report-file [survey-id]
  (let [numbers (models/get-numbers-for-survey survey-id)
        wb (xls/create-workbook "report" (cons ["Number" "Reply"]
                                           (for [{:keys [number reply]} numbers]
                                             [number reply])))
        sheet (xls/select-sheet "report" wb)
        header-row (first (xls/row-seq sheet))]
    (do
      (xls/set-row-style! header-row (xls/create-cell-style! wb {:background :yellow,
                                                                 :font {:bold true}}))
      (xls/save-workbook! (format "resources/public/temp/report_%d.xlsx" survey-id) wb))))

(defn download-report-file [survey-id]
  (create-report-file survey-id)
  (redirect (format "/temp/report_%d.xlsx" survey-id)))

(defn signout []
  (session/clear!)
  (redirect "/signin"))

(def-restricted-routes app-routes
    (GET "/" [] (redirect "/surveys"))
    (GET "/surveys" [] (surveys (session/get :user_id)))
    (GET "/create" [] (create-survey-form))
    (GET "/signout" [] (signout))
    (POST "/create" {params :params} (create-survey params))
    (GET ["/detail/:id", :id #"[0-9]+"] [id]  (detail-survey (Integer/parseInt id)))
    (GET ["/delete/:id", :id #"[0-9]+"] [id] (delete-survey (Integer/parseInt id)))
    (GET ["/numbers/:id", :id #"[0-9]+"] [id] (view-number (Integer/parseInt id)))
    (GET ["/download/report/:id", :id #"[0-9]+"] [id] (download-report-file (Integer/parseInt id))))
