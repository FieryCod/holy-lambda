(ns user
  (:require [sc.api]))

(comment

  ; start a local server
  (do
    (require '[local-server])
    (require '[local-example])
    (local-server/restart! {:routes local-example/routes}))


  )
