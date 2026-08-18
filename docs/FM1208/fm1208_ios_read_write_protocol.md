# FM1208 FMCOS iOS 读写协议

## 1. 适用范围

本文描述当前测试卡的业务数据读写协议，供 iOS Core NFC 联调使用。它不是通用 FM1208/FMCOS 发卡规范。

| 项目 | 当前测试值 |
| --- | --- |
| 卡片 UID | `77:E9:CC:7D` |
| 通信方式 | ISO 14443-4 / ISO-DEP / ISO 7816 APDU |
| FMCOS 应用名 | ASCII `ADF01`，十六进制 `4144463031` |
| 业务 EF | FID `0003`，透明二进制文件 |
| EF 容量 | `0x0400`（1024 字节） |
| 最大业务数据 | 1022 字节（扣除 2 字节长度） |
| 测试认证密钥 | DES `FFFFFFFFFFFFFFFF` |
| 当前测试 URL | `https://yl.test.com/nfc` |
| Android 包名 | `com.vdian.android.lib.testforgradle` |

该卡不是 NFC Forum Type 4 NDEF Tag，选择标准 NDEF AID `D2760000850101` 会返回 `6A82`。iOS 必须使用 `NFCTagReaderSession` 和 `NFCISO7816Tag` 主动读卡，不能使用系统 NDEF 后台读取路径唤起 App。

## 2. iOS 工程配置

1. 开启 Near Field Communication Tag Reading capability。
2. 在 `Info.plist` 配置 `NFCReaderUsageDescription`。
3. 在 `Info.plist` 的 `com.apple.developer.nfc.readersession.iso7816.select-identifiers` 中加入 `4144463031`。
4. 使用 `NFCTagReaderSession(pollingOption: [.iso14443])` 扫描，在 `didDetect` 中确认标签为 `.iso7816`，调用 `session.connect(to:)` 后发送 APDU。

发送命令时应检查独立返回的 `SW1/SW2`。只有 `9000` 视为成功；任一步失败都应停止，不要继续执行后续写入。

```swift
func send(_ bytes: [UInt8], to tag: NFCISO7816Tag,
          completion: @escaping (Result<Data, Error>) -> Void) {
    guard let apdu = NFCISO7816APDU(data: Data(bytes)) else { return }
    tag.sendCommand(apdu: apdu) { data, sw1, sw2, error in
        // 先处理 error，再要求 sw1 == 0x90 && sw2 == 0x00。
    }
}
```

## 3. 会话认证

每次新的 NFC 会话，以及重新选择 ADF 后，都重新执行认证。

### 3.1 选择业务 ADF

```text
-> 00 A4 04 00 05 41 44 46 30 31
<- [FCI] 90 00
```

### 3.2 获取随机数

```text
-> 00 84 00 00 04
<- R0 R1 R2 R3 90 00
```

卡片必须返回 4 字节随机数。构造 8 字节明文：

```text
R0 R1 R2 R3 00 00 00 00
```

使用测试密钥 `FF FF FF FF FF FF FF FF` 执行 `DES/ECB/NoPadding` 加密，得到 8 字节认证数据 `C0...C7`。CryptoKit 不提供 DES，可使用 CommonCrypto 或项目已审核的 DES 实现。

### 3.3 外部认证

```text
-> 00 82 00 00 08 C0 C1 C2 C3 C4 C5 C6 C7
<- 90 00
```

随机数每次不同，认证 APDU 不能缓存或重放。

## 4. 业务数据格式

EF `0003` 内容定义为：

```text
Offset 0: 2-byte unsigned big-endian NDEF_LENGTH
Offset 2: NDEF_LENGTH bytes 的原始 NDEF Message
其余空间不参与解析
```

NDEF Message 固定包含两条短记录：

1. URI Record：TNF `0x01`，Type `U` (`55`)，payload 为 URI 前缀码加 UTF-8 URI。当前 HTTPS URL 使用前缀码 `04`，其后保存 `yl.test.com/nfc`。
2. Android Application Record：TNF `0x04`，Type 为 ASCII `android.com:pkg`，payload 为 UTF-8 Android 包名。iOS 不使用该包名启动应用，但必须保留它以保证 Android 兼容。

当前联调工具应限制每条 record 的 payload 小于 256 字节，使用 NDEF Short Record (`SR=1`) 编码；禁止截断超长 URL 或包名。未来如需支持更长数据，应按 NFC Forum NDEF 规范改用 4 字节大端 payload length，并与 Android 重新做字节一致性测试。

当前默认 URL 和包名编码后，NDEF 长度为 `0x0049`（73 字节），完整 EF 有效数据为：

```text
0049
9101105504796C2E746573742E636F6D2F6E6663
540F23616E64726F69642E636F6D3A706B67
636F6D2E766469616E2E616E64726F69642E6C69622E74657374666F72677261646C65
```

以上换行仅用于展示，写卡时必须连接为连续字节。

## 5. 读取流程

1. 选择 `ADF01`。
2. 完成外部认证。
3. 选择业务 EF：

```text
-> 00 A4 00 00 02 00 03
<- 90 00
```

4. 读取两字节长度：

```text
-> 00 B0 00 00 02
<- LEN_H LEN_L 90 00
```

5. 按大端计算 `length = LEN_H << 8 | LEN_L`。要求 `0 <= length <= 1022`。
6. 从 offset `2` 开始读取 `length` 字节。单次最多读取 `0xF0`（240）字节：

```text
00 B0 OFFSET_H OFFSET_L LE
```

每次把 offset 增加实际返回的数据长度，直到读满。不要把末尾 `SW1/SW2` 放入 NDEF 数据。长度为 0 表示业务内容为空。

7. 解析完整 NDEF Message，提取第一条 URI record；需要跨端校验时再读取 `android.com:pkg` record。

## 6. 写入流程

写入前先校验 URL 为有效 HTTPS URL，并保证编码后的 NDEF Message 不超过 1022 字节。

1. 选择 `ADF01`、完成认证、选择 EF `0003`。
2. 先把长度清零，防止中途移卡后暴露半条有效消息：

```text
-> 00 D6 00 00 02 00 00
<- 90 00
```

3. 从 offset `2` 开始分段写入 NDEF Message，单段最多 `0xF0` 字节：

```text
00 D6 OFFSET_H OFFSET_L LC DATA...
```

4. 所有 payload 分段均返回 `9000` 后，最后提交两字节大端长度：

```text
00 D6 00 00 02 LEN_H LEN_L
```

5. 重新选择 ADF、重新认证并完整读回；只有读回字节与写入 NDEF Message 完全一致才报告成功。

当前 73 字节示例可单段写入：

```text
清长度: 00D60000020000
写 payload: 00D6000249 + 上述 73 字节 NDEF Message
提交长度: 00D60000020049
```

## 7. 状态码与异常处理

| 状态字 | 处理 |
| --- | --- |
| `9000` | 成功 |
| `6A82` | ADF 或 EF 不存在；卡片不是当前 profile |
| `6982` | 安全状态不满足；通常是未认证、认证失效或权限不符 |
| `6A86` | P1/P2 不正确 |
| `6700` | Lc/Le 长度错误 |
| `63Cx` | 认证失败且可能剩余 `x` 次尝试；立即停止，禁止循环重试 |

标签移出射频场、Core NFC 会话失效或任何 APDU 返回非 `9000` 时，应终止本次操作并提示用户移开卡片后重新扫描。

## 8. 安全边界与验收

- 全 `FF` DES 密钥和固定文件 profile 只允许用于当前可反复格式化的测试卡，禁止用于生产。
- iOS 端不得发送 `80 0E`（擦除）、`80 E0`（CREATE）或 `80/84 D4`（WRITE KEY）。
- 日志可以记录 APDU header、offset、长度和状态字，不得输出 DES 密钥、认证密文或完整业务 payload。
- 首轮联调依次验证：读出当前 URL/包名、写入测试 URL、立即读回一致、移卡重扫后仍一致、写入过程中移卡不会留下非零长度的半条消息。
- Android 参考实现位于 `Fm1208TestCardProtocol.kt`、`Fm1208FmcosClient.kt` 和 `Fm1208CardClient.kt`；如双方结果不一致，以 APDU 和 EF 原始字节逐项对比。
