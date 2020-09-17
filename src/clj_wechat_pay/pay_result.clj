(ns clj-wechat-pay.pay-result
  (:require
   [clj-wechat-pay.utils :as utils]
   [clj-wechat-pay.security :as security]))


(defmulti pay-result-sign
  "支付结果签名"
  (fn [result secret]
    (:trade_type result)))
;;小程序支付结果签名
(defmethod pay-result-sign "JSAPI" [result secret]
  (let [params {:appId (:appid result)
                :timeStamp (utils/timestamp-str)
                :nonceStr (:nonce_str result)
                :package (str "prepay_id=" (:prepay_id result))
                :signType "MD5"}]
    (assoc params :paySign (security/sign params secret))))
;;APP支付结果签名
(defmethod pay-result-sign "APP" [result secret]
  (let [params {:appid (:appid result)
                :partnerid (:mch_id result)
                :prepayid (:prepay_id result)
                :package "Sign=WXPay"
                :noncestr (:nonce_str result)
                :timestamp (utils/timestamp-str)}]
    (assoc params :sign (security/sign params secret))))
