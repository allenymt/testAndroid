# NTAG 424 首次唤起与二次认证读取方案

## 1. 业务目标

用户首次碰卡时，由手机系统读取 NDEF 并唤起 App；App 启动后，再主动连接卡片并通过 EV2 双向认证读取敏感业务数据。

本方案只讨论读取，不涉及写卡和个性化。

## 2. 两次读取职责

### 第一次：系统读取 File 02

```text
卡片
  -> Android NFC 系统读取 File 02
  -> 解析 URI / pkg
  -> 唤起业务 App
```

File 02 必须保持系统可读，不能要求系统在发现 NDEF 前完成 EV2 认证。

File 02 保存公开入口信息，例如：

- 业务 URI、域名和跳转参数；
- `android.com:pkg` record；
- SDM 动态字段 `e/c`（如果启用 SDM）。

如果启用 SDM，第一次读取会触发芯片生成新的 `e/c`，并使 `SDMReadCtr` 增长。后台可以使用 Key 2 解密 `e`、使用 Key 1 验证 `c`，确认公开消息来自合法卡片，但这一步不认证读卡器。

### 第二次：App 主动读取敏感数据

App 被唤起后，不应把第一次系统读取的 NDEF 当作敏感数据凭证，而应重新通过 `IsoDep` 连接卡片：

```text
App
  -> Select Application
  -> AuthenticateEV2First(ReadKey)
  -> 双向挑战-响应
  -> 派生 SesAuthENCKey / SesAuthMACKey
  -> Full ReadData
  -> 校验响应 MAC 并解密数据
```

这一步用于确认当前 App 掌握合法密钥，并读取受保护业务数据。

## 3. 文件规划

| 文件 | 内容 | 首次系统读取 | App 二次读取 |
| --- | --- | --- | --- |
| File 02 (`E104`) | NDEF URI、pkg、SDM `e/c` | 可读，不需要 EV2 认证 | 不作为敏感数据通道 |
| File 03 (`E105`) | 订单、权益、状态等敏感业务数据 | 不由系统 NDEF 流程读取 | EV2 Full 认证后读取 |

推荐保持：

```text
File 02：公开可读，用于发现业务入口
File 03：受权限保护，用于 App 内读取敏感数据
```

如果把 File 02 的 Read 权限改为需要 Key，Android 系统可能无法读取 NDEF，也就无法完成首次唤起，因此不适合当前业务模型。

## 4. SDM 与 EV2 的分工

### SDM/SUN

```text
系统读 File 02
  -> 卡片生成 e/c
  -> 后台验证 e/c 和 Counter
```

解决“公开动态消息是否由合法卡片生成”，不要求读卡器先证明自己。

### EV2 Full Secure Messaging

```text
App <-> 卡片
  -> AuthenticateEV2First
  -> 双方验证密钥
  -> 派生会话 ENC/MAC Key
  -> Full ReadData
```

解决“是否为授权 App，以及敏感数据是否受保护”。

两者可以同时启用，但不能互相替代。

## 5. 二次读取流程

```text
用户碰卡
  -> 系统读取 File 02
  -> 解析 URI 并唤起 App
  -> App 获取业务会话/权限上下文
  -> App 连接 IsoDep
  -> 使用 File 03 Read 权限对应的 Key 执行 AuthenticateEV2First
  -> 派生会话密钥
  -> Full ReadData(File 03)
  -> 校验 Response MAC
  -> 解密并解析敏感数据
  -> 将敏感数据绑定当前用户和业务会话
```

每次重新认证都会产生新的会话密钥和新的 `TI/CmdCtr` 状态，不能跨连接复用。

## 6. Random UID 注意事项

开启 Random UID 后：

- 第一次系统读取和第二次 App 读取看到的 RF UID 可能不同；
- 不能用 Android `Tag.id` 判断两次读取是否为同一张卡；
- 不能用 RF UID 直接选择长期密钥或作为永久数据库主键；
- 当前实卡验证表明，SDM `e` 解密后仍可取得真实 UID；
- 服务端应使用 `e` 中的真实 UID、profile 或 generation 关联卡片状态。

如果第二次读取再次读取开启 SDM 的 File 02，会生成新的 `e/c`，并使 `SDMReadCtr` 再次增长。不要重复提交第一次读取得到的旧 URL。

## 7. 核心结论

```text
第一次碰卡：File 02 必须公开可读，用于系统识别 NDEF 并唤起 App。

第二次读取：由 App 主动通过 IsoDep 执行 EV2 双向认证和 Full ReadData，读取 File 03 等敏感数据。

SDM 负责公开消息防伪，EV2 Full 负责受控 App 访问和敏感数据保护。
```
