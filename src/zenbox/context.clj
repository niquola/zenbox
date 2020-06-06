(ns zenbox.context)

;; context is a container for all metadata
;; it is map in atom which contains ref to themself
;; for runtime reload

(defn make-context [initial-map]
  (let [state (atom initial-map)]
    (swap! state assoc :zen/state state)
    state))
