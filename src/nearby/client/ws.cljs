(ns nearby.client.ws
  (:require
   [nearby.client.db :as db]
   [clojure.string :as string]
   [nearby.client.event-source :as es]
   [cljs.reader :as edn]))

;; TODO: actual on-message
(defn on-message-impl [message-obj]
  (let [data (.-data message-obj)]
    (es/dispatch! (assoc (edn/read-string data)
                         :client-ts (js/Date.)))))

(defn handle-error [e]
  (prn "[ws] error! " e))

(defn on-close [o]
  (prn "[ws] socket closed" o))

(def *socket (atom nil))

(defn on-open [o]
  (prn "[ws] socket opened" o)
  o)

(def *geolocation
  (atom nil))

(add-watch *geolocation ::geo
           (fn [& _]
             (prn "[ws] client gelocation: " @*geolocation)))

(defn ws-url [ws-uri lng lat]
  (str "ws://" (.-host (.-location js/window)) ws-uri
       "?coords=" lng "," lat))

(defn setup-navigator-watch! [setup-fn]
  (if-let [nav (aget js/window "navigator")]
    (.getCurrentPosition (.-geolocation nav)
                         (fn [res]
                           (setup-fn {:lng (.-longitude (.-coords res))
                                      :lat (.-latitude (.-coords res))})))
    (prn "[ws] no navigator present")))

(defn setup! [ws-uri {:keys [lng lat]}]
  (let [sock (js/WebSocket. (ws-url ws-uri lng lat))]
    (set! js/window.sock sock)
    (prn :sock sock)
    (set! (.-onmessage sock) on-message-impl)
    (set! (.-onopen sock) on-open)
    (set! (.-onerror sock) handle-error)
    (set! (.-onclose sock) on-close)
    (reset! *socket sock)))

(defn start! [ws-uri]
  (setup-navigator-watch! (partial setup! ws-uri)))
