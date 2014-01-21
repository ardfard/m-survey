(ns mobile-survey.sms
  (:use  compojure.core)
  (:require [clj-msgpack.core :as mp]
            [mobile-survey.models.db :as models]
            [cheshire.core :as json]
            [iron-mq-clojure.client :as mq]))

(def client (let [{:strs [token project_id]} (json/parse-stream (clojure.java.io/reader "iron.json"))]
              (mq/create-client token project_id)))

(defn message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[consumer] Received a message: %r, delivery-tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn survey-reply-handler [id number reply]
    (models/update-reply! number id reply))

#_(defn- publish-survey-to-queue! [{:keys [id content]} responden channel queue]
  (lb/publish channel "" queue (mp/pack {"id" id "content" content "number" responden})
              :content-type "application/json"
              :type nil))

(defn publish-survey! [{:keys [id content]} numbers]
  (let [messages (map mq/create-message
                      (for [number numbers]
                        (json/generate-string {"id" id "content" content "number" number})))]
    (println messages)
    (apply mq/post-messages client "mobile-survey.survey" messages)))

(defn get-all-messages-from-queue []
  (let [messages (mq/get-messages client "mobile-survey.reply")]
    (doseq [message messages]
      (let [{:keys [id number reply]} (get "body" message)]
        (survey-reply-handler id number reply)
        (mq/delete-message client "mobile-survey.reply" message)))))

(defn handle-reply-post-message [body]
  (println "handler incoming message...")
  (println body)
  (let [{:strs [id number reply]} (json/parse-string body)]
    (survey-reply-handler id number reply)
    ""))

(defroutes sms-handler-routes
  (POST "/sms-handler" {body :body} (handle-reply-post-message (slurp body))))