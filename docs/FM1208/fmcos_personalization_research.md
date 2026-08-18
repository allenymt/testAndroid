# FM1208/FMCOS 业务数据个性化研究

## 结论

当前卡不是可直接写入的 NDEF 标签，而是只能选中 MF 的 FMCOS 卡。`SELECT 3F00` 返回的 `88 01 01` 只声明 DIR 的短文件标识符为 1；随后 `READ RECORD` 返回 `6A82` 表明 DIR EF 实际不存在。手册也明确 DIR 不是强制建立的文件。[FMCOS 手册，§4.1.1–4.1.2，PDF pp.40–41；§7.4，pp.54–55]

因此，当前的失败不是 `UPDATE` 参数错误，而是没有可写的目标 EF。配置前必须先建立业务 DF/EF；但现有两份官方资料不足以安全生成建文件 APDU。

## 手册能够确认的内容

- FM1208 是支持 ISO 14443-A 的非接触 CPU 卡芯片，提供 8 KB EEPROM 和 DES/3DES 能力。该芯片手册没有列出 NDEF、NFC Forum Type 4 或 NDEF AID。[FM1208 手册，§1.1–1.2，PDF p.4]
- FMCOS 以唯一 MF、DF 和 EF 组成文件树。EF 可为二进制 `28`、定长记录 `2A`、变长记录 `2C`、循环 `2E`、钱包 `2F` 或 KEY `3F`；MF/DF 类型为 `38`。[FMCOS 手册，§4.1、§4.4，PDF pp.40–43]
- KEY EF 必须是每个 MF/DF 下第一个建立的文件，每个目录最多一个。刚进入一个既无 KEY 也无其他文件的目录时可不受权限限制地建文件；离开后再进入就按已建文件的访问权限执行。[FMCOS 手册，§4.1.3、§4.3，PDF pp.41–42]
- 访问权限在建文件时设定。`F0` 表示当前目录状态 0–F 都可操作；例如 `F1` 要求状态至少为 1。复位或切换目录会将状态重置为 0，可通过 PIN 或外部认证改变状态。[FMCOS 手册，§5.1–5.3，PDF p.45]
- `CREATE` 是专有命令 `CLA=80, INS=E0`，但手册只在命令表列出它，没有 P1/P2/Lc/Data 或文件控制数据的编码定义。`WRITE KEY (80/84 D4)` 和 `ERASE DF (80 0E)` 同样仅被列为专有命令。[FMCOS 手册，§1.4，PDF pp.9–10]

## 已有 EF 的写入规则

`UPDATE BINARY` 为 `CLA=00/04, INS=D6/D0`。选中当前二进制 EF 后，P1/P2 可表示偏移；也可用 P1 的低 5 位选择 1–31 范围的短 FID。普通写入使用明文 Data，线路保护还需 4-byte MAC，加密保护需密文加 MAC。`6982` 表示写条件不满足，`6A82` 表示文件不存在。[FMCOS 手册，§7.7，PDF pp.60–61]

`UPDATE RECORD` 为 `CLA=00/04, INS=DC`，P1 是记录号，P2 同时表示短 FID 和定位方式。变长记录的 Data 必须是 TLV，且修改已有记录时新旧整条长度必须相同。`APPEND RECORD` 可用于可追加的记录 EF。[FMCOS 手册，§7.8–7.9，PDF pp.61–64]

## 建议的安全个性化方案

对于“APP 自用读写 URL 和 Android 包名”，不需要冒充标准 NDEF 应用。建议由复旦微电子或制卡供应商用完整个性化规范/工具预建：

1. 一个业务自有 ADF，分配不与标准 NDEF AID 冲突的 DF 名和唯一 FID。
2. 一个容量充足的二进制 EF，分配唯一 FID；如需短 FID 访问，FID 应位于 1–31。
3. 在业务层保存 `[2-byte big-endian length][NDEF message bytes]`。这是本项目的应用数据设计，便于复用现有 URI 和 `android.com:pkg` 解析；它不会使 FMCOS EF 自动成为 Android NDEF 标签。
4. 如果卡需要可重复写，设计“公开读、认证写”：读权限可为 `F0`，写权限需认证后状态。KEY EF 必须先建，密钥由发卡方生成、注入和管理，不应内置在 APP 或 Git 中。
5. APP 直接唤起依赖 `TECH_DISCOVERED + IsoDep` 的 manifest 匹配，而不依赖 FMCOS EF 中的包名。FMCOS 中的包名仅是 APP 自定义数据。

个性化参数必须先形成一份发卡 profile：DF 名/AID、DF FID、EF 类型、EF FID/SFI、容量、数据编码、读/写/追加权限、是否启用线路保护，以及 KEY 类型、标识、后续状态、错误次数与密钥分发方式。在拿到 `CREATE 80 E0` 完整字段定义和制卡流程前，APP 不应开放 FMCOS 写入。

## NDEF 个性化与不可确定项

两份手册全文都没有 NDEF、NFC Forum Type 4、Capability Container、CC EF `E103`、NDEF EF 或 AID `D2760000850101` 的配置说明。实卡对该 AID 返回 `6A82`，只能证明当前卡没有 NDEF 应用，不能单凭芯片型号推断它能或不能被个性化为 Type 4 Tag。如必须让 Android 系统把卡识别为 NDEF，需供应商明确提供 NFC Forum Type 4 NDEF 个性化方案，不能用当前 FMCOS 通用手册猜测。

手册也没有定义任何“默认密钥”或“默认 PIN”。外部认证章节中的 `01 02 03 04 05 06 07 08` 明确是示例；认证失败会减少重试次数，并可导致密钥锁死。[FMCOS 手册，§7.1–7.2，PDF pp.49–50]

## 禁止的试探操作

- 不猜测 `CREATE 80 E0` 的 Data，因为资料没有其专有编码。
- 不使用手册示例密钥尝试认证，不暴力枚举 PIN/密钥。
- 不发送 `ERASE DF`、`INITIAL EEPROM`、`WRITE EEPROM` 或生产测试命令。手册将后两者标为生产测试命令。[FMCOS 手册，§1.4，PDF pp.9–10]

## 供应商必须补充的资料

1. 与这批卡内 FMCOS 版本匹配的 `CREATE (80 E0)` 命令完整字段表和状态码。
2. KEY EF 和 `WRITE KEY (80/84 D4)` 的 TLV 结构、密钥类型编码、初始发卡流程及制卡工具。
3. 卡片是否已锁定 MF、是否允许继续个性化，以及已存在的密钥/权限 profile。
4. 如要求系统级 NDEF，提供该 FMCOS 版本支持的 Type 4 Tag 个性化文档或预个性化卡片。

## nfcproxy-master 参考评估

### 结论

该库是 EMV APDU 中继、录制、回放和 HCE 工具，不是 FM1208/FMCOS 个性化工具。README 明确说明它是 NFCProxy fork，并以 MITM 方式截取非接触 IC 卡交互。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/README.md:1</code>、<code>:4</code>、<code>:9</code>]

静态全库检索未发现 FM1208、FMCOS、<code>CREATE 80 E0</code>、<code>UPDATE BINARY</code>、<code>WRITE KEY</code>、FMCOS 卡密钥认证、Type 4 NDEF AID <code>D2760000850101</code> 或 MF <code>3F00</code> 的实现。因此它不能补齐当前缺少的 CREATE Data 结构、KEY EF、安全属性和认证流程，没有可直接复用的 FMCOS 写入代码。

### IsoDep、APDU、FMCOS 与 NDEF 的层次

- <code>IsoDep</code> 只是 Android 提供的 ISO-DEP 字节传输通道；该库连接卡片后，把网络收到的任意 APDU 交给 <code>transceive()</code>。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/NFCRelayActivity.java:489</code>、<code>:503</code>、<code>:512</code>]
- APDU 是通道中传输的命令/响应格式。该库只负责透明转发，并不理解 FMCOS 的 MF/DF/EF、状态字、访问权限或写入状态机。
- FMCOS 是卡片 OS 及文件/安全模型，需要专有 CREATE、KEY EF 和认证规范；拥有 IsoDep/APDU 通道不等于知道如何安全个性化 FMCOS。
- NDEF 是更上层的数据格式和标签协议。该库仅通过 Android <code>Ndef</code> API 读取已有消息；注释中的 IsoDep 示例也只是 SELECT 自定义 AID，不含 Type 4 CC/NDEF EF 流程。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/NfcCardReader.java:53</code>、<code>:110</code>、<code>:152</code>]

### 可借鉴部分

- APP 内主动扫描的 Reader Mode 生命周期可参考：使用 <code>enableReaderMode()</code> 注册回调，在暂停时 <code>disableReaderMode()</code>。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/NFCRelayActivity.java:85</code>、<code>:94</code>]
- APDU 录制、保存和展示可用于在合法测试环境中采集厂商官方个性化工具的交互，再人工分析。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/DBHelper.java:45</code>、<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/NFCProxyActivity.java:1126</code>]
- EMV BER-TLV 解码器可有限用于改善 <code>6F/84/A5</code> FCI 日志展示，但不是 FMCOS CREATE 参数解析器。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/utils/EmvBerTlvHelper.java:14</code>]

Reader Mode 示例设置了 <code>FLAG_READER_SKIP_NDEF_CHECK</code>。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/NFCRelayActivity.java:70</code>] 该标志会跳过 Android NDEF 检测，适合只走原始 IsoDep 的场景，但不应直接用于本项目“自动兼容 NDEF + FMCOS”的统一扫描路径，否则 NDEF 卡不会按预期暴露 <code>Ndef</code> tech。当前实现应继续让系统执行 NDEF 检测，再根据 tech 分派到 NDEF 或 FMCOS 客户端。

### 无关或危险部分

- HCE 配置出现 <code>1PAY.SYS.DDF01</code> 和 <code>2PAY.SYS.DDF01</code>，只是让手机模拟卡时接收 SELECT AID，并不是向 FM1208 创建 DF。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/res/xml/apduservice.xml:7</code>、<code>:9</code>]
- 内置事务是 SELECT <code>2PAY.SYS.DDF01</code>、SELECT 支付 AID、GPO、READ RECORD 等查询序列，没有建文件或写卡命令。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/DBHelper.java:29</code>、<code>:32</code>]
- PBKDF2/AES 用于两台设备间的中继网络加密，不是 FMCOS 外部认证或卡片 MAC。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/MyHostApduService.java:192</code>、<code>:198</code>]
- tech filter 将 IsoDep、MifareClassic、MifareUltralight 和 Ndef 放在同一个 <code>tech-list</code> 中。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/res/xml/nfc_tech_filter.xml:5</code>] 同一列表要求全部 tech 同时匹配，不适合当前 FM1208 的 <code>IsoDep + NfcA</code> 或独立 NDEF 分支。
- 中继和回放把未知字节直接发送给真实卡，没有 APDU 白名单、只读限制或认证重试保护。[源码：<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/NFCRelayActivity.java:503</code>、<code>:512</code>；<code>/Users/yulun/Downloads/nfcproxy-master/app/src/main/java/org/eleetas/nfc/nfcproxy/NFCProxyActivity.java:609</code>、<code>:617</code>] 未知序列可能包含 CREATE、ERASE 或 WRITE KEY，禁止直接回放到业务卡。

可行动建议是：仅借鉴 Reader Mode 生命周期和 APDU 采集思路；若能取得厂商工具的合法抓取记录，应逐条审核并形成明确的 FMCOS 发卡 profile，再将允许的命令实现为受约束的业务状态机，不引入透明网络转发或任意回放能力。

## nfc-clone-main 参考评估

### 结论

该库不能解决当前 FMCOS 写入配置问题。静态全库检索未发现 FM1208、FMCOS、`CREATE 80 E0`、DF/EF 创建、`UPDATE RECORD`、FMCOS KEY EF/外部认证、标准 NDEF AID `D2760000850101`、CC EF `E103` 或 NDEF EF `E104` 的实现，也没有 relay、replay、proxy 或真实卡片克隆逻辑。项目名称中的 “Clone” 实际指手机端 HCE 示例；其 HCE 只对 SELECT、READ BINARY、UPDATE BINARY 返回演示响应。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/HostCardEmulationService.kt:29`、`:34`、`:39`]

当前 FM1208 **确实是 ISO-DEP 卡，且当前交互是 APDU**：实卡 tech 包含 `IsoDep`，本项目通过 `IsoDep.get(tag)` 取得连接并用 `IsoDep.transceive()` 发送 ISO/IEC 7816-4/FMCOS APDU。[本项目源码：`app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Fm1208FmcosClient.kt:70`、`:85`] `IsoDep` 是传输层，APDU 是命令格式，FMCOS 是卡 OS/文件与安全模型，NDEF 又是其上的数据/标签协议；支持前两层不等于卡已配置 NDEF，也不等于已知 FMCOS 专有 CREATE Data。

### 可直接复用、仅可借鉴与无关部分

- **可直接复用：无。** 本项目已经有更完整的 NDEF/FMCOS 分派、状态字校验和 APDU 日志。该库写 NDEF 仅调用 Android `Ndef.writeNdefMessage()`。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/NfcManager.kt:110`]
- **仅可借鉴：** `NdefRecord.createTextRecord()` 和 `createUri()` 是标准 Android NDEF 消息构造入口。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/NfcManager.kt:100`、`:105`] 但当前项目已经采用同类 API，并额外需要 `android.com:pkg` record。
- **仅可借鉴：** manifest 中 `TECH_DISCOVERED` 与 tech-filter 的连接方式是正确概念。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/AndroidManifest.xml:34`、`:42`] 但其 filter 把 `IsoDep/NfcA/NfcB/NfcF/NfcV/Ndef` 放在同一个 `tech-list`，Android 将同一列表按 AND 匹配，现实卡片几乎不可能同时拥有这些 tech，不能复制。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/res/xml/nfc_tech_filter.xml:4`]
- **无关：** `HostApduService` 是让**手机模拟卡**并响应外部读卡器，不会把 DF/EF 写进实体 FM1208，也不会使实体卡获得 NDEF 能力。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/HostCardEmulationService.kt:11`]

### 协议与实现风险

- 该库虽然导入 `IsoDep`，实际对实体卡发送数据时使用的是 `NfcA.transceive()`，且所谓 SELECT MF 是 `00 A4 00 00 00`，既没有 `3F00` 数据也没有本卡验证过的 `Lc=02`；不能替换当前 `IsoDep + 00A40000023F00` 流程。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/NfcManager.kt:9`、`:80`、`:84`]
- NDEF URI 解码把 URI Identifier Code 一并转成字符串；Text 解码只跳过状态字而未跳过语言码，解析结果不符合 NDEF RTD Text/URI 格式。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/NfcManager.kt:45`、`:50`、`:59`]
- `Ndef.get(tag)` 为 `null` 时，写入函数因安全调用仍返回 `true`；代码也不支持 `NdefFormatable`，不检查容量、可写状态或只读状态。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/NfcManager.kt:110`、`:112`、`:116`]
- HCE 的 SELECT 不校验 AID，READ BINARY 忽略 offset/Le，UPDATE BINARY 不保存 Data 却直接返回 `9000`，不能作为 Type 4 NDEF 或 FMCOS 行为参考。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/HostCardEmulationService.kt:48`、`:58`、`:64`]
- HCE 声明互相矛盾：服务继承 ISO-DEP 使用的 `HostApduService`，但注释、XML 根节点及 system code 是 NFC-F；manifest 又把该 NFC-F XML 挂到 `host_apdu_service` metadata，不能视为可工作的 HCE 配置。[源码：`/Users/yulun/Downloads/nfc-clone-main/app/src/main/kotlin/com/example/nfcclone/nfc/HostCardEmulationService.kt:8`、`:11`；`/Users/yulun/Downloads/nfc-clone-main/app/src/main/res/xml/nfc_applet_config.xml:2`；`/Users/yulun/Downloads/nfc-clone-main/app/src/main/AndroidManifest.xml:58`、`:61`]
- 文档中的 PIN “认证”只是 HCE 端明文字符串比较，不是 FMCOS challenge-response、DES/3DES、MAC 或密钥状态管理；绝不能移植到实体卡认证。[源码：`/Users/yulun/Downloads/nfc-clone-main/EXAMPLES.md:33`、`:37`、`:41`]

该库没有向实体卡发送 `CREATE/ERASE/WRITE KEY` 的代码，因此本次只读分析本身没有发现会立即改卡的路径；危险来自把其伪成功 HCE handler 或错误 SELECT 当作制卡依据。继续保持当前保护：没有厂商 CREATE 字段表、密钥 profile 和权限定义时，不开放 FMCOS 个性化写入。

### License 与技术年代

源码采用 MIT License，可复制、修改和商用，但分发复制或 substantial portions 时必须保留版权与许可声明。[源码：`/Users/yulun/Downloads/nfc-clone-main/LICENSE:1`、`:3`、`:5`、`:12`] 项目版本为 `1.0.0`，changelog 标注 `2026-03-12`；构建栈是 compile/target SDK 34、AGP 8.2.0、Kotlin 1.9.22、Compose 1.6.4，属于约 2023–2024 年代的 Android 工具链配置，而不是 FMCOS 协议资料来源。[源码：`/Users/yulun/Downloads/nfc-clone-main/CHANGELOG.md:8`；`/Users/yulun/Downloads/nfc-clone-main/app/build.gradle.kts:8`、`:13`、`:60`；`/Users/yulun/Downloads/nfc-clone-main/build.gradle.kts:3`、`:5`] 尽管文档自称完整/生产可用，其认证和 ISO/IEC 7816-4 扩展仍列在未来计划中，应以源码而非宣传判断能力。[源码：`/Users/yulun/Downloads/nfc-clone-main/CHANGELOG.md:62`、`:66`]

## nfcspy-master 参考评估

### 结论与协议层次

当前实卡**是 ISO-DEP/APDU 卡**。Android 已将它暴露为 `IsoDep`；`nfcspy-master` 也仅通过 `TECH_DISCOVERED` 接收 `IsoDep`，连接后用 `transceive()` 原样交换字节。[源码：`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/NfcManager.java:116`、`:139`、`:164`、`:167`]

这与“卡上没有 NDEF 应用”并不矛盾：ISO-DEP 是 ISO 14443-4 传输层，APDU 是命令/响应载体，FMCOS 是其上的卡 OS 和文件/安全模型；NFC Forum Type 4 NDEF 只是 APDU 通道上的一种标准应用与文件布局，并非每张 IsoDep 卡都天然支持。实卡可以正常处理 FMCOS `SELECT MF`，但没有标准 NDEF AID 和业务 EF。

该库定位是两台手机通过 Wi-Fi P2P，把真实读卡器的 APDU 中继到真实卡并记录交互，不是 FM1208 配置 SDK。[源码：`/Users/yulun/Downloads/nfcspy-master/README.md:3`、`:11`、`:19`、`:21`；`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/ServiceAgent.java:194`、`:199`、`:208`]

### 可直接复用、仅可借鉴与无关部分

- **可直接复用：无。** 本项目已有 `IsoDep.connect/transceive`、超时和十六进制日志；引入该库不会增加 FMCOS 协议能力，而且其 GPL 许可证不适合在未确认本仓库授权方式时直接复制代码。
- **仅可借鉴：APDU 观察思路。** 若供应商授权并提供一套成功的发卡流程，可在隔离测试卡上记录命令/响应，结合正式协议逐条还原发卡 profile。其 HCE 服务将读卡器命令发给远端，远端再送真实卡，响应沿原路返回。[源码：`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/ServiceFull.java:54`、`:59`；`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/ServiceAgent.java:277`、`:278`]
- **仅可借鉴：状态字展示。** 字典包含 `6982`、`6A82`、`6A83` 等通用解释，可用于人工核对日志；项目现有状态处理已覆盖本次空 DIR 情况。[源码：`/Users/yulun/Downloads/nfcspy-master/res/xml/apdu7816.xml:231`、`:235`、`:246`、`:249`、`:250`]
- **无关：FMCOS 个性化。** 全库没有 FM1208、FMCOS、`3F00`、`80 E0` 完整 APDU、KEY EF 或 FMCOS 认证实现。XML 只是命令名称字典，解析器仅按 CLA/INS/P1/P2 查名称、按响应末两字节查状态，不定义 Lc/Data/FCP。[源码：`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/ApduParser.java:30`、`:32`、`:36`、`:217`、`:225`]
- **无关：Type 4 NDEF 实现。** NDEF AID `D2760000850101` 只出现在 HCE 路由列表；代码没有 CC EF、NDEF EF、NLEN 或 NDEF 读写流程，Reader Mode 反而明确使用 `FLAG_READER_SKIP_NDEF_CHECK`。[源码：`/Users/yulun/Downloads/nfcspy-master/res/xml/hce_aids.xml:87`；`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/NfcManager.java:101`]

APDU 字典中的 `Create File E0` 也不能补齐 FMCOS `80 E0`。该条目位于 bit7 未置位的 CLA 分组，而 CLA `80` 会进入 bit7 置位分组，后者没有 `E0`；即使能显示命令名，也仍没有 FMCOS 专有创建参数。[源码：`/Users/yulun/Downloads/nfcspy-master/res/xml/apdu7816.xml:38`、`:88`、`:96`；`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/ApduParser.java:161`]

### 风险、许可证与技术年代

- **禁止盲目中继或回放。** 代码把远端未知 APDU 不加白名单地交给卡片；序列可能包含不可逆的 CREATE、密钥写入或擦除操作，也可能依赖随机挑战、会话密钥、MAC 和计数器，录到的字节不等于可安全重放。[源码：`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/ServiceAgent.java:197`、`:199`]
- **不要集成 Xposed 路由。** 它 hook `com.android.nfc` 内部类 `HostEmulationManager.findSelectAid`，需要 root/Xposed 或修改系统，强依赖旧 Android 内部实现，与正常 App 内扫描无关。[源码：`/Users/yulun/Downloads/nfcspy-master/README.md:27`、`:29`；`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/XposedModApduRouting.java:28`、`:30`、`:35`]
- **许可证为 GPLv3-or-later。** README 和源码头均明确 GPL；直接复制或组合并分发会引入相应开源义务，仓库中的“Additional permission”没有附带可确认的宽松授权文本，因此只借鉴概念，不复制实现。[源码：`/Users/yulun/Downloads/nfcspy-master/README.md:33`、`:35`；`/Users/yulun/Downloads/nfcspy-master/LICENSE:1`、`:2`；`/Users/yulun/Downloads/nfcspy-master/src/com/sinpo/nfcspy/NfcManager.java:1`、`:14`]
- **技术基线为 2015 年。** 版本号 `1.0.150710`、target SDK 22，并使用旧式 Ant 工程和 Android 4.4 时代 HCE/Xposed 接口，不宜作为现代 Android NFC 集成模板。[源码：`/Users/yulun/Downloads/nfcspy-master/AndroidManifest.xml:5`、`:9`；`/Users/yulun/Downloads/nfcspy-master/project.properties:6`、`:15`]

因此，`nfcspy-master` 能佐证当前卡走的是 IsoDep/APDU，也能提示如何在合法实验环境中观察一套已知成功的发卡流程，但**不能回答如何配置这张空 FMCOS 卡**。配置仍必须依赖该批卡匹配的 `CREATE 80 E0` 数据结构、KEY EF/认证规范和供应商发卡 profile。

## ohyes-nfc_1.0.5_example 参考评估

### 结论

该项目对“APP 前台主动扫描”和普通 NDEF 解析有少量参考意义，但不能解决 FM1208/FMCOS 空卡的个性化或业务写入。它是 uni-app 的 Android UTS 读取插件，公开 API 只有启停读取、查询 NFC 状态、打开设置和读取回调。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/interface.uts:1`、`:7`；`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/index.uts:13`、`:44`]

全库没有 FM1208、FMCOS、`CREATE 80 E0`、DF/EF/KEY、FMCOS 外部认证、`UPDATE BINARY/RECORD`、Type 4 NDEF AID `D2760000850101`、CC EF `E103` 或 NDEF EF `E104`。也没有 AAR/JAR/SO、厂商 SDK 或隐藏的制卡 API；插件声明无第三方 SDK，依赖列表为空。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/readme.md:13`；`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/package.json:40`]

### 可借鉴、不可复用与误导点

- **仅可借鉴：前台主动扫描。** 代码通过 `enableReaderMode()` 接收 `Tag`，在页面隐藏时调用 `disableReaderMode()`；这与当前 APP 内主动扫描的生命周期思路一致。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/NFCUtil.kt:62`、`:87`、`:111`；`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/pages/index/index.vue:93`、`:97`] 但它没有 `TECH_DISCOVERED` Activity intent-filter，因此不能作为“未打开 APP 时碰卡唤起”的实现参考。
- **仅可借鉴：普通 NDEF。** 代码用 Android `Ndef` 读取已有 NDEF，用 `NdefFormatable.format()` 或 `Ndef.writeNdefMessage()` 写入普通标签，并实现 Text/URI/Smart Poster 解析。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/NFCUtil.kt:166`、`:250`、`:520`、`:549`] 当前项目已有等价且更符合业务的 NDEF 路径；而该插件的两个写入函数没有从 UTS 接口导出，示例页面也只有读取回调，不能直接用其公开 API 写卡。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/interface.uts:1`、`:14`；`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/pages/index/index.vue:104`]
- **与 FMCOS 无关：IsoDep 只读历史字节。** 非 NDEF 卡分支取得 `IsoDep` 后只返回 `historicalBytes`，完全没有调用 `IsoDep.transceive()`，不会 SELECT MF、读取 DIR 或发送任何 APDU。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/NFCUtil.kt:390`、`:409`、`:458`、`:462`] 所以本卡只会显示 `iso_dep` 和历史字节，无法识别为 FMCOS，更无法判断业务 EF、权限或可写性。
- **结果容易误导。** 非 NDEF FM1208 的 `size/isWritable/freeBytes` 保持初始值 `0/false/0`，这不证明卡片只读或容量为零，只说明插件没有查询 FMCOS 文件系统。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/NFCUtil.kt:154`、`:158`、`:161`、`:192`] README 将 ISO-DEP “内容”定义成 Historical Bytes，也明确了这种能力边界。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/readme.md:200`、`:202`、`:215`、`:218`]
- **URI 写入实现不应复制。** `createUriRecord()` 按完整 URI 长度分配 payload，随后却只复制去掉 `https://` 等前缀后的字节，导致 payload 尾部残留多余的零字节。当前项目使用 Android `NdefRecord.createUri()`，应继续保留标准实现。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/NFCUtil.kt:489`、`:490`、`:491`、`:514`、`:515`]
- **认证不可借鉴。** 唯一认证代码是 Mifare Classic 扇区 0 的公开默认 Key A，与 FMCOS 的 KEY EF、DES/3DES challenge-response、安全状态和 MAC 无关。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/NFCUtil.kt:420`、`:424`]

### 集成与许可证判断

本项目是原生 Android Kotlin/Java，而该插件依赖 DCloud UTS 类型和运行时（例如 `io.dcloud.uts.UTSJSONObject`、`UTSAndroid.getUniActivity()`），整体不能直接加入当前 Gradle 模块。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/NFCUtil.kt:38`；`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/index.uts:2`、`:14`] 技术元数据要求 HBuilderX `^4.66`、uni-app `^5.08`，Android 配置文件要求 minSdk 21；`package.json` 却另写 Android minVersion 19，版本信息存在不一致。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/package.json:12`、`:64`；`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/utssdk/app-android/config.json:2`]

目录中没有 LICENSE/COPYING 文件，`package.json` 也没有 `license` 字段且 `repository` 为空；“源码价格 0.00”不等于授予复制、修改或再分发许可。[源码：`/Users/yulun/Downloads/ohyes-nfc_1.0.5_example/uni_modules/ohyes-nfc/package.json:11`、`:17`、`:23`] 因此可以参考 Android 官方 API 的使用思路，但在作者补充明确许可证前，不应复制其源码进入本仓库。

最终判断：保留当前项目的 `TECH_DISCOVERED + IsoDep` 唤起、Reader Mode 主动扫描以及 NDEF/FMCOS 分派；该示例不会补齐 FMCOS 配置。空卡写入仍须取得供应商提供的 `CREATE 80 E0` 完整 Data 结构、KEY EF/认证规范和确定的发卡 profile，不能根据该项目推导或试发命令。

## FM1216_ps_chs.pdf 资料评估

### 结论

该 PDF 是《FM1216 系列 CPU 卡芯片产品说明书》（版本 1.4，2013-09），不是 FMCOS 命令、文件系统或发卡个人化手册。全文 13 页，没有 `CREATE 80 E0` 的 P1/P2/Lc/Data、DF/EF/KEY EF 创建参数、`WRITE KEY`、CPU 卡认证/默认密钥、安全属性、`UPDATE BINARY/RECORD`、NFC Forum Type 4 NDEF AID `D2760000850101`、CC EF `E103`、NDEF EF `E104` 或完整发卡流程。因此它不能补齐当前 FM1208 空卡写入所缺的资料，也不能据此安全实现 `writeFmcos()`。

### 文档实际能够确认的内容

- 第 3 页 1.1、1.2 节说明 FM1216 支持 ISO 14443A 和 ISO 7816、具有 16 KB EEPROM，并称其为 FM1208 非接触 CPU 卡的升级产品；这说明产品定位和底层通信能力，不等于两者采用相同 FMCOS 版本、文件 profile、默认密钥或专有 CREATE 参数。
- 第 6--7 页第 2 章说明 Type A 选卡后通过是否收到 RATS，在逻辑加密卡流程与 CPU/COS 流程之间切换；第 10 页第 4 章进一步说明 ISO 14443-4 RATS 和 CPU 卡命令由 COS 处理、硬件只负责传输。这能佐证 Android `IsoDep.transceive()` 是正确通道，但没有定义 COS 接受的 APDU。
- 第 9 页第 3 章只描述芯片物理地址空间（16 KB EEPROM、xRAM 和寄存器），不是 ISO 7816/FMCOS 的 MF/DF/EF 逻辑文件布局，不能由物理地址直接推导业务 EF。
- 第 10 页表 4-1 中的 `Authentication 60/61`、`Read 30`、`Write A0` 是 Type A 硬件支持的逻辑加密卡指令。它们不是 FMCOS CPU 模式下的 `EXTERNAL AUTHENTICATE`、`READ/UPDATE BINARY` 或 `CREATE FILE` APDU，不能用于当前已经走 RATS/ISO-DEP 的卡片个性化。

### 与 FM1208/FMCOS 的兼容性边界

文档只明确 FM1216 是 FM1208 的升级产品，并在特性中列出兼容 FM11RF08M/FM11RF08SH/FM11RF32M/FM11RF32SH；没有声明其 FMCOS 命令集、卡片文件结构或个人化脚本与当前 FM1208 完全兼容。`Triple-DES` 等算法支持也只是芯片能力，没有给出密钥编号、密钥文件结构、认证挑战响应、MAC 或重试计数规则。

所以该资料新增的有效判断只有：FM1216 和当前 FM1208 一样可以承载基于 ISO 14443-4 的 CPU 卡/COS 交互；它不能回答如何在当前空 FMCOS 卡内建立业务 DF/EF。仍需索取与当前芯片批次及 COS 版本匹配的《FMCOS 发卡/个人化手册》、`80 E0 CREATE` 数据域定义、KEY EF/认证规范及供应商发卡 profile。

## FMCOS 通用技术手册 (2) 副本核对

`/Users/yulun/Downloads/FMCOS通用技术手册-统一格式- (2).pdf` 共 122 页，封面标注《FMCOS2.0 通用技术手册》、版本 1.1、2009-07。它与已分析的 `/Users/yulun/Downloads/FMCOS通用技术手册-统一格式-.pdf` 文件大小均为 768,513 字节，SHA-256 均为 `f3228d6bba9da7c08aa7e85e2bcaae99b6cc76e11f49eecfa6a0ea5c0e91ea10`；`cmp` 确认两者逐字节相同。下载目录中的 `(1)` 副本也具有同一哈希。因此 `(2)` 不是补充版或更完整版本，不会提供新的个性化资料。

逐项复核结果与原文件一致：

- `CREATE` 仅在第 9--10 页 §1.4 命令表中列为专有命令 `80 E0`，没有 P1、P2、Lc、Data、响应状态或 MF/DF/EF/KEY EF 的创建参数；`WRITE KEY 80/84 D4` 和 `ERASE DF 80 0E` 也仅列出命令名和 INS。
- 第 40--45 页 §4--5 定义 MF/DF/EF/KEY 文件关系和安全状态/访问权限，且说明 KEY EF 必须在目录内最先建立；这些是文件模型约束，不是 CREATE Data 编码。第 49--52 页 §7.1--7.3 给出认证与取随机数流程，但没有默认密钥，出现的密钥值只是命令示例。
- 第 60--64 页 §7.7--7.9 完整定义 `UPDATE BINARY/RECORD` 和 `APPEND RECORD`，但这些命令只能操作已经存在且权限满足的 EF，不能替代建文件。全文没有发卡/个人化步骤、NFC Forum Type 4、NDEF AID `D2760000850101`、CC EF `E103` 或 NDEF EF `E104`。

所以该副本仍不能用于安全配置当前只有 MF、没有业务 DF/EF 的 FM1208。需要供应商另行提供与实卡 COS 版本匹配的 CREATE/WRITE KEY 专有命令说明、初始安全 profile 和正式发卡脚本。

## 掘金 FM1208 系列文章评估

### 文章范围与顺序

三篇均由掘金用户“自律使我快乐”（用户 ID `4081846162832781`）发布：

| 顺序 | 文章 | 日期 | 实际内容 |
| --- | --- | --- | --- |
| 1 | [《FM1208 第一篇 错误代码提示》](https://juejin.cn/post/7090434436591779871) | 2022-04-25 | APDU 状态字速查表 |
| 2 | [《FM1208 第二章 发卡流程和读卡流程》](https://juejin.cn/post/7090447743335956511) | 2022-04-25 | 某校园卡的擦除、CREATE、WRITE KEY、认证、读写示例 |
| 补充 | [《cpu 卡 分段读取数据》](https://juejin.cn/post/7091283803884224548) | 2022-04-27 | Python 生成分段 `READ BINARY` 命令 |

第一、二篇可按标题视为系列顺序；第三篇没有 FM1208 系列编号，只是同作者随后发布的读取辅助代码。三篇均未讨论 NDEF、Type 4 Tag、AID `D2760000850101`、CC EF `E103` 或 NDEF EF `E104`。

### 可确认的 FMCOS 线索

第二章首次提供了官方通用手册缺失的专有命令**实例**：先在 MF 下以 `80 E0` 建 KEY EF、用 `80 D4` 写外部认证密钥，再创建 FID `3F01`/名称 `ADF01` 的 ADF，在 ADF 下建 KEY EF 和 FID/SFI 3、容量 `0400` 的二进制 EF，最后用 `00 D6` 写入 2-byte 长度 `01E0` 及分段 JSON 数据。[第二章，“写文件(发卡流程)”代码段] 读取示例则是 SELECT `3F01`、GET CHALLENGE、EXTERNAL AUTHENTICATE 后，通过 `00 B0 83xx Le` 以短文件标识符 3 读取。[第二章，“读取文件内容(读卡)”代码段]

这些步骤在**结构顺序**上与官方手册相符：每个目录的 KEY EF 必须最先建立，外部认证成功改变目录安全状态，`READ/UPDATE BINARY` 可用 P1 高三位 `100` 加低五位 SFI 直接选择二进制 EF。[FMCOS 手册，§4.1.3、§4.3，PDF pp.41--42；§5.1--5.3，p.45；§7.1--7.2，pp.49--51；§7.5、§7.7，pp.56--61] 文章的“4-byte 随机数后补四个 `00` 再 DES”也与官方安全报文流程吻合。[FMCOS 手册，§9.2，PDF p.104]

但该实例显然是一个特定校园卡 profile：ADF 为 `ADF01`，业务 EF 为 SFI 3，负载是含姓名、证件号、学校、证书等字段的 JSON，而非当前项目的 URL/包名，也不是 NDEF。它只能作为向供应商确认 CREATE Data 布局的线索，不能证明当前 FM1208/COS 批次采用相同文件控制参数或初始密钥。

### 文章内部错误与高风险步骤

- KEY EF 的 CREATE 示例前后不一致。主流程写成 `80E0 0000 07 3F 00B0 01F0 FFFF`，后文却写成 `80E0 000007 3F00 0100FAFFFF`；紧接着的解释表仍声称空间为 `00B0`、权限为 `01F0`。[第二章，“写文件”与“创建密钥文件命令”段落] ADF 下 KEY EF 又使用 `3F01 0095FAFFFF`，文章没有定义各字节的正式字段表。不能从这些互相冲突的示例生成通用 CREATE 编码。
- 文章在选中 ADF `3F01` 后执行第二次 `80 D4`，注释却仍称“写入 3F00 目录下”密钥，目录归属描述与当前选择状态矛盾。[第二章，“写文件”代码段]
- “白卡”流程用全 `FF` 密钥进行外部认证后发送 `80 0E` 擦除 DF。[第二章，“清除卡片”代码段] 官方手册只把 `ERASE DF 80 0E` 列为专有命令，没有参数和恢复语义；也从未规定默认密钥为全 `FF`。[FMCOS 手册，§1.4，PDF pp.9--10] 当前卡虽是允许反复格式化的专用测试卡，可在备份初始状态、明确恢复方式并逐步校验状态字后开展受控实验；但试错 `FF` 仍可能消耗认证重试次数，错误的 KEY EF/安全属性也可能造成锁卡。本轮只读评估不发送认证或擦除命令。
- 状态码文章把 `6982` 固定解释成“没有校验 PIN”，范围过窄；官方定义是安全状态不满足，外部认证、PIN 或其他安全条件均可能导致它。[第一篇，“执行 apdu 指令”列表；FMCOS 手册，§6.2，PDF pp.47--48] 其 `6A82=文件未找到`、`6A83=记录未找到` 与官方手册及当前空 DIR 实测一致。
- 分段脚本在最后一段使用 `str(lastCount).zfill(2)`，把十进制字符串直接放进 APDU Le。示例 `218 / 100` 的最后余量 18 会编码成 `18`（十六进制 0x18，即 24），正确 Le 应为十六进制 `12`。代码还默认目标 EF 已被选中且单次 Le 可用，不能直接复制。[分段读取文章，“根据总字节和每次读取的大小来分段”代码段]

### 与本地抓包的交叉验证

`FM1208_scripts-main/capture_raw.txt` 是另一张已经个性化的校园卡抓包，而不是上述文章 profile：它 SELECT MF 后按 15-byte 名称 `D156000001BDF0CACBB4EFD6A7B8B6` 选择 ADF，随后 `READ RECORD`、`READ BINARY`，并在 4-byte GET CHALLENGE 后执行带安全报文的 `04 D6` 和 `00 DC` 更新。[抓包：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/capture_raw.txt:13`、`:16`、`:36`、`:40`、`:42`、`:44`、`:48`、`:50`] 抓包没有 CREATE、WRITE KEY 或 ERASE DF，也没有 `ADF01`、SFI 3 JSON 或 NDEF。这证明 SELECT/READ/UPDATE/随机数这些 FMCOS机制具有参考价值，同时也证明不同校园卡部署的 AID、EF、权限和安全报文 profile 可以完全不同。

仓库中的 `search_file.py` 和 `search_record.py` 只是穷举 FID/记录号的探测脚本，不包含建文件格式或密钥来源。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/search_file.py:1`；`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/search_record.py:1`] 不应在当前卡上运行此类盲扫，更不能把抓包或文章 APDU 原样回放。

最终结论：文章把 CREATE Data 从“完全未知”推进到“有一个未经官方定义且内部不一致的实例”，适合形成供应商问题清单、离线编码测试以及专用卡上的分阶段实验方案；仍不应直接作为生产写卡实现。开始受控实验前，至少要确认或实测当前 COS 版本的 CREATE/WRITE KEY 字段、MF 初始状态、认证重试规则、密钥注入方式和可重复恢复路径。

## Android 官方高级 NFC 文档对照

### 文档范围与直接结论

本地文件 `/Users/yulun/Downloads/高级 NFC 概览  _  Connectivity  _  Android Developers.html` 是 Android 官方“高级 NFC 概览”，正文只有 `#tag-tech`、`#tech-intent`、`#read-write` 和 `#foreground-dispatch` 四个相关锚点，页面标注最后更新时间为 2025-11-01。[本地 HTML：`:5789`、`:5905`、`:5970`、`:6075`、`:6218`] 它明确区分了 `IsoDep` 与 `Ndef`：前者提供 ISO-DEP（ISO 14443-4）属性和 I/O，后者才提供已格式化标签上的 NDEF 数据和操作。[本地 HTML：`:5863`、`:5868`] 因此当前 FM1208 同时暴露 `IsoDep + NfcA`，只证明手机可以通过 ISO-DEP 交换字节；标准 NDEF AID 返回 `6A82` 仍然表示卡内没有 Type 4 NDEF 应用，两者不矛盾。

官方文档还说明：没有 NDEF、或 NDEF 不能映射为 MIME/URI 时，可以用 `ACTION_TECH_DISCOVERED` 处理非 NDEF 数据；取得 `EXTRA_TAG` 后应枚举 `Tag.getTechList()`，再通过对应 `TagTechnology.get(tag)` 通信。[本地 HTML：`:5905`、`:5907`、`:5914`、`:5923`、`:5954`] 对非标准标签执行读写时，应用必须定义自己的上层协议。[本地 HTML `#read-write`：`:5970`、`:5972`] 这正是当前 `IsoDep.transceive()` 上实现 FMCOS SELECT/READ/状态字解析的依据，而不是把 FMCOS 强行当作 NDEF。

### Intent 调度和 tech-list 规则

当前高级页面把完整“标签调度系统”规则链接到了另一篇官方 NFC 基础文档，没有在本页重复全部优先级。[本地 HTML：`:5918`] 同机 Android SDK 保存的官方基础文档给出的顺序为：`ACTION_NDEF_DISCOVERED`（可识别 NDEF 的最高优先级）→ `ACTION_TECH_DISCOVERED`（无匹配的 NDEF handler、无 NDEF、或 NDEF 无法映射时）→ `ACTION_TAG_DISCOVERED`（最终回退）。[本地官方 SDK 文档：`/Users/yulun/Downloads/android-sdk-macosx/docs/guide/topics/connectivity/nfc/nfc.html:1323`、`:1327`、`:1333`、`:1341`] 所以：

- 当前 manifest 的 `TECH_DISCOVERED + IsoDep/NfcA` 能正确唤起这张**非 NDEF FMCOS 卡**；实际日志也已验证收到 `TECH_DISCOVERED`。
- 它不是“所有 NDEF 卡必定以 TECH intent 唤起”的保证。若一张 NDEF 卡的首个 URI/MIME record 被其他 Activity 匹配，系统会优先走 `NDEF_DISCOVERED`。若产品要求相同 Activity 也稳定承接已写入的业务 URI，应再按实际 URI 配置窄范围的 `ACTION_NDEF_DISCOVERED` data filter，而不是注册过宽的 `TAG_DISCOVERED`。
- `TAG_DISCOVERED` 是最低优先级兜底，当前需求已经有明确的 `Ndef`、`NdefFormatable` 和 `IsoDep` 范围，不需要增加这个过宽入口。

`tech-list` 的一个列表是“该列表必须是标签 tech 集合的子集”，等价于列表内 AND；多个列表独立匹配，任一命中即可，等价于列表间 OR。[本地官方 SDK 文档：`/Users/yulun/Downloads/android-sdk-macosx/docs/guide/topics/connectivity/nfc/nfc.html:1451`、`:1454`、`:1480`、`:1482`] 当前 `fm1208_tech_filter.xml` 的三个列表是正确结构：`Ndef` OR `NdefFormatable` OR (`IsoDep` AND `NfcA`)。当前实卡同时报告 `IsoDep/NfcA`，所以第三组精确命中。若未来要兼容基于 NFC-B 的 ISO-DEP 卡，应另加一个仅含 `IsoDep` 的宽组，或增加 (`IsoDep` AND `NfcB`) 组；FM1208 当前没有这个需求。

### 前台扫描、线程和 Reader Mode

官方高级页面说明前台调度会让前台 Activity 优先于其他过滤同类 intent 的应用；`enableForegroundDispatch()` 必须在主线程且 Activity 位于前台时调用，推荐在 `onResume()` 启用、`onPause()` 停用，并通过 `onNewIntent()` 接收标签。[本地 HTML `#foreground-dispatch`：`:6075`、`:6080`、`:6141`、`:6143`] 当前 `NFCIdentificationActivity` 正是在这些生命周期位置调用，且 NFC I/O 放入单线程 `cardExecutor`，符合 `transceive()` 是阻塞 I/O、不得运行在主线程的 API 要求。[当前代码：`app/src/main/java/com/vdian/android/lib/testforgradle/nfc/NFCIdentificationActivity.kt:80`、`:93`、`:112`；本地官方 API：`/Users/yulun/Downloads/android-sdk-macosx/docs/reference/android/nfc/tech/IsoDep.html:2556`]

因此当前前台调度已经满足“App 内主动扫描”：Activity 可见期间持续接卡，按钮只是选择下一张卡的 READ/WRITE 操作。若产品希望按钮开启一个有明确开始/停止状态的扫描会话，可以改用 `enableReaderMode()`，但这不是修复当前 FM1208 通信的必要条件。特别是兼容 NDEF + FMCOS 时**不能**照搬 `FLAG_READER_SKIP_NDEF_CHECK`：官方 API 明确说该 flag 会阻止平台枚举 `Ndef` tech，并使基于 NDEF 的 tag dispatch 失效。[本地官方 API：`/Users/yulun/Downloads/android-sdk-macosx/docs/reference/android/nfc/NfcAdapter.html:2941`、`:2965`、`:3529`、`:3550`] 若改 Reader Mode，应至少轮询 NFC-A，保留系统 NDEF 检查，并把 callback 交给现有后台执行与 UI 回主线程流程。

### AAR、包名与 Android 16 边界

当前高级页面正文没有 Android Application Record、`android.com:pkg`、Reader Mode 或 Android 16 行为变更章节；页面中的“Android 16”仅是网站全局导航项，不能作为 NFC 行为声明。[本地 HTML：`:6576`] 因此不能从这一个文件推导 Android 16 对 AAR 的新保证或变化。

同机保存的官方基础文档（较旧 SDK 版本）说明 AAR 是含应用包名的 NDEF external record；系统会搜索整条 NDEF message，通过包名启动应用，未安装时可转到 Google Play；同时建议 AAR 不要放第一条，因为系统使用第一条 record 推导 URI/MIME intent。[本地官方 SDK 文档：`/Users/yulun/Downloads/android-sdk-macosx/docs/guide/topics/connectivity/nfc/nfc.html:1757`、`:1759`、`:1761`、`:1803`、`:1805`] 当前代码采用“URI 第一条、`NdefRecord.createApplicationRecord(packageName)` 第二条”，符合这项历史规则，但 AAR 是 Android 专有行为，不会让 FMCOS EF 自动成为 NDEF，也不能替代 manifest 的精准 intent filter。由于当前项目 `targetSdkVersion 31`，而所给当前页面没有 Android 16/AAR 迁移说明，Android 16 真机行为仍应作为专项兼容测试，不能仅凭旧 AAR 文档承诺。

### 当前实现判断

- **正确：** 冷启动使用 manifest `TECH_DISCOVERED` 和 XML tech filter；前台使用相同 `TECH_DISCOVERED`/techLists；`singleTop + onNewIntent()` 接收重复碰卡。
- **正确：** 先根据 `Ndef`/`NdefFormatable` 分流标准 NDEF，否则用 `IsoDep` 执行 FMCOS 自定义协议；FMCOS 客户端设置超时、在后台线程阻塞通信并使用 `use` 关闭连接。
- **正确：** NDEF 写入使用标准 Android `Ndef`/`NdefFormatable` API，URI 第一条、AAR 第二条；FMCOS 不因同样走 IsoDep 就被误判为 NDEF。
- **需产品确认：** 若“App 内主动扫描”要求按钮按下前完全不接卡、操作完成后立即停止，应把当前“Activity resumed 全程前台调度”收敛为显式扫描会话；这属于交互语义，不是协议错误。
- **需兼容验证：** 为 NDEF 业务 URL 增加窄范围 `ACTION_NDEF_DISCOVERED` filter，并在 Android 16 设备上验证 URI/App Links/AAR 的实际分派；当前官方高级页面没有给出 Android 16 专项规则。

## FM1208_scripts-main 参考评估

### 结论、结构与运行环境

`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main` 对确认 FM1208 的 ISO-DEP/FMCOS 交互和观察一张**已经个性化的特定业务卡**有参考价值，但不能补齐当前空卡的发卡配置。README 只把项目定位为分析、仿真和复制 FM1208 CPU 卡，并把详细说明外链到作者博客，没有给出支持的 COS 版本、发卡 profile、命令规范或安全边界。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/README.md:1`、`:2`、`:4`]

仓库仅有 7 个文件：两个无第三方依赖的 Python 生成器、一次 Proxmark3 `hf 14a list` 抓包、一个面向 NFCGate Java `CloneMode` 的补丁、一个面向 Proxmark3 ARM 固件 C 源码的补丁、README 和 LICENSE。它没有可独立构建的 Android/Gradle、Python package、PC/SC/pyscard 或串口工程，也没有读卡器初始化代码。Python 脚本本身只生成 `script.cmd`；真正执行其中 `hf 14a apdu` 的环境是 Proxmark3 client，不能把脚本直接集成进当前 Android App。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/search_file.py:1`、`:2`、`:8`；`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/search_record.py:1`、`:3`]

仓库声明 MIT License，允许使用、修改和分发，但复制 substantial portions 时必须保留版权和许可声明，且不提供适用性担保。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/LICENSE:1`、`:3`、`:5`、`:12`、`:15`] 两个补丁仍依赖完整的 NFCGate/Proxmark3 上游工程；该目录本身没有固定上游版本、应用补丁或构建步骤，因此也不能视为可复现构建。

### APDU 与 FMCOS 信息核对

- **有 `SELECT MF 3F00`，但没有创建参数。** `search_file.py` 先生成 `00A40000023F00`，再穷举除 `3F00` 外的 1--`FFFF` 文件 ID；抓包也显示同一 SELECT MF，并返回 `6F15...840E315041592E5359532E4444463031...9000`，即本项目实卡已看到的 `1PAY.SYS.DDF01` FCI。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/search_file.py:2`、`:5`、`:8`；`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/capture_raw.txt:13`、`:14`、`:15`] 这可以交叉验证当前只读探测 APDU，不能说明空卡上有哪些业务 DF/EF。
- **有文件/记录枚举思路。** `search_record.py` 生成 `00B2<record>0400`，遍历记录号 1--255；`search_file.py` 则遍历 65534 个 FID。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/search_record.py:2`、`:3`；`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/search_file.py:5`、`:8`] 这种无 profile 的暴力探测不适合手机生产代码：耗时、噪声大、依赖当前文件选择状态，而且不能发现按 DF name/AID 才可选择的应用。
- **抓包来自已个性化的专用卡。** 它先按 15 字节 DF name/AID 执行 `00 A4 04 00 0F ...` 并获得包含专有 `9F08/9F0C/50/82` 数据的 FCI，随后从 SFI 9 读取 record 1，并执行 READ BINARY。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/capture_raw.txt:16`、`:18`、`:22`、`:36`、`:40`、`:41`] AID、文件位置和返回数据都是该业务卡 profile，不是 FM1208/FMCOS 通用默认值，不能写死到当前 App。
- **抓包包含真实写操作但没有可复用认证算法。** 两次 `00 84 00 00 04` GET CHALLENGE 后分别出现 CLA=`04` 的 `D6` UPDATE BINARY，最后还有 `00 DC` UPDATE RECORD；卡均返回 `9000`。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/capture_raw.txt:42`、`:44`、`:45`、`:46`、`:48`、`:49`、`:50`、`:52`] CLA `04` 和尾随字节显示这套业务很可能使用安全报文/MAC，但仓库没有密钥派生、MAC 计算、随机数关联或认证状态实现；截获的一次随机挑战和 MAC 不能安全重放到另一张卡。
- **没有 `CREATE 80 E0`。** 抓包第 11 行的 `E0 80` 被工具明确注释为 RATS（FSDI=8、CID=0），它是 ISO 14443-4 激活帧，不是 APDU `80 E0 CREATE`。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/capture_raw.txt:11`] 全仓库没有 `80 E0` APDU，也没有 CREATE 的 P1/P2/Lc/Data、DF/ADF/EF/KEY EF 文件控制参数。
- **没有发卡密钥资料。** 全仓库没有 `WRITE KEY 80/84 D4`、KEY EF、INTERNAL/EXTERNAL AUTHENTICATE、DES/3DES、默认密钥、密钥版本、重试次数或安全属性定义。抓包仅有随机挑战和带疑似 MAC 的写命令，无法反推出长期密钥或合法发卡流程。
- **没有 Type 4 NDEF。** 全仓库没有 NDEF AID `D2760000850101`、CC EF `E103`、NDEF EF `E104`、NLEN、NDEF record 编解码或 Android `Ndef` API；它不能使当前空 FMCOS 卡获得 NDEF 能力。

### 可复用性判断

- **可直接复用：无。** 当前 Android 工程已有更安全、可测试的 `IsoDep.connect/transceive`、SELECT MF、状态字解析和后台线程模型。仓库没有 Android TagTechnology 客户端、FMCOS 文件 profile 或 NDEF 实现。
- **仅供参考：** `00A40000023F00` 及其 FCI 可用来交叉验证 MF 探测；抓包可以作为“SELECT 应用 → READ RECORD/BINARY → GET CHALLENGE → 受保护 UPDATE”的协议层次样本；两个仿真补丁展示了如何根据 APDU 返回硬编码响应。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/nfcgate_emulate.patch:42`、`:44`；`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/proxmark3_emulate.patch:23`、`:25`]
- **不能作为真实卡克隆/发卡实现：** NFCGate 补丁只对 SELECT 特定 AID和两个 `80 50` 分支返回截获数据，其他任何命令都无条件伪造 `9000`；Proxmark3 补丁只硬编码特定 SELECT DF 响应，其他 I-block 同样返回 `9000`。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/nfcgate_emulate.patch:52`、`:56`、`:60`、`:62`；`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/proxmark3_emulate.patch:23`、`:35`、`:37`] 它们没有文件状态、offset/record 更新、访问控制、随机挑战、MAC 或交易状态机，只能骗过非常有限的固定读卡流程。
- **健壮性不足：** NFCGate 补丁在未检查 APDU 长度时访问索引 1/2/4，Proxmark3 补丁访问接收缓冲索引 3/4/6；短帧可能越界或崩溃。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/nfcgate_emulate.patch:42`、`:52`；`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/proxmark3_emulate.patch:23`] 硬编码 UID、AID、业务字段和日志也不应进入通用客户端。

### 安全与毁卡风险

- 不要运行抓包中的 `04 D6` 或 `00 DC`：它们是实际 UPDATE 命令，若安全状态或 MAC 在某种环境下被接受，会修改二进制 EF/记录 EF；重放失败也可能触发计数器、风控或应用状态变化。
- 不要把 `search_file.py`/`search_record.py` 生成的脚本直接用于生产卡。虽然它们当前只生成 SELECT/READ，没有 CREATE/ERASE/WRITE KEY，但会向卡连续发送数百到数万条 APDU，并可能读取和落盘业务/个人数据。
- 不要应用两个仿真补丁去“验证写成功”。它们对未知命令返回伪造 `9000`，会掩盖协议缺失；NFCGate 补丁还把收到的完整命令和响应写入 Android log。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/nfcgate_emulate.patch:31`、`:61`、`:65`]
- 抓包含 UID、专有 AID、业务 FCI/记录/二进制数据以及一次受保护写序列，不应提交到应用日志、测试夹具或公开仓库。即便看不到明文长期密钥，也应按敏感卡数据处理。[源码：`/Users/yulun/Downloads/NFC_1208/FM1208_scripts-main/capture_raw.txt:9`、`:16`、`:18`、`:36`、`:37`、`:44`、`:50`]

最终判断：该仓库进一步证明当前路线应是 `TECH_DISCOVERED/Reader Mode → IsoDep → 自有 FMCOS APDU`，并给出了已个性化业务卡的只读/受保护写交互样本；它仍然没有回答如何在空 FM1208 上创建业务 DF/EF。实现 `writeFmcos()` 仍必须取得与当前 COS/批次匹配的 `80 E0 CREATE` 数据结构、KEY EF/WRITE KEY 规范、认证/MAC算法、密钥管理与确定的发卡 profile，不能从抓包硬编码或重放推导。

## 一手来源

- `/Users/yulun/Downloads/FM1208_ds_chs.pdf`：《FM1208 非接触 CPU 卡芯片技术手册》，版本 0.2，复旦微电子，2008-05。
- `/Users/yulun/Downloads/FM1216_ps_chs.pdf`：《FM1216 系列 CPU 卡芯片产品说明书》，版本 1.4，复旦微电子，2013-09。
- `/Users/yulun/Downloads/FMCOS通用技术手册-统一格式-.pdf`：《FMCOS2.0 通用技术手册》，版本 1.1，2009-07。
- `/Users/yulun/Downloads/FMCOS通用技术手册-统一格式- (2).pdf`：上述 FMCOS2.0 手册的逐字节相同副本，SHA-256 `f3228d6bba9da7c08aa7e85e2bcaae99b6cc76e11f49eecfa6a0ea5c0e91ea10`。
- `docs/FM1208/prd.md`：业务 URL、Android 包名和实卡扫描信息。
