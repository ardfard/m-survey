(ns lobos.config
  (:use lobos.connectivity)
  (:require [mobile-survey.models.schema :as schema]))

(open-global schema/db-spec)
