(ns zenbox.match)

(defn match [pattern obj]
  (or (= obj pattern)
      (when (and (map? pattern) (map? obj))
        (loop [[[k pv] & kvs] pattern]
          (let [iv (get obj k)]
            (cond
              (nil? k) true
              (nil? iv) false
              (or (= pv iv)
                  (and (map? iv) (map? pv) (match  pv iv)))
              (recur kvs)

              (and (sequential? pv) (sequential? iv)
                   (->> pv
                        (every? (fn [pv'] (->> iv (some #(or (= pv' %) (match pv' %))))))))
              (recur kvs)

              :else false))))))
