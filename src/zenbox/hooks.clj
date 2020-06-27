(ns zenbox.hooks
  (:require [zenbox.match :refer [match]]))

(defmulti hook (fn [k ctx payload]  k))

(defn define-hook [name schema])

(defmethod hook :default
  [k _ _]
  (println "No hook for " k))

(defn get-hooks [ev ctx payload]
  (->>
   (if (vector? ev) ev [ev])
   (reduce
    (fn [acc ev]
      (if-let [hooks (get-in ctx [:hooks ev])]
        (->> hooks
             (reduce
              (fn [acc [k v]]
                (if-let [mtch (:hooks/match v)]
                  (if (match mtch payload)
                    (conj acc [ev k v])
                    acc)
                  (conj acc [ev k v])))
              acc))
        acc))
    [])))

(defn call-hooks
  "ev is keyword of event or vector of event names
  call hooks and return result; each hook should return payload (maybe modified)
  :hook/event passed
  :hook/def is hook configuration
  "
  [ev ctx payload]
  (let [hooks (get-hooks ev ctx payload)]
    ;; (println "Hooks" hooks)
    (reduce (fn [payload [ev k v]] (hook k ctx (assoc payload :hooks/event ev :hooks/def v)))
            payload hooks)))

(defn notify-hooks
  "ev is keyword of event or vector of event names
  Execute hooks from (:hooks ctx)
  :hook/event passed
  :hook/def is hook configuration
  "
  [ev ctx payload]
  (let [hooks (get-hooks ev ctx payload)]
    (doseq [[ev k v] hooks]
      (hook k ctx (assoc payload :hooks/event ev :hooks/def v))
      :ok)))
