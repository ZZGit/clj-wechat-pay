(ns clj-wechat-pay.security
  (:require
   [clojure.string :as string]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log])
  (:import
   [java.util Base64]
   [java.lang String]
   [org.apache.commons.io IOUtils]
   [org.apache.commons.codec.binary Hex]
   [javax.crypto.spec SecretKeySpec ]
   [javax.crypto Cipher Mac]
   [java.util.zip GZIPInputStream]
   [java.security MessageDigest InvalidKeyException NoSuchAlgorithmException]
   [java.io ByteArrayOutputStream UnsupportedEncodingException]))


(defn- filter-params
  [params]
  (into
   {}
   (filter
    (fn [[k v]]
      (and (not= k :sign)
           (not= k :return-ok?)
           (not= k :result-ok?)
           (or (number? v) (seq v)))) params)))

(defn- join-params
  [params secret]
  (as-> params p
    (sort p)
    (map
     (fn [[k v]]
       (let [v (if (coll? v) (json/write-str v) v)]
         (format "%s=%s" (name k) v))) p)
    (string/join "&" p)
    (format "%s&key=%s" p secret)))

(defn- temp-sign-str
  [params secret]
  (-> params
      (filter-params)
      (join-params secret)))

(defn- ^String aes-decode [^bytes ciphertext ^bytes key]
  (let [cipher (Cipher/getInstance "AES/ECB/PKCS5Padding")
        key-spec (SecretKeySpec. key "AES")]
    (.init cipher Cipher/DECRYPT_MODE key-spec)
    (.doFinal cipher ciphertext)))

(defn- ^String hmac-sha256 [plaintext key]
  (let [algorithm "HmacSHA256"
        text-bytes (.getBytes plaintext "UTF-8")
        key-bytes (.getBytes key "UTF-8")]
    (try
      (let [hmac (Mac/getInstance algorithm)
            key-spec (SecretKeySpec. key-bytes algorithm)]
        (.init hmac key-spec)
        (String. (Hex/encodeHex (.doFinal hmac text-bytes))))
      (catch NoSuchAlgorithmException e
        (log/error e))
      (catch UnsupportedEncodingException e
        (log/error e))
      (catch InvalidKeyException e
        (log/error e)))))

(defn- ^String md5 [plaintext]
  (let [digest (MessageDigest/getInstance "MD5")]
    (.update digest (.getBytes plaintext))
    (String. (Hex/encodeHex (.digest digest)))))

(defn- base64 [s]
  (.decode (Base64/getDecoder) s))

(defmulti sign
  "微信签名算法
  https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_3"
  (fn [params secret]
    (keyword
     (or
      (or (:signType params)
          (:sign_type params))
      "MD5"))))
(defmethod sign :MD5 [params secret]
  (-> params
      (temp-sign-str secret)
      (md5)
      (string/upper-case)))
(defmethod sign :HMAC-SHA256 [params secret]
  (-> params
      (temp-sign-str secret)
      (hmac-sha256 secret)
      (string/upper-case)))
(defmethod sign :default [params secret]
  (log/error "不支持的加密算法" (name (:sign_type params))))

(defn verify-sign
  "验证签名"
  [params secret]
  (= (:sign params) (sign params secret)))

(defn decode-refund-info
  "解密退款信息"
  [info secret]
  (let [md5-secret (md5 secret)]
    (slurp (aes-decode (base64 info) (.getBytes md5-secret)))))

(defn gzip->str
  "gizp解压"
  [input-stream]
  (with-open [out (ByteArrayOutputStream.)]
    (IOUtils/copy (GZIPInputStream. input-stream) out)
    (.close input-stream)
    (.toString out "utf-8")))
