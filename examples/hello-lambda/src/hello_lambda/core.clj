(ns hello-lambda.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h]))

(h/deflambda HelloLambda
  [event context]
  (h/info "Logging...")
  (h/info "Event body" event)
  (h/info "Event context" context)
  {:statusCode 200
   :body {:message "Hello from Eugene Koontz`!!!!! Heerlijke kerst!! .."
          "it's" "me"
          :you "looking"
          :for true}
   :isBase64Encoded false
   :headers {"Content-Type" "application/json"}})

(h/deflambda ByeLambda
  [event context]
  (h/info "Event body" event)
  (h/info "Event context" context)
  {:statusCode 200
   :body "Bye bye"
   :isBase64Encoded false
   :headers {"Content-Type" "application/json"}})

(h/gen-main [#'HelloLambda
             #'ByeLambda])
