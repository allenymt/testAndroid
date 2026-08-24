# NTAG 424 DNA SDM 服务端验证流程

## 1. 适用范围

本文描述当前 Demo 的 File 02 SDM URL 在可信服务端的验证流程。当前 URL 中：

- `e` 是 16-byte `PICCENCData` 的 32 个十六进制字符；
- `c` 是 8-byte truncated `SDMMAC` 的 16 个十六进制字符；
- File 02 的 `SDMAccessRights` 传输字节为 `F1 21`，逻辑值为 `0x21F1`。

当前密钥角色映射如下：

| 数据 | NXP 密钥角色 | 当前 AppKey | 用途 |
| --- | --- | --- | --- |
| `e` / `PICCENCData` | `SDMMetaReadKey` | Key 2 | AES-CBC 解密 UID 和 SDMReadCtr |
| `c` / `SDMMAC` | `SDMFileReadKey` | Key 1 | 派生本次 session MAC key 并验证 MAC |
| 受保护计数器读取 | `SDMCtrRetKey` | Key 1 | NFC 认证后直接读取计数器；不参与普通 URL 验证请求 |

Key 1、Key 2 不是协议规定的固定编号。协议固定的是密钥角色，具体 AppKey 编号由卡片 File 02 的 `SDMAccessRights` 决定。卡片配置与服务端映射必须保持一致。

## 2. 完整验证顺序

```text
收到 SDM URL
  -> 严格解析并校验 e/c
  -> Key 2 解密 e
  -> 校验 PICCDataTag，提取 UID 和 SDMReadCtr
  -> Key 1 + UID + SDMReadCtr 派生 KSesSDMFileReadMAC
  -> 按 File 02 offsets 重算 c
  -> 常量时间比较 c
  -> 按 UID 原子检查并推进 SDMReadCtr
  -> 受理业务
```

验证必须先通过 `c`，再更新防重放状态。MAC 未通过的请求不能占用或推进计数器。

### 2.1 严格解析 URL

1. 解析 URL 的顶层 query，不要从嵌套 `targetUrl` 中误取同名参数。
2. 参数名按约定处理，要求 `e` 和 `c` 各出现且只出现一次。
3. 拒绝重复的 `e` 或 `c`，避免不同组件对重复参数采用“取第一个”或“取最后一个”的差异。
4. 要求 `e` 恰好为 32 个 hex 字符，解码后为 16 bytes。
5. 要求 `c` 恰好为 16 个 hex 字符，解码后为 8 bytes。
6. 对 URL 解码失败、缺失参数、非法字符和错误长度统一拒绝。

### 2.2 使用 Key 2 解密 e

当前 `e` 是 `PICCENCData`，使用 `SDMMetaReadKey`，也就是当前 profile 的 Key 2：

```text
PICCData = AES-128-CBC-Decrypt(
    key        = Key 2,
    iv         = 00000000000000000000000000000000,
    ciphertext = hexDecode(e),
    padding    = NoPadding
)
```

当前明文布局为一个 16-byte block：

```text
byte 0       PICCDataTag
byte 1..7    7-byte UID
byte 8..10   3-byte SDMReadCtr，little-endian
byte 11..15  RandomPadding
```

服务端必须校验 `PICCDataTag` 表示 UID 和 SDMReadCtr 均存在，并确认 UID 长度标志为 7 bytes。当前配置正常读到的 tag 为 `C7h`。不要在没有校验 tag 的情况下直接按固定 offset 信任解密结果。

计数器解析方式为：

```text
SDMReadCtr = plain[8]
           | plain[9] << 8
           | plain[10] << 16
```

### 2.3 使用 Key 1 派生 session MAC key

`c` 对应 `SDMMAC`，其根密钥角色是 `SDMFileReadKey`，也就是当前 profile 的 Key 1。先构造 16-byte session vector：

```text
SV2 = 3CC300010080 || UID || SDMReadCtr_LE
```

其中：

- `UID` 为从 `e` 解密得到的 7 bytes；
- `SDMReadCtr_LE` 为从 `e` 解密得到的原始 3-byte little-endian 计数器。

然后派生：

```text
KSesSDMFileReadMAC = AES-CMAC(Key 1, SV2)
```

这里不使用 Key 2。Key 2 负责 PICCData 元数据的加解密，Key 1 负责 SDMMAC session key 的派生。

### 2.4 重新计算 c

根据 File 02 settings 中配置的 offsets，取得与芯片完全一致的 MAC 输入：

```text
macInput = DynamicFileData[SDMMACInputOffset, SDMMACOffset)
fullMac  = AES-CMAC(KSesSDMFileReadMAC, macInput)
cServer  = fullMac[1] || fullMac[3] || fullMac[5] || fullMac[7]
        || fullMac[9] || fullMac[11] || fullMac[13] || fullMac[15]
```

当前 Demo 配置：

```text
SDMMACInputOffset == SDMMACOffset
```

因此当前 `macInput` 是空 byte array，仍然需要按照 AES-CMAC 的空消息规则计算，不能把结果直接写死。

如果以后调整 offsets，使 MAC 覆盖 URL 中的业务数据，服务端必须使用动态 File 02 中完全相同的 byte 区间。字符编码、URL 转义、URI prefix code 和 NDEF record header 的任何差异都会导致 MAC 不一致。

### 2.5 常量时间比较

将 URL 中的 8-byte `c` 与服务器计算出的 `cServer` 进行常量时间比较：

```text
constantTimeEquals(cServer, hexDecode(c))
```

不能使用普通字符串相等比较替代密码学字节比较。当前 Android Mock 使用 `MessageDigest.isEqual`。

### 2.6 防重放检查

只有 MAC 验证成功后，才按 UID 检查 `SDMReadCtr`：

```text
previous = latestCounter[UID]

if previous exists and SDMReadCtr <= previous:
    reject as replay
else:
    latestCounter[UID] = SDMReadCtr
    accept
```

生产服务必须把以下动作放在同一个数据库事务或等价的原子操作中：

1. 读取该 UID 已接受的最大计数器；
2. 比较新计数器；
3. 条件更新为新计数器；
4. 记录或受理对应业务请求。

仅用进程内 Map 无法覆盖服务重启、多实例和并发请求。数据库更新应使用条件更新、行锁或唯一约束，确保同一个计数器最多成功一次。

计数器耗尽、卡片重新个性化或服务端丢失状态时应进入明确的异常处置流程，不能静默降低为“只验证 MAC”。

## 3. 服务端伪代码

```text
verifySdmUrl(url):
    params = parseUniqueTopLevelQuery(url)
    e = requireHex(params["e"], 16 bytes)
    c = requireHex(params["c"], 8 bytes)

    piccData = aes128CbcDecrypt(key2, zeroIv, e, noPadding)
    requireValidPiccDataTag(piccData[0])

    uid = piccData[1..7]
    counterBytes = piccData[8..10]
    counter = littleEndian24(counterBytes)

    sv2 = hex("3CC300010080") || uid || counterBytes
    sessionMacKey = aesCmac(key1, sv2)

    macInput = resolveMacInputFromFile02Offsets(url)
    fullMac = aesCmac(sessionMacKey, macInput)
    expectedC = oddIndexedBytes(fullMac)

    require(constantTimeEquals(expectedC, c))
    require(replayStore.acceptIfGreaterAtomically(uid, counter))

    return Verified(uid, counter)
```

当前 profile 的 `resolveMacInputFromFile02Offsets` 返回空 byte array。

## 4. 验证失败分类

| 阶段 | 失败示例 | 处理 |
| --- | --- | --- |
| URL 解析 | 缺少、重复或非法 `e/c` | 拒绝，不执行密码学计算 |
| `e` 解密 | 长度错误、PICCDataTag 错误 | 拒绝，不更新计数器 |
| `c` 验证 | MAC 不一致 | 拒绝，不更新计数器 |
| 防重放 | 计数器等于或小于已接受值 | 拒绝，不受理业务 |
| 服务状态 | 数据库不可用、原子更新失败 | 默认拒绝，不能绕过防重放 |

对外响应不应暴露“Key 错误”“UID 解密成功”等过细信息；详细原因只进入受控日志，并避免记录密钥、解密明文或完整可重放 URL。

## 5. 当前 Demo 与生产实现的边界

当前 `Ntag424MockServerVerifier` 用于本地协议联调：

- Key 1、Key 2 是 APK 内固定 Demo key；
- 防重放状态仅存在测试 Activity 进程内；
- 当前 MAC 输入为空；
- 读卡后由 App 自动取出动态 URI 并调用 Mock verifier。

生产实现应：

- 将 SDM key 保存在服务端 KMS/HSM，不下发到普通客户端；
- 按卡片、批次或 key version 正确选择密钥，并支持安全轮换；
- 持久化且原子更新每个 UID 的计数器状态；
- 先验证 MAC，再将 UID、计数器与业务对象绑定；
- 对失败率、计数器异常和重复请求建立审计与告警。

## 6. 容易混淆的 SDMENCFileData

当前 URL 的 `e` 是 `PICCENCData`，由 `SDMMetaReadKey`（当前 Key 2）直接使用 AES-CBC 解密。

`SDMENCFileData` 是另一种可选的 File 02 业务数据加密镜像，它使用 `SDMFileReadKey`（当前 Key 1）派生的 `KSesSDMFileReadENC`。当前 Demo 没有启用 `SDMENCFileData`，不能把它的 Key 1 解密规则套用到 `e`。

## 7. 参考依据

- NXP AN12196 Rev. 2.0：§3.3、§3.4.2、§3.4.4、§5.4；
- NTAG 424 DNA datasheet Rev. 3.0：§8.2.3.4、Table 69；
- 密钥映射专项核对：[sdm_e_c_key_mapping_research.md](sdm_e_c_key_mapping_research.md)；
- 当前 Mock 实现：`app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Ntag424MockServerVerifier.kt`。
