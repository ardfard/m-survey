(ns lobos.migrations
  (:refer-clojure :exclude
                  [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema config)
        (lobos [helper :only [tbl refer-to]])))

(defmigration add-users-table
  (up [] (create
          (tbl :users
                 (varchar :first_name 127)
                 (varchar :last_name 127)
                 (varchar :email 127 :unique)
                 (time    :last_login)
                 (varchar :pass 127))))
  (down [] (drop (table :users))))

(defmigration add-surveys-table
  (up [] (create
           (->
            (tbl :surveys
              (varchar :name 127)
              (varchar :content 255)
              (varchar :description 255)
              (smallint :status)
              (smallint :incentive))
            (refer-to :users))))
  (down [] (drop (table :surveys))))

(defmigration add-numbers-table
  (up [] (create
           (->
             (tbl :numbers
                  (varchar :number 127)
                  (varchar :reply 127))
             (refer-to :surveys))))
  (down [] (drop (table :numbers))))

