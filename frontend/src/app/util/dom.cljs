;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.util.dom
  (:require
   [app.common.data :as d]
   [app.common.data.macros :as dm]
   [app.common.geom.point :as gpt]
   [app.common.logging :as log]
   [app.common.media :as cm]
   [app.util.globals :as globals]
   [app.util.object :as obj]
   [app.util.webapi :as wapi]
   [cuerdas.core :as str]
   [goog.dom :as dom]
   [promesa.core :as p]))

(log/set-level! :warn)

;; --- Deprecated methods

(defn event->inner-text
  [^js e]
  (when (some? e)
    (.-innerText (.-target e))))

(defn event->value
  [^js e]
  (when (some? e)
    (.-value (.-target e))))

(defn event->target
  [^js e]
  (when (some? e)
    (.-target e)))

(defn event->native-event
  [^js e]
  (.-nativeEvent e))

(defn event->browser-event
  [^js e]
  (.getBrowserEvent e))

;; --- New methods

(declare get-elements-by-tag)

(defn set-html-title
  [^string title]
  (set! (.-title globals/document) title))

(defn set-html-lang!
  [^string lang]
  (.setAttribute (.querySelector js/document "html") "lang" lang))

(defn set-html-theme-color
  [^string color scheme]
  (let [meta-node (.querySelector js/document "meta[name='theme-color']")]
    (.setAttribute meta-node "content" color)
    (.setAttribute meta-node "media" (str/format "(prefers-color-scheme: %s)" scheme))))

(defn set-page-style!
  [styles]
  (let [node  (first (get-elements-by-tag globals/document "head"))
        style (reduce-kv (fn [res k v]
                           (conj res (dm/str (str/css-selector k) ":" v ";")))
                         []
                         styles)
        style (dm/str "<style>\n"
                      "  @page {" (str/join " " style) "}\n "
                      "  html, body {font-size:0; margin:0; padding:0}\n "
                      "</style>")]
    (.insertAdjacentHTML ^js node "beforeend" style)))


(defn get-element-by-class
  ([classname]
   (dom/getElementByClass classname))
  ([classname node]
   (dom/getElementByClass classname node)))

(defn get-elements-by-class
  ([classname]
   (dom/getElementsByClass classname))
  ([classname node]
   (dom/getElementsByClass classname node)))

(defn get-element
  [id]
  (dom/getElement id))

(defn get-elements-by-tag
  [^js node tag]
  (when (some? node)
    (.getElementsByTagName node tag)))

(defn stop-propagation
  [^js event]
  (when event
    (.stopPropagation event)))

(defn stop-immediate-propagation
  [^js event]
  (when event
    (.stopImmediatePropagation event)))

(defn prevent-default
  [^js event]
  (when event
    (.preventDefault event)))

(defn get-target
  "Extract the target from event instance."
  [^js event]
  (when (some? event)
    (.-target event)))

(defn select-target
  "Extract the target from event instance and select it"
  [^js event]
  (when (some? event)
    (-> event (.-target) (.select))))

(defn select-node
  "Select element by node"
  [^js node]
  (when (some? node)
    (.-select node)))

(defn get-current-target
  "Extract the current target from event instance (different from target
  when event triggered in a child of the subscribing element)."
  [^js event]
  (when (some? event)
    (.-currentTarget event)))

(defn get-parent
  [^js node]
  (when (some? node)
    (.-parentElement ^js node)))

(defn get-parent-with-selector
  [^js node selector]

  (loop [current node]
    (if (or (nil? current) (.matches current selector) )
      current
      (recur (.-parentElement current)))))

(defn get-value
  "Extract the value from dom node."
  [^js node]
  (when (some? node)
    (.-value node)))

(defn get-input-value
  "Extract the value from dom input node taking into account the type."
  [^js node]
  (when (some? node)
    (if (or (= (.-type node) "checkbox")
            (= (.-type node) "radio"))
      (.-checked node)
      (.-value node))))

(defn get-attribute
  "Extract the value of one attribute of a dom node."
  [^js node ^string attr-name]
  (when (some? node)
    (.getAttribute ^js node attr-name)))

(defn get-scroll-position
  [^js event]
  (when (some? event)
      {:scroll-height (.-scrollHeight event)
       :scroll-left   (.-scrollLeft event)
       :scroll-top    (.-scrollTop event)
       :scroll-width  (.-scrollWidth event)}))

(def get-target-val (comp get-value get-target))

(def get-target-scroll (comp get-scroll-position get-target))

(defn click
  "Click a node"
  [^js node]
  (when (some? node)
    (.click node)))

(defn get-files
  "Extract the files from dom node."
  [^js node]
  (when (some? node)
    (array-seq (.-files node))))

(defn checked?
  "Check if the node that represents a radio
  or checkbox is checked or not."
  [^js node]
  (when (some? node)
    (.-checked node)))

(defn valid?
  "Check if the node that is a form input
  has a valid value, against html5 form validation
  properties (required, min/max, pattern...)."
  [^js node]
  (when (some? node)
    (when-let [validity (.-validity node)]
      (.-valid validity))))

(defn set-validity!
  "Manually set the validity status of a node that
  is a form input. If the state is an empty string,
  the input will be valid. If not, the string will
  be set as the error message."
  [^js node status]
  (when (some? node)
    (.setCustomValidity node status)
    (.reportValidity node)))

(defn clean-value!
  [^js node]
  (when (some? node)
    (set! (.-value node) "")))

(defn set-value!
  [^js node value]
  (when (some? node)
    (set! (.-value ^js node) value)))

(defn select-text!
  [^js node]
  (when (some? node)
    (.select ^js node)))

(defn ^boolean equals?
  [^js node-a ^js node-b]

  (or (and (nil? node-a) (nil? node-b))
      (and (some? node-a)
           (.isEqualNode ^js node-a node-b))))

(defn get-event-files
  "Extract the files from event instance."
  [^js event]
  (when (some? event)
    (get-files (get-target event))))

(defn create-element
  ([tag]
   (.createElement globals/document tag))
  ([ns tag]
   (.createElementNS globals/document ns tag)))

(defn set-html!
  [^js el html]
  (when (some? el)
    (set! (.-innerHTML el) html)))

(defn append-child!
  [^js el child]
  (when (some? el)
    (.appendChild ^js el child))
  el)

(defn remove-child!
  [^js el child]
  (when (some? el)
    (.removeChild ^js el child))
  el)

(defn get-first-child
  [^js el]
  (when (some? el)
    (.-firstChild el)))

(defn get-tag-name
  [^js el]
  (when (some? el)
    (.-tagName el)))

(defn get-outer-html
  [^js el]
  (when (some? el)
    (.-outerHTML el)))

(defn get-inner-text
  [^js el]
  (when (some? el)
    (.-innerText el)))

(defn query
  ([^string selector]
   (query globals/document selector))

  ([^js el ^string selector]
   (when (some? el)
     (.querySelector el selector))))

(defn query-all
  ([^string selector]
   (query-all globals/document selector))

  ([^js el ^string selector]
   (when (some? el)
     (.querySelectorAll el selector))))

(defn get-client-position
  [^js event]
  (let [x (.-clientX event)
        y (.-clientY event)]
    (gpt/point x y)))

(defn get-offset-position
  [^js event]
  (when (some? event)
    (let [x (.-offsetX event)
          y (.-offsetY event)]
      (gpt/point x y))))

(defn get-client-size
  [^js node]
  (when (some? node)
    {:width (.-clientWidth ^js node)
     :height (.-clientHeight ^js node)}))

(defn get-bounding-rect
  [node]
  (let [rect (.getBoundingClientRect ^js node)]
    {:left (.-left ^js rect)
     :top (.-top ^js rect)
     :right (.-right ^js rect)
     :bottom (.-bottom ^js rect)
     :width (.-width ^js rect)
     :height (.-height ^js rect)}))

(defn bounding-rect->rect
  [rect]
  (when (some? rect)
    {:x      (or (.-left rect)   (:left rect)   0)
     :y      (or (.-top rect)    (:top rect)    0)
     :width  (or (.-width rect)  (:width rect)  1)
     :height (or (.-height rect) (:height rect) 1)}))

(defn get-window-size
  []
  {:width (.-innerWidth ^js js/window)
   :height (.-innerHeight ^js js/window)})

(defn focus!
  [^js node]
  (when (some? node)
    (.focus node)))

(defn blur!
  [^js node]
  (when (some? node)
    (.blur node)))

(defn fullscreen?
  []
  (cond
    (obj/in? globals/document "webkitFullscreenElement")
    (boolean (.-webkitFullscreenElement globals/document))

    (obj/in? globals/document "fullscreenElement")
    (boolean (.-fullscreenElement globals/document))

    :else
    (do
      (log/error :msg "Seems like the current browser does not support fullscreen api.")
      false)))

(defn blob?
  [^js v]
  (when (some? v)
    (instance? js/Blob v)))

(defn make-node
  ([namespace name]
   (.createElementNS globals/document namespace name))

  ([name]
   (.createElement globals/document name)))

(defn node->xml
  [node]
  (when (some? node)
    (->  (js/XMLSerializer.)
         (.serializeToString node))))

(defn svg->data-uri
  [svg]
  (assert (string? svg))
  (let [b64 (-> svg
                js/encodeURIComponent
                js/unescape
                js/btoa)]
    (dm/str "data:image/svg+xml;base64," b64)))

(defn set-property! [^js node property value]
  (when (some? node)
    (.setAttribute node property value))
  node)

(defn set-text! [^js node text]
  (when (some? node)
    (set! (.-textContent node) text))
  node)

(defn set-css-property! [^js node property value]
  (when (some? node)
    (.setProperty (.-style ^js node) property value))
  node)

(defn unset-css-property! [^js node property]
  (when (some? node)
    (.removeProperty (.-style ^js node) property))
  node)

(defn capture-pointer [^js event]
  (when (some? event)
    (-> event get-target (.setPointerCapture (.-pointerId event)))))

(defn release-pointer [^js event]
  (when (and (some? event) (.-pointerId event))
    (-> event get-target (.releasePointerCapture (.-pointerId event)))))

(defn get-body []
  (.-body globals/document))

(defn get-root []
  (query globals/document "#app"))

(defn classnames
  [& params]
  (assert (even? (count params)))
  (str/join " " (reduce (fn [acc [k v]]
                          (if (true? (boolean v))
                            (conj acc (name k))
                            acc))
                        []
                        (partition 2 params))))

(defn ^boolean class? [node class-name]
  (when (some? node)
    (let [class-list (.-classList ^js node)]
      (.contains ^js class-list class-name))))

(defn add-class! [^js node class-name]
  (when (some? node)
    (let [class-list (.-classList ^js node)]
      (.add ^js class-list class-name)))
  node)

(defn remove-class! [^js node class-name]
  (when (some? node)
    (let [class-list (.-classList ^js node)]
      (.remove ^js class-list class-name))))

(defn child? [^js node1 ^js node2]
  (when (some? node1)
    (.contains ^js node2 ^js node1)))

(defn get-user-agent []
  (.-userAgent globals/navigator))

(defn get-active []
  (.-activeElement globals/document))

(defn active? [^js node]
  (when (some? node)
    (= (get-active) node)))

(defn get-data [^js node ^string attr]
  (when (some? node)
    (.getAttribute node (str "data-" attr))))

(defn set-attribute! [^js node ^string attr value]
  (when (some? node)
    (.setAttribute node attr value)))

(defn remove-attribute! [^js node ^string attr]
  (when (some? node)
    (.removeAttribute node attr)))

(defn get-scroll-pos
  [^js element]
  (when (some? element)
    (.-scrollTop element)))

(defn get-h-scroll-pos
  [^js element]
  (when (some? element)
    (.-scrollLeft element)))

(defn set-scroll-pos!
  [^js element scroll]
  (when (some? element)
    (obj/set! element "scrollTop" scroll)))

(defn set-h-scroll-pos!
  [^js element scroll]
  (when (some? element)
    (obj/set! element "scrollLeft" scroll)))

(defn scroll-into-view!
  ([^js element]
   (scroll-into-view! element false))

  ([^js element options]
   (when (some? element)
     (.scrollIntoView element options))))

(defn scroll-into-view-if-needed!
  ([^js element]
   (scroll-into-view-if-needed! element false))

  ([^js element options]
   (when (some? element)
     (.scrollIntoViewIfNeeded ^js element options))))

(defn is-in-viewport?
  [^js element]
  (when (some? element)
    (let [rect   (.getBoundingClientRect element)
          height (or (.-innerHeight js/window)
                     (.. js/document -documentElement -clientHeight))
          width  (or (.-innerWidth js/window)
                     (.. js/document -documentElement -clientWidth))]
      (and (>= (.-top rect) 0)
           (>= (.-left rect) 0)
           (<= (.-bottom rect) height)
           (<= (.-right rect) width)))))

(defn trigger-download-uri
  [filename mtype uri]
  (let [link      (create-element "a")
        extension (cm/mtype->extension mtype)
        filename  (if (and extension (not (str/ends-with? filename extension)))
                    (str/concat filename extension)
                    filename)]
    (obj/set! link "href" uri)
    (obj/set! link "download" filename)
    (obj/set! (.-style ^js link) "display" "none")
    (.appendChild (.-body ^js js/document) link)
    (.click link)
    (.remove link)))

(defn trigger-download
  [filename blob]
  (trigger-download-uri filename (.-type ^js blob) (wapi/create-uri blob)))

(defn save-as
  [uri filename mtype description]

  ;; Only chrome supports the save dialog
  (if (obj/contains? globals/window "showSaveFilePicker")
    (let [extension (cm/mtype->extension mtype)
          opts {:suggestedName (str filename "." extension)
                :types [{:description description
                         :accept { mtype [(str "." extension)]}}]}]

      (-> (p/let [file-system (.showSaveFilePicker globals/window (clj->js opts))
                  writable    (.createWritable file-system)
                  response    (js/fetch uri)
                  blob        (.blob response)
                  _           (.write writable blob)]
            (.close writable))
          (p/catch
              #(when-not (and (= (type %) js/DOMException)
                              (= (.-name %) "AbortError"))
                 (trigger-download-uri filename mtype uri)))))

    (trigger-download-uri filename mtype uri)))

(defn left-mouse? [bevent]
  (let [event  (.-nativeEvent ^js bevent)]
    (= 1 (.-which event))))

;; Warning: need to protect against reverse tabnabbing attack
;; https://www.comparitech.com/blog/information-security/reverse-tabnabbing/
(defn open-new-window
  ([uri]
   (open-new-window uri "_blank" "noopener,noreferrer"))
  ([uri name]
   (open-new-window uri name "noopener,noreferrer"))
  ([uri name features]
   (let [new-window (.open js/window (str uri) name features)]
     (when (not= name "_blank")
       (.reload (.-location new-window))))))

(defn browser-back
  []
  (.back (.-history js/window)))

(defn reload-current-window
  []
  (.reload (.-location js/window)))

(defn animate!
  ([item keyframes duration] (animate! item keyframes duration nil))
  ([item keyframes duration onfinish]
    (let [animation (.animate item keyframes duration)]
      (when onfinish
        (set! (.-onfinish animation) onfinish)))))

(defn is-child?
  [^js node ^js candidate]
  (and (some? node)
       (some? candidate)
       (.contains node candidate)))

(defn seq-nodes
  [root-node]
  (letfn [(branch? [node]
            (d/not-empty? (get-children node)))

          (get-children [node]
            (seq (.-children node)))]
    (->> root-node
         (tree-seq branch? get-children))))

(defn check-font? [font]
  (let [fonts (.-fonts globals/document)]
    (.check fonts font)))

(defn load-font [font]
  (let [fonts (.-fonts globals/document)]
    (.load fonts font)))

(defn text-measure [font]
  (let [element (.createElement globals/document "canvas")
        context (.getContext element "2d")
        _ (set! (.-font context) font)
        measure ^js (.measureText context "Ag")]

    {:ascent (.-fontBoundingBoxAscent measure)
     :descent (.-fontBoundingBoxDescent measure)}))

(defn clone-node
  ([^js node]
   (clone-node node true))
  ([^js node deep?]
   (.cloneNode node deep?)))

(defn has-children?
  [^js node]
  (> (-> node .-children .-length) 0))
