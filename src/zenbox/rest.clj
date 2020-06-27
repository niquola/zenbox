(ns zenbox.rest
  (:require
   [clojure.string :as str]
   [org.httpkit.server :as http-kit]
   [ring.util.codec]
   [ring.middleware.cookies :as cookies]
   [clojure.java.io :as io]
   [cheshire.core]
   [ring.util.response]
   [ring.util.request]
   [ring.middleware.head]
   [ring.util.codec :as codec]
   [zenbox.events]
   [zenbox.hooks]
   [clj-yaml.core])
  (:use [ring.middleware.resource]
        [ring.middleware.file]
        [ring.middleware.content-type]
        [ring.middleware.not-modified]))

(defn form-decode [s] (clojure.walk/keywordize-keys (ring.util.codec/form-decode s)))

(defn prepare-request [{meth :request-method qs :query-string body :body ct :content-type headers :headers :as req}]
  (let [params (when qs (form-decode qs))
        params (if (string? params) {(keyword params) nil} params)]
    (cond-> req params (update :params merge (or params {})))))


(defn handle-static [h {meth :request-method uri :uri :as req}]
  (if (and (#{:get :head} meth)
           (or (str/starts-with? (or uri "") "/static/")
               (str/starts-with? (or uri "") "/favicon.ico")))
    (let [opts {:root "public"
                :index-files? true
                :allow-symlinks? true}
          path (subs (codec/url-decode (:uri req)) 8)]
      (-> (ring.util.response/resource-response path opts)
          (ring.middleware.head/head-response req)))
    (h req)))

(defn wrap-static [h]
  (fn [req]
    (handle-static h req)))

(defn format-mw [h]
  (fn [req]
    (let [resp (h (prepare-request req))]
      (if-let [b (:body resp)]
        (->
         resp
         (assoc :body (cheshire.core/generate-string b))
         (assoc-in [:headers "content-type"] "application/json"))
        resp))))

(def manifest
  {:entities {:web-server {:attrs {:http-kit {:attrs {:port {:type "integer" :default 8080}
                                                      :worker-name-prefix {:type "string"}
                                                      :thread {:type "integer"}
                                                      :max-body {:type "integer"}}}
                                   :static-dir {:type "string"}}}
              :config {:attrs {:rest {:type "web-server"}}}}
   :hooks {:system/start {:rest/start {}}
           :system/stop  {:rest/stop {}}}})

(zenbox.hooks/define-hook :rest/start {})
(zenbox.hooks/define-hook :rest/stop {})

(defmethod zenbox.hooks/hook
  :rest/start
  [ctx-atom]
  (when-let [cfg (get-in @ctx-atom [:config :rest])]
    (swap! ctx-atom assoc-in [:services :rest] :server)))

(defmethod zenbox.hooks/hook
  :rest/stop
  [ctx-atom]
  (when-let [srv (get-in @ctx-atom [:services :rest])]
    ctx-atom))

(defn start
  "start server with dynamic metadata"
  [config dispatch]
  (let [web-config (merge {:port 8080
                           :worker-name-prefix "w"
                           :thread 8
                           :max-body 20971520} config)
        handler (-> dispatch
                    format-mw
                    (wrap-static)
                    (wrap-content-type {:mime-types {nil "text/html"}})
                    wrap-not-modified)]
    (println "Starting web server: \n" (clj-yaml.core/generate-string web-config) "\n")
    (http-kit/run-server handler web-config)))


(defn stop [server]
  (server))

