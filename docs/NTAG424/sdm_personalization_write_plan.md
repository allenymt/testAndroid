# NTAG 424 DNA SDM 写卡技术方案

## 1. 范围

本文只描述 File 02 NDEF 模板写入、SDM 配置、关闭和失败恢复，不讨论 App 内二次双向认证，也不讨论敏感业务读取。

目标是让 File 02 保存稳定的 NDEF 模板，由芯片在读取响应中动态返回 `e` 和 `c`。

## 2. 文件与密钥

| 对象 | 作用 |
| --- | --- |
| File 02 (`E104`) | 保存 URI、业务 query 和 `android.com:pkg` record |
| Key 0 | 当前 Change 权限对应的管理密钥 |
| SDMMetaReadKey | 生成 `e/PICCENCData` |
| SDMFileReadKey | 生成 `c/SDMMAC` |
| SDMCtrRet | 可选的受保护计数器读取权限 |

当前 Demo profile 使用 `Change=Key 0`、`e=Key 2`、`c=Key 1`。实际编号必须以卡片当前 `SDMAccessRights` 为准。

## 3. 写卡前预检

写卡程序先读取并检查：

1. 卡片是支持 ISO-DEP 的 NTAG 424 DNA；
2. File 02 存在且容量足够；
3. File 02 当前 SDM 状态符合本次操作预期；
4. Write/RW 权限允许写入；
5. Change 权限对应密钥可认证；
6. URI 不含重复的顶层 `e`/`c`，或已经是合法等长占位符模板；
7. URI record、pkg record 和业务 query 满足业务要求。

## 4. 构造静态 NDEF 模板

最终 NDEF 通常包含：

```text
Record 1: URI
Record 2: android.com:pkg
```

在 URI 中保留原有业务 query，并追加固定长度占位符：

```text
e=00000000000000000000000000000000   // 32 个 ASCII 字符
c=0000000000000000                   // 16 个 ASCII 字符
```

例如：

```text
https://wdb-applink.weidian.com?targetUrl=...
&e=00000000000000000000000000000000&c=0000000000000000
```

占位符长度必须与芯片动态输出长度一致。芯片不会解析 URL，也不会在每次读取时把动态值写回 EEPROM。

## 5. 计算 offsets

先将完整 NDEF 编码成 bytes，再按 File 02 的完整布局计算：

```text
File 02 offset 0..1 = NLEN（2-byte big-endian）
File 02 offset 2...  = NDEF Message
```

所有 offset 都相对于 File 02 byte 0：

```text
PICCDataOffset = e= 后第一个占位字符的 byte offset
SDMMACOffset   = c= 后第一个占位字符的 byte offset
```

不能使用 URL 字符串下标、URL 解码后的字符位置或去掉 NLEN 后的相对位置。每个占位符必须恰好出现一次，且 `NLEN + NDEF` 不得超过 File 02 容量。

## 6. Key 0/1/2 写入与轮换

SDM 写卡前通常需要把 Key 0、Key 1、Key 2 从旧值更新为目标值：

| Key | 写入后的用途 | 依赖它的后续步骤 |
| --- | --- | --- |
| Key 0 | File 02 Change 权限对应的管理密钥 | ChangeFileSettings、后续管理操作 |
| Key 1 | SDMFileReadKey | 芯片生成 `c`，服务端验证 `c` |
| Key 2 | SDMMetaReadKey | 芯片生成 `e`，服务端解密 `e` |

推荐物理写入顺序为：

```text
Key 1 -> Key 2 -> Key 0
```

原因是修改 Key 1/2 时仍需使用旧 Key 0 管理会话；Key 0 必须最后修改。每次 `ChangeKey` 都应：

1. 使用当前有效的管理 Key 0 完成 `AuthenticateEV2First`；
2. 在 Full Secure Messaging 中发送 `ChangeKey`，写入新 Key value 和 KeyVersion；
3. 重新选择 Application，读取并校验目标 KeyVersion；
4. 丢弃旧会话，并使用新 Key 重新认证验证；
5. 记录已完成的 Key，支持中断后重试。

Key value 是 16-byte AES 密钥，KeyVersion 是独立的 1-byte 版本号。不能只根据版本号断言密钥值正确；发行记录必须同时保存 old/new key 或其受控引用。若历史卡片存在旧 version 1，应保留旧 value 用于认证迁移，再写入新的目标 version。

修改 Key 0 后必须立即丢弃旧 TI、CmdCtr 和会话密钥，重新使用新 Key 0 建立管理会话。Key 1/2 修改成功不会自动改变 File 02 权限或 SDMAccessRights 的 Key 编号。

## 7. 开启 SDM 的写入顺序

```text
1. Select NDEF Application
2. GetFileSettings(File 02)
3. 读取并解析当前 NDEF
4. 生成带 e/c 占位符的完整 NDEF
5. 计算 PICCDataOffset、SDMMACInputOffset、SDMMACOffset
6. 按当前 Write/RW 权限完成认证（如需要）
7. 将 NLEN 写为 0000
8. 从 File 02 offset 2 分块写入 NDEF Message
9. 将最终 NLEN 写回 offset 0
10. 使用当前 Change Key（如已执行 Key 0 轮换，则使用新 Key 0）建立受保护会话
11. ChangeFileSettings 开启 SDM，写入 SDM options、rights 和 offsets
12. 重新读取 FileSettings 与 NDEF，确认配置生效
```

写入通信模式必须根据当前 FileSettings 选择 Plain、MAC 或 Full。完成 EV2 认证不代表后续命令自动变成 Full。

## 8. ChangeFileSettings 内容

开启 SDM 时需要配置：

```text
FileOption        = 原通信模式 + SDM enable bit
AccessRights      = 保留原 Read/Write/ReadWrite/Change rights
SDMOptions        = UID、Counter、ENCFileData 等镜像选项
SDMAccessRights   = MetaReadKey、FileReadKey、CtrRetKey
PICCDataOffset    = 计算所得 offset
SDMMACInputOffset = 按 profile 计算
SDMMACOffset      = 计算所得 offset
```

当前只启用 PICCENCData、且 SDMMAC input 为空时，`e` 通常包含：

```text
PICCDataTag + UID + SDMReadCtr + RandomPadding
```

若启用 `SDMENCFileData`，MAC 输入不再为空，必须按 AN12196 对应配置重新构造，不能继续使用零长度 MAC 的实现。

开启 SDM 不应顺带修改 File 02 写保护或其他访问权限。

## 9. 关闭 SDM

关闭是独立操作：

```text
1. 使用当前 Change Key 认证
2. 清除 FileOption 的 SDM 位和 SDM 配置段
3. 从 URI 顶层 query 移除本流程添加的 e/c
4. 保留业务 query、fragment、pkg record 和其他权限
5. 若生成了清理后的 NDEF，按当前 Write/RW 权限写回 File 02
```

关闭 FileSettings 不会自动删除 NDEF 中的 `e/c`；格式化或清空 File 02 也不会自动关闭 SDM settings。

## 10. 写后验证

重新贴卡读取并检查：

- `sdmEnabled=true`；
- URI、pkg 和业务 query 仍存在；
- 动态 `e` 为 32 个 ASCII hex 字符；
- 动态 `c` 为 16 个 ASCII hex 字符；
- 连续读取时 `SDMReadCtr` 递增；
- 正确 Key 2 能解密 `e`，正确 Key 1 能验证 `c`；
- 旧 URL 按服务端计数器策略被拒绝重放。

开启 Random UID 后，应分别记录 RF UID 和 `e` 解密出的 UID；当前实卡已验证 `e` 中仍可取得真实 UID。

## 11. 中断与重试

一次 NFC 连接不是卡片事务。中途移卡后，已成功的 NDEF 写入或 ChangeFileSettings 不会回滚。

实现应保存阶段结果并允许按当前状态重试：

```text
读取 KeyVersion     -> 确定每把 Key 的当前值/版本
读取 FileSettings   -> 检查 SDM 状态
读取 NLEN/NDEF      -> 检查占位符是否完整
Key 已是目标版本    -> 使用目标 Key 认证并跳过 ChangeKey
SDM 已开启          -> 禁止重复开启
SDM 已关闭但仍有占位符 -> 仅重试清理 URL
```

如果 Key 0/1/2 的某一步已经成功而后续步骤中断，不能简单从第一步盲目重放；应先读取版本和权限，按当前状态继续。一次 NFC 连接不是卡片事务，已成功的换钥不会回滚。

## 12. 核心结论

```text
先把完整、等长、带 e/c 占位符的 NDEF 写入 File 02，
再通过 ChangeFileSettings 配置 SDM 和 offsets。

之后 e/c 由芯片在读取响应中动态生成，不会写回 File 02，
也不会改变 NDEF 的静态长度。
```
