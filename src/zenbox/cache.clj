(ns zenbox.cache
  (:require [zenbox.utils]))

(defn get-value
  ([ctx k]
   (if-let [v (get @(:cache/state ctx) k)]
     v
     (throw (Exception. (str "No value in cache for " k)))))
  ([ctx k f]
   (if-let [state (:cache/state ctx)]
     (if-let [v (get @state k)]
       v
       (when-let [v (f)]
         (swap! state assoc k v)
         v))
     (do
       (println "WARN: No cache state provided!" k)
       (f)))))

(defn same-entry? [a b]
  (or (and a (identical? a b)) (= a b)))

(defn same-collections [xs ys]
  (loop [[x & xs] xs
         [y & ys] ys]
    (cond
      (and (nil? x) (nil? y)) true
      (same-entry? x y) (if (and (empty? xs) (empty? ys))
                          true
                          (recur xs ys))
      :else false)))

(defn get-by-identity [ctx k f & args]
  (if-let [cache (:cache/state ctx)]
    (let [{val :val src :src} (get @cache k)]
      (if (same-collections args src) 
        val
        (let [new-val (zenbox.utils/with-time (str "Cache " k) (apply f args))]
          (swap! cache assoc k {:src args :val new-val})
          new-val)))
    (do
      (println "WARN: no cache enabled!" k)
      (apply f args))))
