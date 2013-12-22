(ns mobile-survey.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [mobile-survey.models.schema :as schema]))

(defdb db schema/db-spec)

(declare users surveys numbers)

(defentity users
  (has-many surveys {:fk :user_id}))

(defentity surveys
  (has-many numbers {:fk :survey_id})
  (belongs-to users))

(defentity numbers
  (belongs-to surveys))

(defn create-user! [user]
  (insert users
          (values user)))

(defn update-user-for-id! [id param]
  (update users
    (set-fields param)
    (where {:id id})))

(defn create-survey! [survey]
  (insert surveys
          (values survey)))

(defn get-survey-id [user-id name]
  (:id (first (select surveys
          (where {:name name
                  :user_id user-id})
          (limit 1)))))

(defn get-survey-with-id [id]
  (first (select surveys
               (where {:id id})
               (limit 1))))

(defn delete-survey! [survey-id]
  (delete surveys
          (where {:id survey-id})
          (limit 1)))

(defn create-number! [number]
  (insert numbers
          (values number)))

(defn get-numbers-for-survey [survey-id]
  (for [{number :number} (select numbers
                            (fields :number)
                            (where {:survey_id survey-id}))]
    number))

(defn get-id-for-email [email]
  (:id (first (select users
                 (fields :id)
                 (where {:email email})
                 (limit 1)))))

(defn get-email [id]
  (:email (first (select users
                   (fields :email)
                   (where {:id id})))))

(defn get-surveys [id]
  (select surveys
              (where {:user_id id})))

(defn update-surveys! [id param]
  (update surveys
          (set-fields param)
          (where {:id id})))

(defn get-pass-for-id [id]
  (:pass (first(select users
                 (fields :pass)
                 (where {:id id})))))
