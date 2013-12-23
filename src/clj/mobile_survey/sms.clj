(ns mobile-survey.sms
  (:use [liberator.core :only [resource defresource]]
        compojure.core)
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [langohr.basic :as lb]
            [clj-msgpack.core :as mp]
            [mobile-survey.models.db :as models]))

(defresource test_liberator [name]
  :available-media-types ["text/plain"]
  :handle-ok (fn [_] (str "hello, " name)))

(defresource register-sms-handler
  :allowed-methods [:post]
  :available-media-types ["text/plain"]
  :exists? (fn [ctx]
             (println (get-in ctx [:request :params :password]))
             (= "ulima" (get-in ctx [:request :params :password])))
  :handle-ok (fn [ctx] (str (ctx :request)))
  :post! (fn [ctx]
           (println (str (get-in ctx [:request :params :password]))))
  )

(defroutes sms-routes
  (ANY "/test/:name" [name] (test_liberator name))
  (ANY "/register-handler" [] register-sms-handler))


(defn message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[consumer] Received a message: %r, delivery-tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn survey-reply-handler [_ _ ^bytes body]
  (let [{:strs [id number reply]} (first (mp/unpack body))]
    (models/update-reply! number id reply)))

(defn- publish-survey-to-queue! [{:keys [id content]} responden channel queue]
  (lb/publish channel "" queue (mp/pack {"id" id "content" content "number" responden})
              :content-type "application/json"
              :type nil))

(defn test-publish []
  (let [conn (rmq/connect {:host "oppinet.ppms.itb.ac.id"
                           :username "mobile-survey"
                           :password "lalilulelo"
                           })
        ch (lch/open conn)
        queue-name "mobile-survey.sms"]
    (lq/declare ch queue-name :durable true :exclusive false :auto-delete false)
    (publish-survey-to-queue! {:id 1 :content "test survey" } "+6285793268587" ch queue-name)
    (rmq/close ch)
    (rmq/close conn)))

(defn publish-survey! [{:keys [id content]} numbers]
  (let [conn (rmq/connect {:host "oppinet.ppms.itb.ac.id"
                           :username "mobile-survey"
                           :password "lalilulelo"
                           })
        ch (lch/open conn)
        queue-name "mobile-survey.sms"]
    (lq/declare ch queue-name :durable true :exclusive false :auto-delete false)
    (doseq [number numbers]
      (publish-survey-to-queue! {:id id :content content} number ch queue-name))
    (rmq/close ch)
    (rmq/close conn)))

(defn listen-survey-replies []
  (let [conn (rmq/connect {:host "oppinet.ppms.itb.ac.id"
                           :username "mobile-survey"
                           :password "lalilulelo"
                           })
        ch (lch/open conn)
        queue-name "mobile-survey.reply"]
    (lq/declare ch queue-name :durable true :exclusive false :auto-delete false)
    (println "Start listening to replies ...")
    (lc/subscribe ch queue-name survey-reply-handler :auto-ack true)))

(defn start-subscribe []
  (let [conn (rmq/connect {:host "oppinet.ppms.itb.ac.id"
                           :username "ardfard"
                           :password "lalilulelo"})
        ch (lch/open conn)
        queue-name "hello"]
    (println (format "[main] Connected. Channel id: %d" (.getChannelNumber ch)))
    (lq/declare ch queue-name :durable false :auto-delete false)
    (println "Press CTRL+C to cancel.")
    (
     lc/subscribe ch queue-name message-handler :auto-ack true)
  ))