(ns zenbox.datomic
  (:require [datomic.api :as d]))


;; (def db-uri "datomic:dev://localhost:4334/hello")
(def db-uri "datomic:mem://zenbox")

(def user-schema
  [{:db/ident :user/name
    :db/valueType :db.type/string
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/password
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :user/telecoms
    :db/isComponent true
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident :user.telecom/system
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :user.telecom/value
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(comment
  (d/create-database db-uri)

  (def conn (d/connect db-uri))

  (println @(d/transact conn user-schema))
  (println )

  @(d/transact conn [{:db/ident :zen/type
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}
                     {:db/ident :zen.ent/Resource}])

  @(d/transact conn [{:db/ident :zen.ent/User
                      :zen/type :zen.ent/Resource}
                     {:db/ident :zen.ent/Patient
                      :zen/type :zen.ent/Resource}])

  (def db (d/db conn))

  (d/q '[:find (pull ?e [*])
         :where [?e :zen/type :zen.ent/User]] db)

  @(d/transact conn [{:db/id #db/id[:db.part/user]
                      :zen/type :zen.ent/User
                      :user/name "niquola"
                      :user/password "secret"
                      :user/telecoms [{:user.telecom/system "phone"
                                       :user.telecom/value "911999"}]}])

  @(d/transact conn [{:db/id #db/id[:db.part/user]
                      :zen/type :zen.ent/User
                      :user/name "rich"
                      :user/password "super"}])


  )
