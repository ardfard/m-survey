(ns mobile-survey.common
    (require [net.cgrand.enlive-html :refer [deftemplate set-attr
                                             append content]]
             [mobile-survey.models.db :as models]
             [noir.session :as session]))


