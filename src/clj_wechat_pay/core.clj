(ns clj-wechat-pay.core
  (:require
   [clj-http.client :as client]
   [clojure.tools.logging :as log]
   [clj-wechat-pay.utils :as utils]
   [clj-wechat-pay.security :as security]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 微信支付API                                       ;;
;; https://pay.weixin.qq.com/wiki/doc/api/index.html ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^:private api-url
  {:unifiedorder "https://api.mch.weixin.qq.com/pay/unifiedorder"
   :order-query "https://api.mch.weixin.qq.com/pay/orderquery"
   :close-order "https://api.mch.weixin.qq.com/pay/closeorder"
   :refund "https://api.mch.weixin.qq.com/secapi/pay/refund"
   :refund-query "https://api.mch.weixin.qq.com/pay/refundquery"
   :download-bill "https://api.mch.weixin.qq.com/pay/downloadbill"
   :download-fundflow "https://api.mch.weixin.qq.com/pay/downloadfundflow"
   :report "https://api.mch.weixin.qq.com/payitil/report"
   :shorturl "https://api.mch.weixin.qq.com/tools/shorturl"
   :batch-query-comment "https://api.mch.weixin.qq.com/billcommentsp/batchquerycomment"})

(defn- sign-params [secret params]
  (let [new-params (assoc
                    params
                    :sign_type (or (:sign_type params) "MD5")
                    :nonce_str (utils/rand-str 20))]
    {:body (-> new-params
               (assoc :sign (security/sign new-params secret))
               (utils/map->xml-str))}))

(defn- certificate-params [certificate pass params]
  (merge
   params
   {:keystore certificate
    :keystore-type "PKCS12"
    :keystore-pass pass}))

(defn- gzip-params [tar-type params]
  (if (= tar-type "GZIP")
    (merge params {:as :stream})
    params))

(defn- http-request [url params]
  (log/info "微信支付API开始调用,请求参数:" [url params])
  (let [result (client/post url params)]
    (log/info "微信支付API结束调用,返回结果" (select-keys result [:status :headers :body] ))
    (:body result)))


(defn- pay-result-sign [result secret]
  (let [params {:appId (:appid result)
                :timeStamp (utils/timestamp-str)
                :nonceStr (:nonce_str result)
                :package (str "prepay_id=" (:prepay_id result))
                :signType "MD5"}]
    (assoc params :paySign (security/sign params secret))))

(defn- code-success? [code]
  (= "SUCCESS" code))

(defn- xml->result [xml]
  (let [result (-> xml (utils/xml-str->map) :xml)]
    (if (code-success? (:return_code result))
      (if (utils/hava-key? result :result_code)
        (assoc result :return-ok? true :result-ok? (code-success? (:result_code result)))
        (assoc result :return-ok? true))
      (assoc result :return-ok? false))))

(defn- download->result [params body]
  (let [text (if (= (:tar_type params) "GZIP") (security/gzip->str body) body)]
    (if (utils/xml-str? text)
      (xml->result text)
      {:return-ok? true
       :return-content text})))

(defn unifiedorder
  "统一下单"
  [params secret]
  (->> (sign-params secret params)
       (http-request (:unifiedorder api-url))
       (xml->result)))

(defn native-pay
  "Native支付"
  [params secret]
  (-> (assoc params :trade_type "NATIVE")
      (unifiedorder secret)))

(defn- client-pay [params secret]
  (let [result (unifiedorder params secret)
        sign-result (pay-result-sign result secret)]
    (if (and (:return-ok? result) (:result-ok? result))
      (assoc result :client-result sign-result)
      result)))

(defn jsapi-pay
  "JSAPI支付"
  [params secret]
  (-> (assoc params :trade_type "JSAPI")
      (client-pay secret)))

(defn app-pay
  "APP支付"
  [params secret]
  (-> (assoc params :trade_type "APP")
      (client-pay secret)))

(defn h5-pay
  "H5支付"
  [params secret]
  (-> (assoc params :trade_type "MWEB")
      (client-pay secret)))

(defn refund
  "申请退款"
  [params secret cert]
  (->> (sign-params secret params)
       (certificate-params cert (:mch_id params))
       (http-request (:refund api-url))
       (xml->result)))

(defn order-query
  "查询订单"
  [params secret]
  (->> (sign-params secret params)
       (http-request (:order-query api-url))
       (xml->result)))

(defn close-order
  "关闭订单"
  [params secret]
  (->> (sign-params secret params)
       (http-request (:close-order api-url))
       (xml->result)))

(defn refund-query
  "查询退款"
  [params secret]
  (->> (sign-params secret params)
       (http-request (:refund-query api-url))
       (xml->result)))

(defn download-bill
  "下载对账单"
  [params secret]
  (->> (sign-params secret params)
       (gzip-params (:tar_type params))
       (http-request (:download-bill api-url))
       (download->result params)))

(defn download-fundflow
  "下载资金账单"
  [params secret cert]
  (->> (assoc params :sign_type "HMAC-SHA256")
       (sign-params secret)
       (gzip-params (:tar_type params))
       (certificate-params cert (:mch_id params))
       (http-request (:download-fundflow api-url))
       (download->result params)))

(defn report
  "交易保障"
  [params secret]
  (->> (sign-params secret params)
       (http-request (:report api-url))
       (xml->result)))

(defn short-url
  "转换短链接"
  [params secret]
  (->> (sign-params secret params)
       (http-request (:shorturl api-url))
       (xml->result)))

(defn batch-query-comment
  "拉取订单评价数据"
  [params secret cert]
  (->> (assoc params :sign_type "HMAC-SHA256")
       (sign-params secret)
       (certificate-params cert (:mch_id params))
       (http-request (:batch-query-comment api-url))
       (download->result params)))

(defn pay-callback-data
  "支付回调数据"
  [xml secret]
  (let [result (xml->result xml)
        sign-ok? (security/verify-sign result secret)]
    (assoc result :sign-ok? sign-ok?)))

(defn refund-callback-data
  "申请退款回调数据"
  [xml secret]
  (let [result (xml->result xml)]
    (if (:return-ok? result)
      (let [info-text (security/decode-refund-info (:req_info result) secret)
            info (utils/xml-str->map info-text)]
        (assoc result :req_info (:root info)))
      result)))
