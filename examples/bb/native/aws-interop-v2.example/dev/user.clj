(ns user
  (:require [sc.api]))

(comment
  (do
    (require '[local-server])
    (require '[local-example])
    (local-server/restart! {:routes local-example/routes})
    ))
