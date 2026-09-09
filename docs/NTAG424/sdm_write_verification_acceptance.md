# NTAG 424 DNA SDM/SUN 写入与验证验收标准

## 1. 结论

通过烧录密钥、读取动态 SDM/SUN URL，并从中取得 UID、Counter 和 MAC，说明主链路已经可以运行，但仅“取得这些字段”还不足以证明完整流程成功。

只有同时完成以下闭环，才可以认定覆盖了 SDM 写入与校验全过程：

```text
写入并验证 Key 0/1/2
    ↓
写入带 e/c 占位符的 File 02 NDEF
    ↓
计算并配置 SDM offsets、options 和 access rights
    ↓
开启 SDM
    ↓
通过普通 NDEF 读取获得芯片动态生成的 e/c
    ↓
使用 Key 2 解密 e，取得 UID 和 SDMReadCtr
    ↓
使用 Key 1 重新计算并验证 c
    ↓
校验 UID 与发行记录的绑定关系
    ↓
原子检查并更新 Counter，拒绝重放
```

## 2. “读取成功”与“验证成功”的区别

### 2.1 只读取或解析成功

以下结果只能证明 NDEF 和动态字段可以读取：

- URL 中存在符合长度要求的 `e` 和 `c`；
- `e`、`c` 是合法十六进制字符串；
- 程序可以把字段解析为 bytes；
- 程序可以展示 UID、Counter 或 MAC 字段。

如果 UID 和 Counter 不是用正确密钥从 `e` 解密得到，或者没有重新计算并比较 `c`，则不能证明消息由目标卡片生成。

### 2.2 密码学验证成功

至少满足以下条件，才能认定本次 SDM/SUN 消息有效：

1. 使用当前 profile 指定的 `SDMMetaReadKey` 成功解密 `e/PICCENCData`。
2. 解密结果中的 `PICCDataTag`、UID、`SDMReadCtr` 和 padding 格式合法。
3. UID 与业务侧发行记录中的 UID 一致。
4. 按 profile 规则派生本次 MAC Key。
5. 使用正确的 MAC 输入重新计算 `c/SDMMAC`。
6. 使用常量时间比较确认计算结果与 URL 中的 `c` 一致。
7. 原子检查 `SDMReadCtr`，拒绝已经消费过的旧 Counter。

当前约定的 profile 中：

| Key | 用途 |
| --- | --- |
| Key 0 | 管理 Key，用于换钥和修改 FileSettings，不直接参与日常 SUN 校验 |
| Key 1 | `SDMFileReadKey`，用于派生本次 MAC Key并验证 `c` |
| Key 2 | `SDMMetaReadKey`，用于解密 `e/PICCENCData` |

Key 编号不是 NTAG 424 DNA 永久固定的协议规定，最终必须以实际写入的 `SDMAccessRights` 为准。

## 3. SDM 写入阶段验收

### 3.1 密钥写入

- Key 0/1/2 已写入目标值和目标 KeyVersion。
- 每次 `ChangeKey` 后均已读取并确认 KeyVersion。
- 不能只根据 KeyVersion 判断密钥值正确，还应使用新密钥完成一次认证验证。
- 旧密钥不应继续认证成功，除非设计中明确保留了兼容或轮换窗口。
- 生产密钥不得出现在普通日志、明文表格、代码仓库或工厂主机临时文件中。

### 3.2 File 02 NDEF 写入

- File 02 中是完整、可解析的 NDEF Message。
- URI 中包含固定长度的 `e`、`c` 占位符。
- 当前配置下，`e` 占位符为 32 个 ASCII hex 字符。
- 当前配置下，`c` 占位符为 16 个 ASCII hex 字符。
- 原有业务参数和 `android.com:pkg` record 未被破坏。
- `NLEN + NDEF Message` 未超过 File 02 容量。

### 3.3 offsets 与 FileSettings

- `PICCDataOffset` 准确指向 `e` 占位符的第一个字节。
- `SDMMACOffset` 准确指向 `c` 占位符的第一个字节。
- `SDMMACInputOffset` 与当前 MAC 输入规则一致。
- offsets 相对于 File 02 byte 0 计算，并包含 2-byte NLEN。
- `sdmEnabled=true`。
- `SDMOptions` 与是否镜像 UID、Counter、PICCENCData 等实际需求一致。
- `SDMAccessRights` 与服务端持有的 Key 1/2 及 profile 记录一致。
- 开启 SDM 时没有意外修改 File 02 的写保护或其他访问权限。

## 4. 动态读取阶段验收

- Android、iPhone 或普通 NFC 读卡器无需预先认证即可读取 File 02 NDEF。
- 读取结果中的 `e/c` 已由芯片动态替换，不再是全零占位符。
- 连续读取时，动态 URL 可以正常解析。
- `e/c` 的长度保持不变，静态 NDEF 长度没有因动态替换而变化。
- 开启 Random UID 后，应区分本次 RF UID 和 `e` 解密出的真实 UID；发行记录匹配应使用约定的真实 UID。

## 5. 服务端校验阶段验收

服务端应按以下顺序执行：

1. 解析 URL 顶层 query 中的 `e` 和 `c`。
2. 根据 URL 中的业务标识或其他可信映射找到对应发行 profile。
3. 按 `SDMAccessRights` 取得 Key 2 或实际配置的 `SDMMetaReadKey`。
4. 解密 `e`，解析 UID 和 `SDMReadCtr`。
5. 根据 UID 找到对应卡片发行记录和 Key 1。
6. 使用 Key 1、UID、`SDMReadCtr` 和 profile 参数派生本次 MAC Key。
7. 按当前配置构造 MAC 输入并重新计算 `c`。
8. 使用常量时间比较验证 `c`。
9. 在同一事务中检查并更新该卡片、该 generation 的最大 Counter。
10. 只有以上步骤全部成功后才受理后续业务。

如果当前 SDM 配置只使用 `PICCENCData`，没有启用 `SDMENCFileData`，则 `e` 表示 PICCENCData；`SDMENCFileData` 不是 `e` 内部的一部分。若以后开启加密文件数据，MAC 输入和 offsets 必须按照对应 profile 重新设计。

## 6. 必须执行的反向测试

成功路径只能证明正确配置可以工作。为了证明验证没有被绕过，还必须执行：

| 测试 | 预期结果 |
| --- | --- |
| 使用错误 Key 2 解密 `e` | 解密或结构检查失败 |
| 使用错误 Key 1 验证 `c` | MAC 验证失败 |
| 修改 `e` 任意一个字符 | 验证失败 |
| 修改 `c` 任意一个字符 | 验证失败 |
| 修改 UID 或将 URL 绑定到另一张卡 | 发行记录匹配或 MAC 验证失败 |
| 重复提交同一个已成功 URL | Counter 防重放拒绝 |
| 并发提交同一个 URL | 最多只有一个请求成功 |
| 使用旧 profile 或旧 key version | 验证失败，或仅按明确的轮换策略处理 |
| offsets 配置错误 | 动态字段异常，写后检查失败 |

反向测试通过后，才能确认服务端并非只检查字段存在、长度正确或 Counter 可解析。

## 7. Counter 验收边界

`SDMReadCtr` 是卡片动态读取计数器。验收时应注意：

- 不应依赖第一次业务读取一定得到固定值 0 或 1，因为写卡验证、系统预读或读卡器重试都可能提前触发读取。
- 应验证后续有效读取的 Counter 按协议和 profile 预期变化。
- 防重放状态必须使用数据库事务、条件更新或等价原子操作维护。
- 重新配置 SDM 可能重置 Counter，因此服务端防重放状态应绑定个性化 generation/profile version，不能看到较小 Counter 就自动清空历史状态。

## 8. 最终验收结论

可以按以下三级结果判断完成度：

| 等级 | 已完成内容 | 结论 |
| --- | --- | --- |
| Level 1 | URL 中可以读取并解析 `e/c` | 只证明动态 NDEF 读取可用 |
| Level 2 | Key 2 解密 `e`、Key 1 重算 `c`，UID 和 MAC 均验证通过 | SDM/SUN 密码学主链路成功 |
| Level 3 | 密钥写入、新密钥认证、NDEF、offsets、FileSettings、动态读取、服务端验签、防重放及反向测试全部通过 | 覆盖 SDM 写入与校验全过程 |

因此，“烧录密钥后执行 SUN 校验，并成功取得 UID、Counter、MAC”是否代表完成，取决于 MAC 是否经过重新计算和严格比较、UID 是否与发行记录绑定、Counter 是否执行原子防重放，以及卡片侧密钥、NDEF、offsets 和 FileSettings 是否均已完成写后验证。全部满足时，可以认定该场景已经完整成功。
