(ns mobile-survey.core
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [net.cgrand.enlive-html :refer [deftemplate content defsnippet
                                            clone-for set-attr append do->
                                            nth-of-type]]
            [hiccup.core :refer [html]]
            [noir.response :refer [redirect]]
            [noir.session :as session]
            [noir.util.route :refer [def-restricted-routes]]
            [mobile-survey.common :refer [template-path base]]
            [mobile-survey.models.db :as models]
            [clj-time.format :as time-format]
            [clj-time.core :refer [date-time]]
            [clj-time.coerce :refer [from-date]]))


(defn create-js-script-for [view]
    (cond
        (= view :create) '( {:tag :script, :attrs {:src "js/mobile-survey.js"}}
                            {:tag :script, :content ("mobile_survey.create_survey.init()")})
        (= view :surveys) '({:tag :script, :attrs {:src "js/mobile-survey.js"}})))

(def status_code {0 "On progress", 1 "Complete"})

(defsnippet list-survey (str template-path "list-survey.html") [:#survey-list]
    [surveys]
    [[:tr.survey-item (nth-of-type 1)]]
    (clone-for [{:keys [id name created_on status]} surveys]
        [:td.survey-id] (content (str id))
        [:td.name] (content name)
        [:td.createdOn] (content (let [formatter (time-format/formatter "dd/MM/yyyy")]
                                 (time-format/unparse formatter (from-date created_on))))
        [:td.status] (content (status_code status))
        [:td :> :a.preview] (set-attr :href (str "detail/" id))
        [:td :> :a.delete] (set-attr :href (str "delete/" id))))

(defsnippet create-snippet (str template-path "create-survey.html") [:#create-form]
    [])

(defsnippet detail-snippet (str template-path "detail.html") [:#survey-info]
    [{:keys [name description created-on status]} number-count number-replied]
    [:#name] (content name)
    [:#description] (content description)
    [:#duration] (content created-on)
    [:#progress] (do-> (set-attr {:value number-replied :max number-count})
                    (content (str number-replied " of " number-count " responded.")))
    )

(defn surveys [user-id]
    (let [survey-list (models/get-surveys user-id)]
      (base (list-survey survey-list) (create-js-script-for :surveys))))

(defn create-survey-form []
    (base (create-snippet) (create-js-script-for :create) ))

(defn create-survey-helper [values]
  (models/create-survey! values)
  (models/get-survey-id (:user_id values) (:name values)))

(defn create-survey [user-id name descriptions content incentiveOption numbersText numbersFile]
    (if (= numbersText nil)
        "Number is nil"
        (let [survey-id (create-survey-helper {:name name
                                :description descriptions
                                :content content
                                :incentive 0
                                :user_id user-id
                                })]
            (doseq [number numbersText]
              (models/create-number! {:survey_id survey-id
                                     :number number
                                     :reply "Kosong"})))))

(defn detail-survey [id]
    (let [survey (models/get-survey-with-id id)]
        (base (detail-snippet survey 10 100))))

(defn root [handler]
    (fn [request]
        (when (= (:uri request) "/")
            "mobile_survey/templates/create-survey.html"
            (handler request))))

(def-restricted-routes app-routes
    (GET "/" [] (redirect "/surveys"))
    (GET "/surveys" [] (surveys (session/get :user_id)))
    (GET "/create" [] (create-survey-form))
    (POST "/create" [name descriptions content incentiveOption numbersText numbersFile]
        (create-survey (session/get :user_id) name descriptions content incentiveOption numbersText numbersFile))
    (GET ["/detail/:id", :id #"[0-9]+"] [id]  (detail-survey (int id))))
