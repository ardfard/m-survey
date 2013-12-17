(ns mobile-survey.remotes
    (:require [mobile-survey.signin.java.validators :as v]
              [shoreleave.middleware.rpc :refer [defremote]]))

(defremote email-domain-errors [email]
    (v/email-domain-errors email))

