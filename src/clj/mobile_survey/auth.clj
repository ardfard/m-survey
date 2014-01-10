(ns mobile-survey.auth
  (:require [mobile-survey.models.db :refer [get-id-for-email get-pass-for-id]]
            [mobile-survey.signin.validators :refer [user-credential-errors]]
            [mobile-survey.signin.java.validators :refer [email-domain-errors]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [mobile-survey.templates :as t]
            [compojure.core :refer [defroutes GET POST]]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.util.crypt :as crypt]))

(defn signin-failed [message]
  (do (session/flash-put! :message message)
      (resp/redirect "/signin")))

(defn authenticate-user [email password]
  (let [id (get-id-for-email email)
        real-pass (get-pass-for-id id)]
    (cond
      (boolean (user-credential-errors email password))
          (signin-failed "Email/Password is invalid!")
      (not id) (signin-failed "Email didn't exists!")
      (not (crypt/compare password real-pass)) (signin-failed (str "Password for " email
          " is wrong!"))
      :else (do (session/put! :user_id id)
                (resp/redirect "/surveys")))))

(defn logout []
  (session/clear!)
  (resp/redirect "/"))


(defn signin []
    (t/signin-template) )

(defroutes auth-routes
    (GET "/signin" [] (signin))
    (POST "/signin" [email password] (authenticate-user email password)))
