(ns mobile-survey.handler
    (require [mobile-survey.models.schema :as schema]
             [mobile-survey.core :refer [app-routes]]
             [mobile-survey.auth :refer [auth-routes]]
             [noir.util.middleware :as middleware]
             [noir.session :as session]
             [compojure.handler :refer [site]]
             [compojure.route :as route]
             [compojure.core :refer [defroutes]]
             [shoreleave.middleware.rpc :refer [wrap-rpc]]
             [ring.middleware.params :refer [wrap-params]]
             [mobile-survey.sms :refer [sms-routes listen-survey-replies]]))

(defn init
    "runs when the application starts and checks if the database
     schema exists, class schema actualize if not."
     []
     (if-not (schema/actualized?)
        (schema/actualize))
     (listen-survey-replies))

(defroutes static-routes
    (route/resources "/")
    (route/not-found "Page not found"))

(defn destroy []
    (println "shutting down..."))

(def all-routes [auth-routes app-routes sms-routes static-routes])

(defn user-access [request]
    (session/get :user_id))

(def app (-> (middleware/app-handler all-routes :access-rules [{:rule user-access
                                                                :redirect "/signin"}])
             (wrap-rpc)
             (wrap-params)
             (site)))
