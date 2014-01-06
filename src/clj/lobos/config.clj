(ns lobos.config
  (:use lobos.connectivity)
  (:require [environ.core :as environ]
            [clojure.string :as str])
  (:import [java.net URI]))

(defn get-db-spec-from-env
  []
  (when (environ/env :database-url)
    (let [url (URI. (environ/env :database-url))
          host (.getHost url)
          port (if (pos? (.getPort url))
                 (.getPort url)
                 (5432))
          path (.getPath url)]
      (merge
        {:subname (str "//" host ":" port path)}
        (when-let [user-info (.getUserInfo url)]
          {:user (first (str/split user-info #":"))
           :password (second (str/split user-info #":"))})))))

(defn open-global-when-necessary
  [db-spec]
    ;; If the connection credentials has changed, close the connection.
  (when (and (@lobos.connectivity/global-connections :default-connection)
             (not= (:db-spec (@lobos.connectivity/global-connections :default-connection)) db-spec))
    (lobos.connectivity/close-global))
  ;; Open a new connection or return the existing one.
  (if (nil? (@lobos.connectivity/global-connections :default-connection))
    ((lobos.connectivity/open-global db-spec) :default-connection)
    (@lobos.connectivity/global-connections :default-connection)))

(def db-spec (merge {:classname "org.postgresql.Driver"
                     :subprotocol "postgresql"
                     :subname "//localhost:5432/mobile_survey"
                     :user "ardfard"
                     :password "lalilulelo"}
                    (get-db-spec-from-env)))

(defn init []
  (open-global-when-necessary db-spec))
