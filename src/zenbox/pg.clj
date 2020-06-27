(ns zenbox.pg
  (:import (com.zaxxer.hikari HikariConfig HikariDataSource)
           (java.util Properties)
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           (org.joda.time DateTime)
           java.text.SimpleDateFormat
           java.util.TimeZone
           [java.sql BatchUpdateException Date Timestamp PreparedStatement]
           [org.postgresql.jdbc PgArray]
           org.postgresql.util.PGobject)
  (:require [clojure.string :as str]
            [zenbox.utils]
            [cheshire.core :refer [generate-string parse-string]]
            [clojure.java.jdbc :as jdbc]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clojure.string :as str]))


(def time-fmt
  (->
   (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss")
   (.withZone (java.time.ZoneOffset/UTC))))


(defn- to-date [sql-time]
  (str (.format time-fmt (.toInstant sql-time)) "." (format "%06d"  (/ (.getNanos sql-time) 1000)) "Z"))

(defn- to-sql-date [clj-time]
  (tc/to-sql-time clj-time))


(extend-type java.util.Date
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (java.sql.Timestamp. (.getTime v)))))

(defn parse-int-range [s]
  (let [pair (-> (str/replace s #"\[|\]|\(|\)" "")
                 (str/split #","))]
    (mapv read-string pair)))


(extend-protocol jdbc/IResultSetReadColumn
  Date
  (result-set-read-column [v _ _] (.toString v))

  Timestamp
  (result-set-read-column [v _ _]
    (.toString (.toInstant v)))

  PgArray
  (result-set-read-column [v _ _] (vec (.getArray v)))

  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json"      (parse-string value true)
        "jsonb"     (parse-string value true)
        "int8range" (parse-int-range value)
        "citext" (str value)
        value))))

(extend-type java.util.Date
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (Timestamp. (.getTime v)))))

(defn to-pg-json [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(defn to-pg-array
  ([conn value & [sql-type]]
   (.createArrayOf conn (or sql-type "text") (into-array value)))
  ([value]
   (println "Create array without connection")
   (str "{" (clojure.string/join "," (map #(str "\"" % "\"") value)) "}")))


(extend-protocol jdbc/ISQLValue
  clojure.lang.Keyword
  (sql-value [value] (name value))
  org.joda.time.DateTime
  (sql-value [value] (to-sql-date value))
  java.util.Date
  (sql-value [value] (java.sql.Timestamp. (.getTime value)))
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-array value)))


(def defaults
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  10})

(defn create-pool [opts]
  (let [props (Properties.)]
    (.setProperty props "dataSourceClassName" "org.postgresql.ds.PGSimpleDataSource")
    (doseq [[k v] (merge defaults opts)]
      (when (and k v)
        (.setProperty props (zenbox.utils/propertize k) (str v))))
    (-> props
        HikariConfig.
        HikariDataSource.)))

(defn close-pool [datasource] (.close datasource))


(defn database-url [spec]
  (let [conn spec]
    (str "jdbc:postgresql://" (:host conn) ":" (or (:port! conn) (:port conn))
         "/" (:database conn)
         "?user=" (:user conn)
         "&password=" (:password conn) "&stringtype=unspecified")))

(defn datasource [spec]
  (let [ds-opts   (let [database-url (database-url spec)]
                    {:connection-timeout 30000
                     :idle-timeout 10000
                     :minimum-idle       0
                     :maximum-pool-size  30
                     :connection-init-sql "select 1"
                     :data-source.url   database-url})
        ds (create-pool ds-opts)]
    {:datasource ds}))

(defn close-connection [conn]
  (.close (:connection conn)))

(defn shutdown [{conn :datasource}]
  (close-pool conn))

(defn connection
  "open root connection"
  [db-spec]
  {:connection (jdbc/get-connection {:connection-uri (database-url db-spec)})})

(defn with-connection [db f]
  (if-let [conn (jdbc/db-find-connection db)]
    (f conn)
    (with-open [conn (jdbc/get-connection db)]
      (f conn))))

(defmacro from-start [start]
  `(- (System/currentTimeMillis) ~start))


(defn with-transaction [db f]
  (jdbc/with-db-transaction [conn db]
    (f conn)))


(defmacro pr-error [& body]
  `(try
     ~@body
     (catch java.sql.BatchUpdateException e#
       (if-let [ne# (.getNextException e#)] ;; rethrow exception containing SQL error
         (let [msg# (.getMessage ne#)]
           (throw (java.sql.SQLException. msg#)))
         (do
           (throw e#))))
     (catch org.postgresql.util.PSQLException e#
       (if-let [ne# (.getNextException e#)] ;; rethrow exception containing SQL error
         (let [msg# (.getMessage ne#)]
           (throw (java.sql.SQLException. msg#)))
         (do
           (throw e#))))))

(defn query
  "query honey SQL"
  ([db sql]
   (pr-error
    (let [start (System/currentTimeMillis)]
      (try
        (let [res (jdbc/query db sql)]
          ;; (println :query sql)
          res)
        (catch Exception e
          (println :query sql)
          (throw e))))))
  ([db h & more]
   (query db (into [h] more))))

(defn query-with-timeout
  "query honey SQL"
  ([db timeout sql]
   (pr-error
    (let [start (System/currentTimeMillis)]
      (try
        (with-connection db (fn [conn]
                              (let [ps  (let [ps (jdbc/prepare-statement conn (first sql))]
                                          (loop [ix 1 values (rest sql)]
                                            (when (seq values)
                                              (jdbc/set-parameter (first values) ps ix)
                                              (recur (inc ix) (rest values))))
                                          (.setQueryTimeout ps timeout)
                                          ps)
                                    res (jdbc/query conn ps)]
                                (println :query sql)
                                res)))
        (catch org.postgresql.util.PSQLException e
          (let [msg (let [m (.getMessage e)]
                      (if (= m "ERROR: canceling statement due to user request")
                        (str "Request time is longer than timeout - " timeout " sec")
                        m))]
            (println :error msg sql))
          (throw e))))))
  ([db timeout h & more]
   (query db (into [h] more))))

(defn query-first [db & hsql]
  (first (apply query db hsql)))

(defn query-value [db & hsql]
  (when-let [row (apply query-first db hsql)]
    (first (vals row))))

(defn execute!
  "execute honey SQL"
  [db sql]
  (pr-error
   (let [start (System/currentTimeMillis)]
     (try
       (let [res (jdbc/execute! db sql {:transaction? false})]
         (println :exec sql)
         res)
       (catch Exception e
         (println :err e)
         (throw e))))))

(defn exec!
  "execute raw SQL without escape processing"
  [db sql]
  (pr-error
   (let [start (System/currentTimeMillis)]
     (try
       (with-connection db
         (fn [con]
           (let [stmt (.prepareStatement con sql)
                 _    (.setEscapeProcessing stmt false)
                 res  (.execute stmt)]
             (println :exec sql)
             res)))
       (catch Exception e
         (println :error e)
         (throw e))))))

(defn retry-connection
  "open root connection"
  [db-spec & [max-retry timeout]]
  (let [max-retry (or max-retry 20)]
    (loop [retry-num max-retry]
      (let [res (try (let [conn (connection db-spec)] (query conn "SELECT 1") conn)
                     (catch Exception e
                       (println (str "Error while connecting to " (dissoc db-spec :password) " - " (.getMessage e)))))]
        (cond
          res res

          (> 0 retry-num)
          (let [msg (str "Unable to connect to " (dissoc db-spec :password))]
            (println msg)
            (throw (Exception. msg)))

          :else (do
                  (println "Retry connection to " (dissoc db-spec :password))
                  (Thread/sleep (or timeout 2000))
                  (recur (dec retry-num))))))))


(defn with-retry-connection
  [db-spec f & [max-retry timeout]]
  (with-open [c (:connection (retry-connection db-spec max-retry timeout))]
    (f {:connection c})))

(defn- coerce-entry [conn spec ent]
  (reduce (fn [acc [k v]]
            (assoc acc k (cond
                           (vector? v) (to-pg-array conn v (get-in spec [:columns k :type]))
                           (map? v)    (to-pg-json v)
                           :else v)))
          {} ent))

(defn insert [db {tbl :table :as spec} data]
  (let [values (if (vector? data) data [data])
        values (map #(coerce-entry db spec %) values)
        res (->> {:insert-into tbl
                  :values values
                  :returning [:*]}
                 (query db))]
    (if (vector? data) res (first res))))

(defn do-update [db {tbl :table pk :pk :as spec} data]
  (let [pk (or pk :id)]
    (->> {:update tbl
          :set (coerce-entry db spec (dissoc data :id))
          :where [:= pk (pk data)]
          :returning [:*]}
         (query-first db))))

(defn delete [db {tbl :table :as spec} id]
  (->> {:delete-from tbl :where [:= :id id] :returning [:*]}
       (query-first db)))


(defn quailified-name [tbl]
  (let [[i1 i2] (str/split (name tbl) #"\." 2)
        tbl (if i2 i2 i1)
        sch (if i2 i1 "public")]
    [sch tbl]))

(defn table-exists? [db tbl]
  (let [tbl (if (map? tbl) (:table tbl) tbl)
        [sch tbl] (quailified-name tbl)]
    (= 1
       (->> {:select [1]
             :from [:information_schema.tables]
             :where [:and [:= :table_schema sch] [:= :table_name (name tbl)]]}
            (query-value db)))))

(defn database-exists? [db db-name]
  (->> {:select [true]
        :from [:pg_database]
        :where [:= :datname (name db-name)]}
       (query-value db)))

(defn user-exists? [db user]
  (let [user (if (map? user) (:user user) user)]
    (->> {:select [true] :from [:pg_catalog.pg_roles] :where [:= :rolname user]}
         (query-value db)
         (some?))))

(defn create-user [db {user :user password :password}]
  (when-not (user-exists? db user)
    (exec! db (format "CREATE USER %s WITH ENCRYPTED PASSWORD '%s'" user password))))

(defn drop-user [db {user :user}]
  (exec! db (format "DROP USER IF EXISTS %s" user)))

(defn drop-database [db {dbname :database :as spec}]
  (exec! db (format "DROP DATABASE IF EXISTS %s" dbname)))
