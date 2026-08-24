# NTAG 424 DNA SDM 的 e/c 密钥映射核对

## 结论

对当前 Demo 的 File 02 SDM profile：

| URL 参数 | NXP 数据名称 | 使用的 SDM 角色 | 当前 AppKey |
| --- | --- | --- | --- |
| `e` | `PICCENCData`（encrypted PICCData） | `SDMMetaReadKey` | **Key 2** |
| `c` | `SDMMAC` | `SDMFileReadKey` 派生的 `KSesSDMFileReadMAC` | **Key 1** |

因此，当前服务端验证顺序应当是：用 **Key 2 解密 `e`**，从明文取出 UID 和 SDMReadCtr；再用 **Key 1** 和 UID、SDMReadCtr 派生本次 `KSesSDMFileReadMAC`，重新计算并比较 `c`。

这不是 NTAG 424 DNA 对 Key 1/Key 2 的永久固定分工。具体 Key 编号由 File 02 的 `SDMAccessRights` 决定；若配置改变，服务端密钥映射也必须同步改变。

## NXP 一手资料依据

### 1. e 使用 SDMMetaReadKey

NXP 应用笔记 **AN12196, _NTAG 424 DNA and NTAG 424 DNA TagTamper features and hints_, Rev. 2.0, 4 March 2025**：

- 第 10 页，§3.4.2.1 “Encryption of PICCData”：前提明确为 `SDMMetaReadKey set to App.KeyX (0x0 - 0x4)`，算法为 `PICCENCData = E(KSDMMetaRead; PICCDataTag [|| UID] [|| SDMReadCtr] || Random Padding)`；同页示例 URL 将该值放在 `e=` 中。
- 第 11 页，§3.4.2.2 “Decryption of PICCData”：算法明确为 `PICCData = D(KSDMMetaRead; PICCENCData)`；Table 2 也先读取 `SDMMetaReadKey`，再解出 `PICCDataTag`、UID 和 SDMReadCtr。

NXP 数据手册 **NT4H2421Tx, _NTAG 424 DNA TT - Secure NFC T4T compliant IC with Tag Tamper feature_, Rev. 3.0, 31 January 2019**：

- 第 13 页，§8.2.3.4，Table 10 “SDMMetaRead values”：`0h..4h` 表示 `SDMMetaReadKey` 的 AppKey 编号，该 Key 用于在镜像前加密 PICCData。

所以，只要 URL 的 `e` 是 `PICCENCData`，它就应由 `SDMMetaReadKey` 解密，而不是由 `SDMFileReadKey` 解密。

### 2. c 使用 SDMFileReadKey

同一份 AN12196：

- 第 9 页，§3.3 “SDM Session Key Generation”：明确写明 `Key used: SDMFileReadKey`，并定义：
  - `KSesSDMFileReadENC = MAC(KSDMFileRead; SV1)`；
  - `KSesSDMFileReadMAC = MAC(KSDMFileRead; SV2)`。
- 第 14 页，§3.4.4.1 “SDMMAC”：前提为 File 02 settings 中的 `SDMFileReadKey`，算法为 `SDMMAC = MACt(KSesSDMFileReadMAC; DynamicFileData[SDMMACInputOffset :: SDMMACOffset - 1])`；示例 URL 将结果放在 `c=` 中。
- 第 14-15 页，§3.4.4.2.1：当 `SDMMACInputOffset == SDMMACOffset` 时，先用 `SV2 = 3CC3 0001 0080 || UID || SDMReadCtr || ZeroPadding` 和 `KSDMFileRead` 派生 `KSesSDMFileReadMAC`，再对 zero-length input 计算截断 MAC。

因此 `c` 的根密钥角色是 `SDMFileReadKey`，不是 `SDMMetaReadKey`。

### 3. NXP 官方个性化示例直接采用 Key 2 / Key 1

AN12196 第 22 页，§5 “Personalization example” 对示例 URL `...?e=...&c=...` 明确配置：

- `KSDMMetaRead = 0x02`，用于 encrypted PICCData；
- `KSDMFileRead = 0x01`，用于生成 `KSesSDMFileReadMAC` 并计算 CMAC；
- `SDMMACInputOffset == SDMMACOffset`，即空的额外 MAC 文件输入。

AN12196 第 24 页，§5.4，Table 11 又将 `SDMAccessRights` 的传输字节 `F121` 解释为：

- RFU = `F`；
- SDMCtrRet = Key `1`；
- SDMMetaRead（PICCENCData）= Key `2`；
- SDMFileRead（CMAC）= Key `1`。

这与当前 Demo 使用的 profile 完全相同。

## 当前仓库配置解析

当前实现位于 `Ntag424DnaProtocol.buildSdmFileSettings()`，写入 File 02 的关键字节为：

```text
SDMOptions       C1
SDMAccessRights  F1 21
```

`F1 21` 是 APDU 中的两个实际传输字节，也是 AN12196 个性化示例显示的 `F121`。按 NXP 对该传输值的解释：

```text
F  -> RFU
1  -> SDMCtrRet = Key 1
2  -> SDMMetaRead = Key 2
1  -> SDMFileRead = Key 1
```

数据手册第 70-71 页，Table 69 从逻辑 bit field 描述 `SDMAccessRights`：bit 15..12 为 SDMMetaRead、bit 11..8 为 SDMFileRead、bit 7..4 为 RFU、bit 3..0 为 SDMCtrRet。对应本 profile 的逻辑 16-bit 值是 `0x21F1`；该两字节字段按卡片命令的线序发送后是 `F1 21`。不要仅按打印字符串从左到右把 `F121` 的四个 nibble 错当成逻辑 bit 15 到 bit 0。

当前 Mock 验证实现也与该配置一致：

```text
e --AES-128/CBC, zero IV, Key 2--> PICCDataTag || UID || SDMReadCtr || padding

SV2 = 3CC300010080 || UID || SDMReadCtr_LE
KSesSDMFileReadMAC = AES-CMAC(Key 1, SV2)
c = truncated AES-CMAC(KSesSDMFileReadMAC, SDMMAC input)
```

当前 profile 的 `SDMMACInputOffset == SDMMACOffset`，因此 SDMMAC 的额外文件输入为空；这不会改变 `c` 使用 Key 1 派生 session MAC key 的事实。

## 容易混淆的另一类密文

AN12196 第 12-13 页，§3.4.3 还定义了 `SDMENCFileData`：它是 File 02 中业务文件片段的动态加密镜像，使用由 `SDMFileReadKey` 派生的 `KSesSDMFileReadENC`。它与 `e` 中的 encrypted PICCData 不是同一字段：

- `PICCENCData`：包含 PICCDataTag、UID、SDMReadCtr 等元数据，使用 `SDMMetaReadKey`；当前 URL 参数名为 `e`。
- `SDMENCFileData`：可选的文件内容密文，使用 `SDMFileReadKey` 派生的 session ENC key；当前 Demo 没有启用。

如果把 `SDMENCFileData` 的规则误套到 `PICCENCData`，就会错误地认为 `e` 应使用 Key 1。

## 最终验证流程

当前 Demo 服务端应执行：

1. 从 URL 唯一读取 `e` 和 `c`。
2. 使用 SDMMetaRead **Key 2** 解密 `e`。
3. 校验 PICCDataTag，并取出 UID 和 little-endian SDMReadCtr。
4. 使用 SDMFileRead **Key 1** 与 UID、SDMReadCtr 构造 SV2，派生 `KSesSDMFileReadMAC`。
5. 根据 File 02 的 offsets 重算 truncated SDMMAC。
6. 常量时间比较计算结果与 `c`。
7. MAC 通过后原子检查并推进该 UID 的 SDMReadCtr，拒绝重复或更旧的计数器。

## 资料文件

- `/Users/yulun/Downloads/ntag-424/AN12196.pdf`
- `/Users/yulun/Downloads/ntag-424/Ntag424SdmFeature-master/docs/NTAG 424 DNA datasheet NT4H2421Tx.pdf`
