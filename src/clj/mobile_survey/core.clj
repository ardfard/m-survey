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
            [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clj-time.coerce :refer [from-date]]
            [dk.ative.docjure.spreadsheet :as xls]))


(defn create-js-script-for [view]
    (cond
        (= view :create) '( {:tag :script, :attrs {:src "js/mobile-survey.js"}}
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
    (t/base (t/create-snippet) (create-js-script-for :create) ))

(defn create-survey-helper [values]
  (models/create-survey! values)
  (models/get-survey-id (:user_id values) (:name values)))

(defn handle-file-number [file-number]
    (let [file-num (io/upload-file "/" file-number)]
      (get-numbers-from-file file-num)))

(defn create-survey [user-id name descriptions content selectNumber incentiveOption numbersText numbersFile]
  (let [survey-id (create-survey-helper {:name name
                          :description descriptions
                          :content content
                          :incentive 0
                          :user_id user-id
                          })
        numbers (if (= selectNumber "file-text")
                    numbersText
                    (handle-file-number numbersFile))]
      (doseq [number numbers]
        (models/create-number! {:survey_id survey-id
                               :number number
                               :reply "Kosong"}))
      (redirect "/surveys")))

(defn detail-survey [id]
    (let [{:keys [name description created_on content status]} (models/get-survey-with-id id)
          duration (get-duration (from-date created_on))]
        (t/base (t/detail-snippet name description content duration (status_code status) 100 10)
                (create-js-script-for :detail))))

(defn delete-survey [id]
  (models/delete-survey! id)
  (redirect "/surveys"))

(defn signout []
  (session/clear!)
  (redirect "/signin"))

(def-restricted-routes app-routes
    (GET "/" [] (redirect "/surveys"))
    (GET "/surveys" [] (surveys (session/get :user_id)))
    (GET "/create" [] (create-survey-form))
    (GET "/signout" [] (signout))
    (POST "/create" [name descriptions content incentiveOption selectNumber numbersText numbersFile]
        (create-survey (session/get :user_id) name descriptions content selectNumber incentiveOption numbersText numbersFile))
    (GET ["/detail/:id", :id #"[0-9]+"] [id]  (detail-survey (Integer/parseInt id)))
    (GET ["/delete/:id", :id #"[0-9]+"] [id] (delete-survey (Integer/parseInt id))))
