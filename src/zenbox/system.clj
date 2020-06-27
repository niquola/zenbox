(ns zenbox.system
  (:require [zenbox.hooks :as hooks]))

(hooks/define-hook
  :system/start
  {:args [:context]
   :return :context})

(defn default-services []
  {:hooks {:system/start {:zen.db/pool {}
                          :zen.web/start {}
                          :zen.jobs/start {}
                          :zen.cache/start {}}}})

(defn start [config]
  (let [ctx (zenbox.context/make-context config)]
    (hooks/call-hooks :system/start ctx {})))

(hooks/define-hook
  :system/stop
  {:args [:context]
   :return :context})

(defn stop [state]
  (hooks/notify-hooks :system/stop @state))


