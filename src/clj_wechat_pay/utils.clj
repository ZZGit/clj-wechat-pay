(ns clj-wechat-pay.utils
  (:require
   [clojure.string :as string]
   [clojure.data.xml :as xml]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log])
  (:import
   [java.time Instant]
   [java.time.temporal ChronoUnit]
   [javax.xml.stream XMLStreamException]))


(defn- xml-item [[k v]]
  (if (coll? v)
    [k [:-cdata (json/write-str v)]]
    [k v]))

(defn- same-keys [content]
  (when content
    (= 1 (count (distinct (map :tag content))))))

(defn map->xml
  [params]
  (xml/sexp-as-element
   (into
    [:xml]
    (map xml-item params))))

(defn map->xml-str
  [params]
  (xml/emit-str
   (map->xml params)))

(defn xml->map
  [element]
  (cond
    (nil? element) nil
    (string? element) element
    (map? element) (if-not (empty? element)
                     {(:tag element) (xml->map (:content element))}
                     {})
    (sequential? element) (cond
                            (= (count element) 1) (xml->map (first element))
                            (same-keys element) (mapv #(xml->map (:content %)) element)
                            :else (reduce into {} (map #(xml->map %) element)))
    :else nil))

(defn xml-str->map
  [xml-str]
  (try
    (with-open [r (java.io.StringReader. xml-str)]
      (xml->map  (xml/parse r)))
    (catch XMLStreamException ex
      nil
      #_(log/error (.getMessage ex)))))

(defn process-xml [xml]
  (if (string? xml)
    (xml-str->map xml)
    (xml->map xml)))

(defn xml-str? [xml-str?]
  ((comp not nil?) (xml-str->map xml-str?)))

(defn rand-str [length]
  (->> #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")
       (repeatedly length)
       (string/join)
       (string/upper-case)))

(defn timestamp-str []
  (str
   (.until (Instant/ofEpochSecond 0) (Instant/now) (ChronoUnit/SECONDS))))

(defn hava-key? [m key]
  (some #(when (= % key) %) (keys m)))
