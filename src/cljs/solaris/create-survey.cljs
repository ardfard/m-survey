(ns mobile-survey.create-survey
  (:require [domina :refer [by-id attr set-attr! remove-attr!]]
            [domina.events :refer [listen!]]))

(defn disable-input-number-dom! [element]
  (let [text-dom (by-id "numbers-input")
        file-dom (by-id "upload")]
    (doseq [e [text-dom file-dom]]
      (set-attr! e :disabled "disabled"))
    (cond
      (= element :text) (remove-attr! text-dom :disabled)
      (= element :file) (remove-attr! file-dom :disabled))))

(defn ^:export init []
  (listen! (by-id "input-from-text-radio") :change (fn [] (disable-input-number-dom! :text)))
  (listen! (by-id "input-from-file-radio") :change (fn [] (disable-input-number-dom! :file))))
