(ns mobile-survey.auth
  (:require [mobile-survey.models.db :refer [get-id-for-email get-pass-for-id]]
            [mobile-survey.signin.validators :refer [user-credential-errors]]
            [mobile-survey.signin.java.validators :refer [email-domain-errors]]
            [mobile-survey.common :refer [template-path]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [compojure.core :refer [defroutes GET POST]]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.util.crypt :as crypt]))

(defn check-password-valid [email password]
    (and (= email "test@test.com") (= password "testpass")))

(defn authenticate-user [email password]
  (let [id (get-id-for-email email)
        real-pass (get-pass-for-id id)]
    (cond
      (boolean (user-credential-errors email password))
          (str "Please complete the form")
      (not id) "Email didn't exists"
      (not (crypt/compare password real-pass)) (str "Password for " email
          " is wrong!")
      :else (do (session/put! :user_id id)
                (resp/redirect "/surveys")))))

(defn logout []
  (session/clear!)
  (resp/redirect "/"))

(deftemplate signin-template (str template-path "signin.html") [])

(defn signin []
    (signin-template) )

(defroutes auth-routes
    (GET "/signin" [] (signin))
    (POST "/signin" [email password] (authenticate-user email password)))
