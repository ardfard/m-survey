(ns mobile-survey.models.schema
  (:use [lobos.core :only (defcommand migrate)])
  (:require [noir.io :as io]
            [lobos.migration :as lm]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "mobile_survey"
              :user "ardfard"
              :password "lalilulelo"
              })

(defcommand pending-migrations []
  (lm/pending-migrations db-spec sname))

(defn actualized?
  "checks if there aren't pending migrations"
  []
  (empty? (pending-migrations)))

(def actualize #(binding [lm/*src-directory* "src/clj"] (migrate)))
