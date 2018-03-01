(ns hawkthorne.game
  ;; Phaser convenience functions
  (:require [cljsjs.phaser-ce]
            [hawkthorne.state :as state]
            [hawkthorne.util :as util]
            [tmx2edn.core :refer [tmx->edn]]))

(defn preload-tilesets!
  [game tilesets]
  (doall (map (fn [tileset]
                (.. game -load (image (:name tileset) (:image tileset))))
              tilesets)))

(defn preload-tilemap!
  [game tilemap]
  (.. game -load (binary tilemap tilemap
                         (fn [k data]
                           (let [map-edn (->> (util/arraybuffer->str data)
                                              tmx->edn)]
                             (preload-tilesets! game (:tilesets map-edn))
                             (.. game -load (tilemap tilemap nil
                                                     (clj->js map-edn)
                                                     (-> js/Phaser .-Tilemap
                                                         .-TILED_JSON)))
                             map-edn)))))

(defn create-tileset-images!
  [game tilemap level]
  (doall (map (fn [tileset]
                (.addTilesetImage level (:name tileset)))
              (:tilesets (.. game -cache (getBinary tilemap))))))

(defn create-layers!
  [game tilemap level]
  (doall (map (fn [layer]
                (let [l (.createLayer level (:name layer))]
                  (when (= "collision" (:name layer))
                    (.resizeWorld l)
                    (.setCollision level
                                   (->> (:data layer)
                                        (map-indexed (fn [idx d] [idx d]))
                                        (filter (fn [m]
                                                  (not (zero? (second m)))))
                                        keys)
                                   true (:name layer))
                    (-> game .-physics .-p2 (.convertTilemap level l
                                                             true true)))))
              (filter #(= :tilelayer (:type %))
                      (:layers (.. game -cache (getBinary tilemap)))))))

(defn create-tilemap!
  [game tilemap]
  (let [level (-> game .-add (.tilemap tilemap))]
    (create-tileset-images! game tilemap level)
    (create-layers! game tilemap level)
    (.. game -physics -p2 (setBoundsToWorld true true true true true))))

(defn build-state
  "Pass game object to each state function"
  [init-state game]
  (reduce (fn [accl [k f]]
            (assoc accl k (partial f game)))
          {} init-state))

(defn create-game!
  [width height init-state {:keys [parent]
                            :or [parent (.-body js/document)]
                            :as opts}]
  (let [game (new (.-Game js/Phaser) width height (.-AUTO js/Phaser) parent
                  {} false false)]
    (.. game -state
        (add "Boot" (clj->js (build-state init-state game))
             true))
    (swap! state/state assoc :game game)))
