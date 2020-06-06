(ns zenbox.operation)

;; --> {"jsonrpc": "2.0", "method": "subtract", "params": [42, 23], "id": 1}
;; <-- {"jsonrpc": "2.0", "result": 19, "id": 1}
;; --> {"jsonrpc": "2.0", "method": "subtract", "params": [23, 42], "id": 2}
;; <-- {"jsonrpc": "2.0", "result": -19, "id": 2}

(defmulti apply-operation (fn [cfg ctx request]
                            (:zen/op cfg)))

{:zen/op "keyword/as/string"}

(defmethod apply-operation
  :default
  [cfg ctx request]
  {:id    (:id request)
   :error {:status :zen.op/not-implemented
           :message (format "Operation %s is not implemented")}})
