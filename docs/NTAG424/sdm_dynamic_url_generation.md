# NTAG 424 DNA SDM 动态 URL 生成机制

## 1. 核心结论

开启 SDM 后，NTAG 424 DNA 不是在每次扫码时修改 File 02，也不会真正把新的 `e`、`c` 写回 EEPROM。

实际机制是：

1. 个性化程序先把包含 `e/c` 固定长度占位符的完整 NDEF URL 模板写入 File 02；
2. 个性化程序通过 `ChangeFileSettings` 告诉芯片两个占位区域在 File 02 中的 byte offset；
3. 普通手机读取 File 02 时，芯片根据 UID、SDMReadCtr 和 SDM keys 实时计算 `e/c`；
4. 芯片只在本次读取响应中，用动态 ASCII hex 字符覆盖占位区域；
5. 下一次重新读取时，SDMReadCtr 改变，因此通常会得到新的 `e/c`。

因此，“URL 参数拼接”和“参数动态替换”是两个不同阶段：

| 动作 | 执行者 | 发生时间 |
| --- | --- | --- |
| 保留原业务 query | App 个性化代码 | 开启 SDM 前 |
| 追加 `e=` 和 `c=` | App 个性化代码 | 开启 SDM 前 |
| 写入固定长度占位符 | App 个性化代码 | 开启 SDM 前 |
| 配置占位符 byte offset | App + 芯片 `ChangeFileSettings` | 开启 SDM 时 |
| 计算 `e/c` | NTAG 424 DNA 芯片 | 每次 SDM 读取时 |
| 在读取响应中替换占位符 | NTAG 424 DNA 芯片 | 每次 SDM 读取时 |

芯片不理解 `targetUrl`、`e`、`c` 等业务参数名。它只知道：在哪个 File 02 byte offset 输出 encrypted PICCData，在哪个 byte offset 输出 SDMMAC。

## 2. 开启 SDM 前的业务 NDEF

当前 Demo 要求 File 02 中存在两条 NDEF records：

```text
Record 1：URI record
Record 2：android.com:pkg record
```

例如原始 URI record 为：

```text
https://wdb-applink.weidian.com?targetUrl=https%3A%2F%2Fvmspub.weidian.com%2F...
```

第二条 record 为：

```text
android.com:pkg = com.koudai.weidian.buyer
```

开启 SDM 不应丢弃第一条 URL 已有的 `targetUrl` query，也不应改变第二条 package record。

## 3. App 构造带占位符的 URL 模板

### 3.1 追加 SDM 参数

当前 Demo 先生成两个固定长度占位符：

```text
PICCData 占位符：32 个 ASCII 字符 0
SDMMAC 占位符：16 个 ASCII 字符 0
```

然后保留原 URL 并追加：

```text
&e=00000000000000000000000000000000
&c=0000000000000000
```

如果原 URL 没有 query，则使用 `?e=`；如果已经有 query，则使用 `&e=`。

```text
原 URL 没有 query：
https://example.com/path
  -> https://example.com/path?e=<32 chars>&c=<16 chars>

原 URL 已有 query：
https://example.com/path?targetUrl=xxx
  -> https://example.com/path?targetUrl=xxx&e=<32 chars>&c=<16 chars>
```

当前 Demo 对应实现位于：

- `Ntag424DnaProtocol.buildSdmUrl()`：保留原 query，追加 `e/c`；
- `Ntag424DnaPersonalizationClient.buildTemplate()`：生成占位符并构造 NDEF message。

### 3.2 Java 等价代码

下面是当前模板拼接逻辑的 Java 表达。它只负责生成静态 URL，不负责计算真正的 `e/c`。

```java
import java.net.URI;

public final class SdmUrlTemplate {
    private static final String PICC_DATA_PLACEHOLDER = repeat('0', 32);
    private static final String SDM_MAC_PLACEHOLDER = repeat('0', 16);

    public static String build(String originalUrl) {
        URI uri = URI.create(originalUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("必须是有效的 HTTPS URL");
        }

        int fragmentStart = originalUrl.indexOf('#');
        if (fragmentStart < 0) {
            fragmentStart = originalUrl.length();
        }
        String base = originalUrl.substring(0, fragmentStart);
        String fragment = originalUrl.substring(fragmentStart);

        rejectExistingSdmParameters(base);
        String separator = base.contains("?") ? "&" : "?";
        return base
                + separator + "e=" + PICC_DATA_PLACEHOLDER
                + "&c=" + SDM_MAC_PLACEHOLDER
                + fragment;
    }

    private static void rejectExistingSdmParameters(String urlWithoutFragment) {
        int queryStart = urlWithoutFragment.indexOf('?');
        if (queryStart < 0) {
            return;
        }
        String query = urlWithoutFragment.substring(queryStart + 1);
        for (String parameter : query.split("&")) {
            String name = parameter.split("=", 2)[0];
            if ("e".equalsIgnoreCase(name) || "c".equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("原 URL 已包含 e 或 c");
            }
        }
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            result.append(value);
        }
        return result.toString();
    }
}
```

## 4. 计算动态字段在 File 02 中的位置

### 4.1 Offset 是 File 02 byte offset

模板 URL 需要先编码成完整 NDEF message，然后在前面加上 2-byte NLEN：

```text
File 02 bytes
  = NLEN(2 bytes)
  + NDEF record headers
  + URI prefix code
  + URI payload
  + android.com:pkg record
```

`PICCDataOffset`、`SDMMACInputOffset`、`SDMMACOffset` 都是相对于 File 02 起始位置的 byte offset。

它们不是：

- Java/Kotlin URL 字符串下标；
- URI payload 内部下标；
- NDEF message 去掉 NLEN 后的下标；
- URL 解码后的字符位置。

当前 Demo 会在完整 `NLEN + NDEF message` 中搜索：

```text
e=00000000000000000000000000000000
c=0000000000000000
```

然后跳过 `e=` 或 `c=` 两个 ASCII bytes：

```text
PICCDataOffset = indexOf("e=" + 32-byte placeholder) + 2
SDMMACOffset   = indexOf("c=" + 16-byte placeholder) + 2
```

### 4.2 Java 等价代码

```java
import java.nio.charset.StandardCharsets;

public final class SdmOffsets {
    public final int piccDataOffset;
    public final int sdmMacOffset;

    private SdmOffsets(int piccDataOffset, int sdmMacOffset) {
        this.piccDataOffset = piccDataOffset;
        this.sdmMacOffset = sdmMacOffset;
    }

    public static SdmOffsets find(byte[] file02Bytes) {
        byte[] piccMarker = (
                "e=00000000000000000000000000000000")
                .getBytes(StandardCharsets.US_ASCII);
        byte[] macMarker = (
                "c=0000000000000000")
                .getBytes(StandardCharsets.US_ASCII);

        int piccMarkerOffset = uniqueIndexOf(file02Bytes, piccMarker);
        int macMarkerOffset = uniqueIndexOf(file02Bytes, macMarker);
        return new SdmOffsets(
                piccMarkerOffset + 2, // 跳过 e=
                macMarkerOffset + 2); // 跳过 c=
    }

    private static int uniqueIndexOf(byte[] source, byte[] target) {
        int found = -1;
        for (int offset = 0; offset <= source.length - target.length; offset++) {
            boolean matches = true;
            for (int index = 0; index < target.length; index++) {
                if (source[offset + index] != target[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                if (found >= 0) {
                    throw new IllegalArgumentException("SDM 占位符不唯一");
                }
                found = offset;
            }
        }
        if (found < 0) {
            throw new IllegalArgumentException("未找到 SDM 占位符");
        }
        return found;
    }
}
```

生产实现还必须确认模板总长度不超过 File 02 的 256-byte 容量。

## 5. 通过 ChangeFileSettings 开启 SDM

当前 Demo 使用 Key 0 完成 EV2 认证后，执行受保护的 `ChangeFileSettings`，向 File 02 写入以下配置：

```text
FileOption.SDMEnabled = 1
SDMOptions            = C1
SDMAccessRights       = F1 21
PICCDataOffset        = e 值的起始 byte offset
SDMMACInputOffset     = c 值的起始 byte offset
SDMMACOffset          = c 值的起始 byte offset
```

当前 `SDMOptions=C1h` 表示启用 UID 和 SDMReadCtr mirroring，并以 ASCII 编码输出动态字段。

当前 `SDMAccessRights=F1 21` 对应：

```text
SDMMetaReadKey = AppKey 2   // 生成/解密 e
SDMFileReadKey = AppKey 1   // 生成/验证 c
SDMCtrRetKey   = AppKey 1   // 认证后读取 counter
```

当前 Demo 将：

```text
SDMMACInputOffset == SDMMACOffset
```

因此 SDMMAC 的额外文件输入区间长度为 0。`c` 仍然绑定 UID 和 SDMReadCtr，因为用于计算 `c` 的 session MAC key 已经由二者派生。

配置编码的 Java 等价代码如下：

```java
import java.io.ByteArrayOutputStream;

public final class SdmFileSettingsEncoder {
    public static byte[] build(
            int currentFileOption,
            int accessRights,
            int piccDataOffset,
            int macOffset) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(currentFileOption | 0x40); // SDMEnabled
        output.write(accessRights & 0xFF);
        output.write((accessRights >>> 8) & 0xFF);
        output.write(0xC1);                    // SDMOptions
        output.write(0xF1);                    // SDMAccessRights byte 0
        output.write(0x21);                    // SDMAccessRights byte 1
        writeLittleEndian24(output, piccDataOffset);
        writeLittleEndian24(output, macOffset); // SDMMACInputOffset
        writeLittleEndian24(output, macOffset); // SDMMACOffset
        return output.toByteArray();
    }

    private static void writeLittleEndian24(
            ByteArrayOutputStream output,
            int value) {
        if (value < 0 || value > 0xFFFFFF) {
            throw new IllegalArgumentException("offset 超出 24-bit 范围");
        }
        output.write(value & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write((value >>> 16) & 0xFF);
    }
}
```

## 6. 读卡时芯片如何生成 `e`

手机通过 ISO `READ BINARY` 或 NDEF 未认证读取 File 02 时，不需要持有 Key 1 或 Key 2。芯片根据 File 02 的 SDM settings 生成本次动态响应。

SDM mirroring 只应用于未认证读取。认证后读取 File 02 时，芯片返回 EEPROM 中的静态文件内容，也就是包含全零占位符的 underlying NDEF mapping。这可以用于诊断模板，但不能作为面向用户的动态 SDM URL。

当前 AES profile 中，芯片先组织 16-byte PICCData：

```text
PICCDataTag || UID(7) || SDMReadCtr_LE(3) || RandomPadding(5)
```

当前正常 `PICCDataTag` 为 `C7h`。然后使用 `SDMMetaReadKey`，即当前 AppKey 2，生成 encrypted PICCData：

```text
PICCENCData = AES-128-CBC-Encrypt(
    key       = Key 2,
    iv        = 00000000000000000000000000000000,
    plaintext = PICCData,
    padding   = NoPadding
)
```

16-byte 密文被编码为 32 个 ASCII hex 字符，放入 `PICCDataOffset` 对应的读取响应区域，也就是 URL 的 `e` 值。

```text
静态模板：e=00000000000000000000000000000000
读取响应：e=F9059AC5EB4B02AC9F6E3CB6DE0712C6
```

## 7. 读卡时芯片如何生成 `c`

芯片使用 UID、SDMReadCtr 和 `SDMFileReadKey`，即当前 AppKey 1，派生本次 session MAC key：

```text
SV2 = 3CC300010080 || UID || SDMReadCtr_LE

KSesSDMFileReadMAC = AES-CMAC(Key 1, SV2)
```

然后按照 File 02 offsets 取得额外 MAC 输入：

```text
macInput = DynamicFileData[
    SDMMACInputOffset,
    SDMMACOffset
)
```

当前两个 offset 相等，所以 `macInput` 为空，但芯片仍然执行标准 AES-CMAC 空消息计算：

```text
fullMac = AES-CMAC(KSesSDMFileReadMAC, empty byte array)

c = fullMac[1] || fullMac[3] || fullMac[5] || fullMac[7]
  || fullMac[9] || fullMac[11] || fullMac[13] || fullMac[15]
```

8-byte `c` 被编码为 16 个 ASCII hex 字符，并放入 `SDMMACOffset` 对应的读取响应区域：

```text
静态模板：c=0000000000000000
读取响应：c=0B2902187FF92A61
```

完整的 Java 服务端验证代码见 [sdm_verification_java.md](sdm_verification_java.md)。

## 8. 静态存储与动态读取响应的区别

可以把芯片行为理解为一层“未认证读取时视图”：

```text
File 02 EEPROM 中的静态模板
┌───────────────────────────────────────────────────────────┐
│ https://...?targetUrl=xxx&e=000...000&c=000...000          │
└───────────────────────────────────────────────────────────┘
                          │
                          │ 未认证读取 + SDMEnabled
                          v
芯片本次生成动态读取响应
┌───────────────────────────────────────────────────────────┐
│ https://...?targetUrl=xxx&e=<本次 PICCENCData>&c=<本次 MAC>│
└───────────────────────────────────────────────────────────┘
```

下一次重新贴卡读取时：

```text
SDMReadCtr 改变
    -> PICCData 改变
    -> e 改变
    -> session MAC key 改变
    -> c 改变
```

这种动态替换不会改变 NDEF message 的总长度，因为占位区域和动态输出采用完全相同的固定长度：

```text
e：32 ASCII chars -> 32 ASCII chars
c：16 ASCII chars -> 16 ASCII chars
```

如果一次 NDEF 读取因为 APDU 长度被拆成多个分片，芯片会为同一次文件读取复用 counter 和 random padding，保证各分片拼出的动态 NDEF 一致。收到其他命令或切换 `ReadData`/`ISOReadBinary` 读取方式后，下一次首次读取会生成新的一组动态数据。

## 9. SDMReadCtr 的影响

SDMReadCtr 是 24-bit unsigned counter。对未认证读取，芯片会在本次首次成功读取的响应计算前推进计数器，并将其以 3-byte little-endian 形式放入 PICCData，同时用于派生 `c` 的 session MAC key。同一次文件分片读取不会重复推进；认证读取不会推进 SDMReadCtr。

服务端必须：

1. 先使用 Key 2 解密 `e`；
2. 从 PICCData 中取得 UID 和 SDMReadCtr；
3. 使用 Key 1 重新计算并常量时间比较 `c`；
4. MAC 成功后，按 UID 原子检查并推进已接受的最大 counter；
5. 拒绝已见过或倒退的 counter。

重新执行开启 SDM 的 `ChangeFileSettings` 会重置卡片计数器。因此生产服务端应将防重放状态与可信的个性化 generation/profile version 绑定，不能在看到低 counter 时自动清空状态。

## 10. 关闭 SDM 为什么还要清理 URL

只执行 `ChangeFileSettings` 关闭 SDM，只会停止动态 mirroring，不会自动修改之前写入 File 02 的静态模板。

所以只关闭 settings 后，File 02 仍可能保存：

```text
https://...?targetUrl=xxx
&e=00000000000000000000000000000000
&c=0000000000000000
```

如果不清理模板：

- 普通读取会看到静态 `e/c` 占位参数；
- 再次开启 SDM 时，个性化代码可能检测到已有 `e/c` 并拒绝重复追加；
- 业务端可能误把静态占位值当成动态 SDM 参数。

因此当前 Demo 的关闭流程包含两个原子化的功能步骤：

```text
1. ChangeFileSettings：关闭 File 02 SDM
2. 重写 NDEF：仅移除顶层 e/c，保留 targetUrl、fragment 和 pkg record
```

“关闭 SDM”不会开启或关闭写保护；写保护是独立的 File 02 access-right 操作。

## 11. 常见误区

### 11.1 芯片会自己解析 URL 并追加参数

错误。`?e=`、`&e=`、`&c=` 由 App 在个性化时写入。芯片只按照 offsets 输出固定长度的动态数据。

### 11.2 每次扫码都会把新 `e/c` 写回 File 02

错误。芯片生成的是动态读取响应，不是每次修改 EEPROM 中的 NDEF 模板。

### 11.3 `e` 和 `c` 都由同一把 Key 生成

不一定。当前 profile 中：

```text
e -> SDMMetaReadKey -> Key 2
c -> SDMFileReadKey -> Key 1
```

具体 AppKey 编号由 File 02 的 `SDMAccessRights` 决定，并非协议永久固定。

### 11.4 Offset 是 URL 字符串下标

错误。Offset 是相对于 File 02 起始位置的 byte offset，必须包含 NLEN 和 NDEF record 编码带来的 bytes。

### 11.5 关闭 SDM 会自动删除 `e/c`

错误。关闭 settings 不会重写静态 NDEF 模板，业务代码需要显式移除模板中的顶层 `e/c`。

## 12. 当前代码位置

- `app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Ntag424DnaPersonalizationClient.kt`
  - `enableSdmWithDemoKey0()`：读取现有 NDEF、写模板、开启 SDM；
  - `buildTemplate()`：构造占位符并计算 offsets；
  - `disableSdmWithDemoKey()`：关闭 SDM 并清理 URL 参数。
- `app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Ntag424DnaProtocol.kt`
  - `buildSdmUrl()`：保留业务 query 并追加 `e/c`；
  - `buildSdmFileSettings()`：编码 SDM options、access rights 和 offsets；
  - `removeSdmParameters()`：仅移除顶层 `e/c`。
- `app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Ntag424MockServerVerifier.kt`
  - 解密 `e`、验证 `c`、检查 SDMReadCtr。

## 13. 参考依据

- NXP AN12196 Rev. 2.0，§3.3：SDM mirroring 与编码方式；
- NXP AN12196 Rev. 2.0，§3.4.2：PICCData、PICCENCData 和 `SDMMetaReadKey`；
- NXP AN12196 Rev. 2.0，§3.4.4：SDMMAC、session vector、MAC input 和截断；
- NXP AN12196 Rev. 2.0，§5.4、§5.7：File 02 个性化模板、access rights 和 offsets 示例；
- NTAG 424 DNA datasheet Rev. 3.0，§8.2.3.4、Table 69：SDM options、access rights 和 offset 字段；
- NTAG 424 DNA datasheet Rev. 3.0，§9.3.1：SDMReadCtr 推进、重置和分片读取行为；
- NTAG 424 DNA datasheet Rev. 3.0，§9.3.3、§9.3.4：PICCData mirror position、未认证动态读取和认证静态读取；
- NTAG 424 DNA datasheet Rev. 3.0，§9.3.8、§9.3.9：SDMMAC mirror position 和 DynamicFileData 输入区间；
- NTAG 424 DNA datasheet Rev. 3.0，§11.7.1：`ChangeFileSettings` 只修改文件配置；
- [sdm_e_c_key_mapping_research.md](sdm_e_c_key_mapping_research.md)：当前 `e/c` 密钥映射核对；
- [sdm_verification_java.md](sdm_verification_java.md)：Java 服务端验证实现。
