(ns mobile-survey.templates
    (require [net.cgrand.enlive-html :refer [deftemplate content defsnippet
                                            clone-for set-attr append do->
                                            nth-of-type]]
             [clj-time.format :as time-format]
             [clj-time.coerce :refer [from-date]]
             [mobile-survey.models.db :as models]
             [noir.session :as session]))

(def template-path "mobile_survey/templates/")

(defn create-link-for-user [user-id]
  (models/get-email user-id))

(deftemplate base (str template-path "base.html")
    [main js-script]
    [:#hdr-accountInfo] (content (create-link-for-user (session/get :user_id)))
    [:#main] (content main)
    [:#link-to-surveys] (set-attr :href "/surveys")
    [:#link-to-create-surveys] (set-attr :href "/create")
    [:body] (append js-script))

(deftemplate signin-template (str template-path "signin.html") [])

(defsnippet list-survey (str template-path "list-survey.html") [:#survey-list]
    [surveys]
    [[:tr.survey-item (nth-of-type 1)]]
    (clone-for [{:keys [id name created_on status]} surveys]
        [:td.survey-id] (content (str id))
        [:td.name] (content name)
        [:td.createdOn] (content (let [formatter (time-format/formatter "dd/MM/yyyy")]
                                 (time-format/unparse formatter (from-date created_on))))
        [:td.status] (content status)
        [:td :> :a.preview] (set-attr :href (str "/detail/" id))
        [:td :> :a.delete] (set-attr :href (str "/delete/" id))))

(defsnippet create-snippet (str template-path "create-survey.html") [:#create-form]
    [])

(defsnippet detail-snippet (str template-path "detail.html") [:#survey-info]
    [id name description cont duration status number-count number-replied]
    [:td.name] (content name)
    [:td.description] (content description)
    [:td.content] (content cont)
    [:td.duration] (content duration)
    [:td :> :a.view-number] (set-attr :href (str "/numbers/" id))
    [:progress.progress] (set-attr :value number-replied :max number-count)
    [:span.progress-num] (content (str number-replied " of " number-count " responded."))
    )

(defsnippet numbers-snippet (str template-path "numbers.html") [:#numbers-table]
  [numbers]
  [[:tr.item-row (nth-of-type 1)]]
  (clone-for [[idx number] (map-indexed vector numbers)]
    [:td.item-idx] (content (str (inc idx)))
    [:td.item-number] (content (number :number))
    [:td.item-reply] (content (number :reply))))