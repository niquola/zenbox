(ns zenbox.utils
  (:require [clojure.string :as str]))

(defn deep-merge [m1 m2 & [ms]])

(defn upcase [^String s]
  (str
   (.toUpperCase (.substring s 0 1))
   (.substring s 1)))

(defn propertize [k]
  (let [parts (str/split (name k) #"-")]
    (str (first parts) (str/join "" (map upcase (rest parts))))))

(defmacro with-time
  [msg expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (println (str  ~msg " [" (/ (double (- (. System (nanoTime)) start#)) 1000000.0) "ms]"))
     ret#))
