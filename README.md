# clj-wechat-pay

Clojure 微信支付

## 功能
 * 数据转换
 * 数据加密、解密

## 版本

## 例子

所有请求参数无须指定nonce_str、sign、sign_type，内部自动指定，自动根据参数进行签名。

返回结果的会根据return_code和result_code，生成对于的:return-ok?和:result-ok?，方便后续逻辑判断

返回数据示例如下：
```clojure
{:appid "wx069c4d7574501e36",
 ...
 :return_code "SUCCESS",
 :result_code "SUCCESS",
 :return-ok? true,
 :result-ok? true}
```

### Native支付
具体参数参考微信api文档
```clojure
(require '[clj-wechat-pay.core :refer :all])

(def params {:appid "appid"
             :mch_id "mchid"
             :body "充值测试"
             :out_trade_no "123456"
             :total_fee 1
             :spbill_create_ip "127.0.0.1"
             :notify_url "https://notify-url"})

(def secret "微信api-secret")

(native-pay params secret)
```

### JSAPI支付
```clojure
(jsapi-pay params secret)
```

### APP支付
```clojure
(app-pay params secret)
```

### H5支付
```clojure
(h5-pay params secret)
```

### 查询订单
```clojure
(order-query params secret)
```

### 关闭订单
```clojure
(close-order params secret)
```

### 申请退款
```clojure
;;证书路径，pkcs12格式
(def certificate-path "/path/apiclient_cert.p12")

(refund params secret certificate-path)
```

### 查询退款
```clojure
(refund-query params secret)
```

### 下载对账单
```clojure
(download-bill params secret)
```

### 支付通知回调
```clojure
;;支付回调的xml数据
(def xml-data "<xml>
    <appid>wx069c4d75</appid>
    <bank_type>OTHERS</bank_type>
    <cash_fee>1</cash_fee>
    <fee_type>CNY</fee_type>
    <is_subscribe>N</is_subscribe>
    <mch_id>154962</mch_id>
    <nonce_str>L08ASQ725T2QPTNND5M9</nonce_str>
    <openid>omoIR5aFIw6gL2YOnGjBmNk_9y</openid>
    <out_trade_no>26986907249184</out_trade_no>
    <result_code>SUCCESS</result_code>
    <return_code>SUCCESS</return_code>
    <sign>9278C3DC0E4C69C558E68991AB460</sign>
    <time_end>20200115164337</time_end>
    <total_fee>1</total_fee>
    <trade_type>JSAPI</trade_type>
    <transaction_id>4200000513202001155947790</transaction_id>
</xml>")

(pay-callback-data xml-data secret)
```
返回数据示例
```clojure
{:appid "wx069c4d75",
 ...
 :return_code "SUCCESS",
 :result_code "SUCCESS",
 :return-ok? true,
 :result-ok? true,
 :sign-ok? true}
```
除了:return-ok?和:result-ok?,返回数据还额外添加了:sign-ok?,代表数据签名是否正确。

### 退款通知回调
```clojure
(refund-callback-data xml-data secret)
```
返回数据示例
```clojure
{:return_code "SUCCESS",
:return-ok? true,
...
:req_info {
    :transaction_id "4200000508202001207149695201",
    ...
}}
```
:req_info表示解密后的数据

提示：

退款通知回调的内容，微信使用AES-256-ECB加密算法,JDK 9以下的版本需要扩展以下：

* 进入到[下载页面](https://www.oracle.com/technetwork/java/javase/downloads/jce-all-download-5170447.html),下载指定版本
*解压后，拷local_policy.jar和US_export_policy.jar到$JAVA_HOME/jre/lib/security目录

## License

Copyright © 2020 Tom.Yu

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
