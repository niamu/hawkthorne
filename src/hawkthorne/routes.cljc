(ns hawkthorne.routes
  (:require [domkm.silk :as silk]))

(def routes
  ^{:doc "Route definitions. Not all routes need to be shared to the client."}
  (silk/routes {:lobby      [[]]
                :game       [["game"]]
                :api        [["api"]]
                :css        [["css" "screen.css"]]
                :websocket  [["websocket"]]}))
