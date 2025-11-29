# 微信小程序支付模块开发指南

## 项目架构说明

本项目采用"预订先行，订单后置"的架构设计，减少无效订单，简化取消逻辑。

### 数据表关系

```
bookings (预订表)
├── id (预订ID)
├── order_no (订单号)
├── total_amount (总金额-分)
├── status (0-待支付, 1-已支付, 2-已取消)
└── user_id (用户ID)

orders (订单表)
├── id (订单ID)
├── order_no (订单号，同bookings.order_no)
├── booking_id (关联预订ID)
├── total_amount (总金额-分)
├── pay_amount (支付金额-分)
├── status (0-待支付, 1-已支付, 2-支付失败, 3-已退款)
└── payment_method (支付方式)
```

## 微信支付开发流程

### 1. 统一下单接口（后端）

**接口地址**: `POST /v3/pay/partner/transactions/jsapi`

**请求参数**（关键字段）:

```json
{
  "sp_appid": "服务商AppID",
  "sp_mchid": "服务商商户号",
  "sub_appid": "子商户AppID",
  "sub_mchid": "子商户商户号",
  "description": "订单描述",
  "out_trade_no": "订单号（复用bookings.order_no）",
  "amount": {
    "total": 9999,          // 金额（分）
    "currency": "CNY"
  },
  "payer": {
    "sp_openid": "用户openid"
  },
  "notify_url": "https://api.nianhua.com/api/v1/payment/notify"  // 支付结果回调地址
}
```

**响应参数**:

```json
{
  "prepay_id": "wx201410272009395522657a690389285100"
}
```

### 2. 生成调起支付签名（后端）

签名串格式（5行，每行以`\n`结束）:

```
appId
时间戳
随机字符串
prepay_id=xxx

```

签名算法: `SHA256withRSA`（使用商户API私钥）

生成的5个参数:
- `appId`: 小程序AppID
- `timeStamp`: 时间戳（秒）
- `nonceStr`: 随机字符串
- `package`: `prepay_id=xxx`
- `signType`: `RSA`
- `paySign`: 签名值

### 3. 小程序前端调起支付

```javascript
wx.requestPayment({
  timeStamp: response.data.timeStamp,
  nonceStr: response.data.nonceStr,
  package: response.data.package,
  signType: response.data.signType,
  paySign: response.data.paySign,
  success(res) {
    // 支付成功
    console.log('支付成功', res);
    // 跳转到支付成功页面
    wx.navigateTo({
      url: '/pages/payment/success?orderNo=' + response.data.orderNo
    });
  },
  fail(res) {
    // 支付失败或取消
    console.log('支付失败', res);
    // 跳转到订单详情页或显示错误
    wx.showToast({
      title: '支付失败',
      icon: 'none'
    });
  }
});
```

## 本项目接口设计

### 1. 创建支付订单接口

**URL**: `POST /api/v1/payment/create-order`

**请求参数**:

```json
{
  "bookingId": "123456789",           // 预订ID
  "openid": "oUpF8uMuAJO_M2..."      // 用户openid
}
```

**响应参数**:

```json
{
  "code": 200,
  "message": "创建支付订单成功",
  "data": {
    "orderNo": "202511291235001234",  // 订单号
    "prepayId": "wx201410272009395522657a690389285100",
    "appId": "wx8888888888888888",
    "timeStamp": "1554208460",
    "nonceStr": "593BEC0C930BF1AFEB40B4A08C8FB242",
    "package": "prepay_id=wx201410272009395522657a690389285100",
    "signType": "RSA",
    "paySign": "mI35pfNEQV6777ke/1T+LJLQDNTm7yeoUJH+j/adPGhm...",
    "amount": 99.99,                  // 金额（元）
    "expireTime": "2025-12-01T12:45:00+08:00"
  }
}
```

**业务流程**:

```java
// 伪代码
public CreatePaymentOrderResponse createPaymentOrder(CreatePaymentOrderRequest request) {
    // 1. 查询预订
    Booking booking = bookingMapper.selectById(request.getBookingId());

    // 2. 验证状态
    if (booking.getStatus() != 0) { // 0-待支付
        throw new BusinessException("预订状态错误");
    }

    // 3. 创建订单
    Order order = new Order();
    order.setBookingId(booking.getId());
    order.setOrderNo(booking.getOrderNo());  // 复用预订的order_no
    order.setTotalAmount(booking.getTotalAmount());
    order.setPayAmount(booking.getTotalAmount());
    order.setStatus(0); // 0-待支付
    orderMapper.insert(order);

    // 4. 调微信统一下单
    WechatUnifiedOrderRequest wxRequest = new WechatUnifiedOrderRequest();
    wxRequest.setOutTradeNo(order.getOrderNo());
    wxRequest.setAmount(order.getPayAmount()); // 分
    wxRequest.setOpenid(request.getOpenid());
    // ... 其他参数

    WechatUnifiedOrderResponse wxResponse = wechatPayClient.unifiedOrder(wxRequest);

    // 5. 生成支付签名
    String paySign = generatePaySign(wxResponse.getPrepayId());

    // 6. 返回结果
    return new CreatePaymentOrderResponse(
        order.getOrderNo(),
        wxResponse.getPrepayId(),
        paySign
    );
}
```

### 2. 支付结果回调接口

**URL**: `POST /api/v1/payment/notify`

**说明**: 此接口需要直接暴露给公网，配置在微信支付后台

**接收到的数据格式**:

```json
{
  "id": "EV-2018022511223320873",
  "create_time": "2015-05-20T13:29:35+08:00",
  "event_type": "TRANSACTION.SUCCESS",
  "resource_type": "encrypt-resource",
  "resource": {
    "algorithm": "AEAD_AES_256_GCM",
    "ciphertext": "...",
    "associated_data": "",
    "nonce": "..."
  }
}
```

**解密后的数据**:

```json
{
  "transaction_id": "420000123456789012345678901234567890",
  "amount": {
    "total": 9999,
    "payer_total": 9999,
    "currency": "CNY",
    "payer_currency": "CNY"
  },
  "out_trade_no": "202511291235001234",
  "success_time": "2018-06-08T10:34:56+08:00",
  "trade_state": "SUCCESS"
}
```

**处理流程**:

```java
// 伪代码
public PaymentNotifyResponse handlePaymentNotify(PaymentNotifyRequest notify) {
    try {
        // 1. 解密数据
        String decryptedData = decryptWechatNotification(notify.getResource());

        // 2. 解析JSON
        WechatPaymentNotification payment = parseJson(decryptedData);

        // 3. 查询订单
        Order order = orderMapper.selectByOrderNo(payment.getOutTradeNo());
        Booking booking = bookingMapper.selectByOrderNo(payment.getOutTradeNo());

        // 4. 验证金额
        if (!order.getPayAmount().equals(payment.getAmount().getTotal())) {
            log.error("支付金额不匹配");
            throw new RuntimeException("金额不匹配");
        }

        // 5. 更新状态
        order.setStatus(1); // 1-已支付
        order.setTransactionId(payment.getTransactionId());
        order.setPaymentTime(payment.getSuccessTime());
        orderMapper.updateById(order);

        booking.setStatus(1); // 1-已支付
        bookingMapper.updateById(booking);

        // 6. 释放座位锁（可选，因为超时也会释放）
        // seatLockManager.releaseAllUserSeatLocks(...);

        // 7. 返回成功
        return new PaymentNotifyResponse("SUCCESS", "OK");

    } catch (Exception e) {
        log.error("支付回调处理失败", e);
        return new PaymentNotifyResponse("FAIL", "SYSTEM_ERROR");
    }
}
```

### 3. 查询支付状态接口

**URL**: `GET /api/v1/payment/status/{orderNo}`

**响应**:

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "orderNo": "202511291235001234",
    "bookingId": "123456789",
    "status": "SUCCESS",
    "amount": 99.99,
    "paidTime": "2025-12-01T12:30:00+08:00",
    "transactionId": "420000123456789012345678901234567890"
  }
}
```

**前端轮询逻辑**:

```javascript
// 调起支付后，轮询查询支付状态
let checkCount = 0;
const maxCheckCount = 30; // 最多查询30次（约2分钟）

const interval = setInterval(() => {
  checkCount++;

  wx.request({
    url: 'https://api.nianhua.com/api/v1/payment/status/' + orderNo,
    method: 'GET',
    header: {
      'Authorization': 'Bearer ' + token
    },
    success(res) {
      if (res.data.code === 200) {
        const status = res.data.data.status;

        if (status === 'SUCCESS') {
          // 支付成功
          clearInterval(interval);
          wx.navigateTo({
            url: '/pages/payment/success?orderNo=' + orderNo
          });
        } else if (status === 'FAILED' || status === 'CLOSED') {
          // 支付失败或关闭
          clearInterval(interval);
          wx.showToast({
            title: '支付失败',
            icon: 'none'
          });
        }

        if (checkCount >= maxCheckCount) {
          // 超时，停止轮询
          clearInterval(interval);
        }
      }
    }
  });
}, 4000); // 每4秒查询一次
```

## 前后端配合开发步骤

### 后端开发任务

#### 1. 配置微信支付参数

```java
@Configuration
public class WechatPayConfig {

    @Value("${wechat.pay.app-id}")
    private String appId;

    @Value("${wechat.pay.mch-id}")
    private String mchId;

    @Value("${wechat.pay.mch-serial-number}")
    private String mchSerialNumber;

    @Value("${wechat.pay.private-key-path}")
    private String privateKeyPath;

    @Value("${wechat.pay.api-v3-key}")
    private String apiV3Key;

    @Value("${wechat.pay.notify-url}")
    private String notifyUrl;

    @Bean
    public WechatPayClient wechatPayClient() {
        return new WechatPayClient(
            appId,
            mchId,
            privateKeyPath,
            mchSerialNumber,
            apiV3Key,
            notifyUrl
        );
    }
}
```

`application.yml`:

```yaml
wechat:
  pay:
    app-id: wx8888888888888888      # 小程序AppID
    mch-id: 1230000109              # 商户号
    mch-serial-number: 1234567890   # 商户证书序列号
    private-key-path: /path/to/apiclient_key.pem  # API私钥文件路径
    api-v3-key: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # APIv3密钥
    notify-url: https://api.nianhua.com/api/v1/payment/notify  # 支付回调地址
```

#### 2. 实现创建支付订单接口

参考：`/api/v1/payment/create-order` 的Swagger定义

关键步骤：
1. 查询预订
2. 验证状态
3. 创建订单（复用order_no）
4. 调用微信统一下单
5. 生成支付签名
6. 返回支付参数

#### 3. 实现支付结果回调接口

参考：`/api/v1/payment/notify` 的Swagger定义

关键步骤：
1. 解密微信通知数据
2. 验证签名
3. 验证金额
4. 更新订单和预订状态
5. 返回微信要求的响应格式

解密示例代码：

```java
private String decryptWechatNotification(NotificationResource resource) {
    // 使用APIv3密钥解密
    AesUtil aesUtil = new AesUtil(apiV3Key.getBytes(StandardCharsets.UTF_8));
    return aesUtil.decryptToString(
        resource.getAssociatedData().getBytes(),
        resource.getNonce().getBytes(),
        resource.getCiphertext()
    );
}
```

#### 4. 实现查询支付状态接口

参考：`/api/v1/payment/status/{orderNo}` 的Swagger定义

#### 5. 实现退款接口（可选）

参考：`/api/v1/payment/refund` 的Swagger定义

### 前端开发任务

#### 1. 创建预订成功后，显示"去支付"按钮

```javascript
// 在预订成功页面
Page({
  data: {
    bookingId: '',
    orderNo: '',
    amount: 0
  },

  // 点击"去支付"
  goToPay() {
    wx.showLoading({ title: '准备支付...' });

    // 1. 获取用户openid（需要提前获取并存储）
    const openid = wx.getStorageSync('openid');

    // 2. 调用后端创建支付订单接口
    wx.request({
      url: 'https://api.nianhua.com/api/v1/payment/create-order',
      method: 'POST',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token'),
        'Content-Type': 'application/json'
      },
      data: {
        bookingId: this.data.bookingId,
        openid: openid
      },
      success: (res) => {
        wx.hideLoading();

        if (res.data.code === 200) {
          const payParams = res.data.data;

          // 3. 调起微信支付
          wx.requestPayment({
            timeStamp: payParams.timeStamp,
            nonceStr: payParams.nonceStr,
            package: payParams.package,
            signType: payParams.signType,
            paySign: payParams.paySign,
            success: (payRes) => {
              console.log('支付成功', payRes);
              // 跳转到成功页面
              wx.redirectTo({
                url: '/pages/payment/success?orderNo=' + payParams.orderNo
              });
            },
            fail: (payErr) => {
              console.log('支付失败', payErr);
              // 查询支付状态，判断是否已支付
              this.checkPaymentStatus(payParams.orderNo);
            }
          });
        } else {
          wx.showToast({
            title: res.data.message || '创建支付订单失败',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      }
    });
  }
});
```

#### 2. 轮询查询支付状态

```javascript
// 查询支付状态
function checkPaymentStatus(orderNo) {
  let checkCount = 0;
  const maxCheckCount = 30;

  const interval = setInterval(() => {
    checkCount++;

    wx.request({
      url: 'https://api.nianhua.com/api/v1/payment/status/' + orderNo,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      success: (res) => {
        if (res.data.code === 200) {
          const status = res.data.data.status;

          if (status === 'SUCCESS') {
            clearInterval(interval);
            wx.redirectTo({
              url: '/pages/payment/success?orderNo=' + orderNo
            });
          } else if (status === 'FAILED' || status === 'CLOSED') {
            clearInterval(interval);
            wx.showToast({
              title: '支付失败',
              icon: 'none'
            });
          }

          if (checkCount >= maxCheckCount) {
            clearInterval(interval);
            wx.showToast({
              title: '查询超时，请稍后重试',
              icon: 'none'
            });
          }
        }
      }
    });
  }, 4000);
}
```

#### 3. 支付成功页面

```xml
<!-- pages/payment/success.wxml -->
<view class="container">
  <view class="success-icon">✓</view>
  <view class="success-title">支付成功</view>
  <view class="order-info">
    <text>订单号：{{orderNo}}</text>
    <text>支付金额：{{amount}}元</text>
  </view>
  <button bindtap="goToOrderDetail">查看订单详情</button>
  <button bindtap="backToHome">返回首页</button>
</view>
```

## 注意事项

### 1. 金额单位

- 数据库: **分** (Long类型)
- 接口响应: **元** (前端显示)
- 微信支付: **分** (微信要求)

转换关系:
```java
// 元转分
Long fen = yuan * 100;

// 分转元
Long yuan = fen / 100;
```

### 2. 订单号复用

预订时生成的`order_no`会直接用于微信支付的`out_trade_no`，确保全流程订单号一致。

### 3. 超时处理

- 预订超时：10分钟未支付，自动取消预订，释放座位
- 支付超时：前端轮询2分钟，超时后提示用户手动查询

### 4. 幂等性

创建支付订单时，需要检查订单是否已存在，避免重复创建：

```java
Order existingOrder = orderMapper.selectByOrderNo(booking.getOrderNo());
if (existingOrder != null) {
    // 已存在，直接返回支付参数
    return buildPaymentResponse(existingOrder);
}
```

### 5. 错误处理

前端需要处理各种错误情况：
- 网络错误
- 参数错误
- 支付失败
- 查询超时

### 6. 安全问题

- APIv3密钥安全存储
- 商户私钥文件权限控制（600）
- 回调接口签名验证
- HTTPS强制使用

## 测试建议

### 1. 单元测试

- 创建支付订单逻辑
- 支付回调处理逻辑
- 金额转换逻辑

### 2. 集成测试

- 完整支付流程
- 支付回调通知流程
- 超时取消流程

### 3. 生产环境验证

- 使用微信沙箱环境测试
- 小额真实支付测试
- 支付失败、退款等异常流程测试

## 参考文档

- [微信支付JSAPI文档](https://pay.weixin.qq.com/doc/v3/merchant/4012365339)
- [统一下单接口](https://pay.weixin.qq.com/doc/v3/partner/4012738519)
- [支付结果通知](https://pay.weixin.qq.com/doc/v3/partner/4012365867)

## 相关文件

- OpenAPI/Swagger文档: `src/main/resources/static/wechat-pay-api.yaml`
- 支付Controller: `src/main/java/org/exh/nianhuawechatminiprogrambackend/controller/PaymentController.java`
- 支付Service: `src/main/java/org/exh/nianhuawechatminiprogrambackend/service/WechatPayService.java`
