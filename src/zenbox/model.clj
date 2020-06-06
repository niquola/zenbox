(ns zenbox.model)

(def manifest
  {:ns "zen"
   :entities {:string     {:type "primitive"}
              :integer    {:type "primitive"}
              :identifier {:type "primitive"}
              :boolean    {:type "primitive"}
              :path       {:type "primitve"}
              :code       {:type "primitive"}
              :Reference  {:type "type"
                           :attrs {:id {:type "identifier"}
                                   :type {:type "identifier"}
                                   :display {:type "string"}}}
              :Coding     {:attrs {:code {:type "string"}
                                   :system {:type "string"}}}
              :Identifier {:attrs {:system {:type "string"}
                                   :value {:type "string"}}}
              :Meta       {:attrs {:version {:type "integer"}
                                   :updated {:type "timestamp"}
                                   :created {:type "timestamp"}}}
              :Resource   {:attrs {:resourceType {:type "identifier"}
                                   :id {:type "identifier"}
                                   :meta {:type "Meta"}}}
              :Concept    {:isa ["Resource"]
                           :storage ["jsonb" "memory"]
                           :attrs {:code {:type "string"}
                                   :system {:type "string"}}}
              :Entity    {:isa ["Resource"]
                          :storage ["jsonb" "memory"]
                          :attrs {:id {:type "identifier" :constraints {:required true}}
                                  :type {:type "code" :constraints {:enum "zen.entity.type"}}
                                  :storage {:type "code"}
                                  :isa {:type "identifier" :collection true}}}
              :Attribute {:isa ["Resource"]
                          :storage ["jsonb" "memory"]
                          :attrs {:entity {:type "Reference" :constraints {:refers "zen.entity-ref" :required true}}
                                  :type   {:type "Reference" :constraints {:refers "zen.entity-ref" :required true}}
                                  :path   {:type "path" :constraints {:required true}}
                                  :collection {:type "boolean"}}}
              :Constraint {:isa ["Resource"]
                           :storage ["jsonb" "memory"]
                           :attrs {:entity   {:type "Reference" :constraints {:refers "zen.entity-ref" :required true}}
                                   :path     {:type "path"}
                                   :refers   {:type "url"}
                                   :enum   {:type "url"}
                                   :required {:type "boolean"}}}
              :Operation {:isa ["Resource"]
                          :storage ["jsonb" "memory"]
                          :attrs {:description {:type "string"}
                                  :config   {:type "Schema"}
                                  :request  {:type "Schema"}
                                  :response {:type "Schema"}}}

              :Transform {:isa ["Resource"]
                          :storage ["jsonb" "memory"]
                          :open true
                          :attrs {:engine {:type "code" :constraints {:enum "zen.transform.engine"}}}}

              :Route   {:is ["Resource"]
                        :storage ["jsonb" "memory"]
                        :attrs {:operation {:type "Reference" :constraints {:refers "zen.operation-ref"}}}}}

   :concepts {"zen.entity.type"    [{:code "primitive"}]
              "zen.entity-ref"     [{:code "Entity"}]
              "zen.operation-ref"  [{:code "Operation"}]
              "zen.entity.storage" [{:code "jsonb"} {:code "memory"}]}})

(comment
  ;; manifest

  {:id "namespace"
   :entities {}

   }



  )
