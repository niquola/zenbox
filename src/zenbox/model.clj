(ns zenbox.model)

(def zen-manifest
  {:ns "zen"
   :entities {:string     {:type "primitive"}
              :integer    {:type "primitive"}
              :identifier {:type "primitive"}
              :boolean    {:type "primitive"}
              :path       {:type "primitve"}
              :code       {:type "primitive"}
              :Reference  {:attrs {:id {:type "identifier"}
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
                          :attrs {:id {:type "identifier" :required true}
                                  :type {:type "code" :enum ["primitive"]}
                                  :storage {:type "code"}
                                  :isa {:type "identifier" :collection true}}}

              :Attribute {:isa ["Resource"]
                          :storage ["jsonb" "memory"]
                          :attrs {:entity {:type "Reference" :refers ["Entity"] :required true}
                                  :type   {:type "Reference" :refers ["Entity"] :required true}
                                  :path   {:type "path" :required true}
                                  :collection {:type "boolean"}}}
              ;; ????
              :Constraint {:isa ["Resource"]
                           :storage ["jsonb" "memory"]
                           :attrs {:entity   {:type "Reference" :refers ["Entity"] :required true}
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
                          :attrs {:engine {:type "code" :valueset "zen.transform.engine"}}}

              :Route   {:is ["Resource"]
                        :storage ["jsonb" "memory"]
                        :attrs {:operation {:type "Reference" :refers ["Operation"]}}}}})

(comment
  ;; manifest

  {:id "namespace"
   :entities {}

   }



  )
