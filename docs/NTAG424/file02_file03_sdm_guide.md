# NTAG 424 DNA File 02、File 03 与 SDM 使用指南

## 1. 范围与核心概念

本文说明 NTAG 424 DNA 中以下三个对象的功能、联系和密钥依赖：

- File 02：标准 NDEF File，保存 URL、`android.com:pkg` 等 NDEF records；
- File 03：专有业务数据文件，保存由业务自行定义的 128-byte 原始数据；
- SDM（Secure Dynamic Messaging）：只作用于 File 02，在未认证读取 NDEF 时动态生成防伪字段。

需要区分两个概念：

1. **Key 编号**：AppKey 0、1、2、3、4 在权限和协议中承担不同角色；
2. **Key value**：每个 Key 编号对应的 16-byte AES 密钥内容。

当前 Demo 支持把 AppKey 0、1、2 从 version 0 的 16-byte 全零 transport key，分别修改为代码内三个不同的固定 Demo key value；Key 0 还支持从固定 Demo key/version 1 改回全零 transport key/version 0。AppKey 3、4 暂时仍使用全零 transport key。Key 编号始终独立，例如 File 03 默认写权限是 Key 3，因此仍必须以 Key 3 认证。

全零密钥仅用于受控测试卡，不得用于生产。

## 2. File 02 与 File 03 的定位

| 项目 | File 02 | File 03 |
| --- | --- | --- |
| File No. | `02h` | `03h` |
| ISO File ID | `E104h` | `E105h` |
| 类型 | StandardData file / NDEF File | StandardData file / Proprietary File |
| 容量 | 256 bytes，包含 2-byte NLEN | 128 bytes |
| 数据格式 | NFC Forum NDEF | 业务自定义原始 bytes |
| Android 标准 NDEF 流程 | 会读取并解析 | 不会自动读取或解析 |
| 是否支持 SDM | 支持，且只有 File 02 支持 | 不支持 |
| 出厂通信模式 | Plain | Full |
| 主要用途 | URL、文本、MIME、AAR/pkg 等 NDEF records | 业务 ID、状态位、短配置、紧凑二进制数据 |

File 01（ISO File ID `E103h`）是 Capability Container。它描述 File 02 和 File 03 的文件 ID、容量及 NFC 接口访问能力，本身不用于保存业务 NDEF 或 SDM URL。

## 3. File 02（NDEF）

### 3.1 数据结构

File 02 的 256 bytes 结构如下：

```text
offset 0..1   NLEN，2-byte big-endian
offset 2..    NDEF Message，长度由 NLEN 指定
```

因此 File 02 可容纳的最大 NDEF Message 是 254 bytes：

```text
2-byte NLEN + 254-byte NDEF Message = 256 bytes
```

当前业务 NDEF 使用两条 records：

```text
Record 1: URI
Record 2: android.com:pkg
```

### 3.2 出厂默认权限

File 02 出厂 AccessRights 通常为 `EEE0h`：

| 权限 | 默认值 | 含义 |
| --- | --- | --- |
| Read | `Eh` | Free，不需要认证 |
| Write | `Eh` | Free，不需要认证 |
| ReadWrite | `Eh` | Free，不需要认证 |
| Change | `0h` | 修改 FileSettings 需要 AppKey 0 |

File 02 出厂通信模式为 Plain。

### 3.3 File 02 操作与 Key 依赖

| 操作 | 协议上是否需要 Key | 使用哪个 Key | 说明 |
| --- | --- | --- | --- |
| 普通读取 NDEF | 出厂状态不需要 | 无 | 通过 ISO `ReadBinary` 读取 NLEN 和 NDEF Message |
| 普通写入 NDEF | 取决于当前 Write/RW rights | 出厂状态无；开启 Demo 写保护后为 Key 0 | 必须遵守当前通信模式和访问权限 |
| 修改 File 02 通信模式 | 需要 | 当前 Change right 指定的 Key，出厂为 Key 0 | 使用 `ChangeFileSettings` |
| 修改 Read/Write/RW/Change rights | 需要 | 修改前的 Change Key，出厂为 Key 0 | 修改后的权限只影响后续操作 |
| 开启 SDM | 需要 | 当前 Change Key，出厂为 Key 0 | 还需要先把 SDM 占位符写入 NDEF |
| 关闭 SDM | 需要 | 当前 Change Key，当前 Demo 为 Key 0；清理 URL 还需满足 Write/RW rights | 清除 SDM 设置和顶层 `e/c`，不改变写权限或业务数据 |

访问权限中的 `Fh` 表示 Never。若 Write 和 ReadWrite 均为 Never，则不能通过普通写命令更新内容；Key 0 不是能够绕过任意文件权限的“万能 Key”。

### 3.4 安全写入顺序

更新 NDEF 应采用以下顺序，避免中途断电或移卡留下一个“长度有效、内容不完整”的 NDEF：

```text
1. 读取 File 02 settings，确认容量、通信模式和写权限
2. 如写权限要求 Key，执行 AuthenticateEV2First
3. 把 NLEN 写为 0000h
4. 从 offset 2 分块写入完整 NDEF Message
5. 把最终 NLEN 写回 offset 0，提交新 NDEF
```

第 2 步是否需要 Key 由当前 Write/ReadWrite rights 决定。当前 Demo 即使 File 02 为 Free，也会按 KeyVersion 选择 transport Key 0 或固定 Demo Key 0 建立受控测试 session；这属于 Demo 的实现选择，不代表出厂 File 02 在协议上必须使用 Key 0 才能写。

## 4. File 03（Proprietary Data）

### 4.1 数据性质

File 03 是 128-byte StandardData file。芯片只按 offset/length 保存和返回 bytes，不理解其中的业务格式。

File 03：

- 不是 NDEF；
- 不包含 NLEN；
- 不会被 Android 标准 NDEF reader 自动返回；
- 不能开启 SDM；
- 不会参与 File 02 URL 中 `e`、`c` 的生成；
- 修改 File 03 不会修改 File 02、NDEF 或 SDM settings。

### 4.2 出厂默认权限

| 权限 | 默认值 | 含义 |
| --- | --- | --- |
| Read | `2h` | 使用 AppKey 2 认证后可读 |
| Write | `3h` | 使用 AppKey 3 认证后可写 |
| ReadWrite | `3h` | 使用 AppKey 3 认证后可读写 |
| Change | `0h` | 使用 AppKey 0 修改 FileSettings |

File 03 出厂通信模式为 Full，所以读写不仅需要权限认证，还需要 EV2 Full secure messaging。

### 4.3 File 03 操作与 Key 依赖

| 操作 | 是否需要 Key | 默认 Key | Secure Messaging |
| --- | --- | --- | --- |
| 读取 File 03 | 需要 | Key 2；也可由满足 RW right 的 Key 3 执行 | Full：命令 MAC、响应 MAC、响应数据加密 |
| 写入 File 03 | 需要 | Key 3 | Full：写入数据加密、命令 MAC、响应 MAC |
| 修改 File 03 权限/通信模式 | 需要 | Key 0 | `ChangeFileSettings`，Full |
| 修改 File 03 内容格式 | 芯片不管理 | 由业务定义 | 不需要 ChangeFileSettings，只需要正常 WriteData 权限 |

默认读流程：

```text
Select NDEF application
  -> GetFileSettings(File 03)
  -> AuthenticateEV2First(Key 2)
  -> Full ReadData(File 03, offset, length)
  -> 验证 response MAC
  -> 使用 session ENC key 和 response IV 解密
```

默认写流程：

```text
Select NDEF application
  -> GetFileSettings(File 03)
  -> AuthenticateEV2First(Key 3)
  -> 使用 session ENC key 加密数据
  -> 计算 command MAC
  -> Full WriteData(File 03, offset, length, encrypted data)
  -> 验证 response MAC
```

当前 Demo 的 File 03 测试格式是：

```text
2-byte big-endian payload length
N-byte UTF-8 payload，最多 126 bytes
0x00 padding，补齐到 128 bytes
```

这是 Demo 自定义格式，不是 NXP 或 NFC Forum 标准。每次覆盖完整 128 bytes，可以避免短数据写入后残留旧数据尾部。

## 5. SDM 的功能与 Key 依赖

### 5.1 SDM 与 File 02 的关系

SDM 不是独立文件。它是 File 02 的一种动态读取能力。

开启 SDM 时，File 02 中实际保存的是带等长占位符的静态 NDEF，例如：

```text
https://example.com/path?business=123
  &e=00000000000000000000000000000000
  &c=0000000000000000
```

同时，File 02 settings 保存 `e`、`c` 占位区域的 byte offsets 和 SDM 权限配置。

未认证读取时，芯片在响应阶段动态替换占位符：

```text
静态 File 02 模板
  -> 芯片实时生成 encrypted PICCData
  -> 芯片实时生成 SDMMAC
  -> 读取响应中的 e/c 被替换
```

这个过程不会在每次读取时重写 File 02。`e`、`c` 的输出长度和占位符长度相同，因此不会导致 NDEF 变长。

### 5.2 SDM 涉及的 Key

当前 SDM profile 使用：

| SDM 角色 | 当前 Key 编号 | 谁使用 | 用途 |
| --- | --- | --- | --- |
| Change Key | Key 0 | 配置 App | 开启/关闭 SDM，修改 File 02 settings |
| SDMMetaRead Key | Key 2 | 芯片和验证后端 | 加密/解密 `e` 中的 PICCData |
| SDMFileRead Key | Key 1 | 芯片和验证后端 | 派生 SDM session MAC key，生成/验证 `c` |
| SDMCtrRet Key | Key 1 | 需要读取计数器的受信任客户端 | 受保护地取得 SDMReadCtr；普通 URL 读取不调用此命令 |

这些编号由 SDMAccessRights 决定，并不是 NTAG 424 DNA 永远固定为 1/2。当前 Demo 明确配置为上述编号。

### 5.3 哪些 SDM 步骤需要认证

| 步骤 | NFC 侧是否需要认证 | 使用哪个 Key | 后端是否需要密钥 |
| --- | --- | --- | --- |
| 未认证手机读取动态 NDEF | 不需要 | 无 | 需要 Key 2 解密 `e`，需要 Key 1 验证 `c` |
| 认证读取静态 File 02 模板 | 取决于 File 02 Read/RW rights | 对应 Read/RW Key | 不适用 |
| 把 `e/c` 占位符写入 File 02 | 取决于 File 02 Write/RW rights | 对应 Write/RW Key | 不适用 |
| 执行 ChangeFileSettings 开启 SDM | 需要 | 当前 Change Key，出厂为 Key 0 | 不适用 |
| 执行 ChangeFileSettings 关闭 SDM | 需要 | 当前 Change Key，当前 Demo 为 Key 0 | 不适用 |
| 从 File 02 URL 移除 `e/c` | 取决于 File 02 Write/RW rights | Free 或对应 Write/RW Key；当前 Demo 支持 Free/Key 0 | 不适用 |
| 后端解密 `e` | 不是 NFC 认证 | SDMMetaRead Key，当前为 Key 2 | 必须 |
| 后端验证 `c` | 不是 NFC 认证 | SDMFileRead Key，当前为 Key 1 | 必须 |
| 受保护读取 SDMReadCtr | 需要 | SDMCtrRet Key，当前为 Key 1 | 视业务需要 |

“普通手机读取 SDM URL 不需要 Key”不等于 SDM 不使用 Key。正确理解是：读卡手机不持有 Key；芯片内部使用 Key 生成动态字段，可信后端持有对应 Key 验证这些字段。

### 5.4 开启 SDM 的完整步骤

```text
1. 读取 File 02 settings
   - 不需要文件内容读取 Key
   - 用于确认容量、通信模式、Write/RW/Change rights

2. 读取并解析当前 NDEF
   - 出厂 Read=Free 时不需要 Key

3. 构造保留业务 URL 和 pkg 的静态 SDM 模板
   - 追加等长 e/c 占位符
   - 根据最终 File 02 bytes 计算 offsets
   - 纯 App 计算，不需要卡片 Key

4. 写入静态 SDM 模板
   - 是否需要 Key 取决于当前 Write/RW rights
   - 当前 Demo 使用 Key 0 session

5. ChangeFileSettings 开启 SDM
   - 必须使用当前 Change Key，出厂为 Key 0
   - 配置 SDMOptions、SDMAccessRights 和 offsets
   - 不应顺带改变 Write/RW rights

6. 未认证读取验证
   - 手机不需要 Key
   - 应看到动态 e/c 和保留的 pkg record
```

步骤 4 和步骤 5 是两个不同权限域：能够写 File 02 内容，不代表能够修改 SDM settings；能够使用 Change Key，也不代表能够绕过 File 02 的 Write/RW rights。

### 5.5 关闭 SDM 的步骤

```text
1. 读取 File 02 settings，并尝试读取当前动态 NDEF；允许 NLEN=0 或没有 URI record
2. 若存在 URI record，只从其顶层 query 移除 `e/c`，保留业务 query、fragment 和 pkg record
3. 仅在需要写回清理后 NDEF 时，先确认当前 Demo Key 0/Free 权限可写，避免先改 settings 后才发现不可写
4. 使用当前 Change Key 认证，当前 Demo 为 Key 0
5. ChangeFileSettings 清除 SDM enable 位和 SDM 配置段
6. 若第 2 步产生了清理后的 NDEF，再将其写回；保持通信模式和所有访问权限不变
```

Android 或其他工具“格式化 NDEF”只会改写 File 02 内容，不会自动清除 NTAG 424 的 SDM FileSettings。因此可能出现“SDM 仍开启，但 NDEF 为空或没有 URI”的状态。关闭操作在这种状态下仍会使用 ChangeFileSettings 关闭 SDM，只是没有 `e/c` 内容需要清理。

关闭完成后，原 NDEF 中由 Demo 添加的静态 `e/c` 占位符也会被移除，因此同一业务 URL 可以再次开启 SDM。若上一次操作在 settings 已关闭后、NDEF 清理完成前中断，重复执行“关闭 SDM”可识别全零占位符并重试清理；不会把普通业务 URL 中任意同名参数当作恢复目标。

### 5.6 使用 File 02 并开启 SDM 需要哪些 Key

按当前 SDM profile，完整流程会涉及 AppKey 0、1、2，但三个 Key 的使用阶段和持有方不同：

| Key | 使用阶段 | 谁需要持有 | 当前作用 |
| --- | --- | --- | --- |
| Key 0 | 写卡和配置阶段 | 受控写卡/发卡工具 | 使用 ChangeFileSettings 开启或关闭 SDM；File 02 写保护开启后也用于写 NDEF |
| Key 1 | SDM 生成和后端验证阶段 | 芯片内部、可信验证后端/HSM | 作为 SDMFileRead Key 派生 SDM session MAC key，生成和验证 `c`；当前也作为 SDMCtrRet Key |
| Key 2 | SDM 生成和后端验证阶段 | 芯片内部、可信验证后端/HSM | 作为 SDMMetaRead Key 加密和解密 `e` 中的 PICCData |

分步骤的 Key 依赖如下：

| 步骤 | 是否需要认证 | 使用哪个 Key |
| --- | --- | --- |
| 写入普通 URL + pkg NDEF | 取决于 File 02 当前 Write/RW rights | 出厂 `EEE0` 为 Free；增加 Demo 写保护后为 Key 0 |
| 把 `e/c` 等长占位符写入 File 02 | 取决于 File 02 当前 Write/RW rights | Free 或 rights 指定的 Key；当前 Demo 会建立 Key 0 session |
| ChangeFileSettings 开启 SDM | 必须认证 | 当前 Change right 指定的 Key，出厂和当前 Demo 为 Key 0 |
| 普通手机读取动态 SDM URL | 不需要认证 | 无；手机不持有 Key 1/2 |
| 芯片生成 `e` | 读卡手机不参与认证 | 芯片内部使用当前 SDMMetaRead Key 2 |
| 芯片生成 `c` | 读卡手机不参与认证 | 芯片内部使用当前 SDMFileRead Key 1 |
| 后端解密 `e` | 不是 NFC 认证 | 后端使用与卡片一致的 Key 2 |
| 后端验证 `c` | 不是 NFC 认证 | 后端使用与卡片一致的 Key 1 |

可以概括为：

```text
只写出厂状态的普通 File 02 NDEF
  -> 协议上不需要 Key（Write/RW=Free）

开启当前 SDM profile
  -> 配置端需要 Key 0

普通手机读取 SDM URL
  -> 不需要 Key

可信后端验证 SDM
  -> Key 2 解密 e
  -> Key 1 验证 c
```

如果 File 02 的 Write/RW/Change rights 或 SDMAccessRights 被重新配置，实际 Key 编号必须以卡片当前 FileSettings 为准，不能永远写死为 0/1/2。当前 Demo 的 Key 0、1、2 使用三个不同的固定值，它们仍是三个独立的 Key slot。

## 6. File 02 写保护与 SDM 的独立性

写保护和 SDM 是两个独立功能：

| 操作 | 改变的字段 | 不应改变 |
| --- | --- | --- |
| 开启 SDM | SDM enable、options、rights、offsets、必要的 NDEF 占位符 | 原 File 02 Read/Write/RW/Change rights 和通信模式 |
| 关闭 SDM | 清除 SDM enable/配置段，并从 URI 顶层 query 移除 SDM `e/c` | 业务 query、pkg record、权限和通信模式 |
| 增加 Demo 写保护 | Write、ReadWrite 改为 Key 0 | SDM、Read、Change、通信模式、NDEF |
| 移除 Demo 写保护 | Write、ReadWrite 改为 Free | SDM、Read、Change、通信模式、NDEF |

增加或移除写保护都需要使用修改前的 Change Key 执行 `ChangeFileSettings`，当前 Demo 为 Key 0。

## 7. 当前 Demo 操作矩阵

| 页面操作 | 文件 | 是否认证 | Demo Key 编号 | 是否改变其他功能 |
| --- | --- | --- | --- | --- |
| 只读检测 NDEF | File 02 | 普通读取不认证 | 无 | SDM 读取会推进 SDMReadCtr |
| 写入 URL + pkg | File 02 | 当前实现会认证 | Key 0 | 只写 NDEF；SDM 已开启时拒绝写入 |
| 开启 SDM | File 02 | 是 | Key 0 | 只增加 SDM，不改变写保护 |
| 关闭 SDM | File 02 | 是 | Key 0 | 关闭 SDM 并移除其 `e/c`；不改变写保护和业务数据 |
| 一次贴卡修改 Key 0/1/2 + 写 NDEF + 开启 SDM | 应用 Key + File 02 | 是 | Key 0/1/2 | 依次完成 Key 个性化、两条 NDEF records 写入和 SDM 配置 |
| 增加 File 02 写保护 | File 02 | 是 | Key 0 | 只修改 Write/RW rights |
| 移除 File 02 写保护 | File 02 | 是 | Key 0 | 只修改 Write/RW rights |
| 写入 File 03 | File 03 | 是 | 默认 Key 3 | 不改变 File 02/SDM |
| 读取 File 03 | File 03 | 是 | 默认 Key 2 | 不改变 File 02/SDM |

若卡片 FileSettings 已被修改，实际所需 Key 以卡片当前 rights 为准，而不是以上出厂默认值。当前 File 03 Demo 会先读取 settings，再从实际 Read/Write/RW rights 中选择 AppKey 0..4。

## 8. 生产环境注意事项

- 不得继续使用全零 key value；AppKey 0..4 应使用受控的随机 AES-128 密钥。
- SDM 验证密钥应保存在后端或 HSM，不应放在普通 Android App、日志或 Git 中。
- Key 编号、key version、diversification 规则、SDMOptions、SDMAccessRights 和 offsets 应作为同一个发卡 profile 管理。
- 后端必须同时解密 `e`、验证 `c` 并检查 SDMReadCtr 防重放，不能只判断 URL 每次不同。
- Change Key、Write Key、SDM Meta Key 和 SDM File Key 应按权限边界设计；当前“所有编号使用同一 key value”只为 Demo 调试方便。
- NXP OriginalityKey 与 AppKey 0..4 是两套不同机制。OriginalityKey 用于制造商原始性验证，不能代替 File 02/File 03/SDM 的应用密钥。

## 9. AppKey 0、1、2、3、4 的区别和联系

### 9.1 五个 Key slot 的本质

NTAG 424 DNA 的 NDEF Application 内有五个客户可管理的 AES-128 key slot，编号为 AppKey 0..4。每个 slot 保存：

- 一份独立的 16-byte AES key value；
- 对应的 key version 元数据；
- 固定的 Key 编号 0..4，供认证命令和访问权限引用。

除 Key 0 的应用主密钥职责外，Key 1..4 没有芯片强制规定的永久业务含义。“Key 1 必须用于 SDM”或“Key 3 永远只能写 File 03”都不是通用规则。这些角色来自 FileSettings 中的 AccessRights 和 SDMAccessRights，可以通过受授权的 `ChangeFileSettings` 重新分配。

五个 slot 在密码材料上彼此独立：

- 芯片不会自动通过 Key 0 派生 Key 1..4；
- 修改 Key 1 不会自动修改 Key 2；
- 知道 Key 2 不等于知道 Key 3；
- 同一个 key value 写入两个 slot，也不会把两个 slot 合并；认证时仍必须指定正确的 Key 编号；
- 生产系统可以在卡外使用同一主密钥按 UID 做 key diversification，但这是发卡系统的规则，不是芯片在 AppKey slot 之间自动建立的关系。

### 9.2 Key 0 与 Key 1..4 的区别

| Key | 芯片层面的特殊性 | 当前 Demo 分配的角色 |
| --- | --- | --- |
| AppKey 0 | AppMasterKey；控制应用级密钥和关键配置管理，并且出厂 File 01/02/03 的 Change right 默认指向它 | 开启/关闭 SDM；增加/移除 File 02 写保护；当前 File 02 受保护写入；修改 File 02/03 settings |
| AppKey 1 | 普通应用 Key，没有固定业务角色 | SDMFileRead Key；生成/验证 `c`；同时作为 SDMCtrRet Key |
| AppKey 2 | 普通应用 Key，没有固定业务角色 | SDMMetaRead Key，用于 `e`；同时是 File 03 默认 Read Key |
| AppKey 3 | 普通应用 Key，没有固定业务角色 | File 03 默认 Write 和 ReadWrite Key |
| AppKey 4 | 普通应用 Key，没有固定业务角色 | 当前 Demo 未使用，仍是可独立管理的 key slot |

“AppMasterKey”不表示 Key 0 可以无条件绕过文件权限。文件内容能否读写仍由目标文件当前的 Read/Write/ReadWrite rights 决定。例如 File 03 的 Write right 为 Key 3 时，只认证 Key 0 不能代替 Key 3 执行 WriteData，除非先通过有权的配置流程修改 File 03 rights。

### 9.3 五个 Key 如何产生联系

它们之间没有自动的密码派生关系，联系来自配置对 Key 编号的引用：

```text
File 02 AccessRights
  -> Read / Write / ReadWrite / Change 各引用一个 Key 编号或 Free/Never

File 03 AccessRights
  -> Read / Write / ReadWrite / Change 各引用一个 Key 编号或 Free/Never

File 02 SDMAccessRights
  -> SDMMetaRead / SDMFileRead / SDMCtrRet 各引用一个 Key 编号或特殊值
```

因此可能出现以下配置：

- 一个 Key 同时承担多个角色，例如当前 Key 1 同时用于 SDMFileRead 和 SDMCtrRet；
- 一个文件把读和写分给不同 Key，例如 File 03 默认 Key 2 读、Key 3 写；
- 两个文件引用同一个 Key；
- 某个 Key 暂时没有被任何权限引用，例如当前 Demo 的 Key 4；
- 通过 ChangeFileSettings 把某个角色从 Key 2 改为 Key 4，而不修改 Key 2 或 Key 4 的 key value。

权限判断同时依赖“认证使用的 Key 编号”和“该 slot 中的 key value”。即使两个 slot 恰好存放相同值，用 Key 0 成功认证后，芯片的 active key number 仍是 0；它不会因此授予只配置给 Key 2 或 Key 3 的权限。

### 9.4 能否修改 AppKey

AppKey 0..4 都属于客户应用密钥，交付状态下可以通过 `ChangeKey` 更新，但必须满足卡片当前 KeySettings 和认证条件。当前测试卡的基本流程是：

```text
1. Select NDEF Application
2. 使用当前 AppMasterKey（Key 0）执行 AuthenticateEV2First
3. 在 Full secure messaging session 中执行 ChangeKey
4. 验证响应 MAC
5. 使用新 key value 重新认证，确认更新成功
```

严格来说，修改某个 Key 时允许由哪个 Key 授权，取决于应用的 KeySettings/ChangeKey access-right 配置；不能把“所有卡片、所有状态下都固定使用 Key 0”当作通用结论。交付配置和当前 Demo 以 AppMasterKey 0 管理为前提。KeySettings 也可能被配置为由指定 Key 授权、由待修改 Key 自身授权，或者禁止继续修改。执行 ChangeKey 前必须先读取并确认目标卡片的实际配置。

修改 Key 时需要注意：

- `ChangeKey` 改的是指定 Key 编号中的 key value，不会自动改 FileSettings 对 Key 编号的引用；
- 修改 Key 1 后，所有仍引用 Key 1 的 SDM/File 权限会立即要求新 Key 1；
- 修改 Key 0 后，后续 ChangeFileSettings、ChangeKey 及所有引用 Key 0 的文件操作都必须使用新 Key 0；
- key version 是用于识别密钥版本的元数据，不是 key value，也不能代替 AES 密钥；
- 某些 KeySettings 可以限制或禁止后续密钥/配置修改。生产个性化前必须确认这些设置；不可逆地锁定前必须完成新密钥验证和恢复方案设计；
- 当前 Demo 为 Key 0、1、2 提供独立 ChangeKey 操作；Key 0 还可从固定 Demo value/version 1 恢复为全零 transport value/version 0，再重新改为 version 1，用于验证 Key 0 可以反复修改。NDEF、SDM、写保护和 File 03 操作本身仍不会修改任何 AppKey。

### 9.5 修改各 Key 对当前功能的影响

| 修改对象 | 当前功能影响 | 必须同步更新的位置 |
| --- | --- | --- |
| Key 0 | 当前 App 无法继续开启/关闭 SDM、改写保护、修改 settings；受 Key 0 保护的 File 02 写入也会失败 | 受控发卡工具/管理端中的 Key 0；不得放入普通客户端或日志 |
| Key 1 | 芯片之后使用新 Key 1 生成 SDMMAC；旧后端将无法验证 `c`，受保护计数器读取也会失败 | SDM 验证后端/HSM 的 SDMFileRead、SDMCtrRet key material 和版本 |
| Key 2 | 芯片之后使用新 Key 2 生成 encrypted PICCData；旧后端无法解密 `e`；File 03 默认读取也会失败 | SDM 后端的 MetaRead key，以及获准读取 File 03 的客户端安全环境 |
| Key 3 | File 03 默认写入/RW 认证失败 | 获准写 File 03 的发卡工具或可信客户端安全环境 |
| Key 4 | 当前功能通常不受影响，因为当前没有 rights 引用 Key 4 | 若未来给 Key 4 分配权限，则对应系统必须持有它 |

如果需要轮换 SDM Key，必须把“卡片 ChangeKey”“后端新旧 key version”“请求验证切换时间”和“失败恢复”设计成一个完整流程。仅在卡片上换 Key 1/2 而不同步后端，会立即导致真实卡片验证失败。

### 9.6 当前 Demo 的固定密钥关系

修改前的 transport 状态是 Key 0..4 均为全零 version 0。执行独立 ChangeKey 操作后，目标状态可表示为：

```text
Key 0 version = 1，value = 00112233445566778899AABBCCDDEEFF
Key 1 version = 1，value = 0102030405060708090A0B0C0D0E0F10
Key 2 version = 1，value = 102030405060708090A0B0C0D0E0F000
Key 3 value = 00000000000000000000000000000000
Key 4 value = 00000000000000000000000000000000
```

当前代码先读取 KeyVersion：version 0 使用全零 transport key，version 1 使用对应固定 Demo key；未知 version 会拒绝认证。修改顺序建议为 Key 1、Key 2、最后 Key 0，避免中途丢失管理入口。当前代码会根据操作选择 Key 编号：

```text
File 02 settings / SDM 配置 -> 认证 Key 0
SDMMAC 后端角色             -> 使用 Key 1
encrypted PICCData 后端角色 -> 使用 Key 2
File 03 读取                -> 默认认证 Key 2
File 03 写入                -> 默认认证 Key 3
```

Key 0 的反复修改路径为 `version 0 / 全零 value -> version 1 / 固定 Demo value -> version 0 / 全零 value`。每次 ChangeKey 后都会重新选择应用、读取 KeyVersion，并使用目标 key value 再执行一次 EV2 认证；只有三项都成功，页面才报告修改成功。该回退仅是测试能力，不代表生产环境应该继续使用全零 transport key。

这些固定值和全零 transport key 都公开存在于 Demo APK，只用于协议联调，不提供生产安全性。生产环境应使用受控 key value，并为每张卡或每个发行批次设计明确的 diversification 和 key version 策略。

## 10. Mock 服务器验证 SDM URL

服务端算法、输入约束、错误处理和生产防重放要求详见 [SDM 服务端验证流程](sdm_verification_flow.md)。

当前测试页提供“读卡并自动验证 SDM URL”入口，不再要求手工输入 URL。点击后，下一次贴卡会读取 File 02 的 URI record，从卡片返回的动态 URI 中自动提取 `e/c`，再使用固定 Demo Key 2 解密 `e`、使用固定 Demo Key 1 验证 `c`，模拟可信服务器收到 URL 后的完整流程：

```text
1. 严格解析 URL，要求 e/c 各出现一次
2. 要求 e 为 32 个 hex 字符、c 为 16 个 hex 字符
3. 使用 Key 2 和全零 IV 执行 AES-CBC 解密 e
4. 校验 PICCDataTag，并取出 7-byte UID 和 3-byte little-endian SDMReadCtr
5. 构造 3CC300010080 || UID || SDMReadCtr_LE
6. 使用 Key 1 对该 16-byte 向量计算 AES-CMAC，派生本次 session MAC key
7. 使用 session MAC key 对 SDMMAC 输入区间计算 AES-CMAC
8. 按 NTAG 424 规则取 CMAC 的奇数字节，得到 8-byte c
9. 使用 MessageDigest.isEqual 常量时间比较服务器 c 与 URL c
10. MAC 成功后，按 UID 原子记录更大的 SDMReadCtr；重复或更小计数器按重放拒绝
```

当前 SDM profile 配置 `SDMMACInputOffset == SDMMACOffset`，所以步骤 7 的额外文件 MAC 输入是空 byte array。若未来修改 offsets 让 MAC 覆盖业务 URL bytes，服务器必须从动态 File 02 的相同 `[SDMMACInputOffset, SDMMACOffset)` 区间传入完全一致的 bytes。

防重放状态当前仅保存在测试 Activity 进程内的内存 Map 中，重启 App 后会丢失。生产服务必须使用数据库或一致性存储，把“验证 MAC、比较计数器、写入新计数器、受理业务”设计为原子事务；否则并发请求仍可能重放成功。

## 11. 一次贴卡复合 Key、NDEF 与 SDM 配置

测试页的“一次贴卡：修改 Key 0/1/2 + 写 NDEF + 开启 SDM”从页面读取 URL 和 pkg，要求 File 02 初始未开启 SDM，并在同一次 ISO-DEP 连接内执行：

```text
1. 读取 Key 0、1、2 version；只接受 transport v0 或固定 Demo v1
2. 在任何 ChangeKey 前预检 File 02：SDM 关闭、Change=Key 0、Demo Key 0 可写、NDEF 不超容量
3. 按 Key 1 -> Key 2 -> Key 0 修改仍为 v0 的 Key
4. 对已经是 v1 的 Key 不重复 ChangeKey，但用对应固定 Demo Key 执行 EV2 认证
5. 使用新 Key 0 向 File 02 原子写入 URL record 和 android.com:pkg record
6. 重新读取刚写入的 NDEF，追加等长 e/c 占位符并计算 SDM offsets
7. 使用新 Key 0 写入 SDM 模板并执行 ChangeFileSettings 开启 SDM
```

底层仍按 Key 1、Key 2、最后 Key 0 的顺序修改，避免在其他 Key 尚未完成时先失去旧 Key 0 管理入口；按钮文案中的 Key 0/1/2 表示本操作覆盖的三个 Key，不表示底层修改顺序。

这是一项明确选择的复合测试操作，不表示 `ChangeKey`、NDEF 写入和 `ChangeFileSettings` 在卡片内部构成事务：每个成功命令都会立即持久化。若在 Key 阶段中途移卡，再次执行会跳过并认证已是 v1 的 Key；若在 NDEF 写完但 SDM 尚未开启时移卡，重试会覆盖相同 NDEF 后继续开启 SDM。若卡片已成功开启 SDM，复合操作会在任何 Key 或文件写入前拒绝重复执行。完成后应移开卡片，再单独执行“读卡并自动验证 SDM URL”。
