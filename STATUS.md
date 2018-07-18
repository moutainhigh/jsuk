# 数据库中的复杂状态

> 所有的状态以此文档为准

### 快递跑腿订单状态

* 状态码在用户端和骑手端不一样

| 状态码 | 状态（用户） | 状态（骑手） |
| :--- | :--- | :--- |
| 0 | 取消 |  取消|
| 1 | 待支付 |  待支付|
| 2 | 待接单 | 待抢单 |
| 3 | 待送货 | 待取货 |
| 4 | 待送货 | 待送达 |
| 5 | 待评价 | 完成   |
| 6 | 完成  |    完成    |

### 订单状态

> 订单的受理 拒绝 有没有？

| 状态码 | 状态| 商家 | 用户 |
| :--- | :---|:--- | :---|
|   |  | 取消         |      |
|   |  |待买家付款    |待付款   |
|   |  | 待发货       |待发货     |
|   |  |             | 待收货    |
|   |  | 售后         | 售后    |
|   |  | 已完成       |      |
|   | 退款 |        |      |
|   |  退货 |        |      |

### 订单退款状态

|状态码 | 状态|
|---| --- |
| |待处理 |
| |已处理|
| |已拒绝|

### 订单退货状态

|状态码 | 状态|
|---| --- |
| |待处理 |
| |退货中|
| |已完成|
| |已拒绝|
