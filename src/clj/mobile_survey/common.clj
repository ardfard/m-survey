(ns mobile-survey.common
    (require [net.cgrand.enlive-html :refer [deftemplate set-attr
                                             append content]]
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

