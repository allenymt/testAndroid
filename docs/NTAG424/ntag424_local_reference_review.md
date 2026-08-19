# NTAG 424 DNA 本地参考资料核对

日期：2026-08-19

## 结论摘要

当前问题的正确处理方式是：读取并解析完整的原始 NDEF message，只替换 URI record，在原 query 后追加 `e=<32 ASCII hex>` 和 `c=<16 ASCII hex>` 占位符，原样保留包括 `android.com:pkg` 在内的其余 records；随后由最终序列化的 **File 02 原始字节** 计算所有 SDM offsets。

不能把 URI 改成固定的 `/ntag424?e=...&c=...`，也不能用 URI 字符串下标计算 offset。旧版本已经覆盖掉的 `targetUrl` 或 application record 不存在于卡片上，无法从动态 PICCData/MAC 反推，必须从外部已知数据恢复。

## 资料范围与优先级

本次优先采用以下原厂资料：

1. `/Users/yulun/Downloads/ntag-424/AN12196.pdf`，NXP AN12196 Rev. 2.0，2025-03-04，共 59 页。下文页码是 PDF 文件页码，与印刷页码一致。
2. `/Users/yulun/Downloads/ntag-424/Ntag424SdmFeature-master/docs/NTAG 424 DNA datasheet NT4H2421Tx.pdf`，NXP NT4H2421Tx Product data sheet Rev. 3.0，2019-01-31，共 102 页。下文页码是 PDF 文件页码，与印刷页码一致。
3. `/Users/yulun/Downloads/ntag-424/Ntag424SdmFeature-master/docs/NFC Forum Type 4 Tag Technical Specification 97-NFCForum-TS-T4T-1.2.pdf`，NFC Forum Type 4 Tag Technical Specification 1.2。

参考源码包括：

- `/Users/yulun/Downloads/ntag-424/ntag424-java-main`
- `/Users/yulun/Downloads/ntag-424/Ntag424SdmFeature-master`（其中 `net.bplearning.ntag424` 基本是前一仓库的内嵌副本）

参考源码不是 NXP 原厂规范，出现冲突时以 NXP datasheet 和 AN12196 为准。

## 1. NDEF 容量和 offset 基准

### 1.1 File 02 的 256 bytes 包含 NLEN

AN12196 第 3 页说明 NDEF file 为 256 bytes。Datasheet 第 10 页 §8.2.3/Table 5 和 §8.2.3.2 进一步确认：File No. `02h`、File ID `E104h`、File Size `0100h`，即 256 bytes。

File 02 的字节布局为：

```text
offset 0..1    2-byte big-endian NLEN
offset 2..     NLEN bytes of encoded NDEF message
remaining      unused/static file bytes
```

因此容量约束是：

```text
2 + encodedNdefMessage.size <= 256
```

不能把 256 当作 URI payload 或 NDEF message 的可用长度。用户给出的原 URI 为 152 个 ASCII bytes；追加 `&e=` + 32 chars + `&c=` + 16 chars 后为 206 bytes。再加 URI record 编码及 `android.com:pkg` AAR，结果已经非常接近容量上限。必须以 Android `NdefMessage.toByteArray()` 的最终结果核算，不能靠字符数估算。

Android URI record 的 URI Identifier Code 通常会压缩 `https://`，可比参考库使用 code `0x00` 节省约 7 bytes；即便如此仍应检查实际结果。任何为了省空间而把外层 query 中 `%3A%2F%2F` 改成 `://` 的做法都会改变 URI 的线上表示，虽然 RFC 3986 允许 query 中出现 `:` 和 `/`，仍需确认业务端 query 解析、签名和跳转语义后才能采用。

### 1.2 所有 SDM offsets 都从 File 02 byte 0 开始

Datasheet 第 71-72 页 Table 69 把 `PICCDataOffset`、`SDMMACInputOffset`、`SDMMACOffset` 定义为 “Offset within the file”，并规定 ChangeFileSettings 中使用 3-byte LSB-first 编码。

AN12196 第 28 页 §5.7.3/Table 15 给出了可直接校验的例子：File 02 从 `0051 D1014D5504 ...` 开始，ENCPICCData offset 为 `0x20`，SDMMAC offset 为 `0x43`。这证明 offset 包含：

- 2-byte NLEN；
- NDEF record header；
- URI Identifier Code；
- URI payload 中 placeholder 之前的全部 bytes。

offset 指向 placeholder 的第一个 ASCII hex 字符，而不是参数名或 `=`。例如应在最终 File 02 bytes 中定位 `e=000...`，再取 marker 起点 `+2`；`c=` 同理。

应使用 byte offset，而不是 Kotlin/Java `String` 字符索引。URI 出现非 ASCII 字符、percent encoding、长记录从 SR 切换为非 SR，或 records 数量变化时，字符串下标都会失真。

### 1.3 三个 offset 的准确含义

| 字段 | 含义 | 本场景长度/约束 |
| --- | --- | --- |
| `PICCDataOffset` | encrypted PICCData 镜像的起始 byte | AES + ASCII 为 32 bytes；LRP + ASCII 为 48 bytes。Datasheet 第 37-39、72 页。 |
| `SDMMACOffset` | SDMMAC 镜像的起始 byte | 8-byte truncated MAC 被 ASCII 编码为 16 chars；必须满足 `offset + 16 <= fileSize`。Datasheet 第 41、72 页。 |
| `SDMMACInputOffset` | MAC 输入在动态文件数据中的起始 byte | MAC 输入严格是 `[SDMMACInputOffset, SDMMACOffset)`；必须 `<= SDMMACOffset`。Datasheet 第 41-42 页；AN12196 第 14、17 页。 |

当 `SDMMACInputOffset == SDMMACOffset` 时 MAC 输入长度为 0，官方示例就是这种配置。此时 SDMMAC 仍通过 SDM session key 绑定 PICCData/UID/计数器，但不覆盖 URL 中 `e` 与 `c` 之前的其他动态文件 bytes。若产品要求 MAC 同时覆盖业务 query 或 `e=` 区域，需要把 input offset 设置到希望覆盖的 byte 起点，并确保后端使用完全相同的 `[start, macOffset)` bytes 验证。

若启用 SDMENCFileData，Datasheet 第 39-41 页要求：

- `SDMMACInputOffset` 必须使完整 SDMENCFileData 被纳入 MAC；
- `SDMMACOffset >= SDMENCOffset + SDMENCLength`；
- 各镜像区域不得重叠。

当前仅配置 encrypted PICCData + SDMMAC、没有 SDMENCFileData 时，令 `SDMMACInputOffset = SDMMACOffset` 与 AN12196 官方示例一致。

## 2. 保留原 query 和 application record

正确算法如下：

1. 读取 File 02 的静态 NDEF message。
2. 用 NDEF parser 找到目标 URI record；不要假定它一定是唯一 record 或固定在首位。
3. 在 URI fragment `#...` 之前、原 query 之后追加 `e`/`c` 参数：已有 `?` 用 `&`，否则用 `?`。
4. 只替换该 URI record；原样保留其他 records 的顺序和内容，包括 TNF External Type `android.com:pkg`、payload `com.koudai.weidian.buyer`。
5. 让 NDEF 库重新序列化完整 message，使 MB/ME/SR、payload length 等字段自动更新。
6. prepend 2-byte big-endian NLEN，得到最终 File 02 bytes。
7. 在该 byte array 内定位唯一的 PICCData/MAC placeholder，并验证唯一性、顺序、不重叠和边界。
8. 用同一 byte array 同时进行容量检查、写入和 offset 计算，避免“检查一份、写另一份”。

推荐使用随机且足够唯一的临时 marker 构造 URI，再在最终 bytes 中定位后替换成等长的 `0` placeholder；至少也应验证 `e=`/`c=` marker 恰好命中一次且位于 URI record payload 内，避免其他 record 或原业务 query 中同名参数造成误命中。

如果原 URI 已有 `e` 或 `c` 参数，应先定义明确策略（拒绝、替换，或使用业务专属参数名），不能无条件重复追加。

### 参考代码能借鉴和不能照搬的部分

`/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/sdm/NdefTemplateMaster.java:65` 会在完整文件 bytes 中替换 placeholder，并在 `:88` 调整因替换长度变化而移动的 offsets，最后在 `:144` 将结果写入 `SDMSettings`。AES ASCII PICCData=32 chars、MAC=16 chars 的长度计算见 `:121`。动态计算思想是正确的。

但其 URL convenience API 有明显局限：

- `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/sdm/NdefTemplateMaster.java:41` 总是创建新的单 URI record，无法保留原 AAR。
- `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/util/Ndef.java:4` 固定使用 SR 和 URI Identifier Code `0x00`，只维护 1-byte payload length。
- `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/sdm/NdefTemplateMaster.java:47` 用固定下标回填长度，只适用于该单 record 布局。
- 示例 `/Users/yulun/Downloads/ntag-424/Ntag424SdmFeature-master/app/src/main/java/de/androidcrypto/ntag424sdmfeature/EncryptedSunActivity.java:274` 至 `:327` 也会用硬编码 URL 覆盖 File 02。

因此只能借鉴“基于最终 bytes 定位 placeholder”的思路，不能直接采用单 URI convenience API。

## 3. Plain/Full WriteData、分块和 CmdCtr

### 3.1 WriteData 上限

Datasheet 第 81-82 页 §11.8.2/Table 81 规定 WriteData 的 Data field “up to 248 byte including secure messaging”。这不是 Full 模式下可写 248 bytes 明文的意思。

Native APDU 的 `Lc` 是 1 byte。每条 WriteData 至少包含：

```text
7 bytes  FileNo + Offset(3) + Length(3)
N bytes  plain data, or AES-padded encrypted data
8 bytes  MACt in MAC/Full mode
```

所以：

- Plain：明文 chunk 最大 248 bytes，`7 + 248 = 255`。
- Full：明文需 `0x80` padding 到 16-byte 边界，再加 7-byte header 和 8-byte MAC。采用 224-byte 明文 chunk 时 padded ciphertext 为 240 bytes，`7 + 240 + 8 = 255`，恰好安全。

参考库 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/command/WriteData.java:11` 不提供通用自动分块；`/Users/yulun/Downloads/ntag-424/Ntag424SdmFeature-master/app/src/main/java/de/androidcrypto/ntag424sdmfeature/UnsetActivity.java:292` 仅用很保守的 `51 * 5 + 1` 六次写演示清空 256 bytes。

### 3.2 必须按当前 File 02 CommMode 发送

Datasheet 第 11-13、81 页说明 WriteData 使用目标文件当前配置的 Plain/MAC/Full mode。参考实现 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/command/WriteData.java:32` 会先 GetFileSettings，再按读到的 comm mode 派发；派发代码在 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/DnaCommunicator.java:258`。

若调用方已经读取过并锁定了当前设置，建议显式使用该 mode，避免每个 chunk 前额外执行一次 GetFileSettings。否则这些额外命令也会影响 session 命令计数，调用方必须全部纳入 CmdCtr 状态机。

### 3.3 每条成功命令都推进 CmdCtr

Datasheet 第 23-24 页 §9.1.2：AuthenticateEV2First 后双方 CmdCtr 从 `0000h` 开始；命令 MAC/IV 使用当前 CmdCtr，PICC 成功验证命令后将其加一，响应 MAC/IV使用已增加的值。普通 ISO/IEC 14443-4 additional frame 不影响 CmdCtr，但下一条独立 WriteData 是新命令。

参考实现中：

- Plain command 的 counter 处理见 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/DnaCommunicator.java:148`；
- MAC/Full command 用当前 counter 计算命令 MAC，成功响应后递增并验证 response MAC，见同文件 `:161` 至 `:213`；
- AES command IV 使用当前 CmdCtr，见 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/encryptionmode/AESEncryptionMode.java:48`。

分块 WriteData 的每个 chunk 必须使用同一 session 的下一个 CmdCtr。随后 ChangeFileSettings 也必须继续使用再下一个 CmdCtr。任何 GetFileSettings、ReadData 或其他 native command 插入中间，都必须由同一 session 状态机处理，不能重用旧 counter。

AN12196 第 29-31 页的 Full 示例也明确展示：WriteData 使用 `CmdCtr=0000`，随后 ChangeFileSettings 使用 `CmdCtr=0100`（协议字段为 LSB-first）。

## 4. 写入顺序和掉电一致性

AN12196 第 27-31 页 §5.7-5.9 的个性化顺序是：先构造并写入 NDEF 模板，再用 ChangeFileSettings 开启 SDM。这是新卡/当前 SDM disabled 时应遵循的基本顺序。

但 File 02 是 StandardData file，不是事务型 BackupData file。Datasheet 第 10、81 页明确指出：

- 无 transaction/backup；写入后立即可读；
- 单 communication frame 最多 128 bytes 的写具有 tearing protection；
- chained 或多次写中断可能留下不一致文件；
- 单帧 WriteData 比 chaining 更易控制，但多个独立命令仍不构成整体事务。

NFC Forum Type 4 Tag spec 第 56-58 页 §7.5.5 给出的多步 NDEF 更新协议是：

1. 先把 NLEN 写成 `0000`；
2. 从 file offset 2 开始依次写完整 NDEF message；
3. 最后单独写入真实 NLEN。

这不能让 File 02 的多个 writes 变成真正原子事务，但可避免中途读取者把半条 message 当成有效 NDEF。只有整个 File 02 能用一条命令写完时，才可把 NLEN 与 message 合并写；规范同时警告掉电时不能假设这种单命令合并写一定保持一致。

因此接近 256 bytes、必然分块时，不应在第一块就写入最终非零 NLEN。推荐先 `NLEN=0`，再从 offset 2 写 message chunks，最后写 NLEN；每条命令都推进 CmdCtr。

## 5. 已开启 SDM 后的安全更新

### 5.1 静态模板必须认证读取

未认证读取启用 SDM 的 File 02 时，PICC 会把 placeholder 替换为动态 PICCData/MAC。Datasheet 第 11-13、37、40-41 页明确说明：认证状态不做 SDM mirroring，常规 secure messaging 返回 static file data。

因此：

- 不要把 Android NFC framework 未认证读到的动态 URL 当成可再次个性化的静态模板；
- 先用满足 Read/ReadWrite 权限的 key 认证；
- 按当前 CommMode 使用 authenticated native ReadData 读取 File 02 静态 bytes；
- 结合 GetFileSettings 中的现有 offsets 校验 placeholder，而不是把某次动态值固化写回。

ISOUpdateBinary 不是受保护更新的替代方案。Datasheet 第 88-89 页 §11.10.3/Table 95 将它限制为 free write + CommMode.Plain，并明确不允许 AuthenticatedEV2/LRP。受权限保护的更新应使用 native WriteData。

### 5.2 推荐两阶段更新流程

下面的两阶段流程是根据原厂 access/SDM/非事务语义推导出的安全实践，不是 NXP 文档定义的单一固定事务：

1. 读取 GetFileSettings，完整解析 FileOption、AccessRights、SDMOptions、SDMAccessRights 和所有可选 offsets。
2. 用相应 key 认证并读取静态 File 02；解析并备份完整原始 bytes 和设置。
3. 离线生成最终 NDEF，保留原 query/AAR，计算并验证容量与 offsets；所有检查通过前不写卡。
4. 用 Change 权限 key 暂时关闭 SDM，最好同时保持原 CommMode/权限不变或暂时收紧未认证读。ChangeFileSettings 会依据**当前** file comm mode 执行（Datasheet 第 70 页）。如果切换认证 key/session，重新从 CmdCtr=0 管理新 session。
5. 用 Write/ReadWrite 权限 key 按当前 CommMode 执行 NDEF 三段式更新：`NLEN=0`、message chunks、最终 NLEN。
6. 用 Change 权限 key，一次 ChangeFileSettings 写入最终 SDM options、rights 和重新计算的 offsets，再启用 SDM。Datasheet 第 35 页说明启用 SDM 会把 SDMReadCtr reset 为 `000000`，后端去重/计数逻辑必须知道这一点。
7. 认证读取一次静态 bytes，逐 byte 验证模板和 NLEN；断开并重新建立未认证会话，再读一次动态 NDEF，验证 URI、原 query、AAR、PICCData/MAC 长度与后端 MAC。

若不能暂时关闭 SDM，也可按当前设置直接写静态 file 并最后更新 offsets，但多块写期间未认证读者可能看到“旧 offsets + 部分新内容”的混合响应，风险更高。

必须先确认 Update 所需的 RW key 与 Change/CAR key；它们可能不是同一 key。参考 app 的示例也是按 RW key 写模板，再按 CAR key ChangeFileSettings，见 `/Users/yulun/Downloads/ntag-424/Ntag424SdmFeature-master/app/src/main/java/de/androidcrypto/ntag424sdmfeature/EncryptedSunActivity.java:238` 至 `:327`。

### 5.3 不可恢复情形

如果旧逻辑已经用固定 `/ntag424?e=...&c=...` 覆盖 File 02，并删除 AAR，那么认证读取只能得到这个旧逻辑写入的静态模板，原 `targetUrl` 和 `com.koudai.weidian.buyer` 已经不存在。PICCData 和 SDMMAC 都不携带这些业务内容，不能逆推出原数据。恢复必须依靠后端、日志、UID 到业务数据映射或用户重新输入。

## 6. 参考源码风险

参考源码可用于理解协议，但不适合原样复制：

1. 单 URI API 会覆盖其他 records，详见上文。
2. placeholder 每种只找第一次，并且在全文件搜索；相关实现位于 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/util/ByteUtil.java:240`。生产实现应验证唯一性及 record 范围。
3. `String.getBytes()` 未指定 charset，见 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/util/Ndef.java:15`；应显式使用 UTF-8/ASCII。
4. `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/command/FileSettings.java:64` 的 decode 将先读到的 SDM access-right byte 当作 meta/file，而 `:135` 的 encode 却先写 RFU/counter-retrieval、再写 meta/file，decode/encode 顺序不对称。对已启用 SDM 的设置做 read-modify-write 可能破坏权限，不能照抄；应严格依照 Datasheet Table 69，并添加真实 bytes 的 decode/encode round-trip 测试。
5. `WriteData.run(communicator, fileNum, data, offset)` 每次会先执行 GetFileSettings，见 `/Users/yulun/Downloads/ntag-424/ntag424-java-main/src/main/java/net/bplearning/ntag424/command/WriteData.java:32`。在认证 session 内分块调用时，这些额外命令也必须推进 CmdCtr。

## 7. 对当前仓库实现的核对结果

当前未提交实现已经具备以下正确方向：

- `app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Ntag424DnaPersonalizationClient.kt:226` 复制原 records，只替换 URI record，并从最终 `fileBytes` 定位 offsets。
- 同文件 `:248` 以 GetFileSettings 的 file size 检查最终 File 02 bytes。
- 同文件 `:80` 和 `:94` 区分 Full/Plain 分块，并在每块成功后推进 CmdCtr。
- 同文件 `:116` 将 `SDMMACInputOffset` 设为 `SDMMACOffset`，符合 zero-length MAC input 官方示例。

仍建议后续补齐以下事项：

1. 当前 `:36` 直接拒绝已开启 SDM，尚未实现第 5 节的认证静态读取和安全更新流程。
2. 当前 `:240` 把非零 NLEN 与 message 一起从 offset 0 分块写；接近容量时中断可能留下“NLEN 有效但 message 不完整”。应改为 Type 4 Tag 三段式写入。
3. 当前 marker 搜索 `:241` 至 `:247` 只找第一次；应验证唯一性、位于目标 URI payload，并处理原 URL 已存在 `e/c` 的情况。
4. 当前 `compactUrlForSdm` 在 `app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Ntag424DnaProtocol.kt:81` 改写 percent-encoded query 内容。上线前需用真实业务跳转链验证等价性；若要求字节级保留原 URI，则不能这样改写，只能通过缩短参数名/业务 URL、移除可选 record 或改业务承载方式解决容量。
5. 当前目标只开启 SDM，必须保留 File 02 原有的 Read/Write/ReadWrite/Change rights 和通信模式。若未来需要收紧写权限，应作为独立需求显式设计和验证，不能夹带在开启 SDM 的操作中。

## 8. 建议测试矩阵

- 原 URI 无 query、有 query、有 fragment、query 末尾为空。
- 原 query 已包含 `e` 或 `c`。
- 两条 records（URI + AAR）及三条以上 records；URI 不在第 1 条。
- URI 含非 ASCII、percent encoding；URI record 从 SR 跨到非 SR 的边界。
- 最终 File 02 size 为 255、256、257 bytes。
- PICCData/MAC marker 在其他 record 出现，验证拒绝歧义。
- Plain 和 Full 各执行两块以上 WriteData，逐条断言 CmdCtr、command MAC 和 response MAC。
- 在每个写入阶段模拟失败：NLEN 清零后、message 中段、最终 NLEN 前、ChangeFileSettings 前。
- 已开启 SDM：未认证读取动态 bytes，认证读取静态 placeholder；更新后两种读取都正确。
- FileSettings 已启用/禁用 SDM 的 decode/encode round-trip，覆盖所有可选 offset 组合和 SDMAccessRights 字节顺序。

## 9. File 03 是否可写业务数据

可以。File No. `03h` 就是原厂预留给专有业务数据的文件，但它与 File 02 的 NDEF/SDM 用途不同。

### 9.1 文件属性与默认权限

NXP Datasheet 第 10 页 §8.2.3/Table 5 将 File 03 定义为：

- File No. `03h`，ISO File ID `E105h`；
- `StandardData file`，容量固定为 128 bytes；
- 用途是 “Proprietary file, storage of raw data”，即按 offset/length 存取原始 bytes，芯片不解析其业务格式。

Datasheet 第 12 页 Table 8 给出交付时的默认 access rights：

| 权限 | 默认 access condition | 实际含义 |
| --- | --- | --- |
| Read | `2h` | 认证 AppKey 2 可读 |
| Write | `3h` | 认证 AppKey 3 可写 |
| ReadWrite | `3h` | 认证 AppKey 3 后可读写 |
| Change | `0h` | 认证 AppKey 0 后可 ChangeFileSettings |

因此默认状态下，读可以用 Key 2 或 Key 3，写必须用 Key 3；要改通信模式或权限则用 Key 0。Datasheet 第 14 页 §8.2.4.2 说明 AppKey 0..4 是 AES-128 keys，交付值均为 16 bytes `00h`；个性化后应使用实际密钥，不应依赖默认零密钥。

File 03 的默认 communication mode 是 `CommMode.Full`（Datasheet 第 14 页 Table 13）。所以认证后的 native `ReadData`/`WriteData` 必须按 Full secure messaging 处理加密、padding、CmdCtr 和 MAC，不能按 Plain APDU 直接读写。

AN12196 第 32-34 页 §5.10-5.12 给出了对应的原厂实例：先使用 Key 03 执行 `AuthenticateEV2First`，随后对 E105/File 03 执行 `Cmd.WriteData, CommMode.FULL`；若直接使用 native WriteData，不需要先执行 ISOSelectFile。

### 9.2 ReadData/WriteData 的边界

- `ReadData`：Datasheet 第 79-80 页 §11.8.1/Table 78-80 允许指定 3-byte offset/length，length=`000000h` 表示从 offset 读到文件末；短 `Le` 将响应限制为包含 secure messaging 的 256 bytes。越界、未获授权或 MAC 错误都会被拒绝。
- `WriteData`：Datasheet 第 81-82 页 §11.8.2/Table 81-83 允许指定 3-byte offset/length，写入不得超过 `FileSize - Offset`；Data field 最多 248 bytes，且该数字包含 secure messaging 开销。File 03 只有 128 bytes，整文件在 Full 模式的命令长度上可容纳，仍必须正确实现 AES padding/MAC 并检查边界。
- File 03 是 StandardData file，没有 transaction/backup 机制；数据写入后立即可读。Datasheet 第 10 页 §8.2.3.1 仅保证单个 communication frame 中最多 128 bytes 的写入具有 tearing protection，不应把多命令或中断的 chaining 当成原子事务。
- 虽然 StandardData file 在命令集上也支持 `ISOReadBinary`/`ISOUpdateBinary`，但这两个 ISO 命令只支持 Plain/free access（Datasheet 第 86-89 页 §11.10.2-11.10.3）。File 03 默认需要 Key 2/3 且为 Full，因此默认配置下应使用 EV2/LRP 认证后的 native `ReadData`/`WriteData`，不能用 ISO 命令绕过权限。

### 9.3 与 NDEF/SDM 的关系及建议用途

File 03 不是 NDEF file。NFC Forum 普通 NDEF 读卡流程从 File 02 (`E104h`) 取 NDEF message，不会把 File 03 的 bytes 当成第三条 NDEF record。CC file 会将 `E105h` 声明为 proprietary file（Datasheet 第 11 页 §8.2.3.2），了解该私有协议且拥有密钥的应用才能读写。

File 03 也不能开启 SDM。Datasheet 第 10 页 §8.2.3.1 和第 74 页 ChangeFileSettings 错误条件都明确限定：启用 SDM 只允许针对 File No. `02h`。因此 File 03 的数据不会被 SDM 镜像到 URL，也不会自动参与 `e`/`c` 的动态生成。

适合存放不超过 128 bytes、只由业务 App 在认证后读写的定长/短二进制数据，例如业务对象 ID、版本号、状态位、短配置或带版本与完整性字段的紧凑结构。不适合存 URL + pkg NDEF records（应放 File 02）、大文本/JSON、历史列表或需要多步事务一致性的数据。`CommMode.Full` 保护的是认证会话中 PICC-PCD 通信的机密性和完整性，业务格式仍应自带版本/长度，并在需要时加入 CRC/MAC 或采用后端数据 ID 而非直接存放敏感明文。

### 9.4 当前 Demo 的 File 03 读写测试

测试页提供相互独立的 File 03 写入和读取操作，不修改 File 01、File 02、NDEF、SDM 或任何 FileSettings：

- 写入前读取 File 03 settings，要求容量为 128 bytes、CommMode 为 Full；默认权限下使用 AppKey 3 执行 `AuthenticateEV2First`，随后以 Full `WriteData` 从 offset 0 覆盖完整 128 bytes。
- 读取前同样校验 settings；默认权限下使用 AppKey 2 认证，再以 Full `ReadData` 读取完整 128 bytes，验证 response MAC 后使用 response IV 解密。
- 若卡片权限编号已改变，Demo 会选择 Read/Write 或 ReadWrite rights 中实际配置的 AppKey 0..4。当前 AppKey 0..4 的测试 key value 统一为 16-byte 全零值，但 key number 仍按 FileSettings 权限执行。
- 测试数据格式固定为 `2-byte big-endian payload length + UTF-8 payload + zero padding`，因此 UTF-8 payload 最大为 126 bytes。该格式只是 Demo 自定义格式，并非 NXP 或 NFC Forum 标准。

完整覆盖 128 bytes 可避免短数据覆盖后残留上一次写入的尾部内容。File 03 是 StandardData file，单帧 128-byte 写入具备芯片声明的 tearing protection；这不代表由多个命令组成的更大业务事务具有原子性。

## 10. 动态 URL 中 `e` 和 `c` 的含义与验证

当前测试卡启用 SDM 后的 GetFileSettings 数据为：

```text
004000E0000100 C1 F121 940000 B70000 B70000
```

可以解码为：

- FileOption `40h`：File 02 使用 Plain communication，并启用 SDM；
- AccessRights `00E0h`：Read 免费，Write/ReadWrite/Change 使用 Key 0。这是修复前 Demo 写死权限产生的实卡历史结果，不是开启 SDM 的协议要求；当前实现会保留卡片开启前的 `EEE0` 权限。
- SDMOptions `C1h`：镜像 UID、镜像 SDMReadCtr，并用 ASCII hex 输出；
- SDMAccessRights `F121h`：SDMMetaRead 使用 Key 2，SDMFileRead 使用 Key 1，SDMCtrRet 使用 Key 1；
- PICCDataOffset `940000h`：little-endian 值为 `0x94`；
- SDMMACInputOffset 和 SDMMACOffset 均为 `B70000h`：little-endian 值为 `0xB7`。

对应一次未认证读取返回的动态字段示例为：

```text
e=33D34C7FF92E1758EDC9059C92C8136C
c=A2FF6550FE37457E
```

### 10.1 `e` 是 encrypted PICCData

`e` 是芯片在每次未认证读取时动态生成的 encrypted PICCData。当前使用 AES 和 ASCII encoding，所以它是 32 个十六进制字符，代表 16 bytes AES 密文。它不是 App 写入的固定业务数据，也不会覆盖保存于 File 02 的静态模板。

使用 SDMMetaRead Key（当前配置为 Key 2）AES-128 解密后，16-byte 明文结构为：

```text
1 byte   PICCDataTag
7 bytes  UID
3 bytes  SDMReadCtr（little-endian）
5 bytes  RandomPadding
```

其中 `PICCDataTag` 的标志位描述 UID、SDMReadCtr 是否存在以及 UID 长度。当前 `SDMOptions=C1h` 同时启用了 UID 和计数器，因此二者都会出现在解密结果中。NXP AN12196 Rev. 2.0 第 10-12 页 §4.4.2 给出了 encrypted PICCData 的生成、解密和字段解析流程；Datasheet 第 37-39 页定义了 PICCData mirroring 和 offset/长度约束。

Key 2 通常不应按 UID diversified，因为服务端必须先解密 `e` 才能取得 UID；如果解密密钥本身依赖这个尚未知的 UID，会形成循环依赖。NXP AN12196 第 10 页对此有明确提醒，并要求特别保护该密钥的服务端存储。

### 10.2 `c` 是 SDMMAC

`c` 是芯片为本次读取计算的 truncated AES-CMAC。当前采用 ASCII encoding，因此 16 个十六进制字符代表 8-byte MAC。芯片使用 SDMFileRead Key（当前配置为 Key 1）结合从 UID 和 SDMReadCtr 派生的 SDM session MAC key 生成它。

当前配置满足：

```text
SDMMACInputOffset = SDMMACOffset = 0xB7
```

因此 DynamicFileData 的额外 MAC 输入区间 `[SDMMACInputOffset, SDMMACOffset)` 长度为 0，即不额外覆盖 URL 中 `targetUrl` 或 `e=` 前面的静态字符。但 SDMMAC session key 仍绑定从 `e` 解出的 UID 和 SDMReadCtr，所以 `c` 仍可证明本次动态响应来自持有正确 Key 1 的芯片，并可配合计数器检测重放。NXP AN12196 第 14-17 页 §4.4.4 给出了 zero-length input 和非空 input 两种 SDMMAC 算法；Datasheet 第 40-42 页定义了 MAC 输入区间和 offset 约束。

若产品要求 `c` 同时保护 URL 中的业务 query，应把 `SDMMACInputOffset` 配置到需要纳入 MAC 的起始 byte，并让后端以完全相同的 File 02 byte 区间 `[inputOffset, macOffset)` 计算。不能只调整后端算法而不修改卡片 FileSettings。

### 10.3 服务端验证顺序

生产环境不能只判断 `e` 或 `c` 是否发生变化。完整验证流程应为：

1. 严格验证 `e` 为 32 个 hex 字符、`c` 为 16 个 hex 字符，并拒绝重复参数或非规范编码。
2. 使用该发行域的 SDMMetaRead Key 2 解密 `e`。
3. 解析并校验 `PICCDataTag`、UID、SDMReadCtr 和 padding 结构。
4. 确认 UID 属于已发行卡片，并加载对应的 SDMFileRead Key 1；若 Key 1 使用 UID diversification，则此时按 UID 派生卡片密钥。
5. 根据 UID、SDMReadCtr 和 Key 1 派生本次 SDM session MAC key。
6. 按卡片配置的 `[SDMMACInputOffset, SDMMACOffset)` 数据重新计算 truncated SDMMAC。
7. 使用常量时间比较计算结果与 `c`，不接受只解密成功但 MAC 不匹配的请求。
8. 在 MAC 验证成功后检查 SDMReadCtr 是否符合服务端防重放策略，例如大于该 UID 上次接受的计数值；计数器状态更新应与业务受理保持原子性。

简化流程如下：

```text
e --Key 2 解密--> UID + SDMReadCtr
                       |
                       +--结合 Key 1 派生 session MAC key
                                      |
动态文件 MAC 输入 --------------------+--> 重新计算 SDMMAC --> 常量时间比较 c
                                                                        |
                                                                        +--> UID/计数器防重放检查
```

必须先验证 `c`，再把 UID 和计数器作为可信业务输入。仅检查 UID、仅检查计数器递增、仅判断 URL 每次不同，均不能证明响应真实性。

### 10.4 静态模板与读取时动态替换

开启 SDM 时，App 实际写入的是带等长占位符的静态 NDEF：

```text
原业务 URL&e=00000000000000000000000000000000&c=0000000000000000
```

同时通过 ChangeFileSettings 告诉芯片两个占位区域的 byte offset。之后：

- 未认证读取：芯片实时生成 `e` 和 `c` 并在响应中替换占位符；静态 File 02 内容不被改写；
- 认证读取：返回静态 NDEF 和占位符，不执行 SDM mirroring；
- 每次有效 SDM 读取会推进 SDMReadCtr，因此后续 `e` 和 `c` 通常不同。

当前 Demo 使用全零 Key 1 和 Key 2，只能验证协议和 App 流程，不能提供生产级防伪。生产密钥必须使用安全随机值，由服务端/HSM 管理；不得写入 Android App、日志或 Git。服务端还应记录密钥版本和卡片个性化 profile，确保验证时采用与卡片完全一致的 SDMOptions、offset 和 key diversification 规则。

## 11. 功能操作边界与 Demo 密钥约定

当前 Demo 将卡片配置拆成相互独立的一次性操作，调用方选择哪个操作，就只改变该功能所必需的字段：

| 操作 | 允许改变 | 必须保持不变 |
| --- | --- | --- |
| 开启 SDM | FileOption 的 SDM 位、SDMOptions、SDMAccessRights、SDM offsets，以及 URL 中 SDM 所需的等长占位符 | 原 Read/Write/ReadWrite/Change rights、原通信模式、pkg record 和原业务 query |
| 关闭 SDM | 清除 FileOption 的 SDM 位，并按协议移除 FileSettings 中不再适用的 SDM 配置段 | NDEF 内容、Read/Write/ReadWrite/Change rights 和通信模式 |
| 增加写保护 | File 02 的 Write、ReadWrite rights 改为 Demo Key 0 | SDM 全部配置、Read/Change rights、通信模式和文件内容 |
| 移除写保护 | File 02 的 Write、ReadWrite rights 改为 Free (`Eh`) | SDM 全部配置、Read/Change rights、通信模式和文件内容 |

这里的“原子化”是功能语义边界：开启或关闭 SDM 不能顺带改变写保护，增加或移除写保护也不能重建 NDEF 或改变 SDM。每项操作仍通过一次 `ChangeFileSettings` 提交对应的最终 FileSettings；开启 SDM 之前写入等长占位符是 SDM 本身必需的准备步骤，不视为额外的业务功能。关闭 SDM 不删除 URL 中原有占位符；如需替换业务 NDEF，应在关闭成功后单独执行写入操作。

为方便当前测试，AppKey 0、SDMMetaRead 使用的 AppKey 2、SDMFileRead 使用的 AppKey 1 均采用同一份 16-byte 全零 key material。Key 编号仍承担各自协议角色，只是测试阶段的 key value 相同。此约定不包含 `ChangeKey`，上述三个操作都不会修改卡片密钥。生产环境必须替换为受控密钥，并单独设计密钥轮换流程。
