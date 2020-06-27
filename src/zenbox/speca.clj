(ns zenbox.speca
  (:require [clojure.alpha.spec :as s]))

(s/def :zen/string string?)

(s/register :user/name :zen/string)

(s/register :zenbox/User (s/schema [:user/name]))

;; (s/def :zenbox/User (s/schema [:user/name]))

;; (s/resolve-spec (cons 's/schema [:user/name]))

(:zenbox/User (s/registry))
(:user/name (s/registry))
(keys (s/registry))

(s/conform :user/name "ups")
(s/explain-data :user/name 1)

(s/explain-data :zenbox/User {:user/name "upss"})
(s/explain-data :zenbox/User {:user/name ["upss"]})


