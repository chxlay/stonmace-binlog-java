### binlog 相关封装，canal 框架台累赘，需要安装客户端，才可以使用客户端，所以再此封装一个可以直接使用的 binlog 客户端
#### 接收到 binlog 事件解析成 Message 数据进行发送，默认使用的是 SpringEvent 进行发送，实现接口 MessagePublisher 可扩展为 kafka Rabbit 等消息中间件进行发布