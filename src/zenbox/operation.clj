(ns zenbox.operation)

;; --> {"jsonrpc": "2.0", "method": "subtract", "params": [42, 23], "id": 1}
;; <-- {"jsonrpc": "2.0", "result": 19, "id": 1}
;; --> {"jsonrpc": "2.0", "method": "subtract", "params": [23, 42], "id": 2}
;; <-- {"jsonrpc": "2.0", "result": -19, "id": 2}
;; see https://www.jsonrpc.org/specification

(defmulti apply-operation (fn [cfg ctx request]
                            (:type cfg)))


(defmethod apply-operation
  :default
  [cfg ctx request]
  {:id    (:id request)
   :error {:status :no-operation
           :message (format "Operation %s is not implemented" (:type cfg))}})
