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
             [mobile-survey.sms :as sms]
             [lobos.config :as dbconf]
             ))

(defn init
    "runs when the application starts and checks if the database
     schema exists, class schema actualize if not."
     []
     (dbconf/init)
     #_(sms/start-subscribe)
     (if-not (schema/actualized?)
        (schema/actualize)))

(defroutes static-routes
    (route/resources "/")
    (route/not-found "Page not found"))

(defn destroy []
    (println "shutting down..."))

(def all-routes [auth-routes app-routes static-routes])

(defn user-access [request]
    (session/get :user_id))

(def app (-> (middleware/app-handler all-routes :access-rules [{:rule user-access
                                                                :redirect "/signin"}])
             (wrap-rpc)
             (wrap-params)
             (site)))
