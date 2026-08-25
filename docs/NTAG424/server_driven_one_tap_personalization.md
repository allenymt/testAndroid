# NTAG 424 DNA 服务端驱动的一次碰卡个性化方案

## 1. 目标

服务端生成并下发一次 NTAG 424 DNA 个性化任务，App 在一次 NFC 贴卡会话中完成：

1. 将 AppKey 0、AppKey 1、AppKey 2 从 old key 更新为 new key；
2. 将服务端生成的完整 NDEF message 写入 File 02；
3. NDEF 第一条 URL record 已完整包含业务 query、`e=`、`c=` 和固定长度占位符；
4. NDEF 第二条为 `android.com:pkg` record；
5. 根据该 NDEF 的实际 byte offsets 开启 File 02 SDM；
6. 回读并确认卡片最终状态，将结果上报服务端；
7. 后续扫码由服务端使用 new Key 2 解密 `e`、使用 new Key 1 验证 `c`。

本方案只描述技术设计，不修改当前 Demo 代码。

## 2. 关键结论

### 2.1 一次碰卡不等于芯片事务

“一次碰卡”表示在一次 NFC 射频连接中连续执行多个 APDU，并不表示这些命令在卡片内部组成可回滚事务。

以下操作一旦成功就会立即产生持久化效果：

- `ChangeKey(1)`；
- `ChangeKey(2)`；
- File 02 `WriteData`；
- File 02 `ChangeFileSettings`；
- `ChangeKey(0)`。

如果中途移开卡片，已完成的步骤不会自动恢复为 old state。因此方案必须依靠：

- 修改前完整预检；
- 可恢复的命令顺序；
- old/new key version；
- 幂等重试；
- 服务端任务状态机；
- 最终回读确认。

### 2.2 物理执行顺序不能按 Key 0、1、2 排列

业务目标虽然是更新 Key 0、1、2，但卡内推荐执行顺序为：

```text
Key 1
  -> Key 2
  -> Key 0（最后一把修改的 Key）
  -> 丢弃旧 session
  -> 使用 new Key 0 重新认证
  -> 写完整 NDEF
  -> 开启 SDM（最后一个持久化配置步骤）
  -> 回读确认
```

原因是当前卡片配置下，`ChangeKey`、File 02 写入和 `ChangeFileSettings` 都依赖 Key 0 授权。Key 1/2 必须在 old Key 0 管理会话仍然有效时优先修改；Key 0 作为最后一把 Key 修改后，立即使用 new Key 0 建立新的管理会话，再完成文件操作。

修改非当前认证 Key 1/2 后，Key 0 secure session 可以继续使用；修改当前认证 Key 0 后，必须丢弃旧 session、TI 和 CmdCtr，并使用 new Key 0 重新执行 `AuthenticateEV2First`。

SDM 必须在目标 NDEF 完整写入并验证后最后开启。这样不会在动态 URL 已对外生效后继续改写 File 02，也避免重复开启造成新的 counter epoch。

### 2.3 服务端提供完整 NDEF，App 不再拼业务 URL

服务端是 NDEF 模板的唯一生成方。App 不再负责：

- 组装 `targetUrl`；
- 决定使用 `?e=` 还是 `&e=`；
- 添加 package name；
- 添加 `e/c` 参数；
- 决定占位符长度。

App 只接收 canonical NDEF message bytes，并做结构和安全校验。当前 AES ASCII profile 要求：

```text
e=00000000000000000000000000000000   // 32 ASCII chars
c=0000000000000000                   // 16 ASCII chars
```

芯片开启 SDM 后，只在未认证读取响应中动态替换占位区域，不会解析 URL，也不会将动态 `e/c` 写回 File 02。

## 3. 系统边界

```text
发卡服务 / KMS / HSM
  -> 创建 personalization job
  -> 生成 old/new Key 0/1/2 引用和 key version
  -> 生成完整 NDEF message
  -> 计算并签名 NDEF hash、SDM offsets 和目标 profile
  -> 将密钥封装给被授权设备

Android 个性化 App
  -> 下载并验证任务
  -> 只在内存中解封 key material
  -> 贴卡后预检卡片状态
  -> 执行可恢复的复合 APDU 流程
  -> 回读最终状态
  -> 上报 receipt
  -> 清除内存中的 key material

NTAG 424 DNA
  -> 更新 AppKey 1/2
  -> 写 File 02 静态模板
  -> 开启 SDM 并重置 SDMReadCtr
  -> 更新 AppKey 0

SDM 验证服务
  -> 根据 profile/keySet 选择 new Key 2 和 new Key 1
  -> 解密 e、验证 c
  -> 按 UID + personalizationGeneration 防重放
```

## 4. 服务端任务数据模型

### 4.1 推荐接口

```text
POST /ntag424/personalization-jobs/{jobId}/claim
POST /ntag424/personalization-jobs/{jobId}/receipts
GET  /ntag424/personalization-jobs/{jobId}
```

`claim` 必须是短时、一次性、绑定操作员/设备的授权动作。任务完成、过期或被其他设备占用后，不允许重复领取明文 key material。

### 4.2 推荐响应结构

下面展示字段语义。生产接口不应直接返回可长期复用的明文 Base64 key，而应返回设备绑定的加密 envelope。

```json
{
  "jobId": "pjob_20260824_000001",
  "profileId": "ntag424-aes-sdm-v3",
  "personalizationGeneration": 3,
  "expiresAt": "2026-08-24T18:00:00+08:00",
  "expectedUid": "optional-7-byte-uid-hex",
  "keyEnvelope": "base64(device-bound-encrypted-envelope)",
  "sourceState": {
    "sdmPolicy": "REQUIRE_DISABLED",
    "allowedFileSettingsHash": "hex-sha256-or-null",
    "allowedNdefMessageSha256": "hex-sha256-or-null"
  },
  "keys": [
    {
      "keyNo": 0,
      "oldVersion": 2,
      "newVersion": 3,
      "oldKeyRef": "kms://.../key0/v2",
      "newKeyRef": "kms://.../key0/v3"
    },
    {
      "keyNo": 1,
      "oldVersion": 2,
      "newVersion": 3,
      "oldKeyRef": "kms://.../key1/v2",
      "newKeyRef": "kms://.../key1/v3"
    },
    {
      "keyNo": 2,
      "oldVersion": 2,
      "newVersion": 3,
      "oldKeyRef": "kms://.../key2/v2",
      "newKeyRef": "kms://.../key2/v3"
    }
  ],
  "ndef": {
    "messageBase64": "base64(canonical NDEF message bytes without NLEN)",
    "messageSha256": "hex-sha256",
    "fileSize": 256,
    "piccDataOffset": 148,
    "sdmMacInputOffset": 183,
    "sdmMacOffset": 183
  },
  "sdm": {
    "mode": "AES",
    "sdmOptions": "C1",
    "sdmAccessRightsWire": "F121",
    "sdmMetaReadKeyNo": 2,
    "sdmFileReadKeyNo": 1,
    "sdmCtrRetKeyNo": 1
  },
  "targetFileSettings": {
    "communicationMode": "PLAIN_OR_CURRENT",
    "readKey": 14,
    "writeKey": 0,
    "readWriteKey": 0,
    "changeKey": 0
  },
  "planSignature": "base64(server-signature)"
}
```

示例 offset 仅用于展示字段，不能写死。服务端必须基于本次 canonical NDEF bytes 计算，App 必须独立复算并比对。

`sourceState.sdmPolicy` 建议支持两种明确模式：

```text
REQUIRE_DISABLED
  -> 只接受初始 SDM 已关闭的卡片

MIGRATE_FROM_DECLARED_PROFILE
  -> 只接受 settings/NDEF hash 与服务端声明的旧 profile 完全一致的卡片
  -> 在任何 ChangeKey 前，使用当前 Key 0 关闭旧 SDM
  -> 不必清理旧 e/c，因为随后会被目标完整 NDEF 覆盖
```

不允许使用“发现 SDM 已开就无条件关闭”的隐式策略。

### 4.3 NDEF 数据边界

接口统一下发：

```text
ndef.messageBase64 = NDEF message bytes，不包含 2-byte NLEN
```

App 写卡前构造：

```text
File 02 image = NLEN(2-byte big-endian) || NDEF message bytes
```

这样可以明确：

- `messageSha256` 对哪一段 bytes 计算；
- NLEN 由 App 根据 message length 严格生成；
- SDM offsets 相对于 `NLEN || NDEF message` 的 File 02 起始位置；
- 重试时可以精确比较静态 File 02 image。

如果接口选择直接下发完整 `NLEN || message`，也可以实现，但必须另设格式版本并禁止两种形式混用。

### 4.4 Key 数据边界

每个 key entry 必须同时包含：

```text
keyNo
oldVersion
newVersion
oldKey material
newKey material
```

要求：

- key value 固定为 16 bytes AES-128；
- oldVersion 和 newVersion 必须不同；
- version 在同一张卡的可恢复窗口内不能复用；
- Key 0/1/2 的 new key value 必须相互独立；
- key version 是状态路由信息，不是 key value 的密码学证明；
- App 对已经处于 newVersion 的 key，仍应通过认证或已验证 receipt 判断其 material 是否正确。

服务端必须保存 old/new 两套 key，直到任务确认完成或进入人工恢复状态，不能在 App 领取任务后立即删除 old keys。

## 5. 为什么静态 URL 需要 profile/keySet 路由信息

服务端收到动态 URL 后，必须先选择 Key 2 才能解密 `e`，但 UID 又位于加密后的 `e` 内。如果不同卡片或批次使用不同 Key 2，不能先依赖 UID 选择 Key 2，否则会形成循环依赖。

因此完整 URL 应至少包含一个非敏感、稳定的路由信息，例如：

```text
https://example.com/ntag424/verify
  ?pid=ntag424-aes-sdm-v3
  &cardRef=opaque-issued-card-reference
  &e=00000000000000000000000000000000
  &c=0000000000000000
```

或者通过 URL path 唯一确定 profile/keySet：

```text
https://example.com/ntag424/v3/{opaqueCardRef}
  ?e=<placeholder>
  &c=<placeholder>
```

服务端流程为：

1. 使用 `pid/cardRef` 找到 pending/active personalization generation；
2. 从 KMS/HSM 加载该 generation 的 Key 2；
3. 解密 `e`，得到 UID 和 SDMReadCtr；
4. 校验 UID 与 `cardRef` 的发行记录一致；
5. 加载同一 generation 的 Key 1；
6. 验证 `c`；
7. 在 `(UID, personalizationGeneration)` 命名空间中检查 counter。

`cardRef` 不应直接暴露 key material。当前 profile 的额外 MAC input 为空，因此静态业务 query 不直接包含在 SDMMAC 输入内；最终仍必须通过成功解密、成功 MAC 和 UID 发行绑定共同确认请求。

## 6. App 下载任务后的离线预检

在贴卡前完成所有不依赖卡片的检查，减少卡片停留时间。

### 6.1 任务安全检查

- 校验 HTTPS、服务端身份和 `planSignature`；
- 校验任务未过期、未撤销、未被其他设备完成；
- 校验任务绑定的 operator、device 和业务环境；
- 解封 `keyEnvelope`，但只保存在当前操作的内存中；
- 禁止写入 SharedPreferences、数据库、崩溃日志和埋点；
- 操作结束后覆盖可控 byte arrays 并释放引用。

### 6.2 Key 检查

- 必须恰好包含 Key 0、1、2；
- old/new key 都必须为 16 bytes；
- oldVersion != newVersion；
- key number、version 和 envelope 内容必须与签名 plan 一致；
- 不接受未知 Key 3/4 变更或隐式全零默认值；
- 不允许 App 在缺少 old key 时“猜测”卡片状态。

### 6.3 NDEF 检查

解码 `messageBase64` 后，App 必须验证：

1. SHA-256 与 `messageSha256` 一致；
2. 能解析为一个完整 NDEF message；
3. 恰好包含预期 records；
4. 第一条是 URI record；
5. 第二条是合法的 `android.com:pkg` record；
6. 顶层 query 中 `e`、`c` 各出现且只出现一次；
7. `e` 恰好等于 32 个 ASCII `0`；
8. `c` 恰好等于 16 个 ASCII `0`；
9. `pid/cardRef` 与任务一致；
10. `NLEN || message` 不超过目标 File 02 的 256-byte 容量；
11. 在完整 File 02 image 中，占位 marker 各出现且只出现一次；
12. App 复算的 offsets 与服务端下发值完全一致；
13. offsets 和动态字段长度均位于 File 02 边界内。

任一检查失败都必须在贴卡前拒绝任务，不能由 App 自动修复、重新拼 URL 或重新编码 NDEF。

## 7. 贴卡后的卡片预检

所有不可逆操作必须在预检全部通过之后开始。

### 7.1 识别卡片

```text
SELECT NDEF application
GET VERSION
GET KEY VERSION 0
GET KEY VERSION 1
GET KEY VERSION 2
GET FILE SETTINGS 02
```

检查：

- 芯片型号和版本属于支持的 NTAG 424 DNA profile；
- 若任务包含 `expectedUid`，实际 UID 必须一致；
- 如果任务采用首次贴卡绑定，必须在第一条不可逆命令前向用户明确显示并锁定实际 UID；
- File 02 size、communication mode、Change/Write rights 满足任务；
- ChangeKey 配置允许由当前 Key 0 管理；
- 每个 KeyVersion 只能处于该任务声明的 oldVersion 或 newVersion；
- File 02 SDM 状态只能是允许的初始状态或可证明的本任务恢复状态。

### 7.2 根据当前 Key 0 状态认证

```text
Key 0 version == oldVersion
  -> AuthenticateEV2First(old Key 0)

Key 0 version == newVersion
  -> AuthenticateEV2First(new Key 0)

其他 version
  -> 拒绝自动操作，进入人工恢复
```

认证成功才证明 key version 对应的 key material 确实正确。不能只根据 `GetKeyVersion` 决定继续写卡。

### 7.3 初始状态与恢复状态

新任务的正常初始状态：

```text
Key 0/1/2 均为 oldVersion
File 02 SDM disabled
File 02 权限允许当前 Key 0 修改和写入
```

如果这是从已开启 SDM 的旧 profile 迁移，任务必须显式声明 `MIGRATE_FROM_DECLARED_PROFILE`。App 使用当前 Key 0 认证，认证态读取并确认旧静态 NDEF/settings hash，然后先关闭旧 SDM，再开始修改 Key 1/2。这样个性化过程中不会对外持续产生一部分使用 old keys、一部分使用 new keys 的动态 URL。

允许自动恢复的中间状态必须能够由同一任务唯一解释，例如：

```text
Key 1 已 new，Key 2/0 仍 old
Key 1/2 已 new，Key 0 仍 old
Key 0/1/2 已 new，但目标 NDEF 尚未写入
目标 NDEF 已写入，但 SDM 尚未开启
全部完成，但 receipt 尚未上传
```

如果 SDM 已开启但静态 NDEF hash、offsets、access rights 或 generation 与本任务不一致，必须拒绝自动覆盖，不能把未知卡片状态当作本任务恢复状态。

### 7.4 将允许的源状态归一化为 SDM disabled

```text
source policy = REQUIRE_DISABLED
  -> settings 必须已经是 SDM disabled

source policy = MIGRATE_FROM_DECLARED_PROFILE
  -> 使用当前 Key 0 认证
  -> 认证态读取静态 File 02
  -> 校验旧 NDEF/settings hash
  -> ChangeFileSettings 关闭旧 SDM
  -> 回读确认 disabled
```

关闭旧 SDM 是迁移任务明确授权的预处理步骤，不改变写保护，也不把最后一次动态 `e/c` 写回文件。旧模板无需单独清理，因为后续 NLEN 发布流程会完整覆盖目标 NDEF。

## 8. 一次碰卡执行流程

### 8.1 总体顺序

```text
贴卡
  -> 连接 ISO-DEP
  -> 卡片预检
  -> 用当前 old/new Key 0 认证
  -> 按 source policy 确保旧 SDM 已关闭
  -> 确保 Key 1 为 new
  -> 确保 Key 2 为 new
  -> Key 0 作为最后一把 Key 切换为 new
  -> 丢弃旧 secure session
  -> 使用 new Key 0 重新认证
  -> 确保 File 02 静态 NDEF 等于目标 image
  -> 确保 File 02 SDM settings 等于目标 profile
  -> 回读 KeyVersion、静态 NDEF 和 FileSettings
  -> 生成 receipt
  -> 关闭 ISO-DEP
  -> 上报服务端
```

“确保”为幂等动作：已处于目标状态时验证并跳过，处于 old state 时执行变更，未知状态时停止。

### 8.2 Key 1 更新

当前认证 Key 为 Key 0，目标 Key 1 与当前认证 Key 不同：

```text
ChangeKeyData = (newKey1 XOR oldKey1)
              || newVersion1
              || CRC32(newKey1)
```

执行：

```text
ChangeKey(1, ChangeKeyData), Full secure messaging
GET KEY VERSION 1
```

如果贴卡预检发现 Key 1 已是 newVersion，建议用 new Key 1 执行一次认证证明 material 正确，再重新使用当前 Key 0 建立管理 session。若任务 receipt 已经证明该步骤成功，也可按受控恢复策略减少重复认证。

### 8.3 Key 2 更新

与 Key 1 相同：

```text
ChangeKeyData = (newKey2 XOR oldKey2)
              || newVersion2
              || CRC32(newKey2)

ChangeKey(2, ChangeKeyData), Full secure messaging
GET KEY VERSION 2
```

Key 2 更新成功后，未来动态 URL 的 `e` 将由 new Key 2 加密；服务端必须保持该 keySet 处于 pending，直到任务最终确认。

### 8.4 Key 0 最后更新

在 Key 1/2 均确认完成后，将 Key 0 作为最后一把 Key 修改：

```text
ChangeKeyData = newKey0 || newVersion0
ChangeKey(0, ChangeKeyData), Full secure messaging
```

修改当前认证 Key 0 后：

1. 不再使用旧 session 的 TI、CmdCtr、session ENC key 或 session MAC key；
2. 重新 `SELECT NDEF application`；
3. 使用 new Key 0 执行新的 `AuthenticateEV2First`；
4. 确认 KeyVersion 0/1/2 均为目标 newVersion；
5. 只有新认证成功，才能继续写 File 02 和开启 SDM。

如果 `ChangeKey(0)` 响应丢失，重贴后先读取 KeyVersion 0：oldVersion 使用 old Key 0 认证并重试；newVersion 使用 new Key 0 认证并继续。不能沿用响应丢失前的 secure session。

### 8.5 写入完整 NDEF

写入前必须确认 File 02 SDM disabled，或者当前状态已被证明是本任务目标 SDM 状态并可以跳过写入。不要在未知 SDM profile 上直接覆盖动态读取视图。

File 02 是 StandardData file，不是事务型 BackupData file。推荐使用 NLEN 发布协议降低中断风险：

```text
1. 写 NLEN = 0000
2. 从 File 02 offset 2 开始分块写入完整 NDEF message
3. 回读并比较 messageSha256
4. 最后写入真实 NLEN
5. 回读并比较 NLEN || NDEF message
```

这样中途移卡时，普通 NDEF reader 最多看到空 NDEF，不会把半条 message 当成有效业务 URL。它仍不是跨命令事务，重试时需要重新覆盖目标 image。

每个 write command 必须服从卡片当前 File 02 communication mode 和 write access rights；不能为了方便擅自将 Full 降为 Plain。

### 8.6 开启 SDM

在目标 NDEF 已完整验证后，使用 new Key 0 secure session 执行：

```text
ChangeFileSettings(File 02):
  SDMEnabled          = true
  SDMOptions          = C1
  SDMAccessRights     = F1 21
  PICCDataOffset      = 服务端值且 App 复算一致
  SDMMACInputOffset   = 服务端值且 App 复算一致
  SDMMACOffset        = 服务端值且 App 复算一致
```

当前 profile：

```text
e -> new Key 2
c -> new Key 1
SDMCtrRet -> new Key 1
```

开启 SDM 会重置 SDMReadCtr。服务端在收到成功 receipt 前应准备新的 `personalizationGeneration`，但不能静默覆盖旧 generation 的防重放状态。

`ChangeFileSettings` 是本流程最后一个持久化配置命令。如果启用命令的响应丢失，重贴后必须先读取 FileSettings：settings 已与目标完全一致则视为完成；仍为 disabled 才重新发送启用命令。不能盲目重复开启 SDM。

## 9. 最终确认

使用 new Key 0 建立新 session 后，App 必须确认：

```text
KeyVersion(0) == target newVersion0
KeyVersion(1) == target newVersion1
KeyVersion(2) == target newVersion2
File 02 settings == target settings
File 02 static image SHA-256 == target message/file hash
PICCDataOffset == target piccDataOffset
SDMMACInputOffset == target sdmMacInputOffset
SDMMACOffset == target sdmMacOffset
SDMEnabled == true
```

认证读取返回 File 02 的静态模板，因此应看到全零 `e/c` 占位符，而不是动态值。这正适合用于最终比较服务端下发的 canonical NDEF。

动态 SDM URL 验证建议在移开卡片后作为独立验收步骤完成：下一次未认证读取产生真实 `e/c`，服务端使用 new Key 2/new Key 1 验证。不要把认证态静态读取误判为 SDM 失败。

## 10. Receipt 与服务端状态机

### 10.1 Receipt 内容

```json
{
  "jobId": "pjob_20260824_000001",
  "uid": "04187262E51690",
  "profileId": "ntag424-aes-sdm-v3",
  "personalizationGeneration": 3,
  "keyVersions": {
    "0": 3,
    "1": 3,
    "2": 3
  },
  "ndefMessageSha256": "...",
  "fileSettingsHex": "...",
  "piccDataOffset": 148,
  "sdmMacInputOffset": 183,
  "sdmMacOffset": 183,
  "sdmEnabled": true,
  "completedAt": "2026-08-24T17:30:00+08:00",
  "deviceId": "provisioner-device-id",
  "receiptSignature": "base64(hardware-backed-device-signature)"
}
```

Receipt 不包含任何 key value。

### 10.2 服务端状态

```text
CREATED
  -> CLAIMED
  -> APPLYING
  -> CARD_CONFIRMED
  -> ACTIVE

任意执行阶段
  -> RETRYABLE
  -> APPLYING

未知 version / UID 不符 / profile 不符
  -> MANUAL_RECOVERY

过期或撤销且尚未改卡
  -> CANCELLED
```

状态含义：

- `CLAIMED`：设备已获得短时任务 envelope；
- `APPLYING`：App 已开始贴卡操作，不能把同一任务并发下发给其他设备；
- `CARD_CONFIRMED`：App 已用 new Key 0 回读确认卡片；
- `ACTIVE`：服务端已接受 receipt，并启用 new Key 1/2 验证路由；
- `RETRYABLE`：卡片可能处于可识别中间状态，保留 old/new keys；
- `MANUAL_RECOVERY`：不能由本任务唯一解释，禁止自动尝试多个未知 key。

### 10.3 服务端 Key 激活

任务创建时：

```text
old generation = ACTIVE
new generation = PENDING
```

App 完成并上传 receipt 后：

```text
new generation = ACTIVE
old generation = RETIRED_FOR_VERIFICATION
```

如果 receipt 上传失败但卡片已完成，App 再次贴卡应通过 new Key 0 认证、确认目标状态并重新上传 receipt，而不是再次执行 ChangeKey。

服务端可以在受控短窗口内为同一卡片保留 old/new verification route，但必须依据 job/profile 明确选择，不能无限期对所有请求盲试所有历史 keys。

## 11. 幂等恢复矩阵

| 中断点 | 卡片可能状态 | 重试策略 |
| --- | --- | --- |
| 开始前 | 全部 old，旧 NDEF，SDM off | 从预检开始 |
| Key 1 后 | K1 new，K2/K0 old | 验证 K1，继续 K2 |
| Key 2 后 | K1/K2 new，K0 old | 跳过已完成 keys，继续修改 K0 |
| Key 0 命令后 | K0 可能 old 或 new | 读 version，用对应 old/new K0 认证；new 时继续写 NDEF |
| NLEN 清零后 | NDEF 对外为空 | 重写完整 message，最后提交 NLEN |
| NDEF 分块中 | NLEN=0，body 部分更新 | 从 NLEN=0 开始覆盖目标 image |
| NDEF 完成后 | 目标静态 NDEF，SDM off | 校验 hash，继续开启 SDM |
| SDM 启用响应丢失 | SDM 可能 off 或目标 enabled | 读完整 settings；目标一致则完成，off 才重发 |
| 全部完成后 | 卡片正确，receipt 未上传 | 只回读确认并重传 receipt |

任何 key version 不属于任务 old/new 集合、NDEF hash 不属于旧状态或目标状态、SDM settings 不属于允许状态时，都进入人工恢复。

## 12. 错误处理

### 12.1 可重试错误

- TagLost；
- ISO-DEP 临时 I/O 错误；
- App 进程被系统中止；
- receipt 网络上传失败；
- 卡片处于本任务能够唯一解释的中间状态。

处理原则：重新贴卡、重新预检、从卡片实际状态规划剩余步骤，不依赖 App 上次停留的内存 step。

### 12.2 不可自动重试错误

- UID 与任务绑定不符；
- 任一 KeyVersion 不属于任务 old/new version；
- old/new Key 0 均无法认证；
- File 02 size 或权限不符合 profile；
- 已开启 SDM，但 NDEF hash/offsets/rights 与目标不符；
- NDEF 中占位符缺失、重复或长度错误；
- 服务端签名、任务有效期或设备绑定校验失败。

这些情况必须停止发送写命令，并进入人工恢复或重新创建明确的迁移任务。

## 13. App 侧 Java 编排模型

下面只表达接口边界和顺序，不是对当前项目的代码修改。

```java
public final class PersonalizationPlan {
    public String jobId;
    public String profileId;
    public long generation;
    public KeyTransition key0;
    public KeyTransition key1;
    public KeyTransition key2;
    public byte[] ndefMessage;
    public byte[] ndefMessageSha256;
    public SdmProfile sdmProfile;
}

public final class KeyTransition {
    public int keyNo;
    public int oldVersion;
    public int newVersion;
    public byte[] oldKey;
    public byte[] newKey;
}

public interface Ntag424Personalizer {
    PersonalizationReceipt apply(
            IsoDepChannel channel,
            PersonalizationPlan plan) throws PersonalizationException;
}
```

核心编排伪代码：

```java
public PersonalizationReceipt apply(
        IsoDepChannel channel,
        PersonalizationPlan plan) throws Exception {
    ValidatedTemplate template = validatePlanBeforeTag(plan);
    CardSnapshot snapshot = inspectCard(channel);
    RecoveryPlan recovery = planRemainingSteps(snapshot, plan, template);

    Ev2Session key0Session = authenticateCurrentKey0(channel, snapshot, plan);
    key0Session = ensureSourceSdmDisabled(
            channel,
            key0Session,
            snapshot,
            plan);

    key0Session = ensureKeyTransition(channel, key0Session, plan.key1);
    key0Session = ensureKeyTransition(channel, key0Session, plan.key2);

    if (recovery.mustChangeKey0()) {
        changeKey0(channel, key0Session, plan.key0);
    }

    // 修改当前认证 Key 0 后，旧 session 不能继续使用。
    Ev2Session newKey0Session = authenticateEv2First(
            channel,
            0,
            plan.key0.newKey);

    ensureTargetKeyVersions(channel, newKey0Session, plan);
    ensureStaticNdef(channel, newKey0Session, template);
    ensureSdmSettings(channel, newKey0Session, template, plan.sdmProfile);

    CardSnapshot confirmed = inspectAuthenticated(
            channel,
            newKey0Session);
    requireTargetState(confirmed, plan, template);
    return buildReceipt(confirmed, plan);
}
```

`planRemainingSteps()` 必须是纯状态规划，不应在发现未知状态时“尽量继续”。

## 14. 密钥下发安全

该方案要求 App 参与 EV2 secure messaging 和 `ChangeKey`，因此 key material 在个性化设备内存中短暂可见。生产环境至少需要：

- old/new keys 由 KMS/HSM 管理；
- 服务端只向授权、设备绑定、短时有效的个性化终端下发；
- 使用 Android Keystore hardware-backed key 加密 `keyEnvelope`；
- envelope 绑定 `jobId + deviceId + operator + expiresAt + profile hash`；
- 禁止截屏、剪贴板、日志、崩溃上报和本地持久化 key；
- 操作页面进入后台或任务过期时立即清除 key material；
- 服务端审计领取、开始、失败、恢复和完成事件；
- 普通消费者 App 永远不获得 Key 0/1/2；
- SDM 验证服务通过 HSM 使用 Key 1/2，不将其下发给扫码客户端。

仅使用普通 HTTPS 加明文 JSON/Base64 不能充分保护生产 key。Demo 可以使用明文固定 key 验证协议，但必须与生产 build 和生产后端隔离。

## 15. 服务端 SDM 验证切换

任务完成后的动态 URL 验证流程：

```text
读取 pid/cardRef/e/c
  -> 选择 new personalizationGeneration
  -> 从 HSM 取得 new Key 2
  -> 解密 e，取得 UID 和 SDMReadCtr
  -> 校验 UID 与发行记录一致
  -> 从 HSM 取得同 generation 的 new Key 1
  -> 派生 session MAC key 并重新计算 c
  -> 常量时间比较 c
  -> 在 UID + generation 下原子检查 counter
```

完整 Java 算法见 [sdm_verification_java.md](sdm_verification_java.md)。

不能只换卡片 Key 1/2 而不切换服务端验证 keySet；否则新生成的 `e/c` 会立即无法验证。

## 16. 验收标准

### 16.1 正常流程

- old Key 0/1/2 卡片一次贴卡完成全部步骤；
- KeyVersion 全部变为 newVersion；
- 认证读取的静态 File 02 与服务端 NDEF bytes 完全一致；
- FileSettings 中 SDM enabled、rights 和 offsets 正确；
- 重新贴卡未认证读取产生非零动态 `e/c`；
- 服务端使用 new Key 2/new Key 1 验证成功；
- 相同动态 URL 第二次提交被防重放拒绝。

### 16.2 中断恢复

至少在以下位置模拟移卡并验证重试：

- Key 1 完成后；
- Key 2 完成后；
- Key 0 命令响应丢失时；
- NLEN 清零后；
- NDEF body 写入一半后；
- NDEF 完成、SDM 未开启时；
- SDM 启用命令响应丢失时；
- 全部完成、receipt 未上传时。

每种状态都必须满足：要么自动恢复到唯一目标状态，要么在任何新写命令前明确进入人工恢复。

### 16.3 错卡与攻击输入

- 错误 UID 卡片不得执行任何 ChangeKey/WriteData；
- unknown KeyVersion 不得自动尝试历史 key 列表；
- NDEF hash、签名、占位符或 offsets 被修改时不得贴卡写入；
- 重复领取、过期任务和并发设备操作必须被服务端拒绝；
- App 日志、数据库、崩溃文件中不得出现 key material。

## 17. 与当前 Demo 的差异

| 项目 | 当前 Demo | 新方案 |
| --- | --- | --- |
| Key 来源 | APK 固定 Demo keys | 服务端 KMS/HSM 任务 envelope |
| old/new key | 按固定 version 推断 | 服务端明确下发 old/new transition |
| NDEF 生成 | App 根据 URL/pkg 构造 | 服务端下发 canonical NDEF bytes |
| `e/c` 占位符 | App 追加 | 已包含在服务端 NDEF 中 |
| offsets | App 构造模板后计算 | 服务端计算，App 独立复算校验 |
| Key 顺序 | 当前复合 Demo 先完成所有 keys | K1 -> K2 -> K0，随后 new K0 认证 |
| SDM 时机 | App 本地构造模板后开启 | 服务端 NDEF 验证完成后，作为最后配置步骤开启 |
| 断卡恢复 | 固定 version 0/1 | old/new version + 服务端任务状态机 |
| 完成确认 | 页面显示结果 | new K0 认证回读 + signed receipt |
| 防重放 generation | App 内存 Map | 服务端按 UID + generation 持久化 |

## 18. 参考依据

- NXP NTAG 424 DNA datasheet Rev. 3.0，§8.2.3.1：File 02 StandardData file 行为；
- NXP NTAG 424 DNA datasheet Rev. 3.0，§8.2.4.1、§11.6.1：AppKey、ChangeKey 授权、数据格式和 Full communication；
- NXP NTAG 424 DNA datasheet Rev. 3.0，§9.3.1：开启 SDM 后 SDMReadCtr 重置和读取行为；
- NXP NTAG 424 DNA datasheet Rev. 3.0，§11.7.1、Table 69：ChangeFileSettings、SDM options、rights 和 offsets；
- NXP AN12196 Rev. 2.0，§5.7、§5.8、§5.9：先构造/写入 NDEF，再开启 SDM；
- NXP AN12196 Rev. 2.0，§5.16.1、§5.16.2：修改非当前认证 Key 与修改当前认证 Key 的区别；
- [sdm_dynamic_url_generation.md](sdm_dynamic_url_generation.md)：静态占位模板和读取时动态替换；
- [sdm_verification_java.md](sdm_verification_java.md)：Java 服务端 `e/c` 验证流程；
- [ntag424_local_reference_review.md](ntag424_local_reference_review.md)：File 02 非事务写入、权限和恢复设计核对。
