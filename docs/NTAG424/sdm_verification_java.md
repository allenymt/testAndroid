# NTAG 424 DNA SDM 服务端验证（Java）

## 1. 文档范围

本文给出当前 Demo 的 File 02（NDEF）SDM URL 服务端验证方法，并提供不依赖第三方密码库的 Java 8 示例。

当前动态 URL 包含：

- `e`：32 个十六进制字符，表示 16-byte `PICCENCData`；
- `c`：16 个十六进制字符，表示 8-byte truncated `SDMMAC`；
- `targetUrl`：原有业务参数，不参与当前 profile 的 SDMMAC 计算。

当前 File 02 的 `SDMAccessRights` 传输字节为 `F1 21`，密钥角色映射如下：

| URL 数据 | NXP 密钥角色 | 当前 AppKey | 服务端用途 |
| --- | --- | --- | --- |
| `e` / `PICCENCData` | `SDMMetaReadKey` | Key 2 | AES-CBC 解密 UID 和 SDMReadCtr |
| `c` / `SDMMAC` | `SDMFileReadKey` | Key 1 | 派生本次 session MAC key 并验证 MAC |
| 受保护计数器读取 | `SDMCtrRetKey` | Key 1 | NFC 认证后读取计数器，不参与普通 URL 验证 |

> Key 1、Key 2 是当前卡片 profile 的配置，不是 NTAG 424 DNA 协议规定的永久编号。服务端应读取发行记录或 profile 配置，按 `SDMAccessRights` 找到对应 key material。

## 2. 验证顺序

```text
收到动态 URL
  -> 严格解析顶层 e 和 c
  -> 使用 SDMMetaReadKey（当前 Key 2）解密 e
  -> 校验 PICCDataTag，提取 UID 和 SDMReadCtr
  -> 使用 SDMFileReadKey（当前 Key 1）派生 KSesSDMFileReadMAC
  -> 按 File 02 offsets 取得 MAC 输入并重新计算 c
  -> 常量时间比较服务端 c 和 URL c
  -> MAC 成功后，按 UID 原子检查并推进 SDMReadCtr
  -> 受理业务
```

验证顺序不能交换。特别是，MAC 失败的请求不能推进服务端计数器，否则攻击者可以用伪造请求消耗合法卡片的计数器窗口。

## 3. 完整 Java 8 实现

下面的类实现当前 Demo profile 的完整验证流程。示例自行实现 AES-CMAC，是为了展示全部算法步骤；生产项目也可以使用经过审计的密码库实现 CMAC，但截断规则必须保持一致。

```java
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Ntag424SdmVerifier {
    private static final int AES_BLOCK_SIZE = 16;
    private static final int CMAC_RB = 0x87;
    private static final byte[] ZERO_IV = new byte[AES_BLOCK_SIZE];
    private static final byte[] SV2_PREFIX = hex("3CC300010080");

    private final byte[] sdmFileReadKey;
    private final byte[] sdmMetaReadKey;
    private final ReplayStore replayStore;

    /**
     * @param sdmFileReadKey 当前 profile 为 AppKey 1，用于验证 c
     * @param sdmMetaReadKey 当前 profile 为 AppKey 2，用于解密 e
     */
    public Ntag424SdmVerifier(
            byte[] sdmFileReadKey,
            byte[] sdmMetaReadKey,
            ReplayStore replayStore) {
        requireLength(sdmFileReadKey, 16, "SDMFileReadKey");
        requireLength(sdmMetaReadKey, 16, "SDMMetaReadKey");
        if (replayStore == null) {
            throw new IllegalArgumentException("ReplayStore 不能为空");
        }
        this.sdmFileReadKey = sdmFileReadKey.clone();
        this.sdmMetaReadKey = sdmMetaReadKey.clone();
        this.replayStore = replayStore;
    }

    /** 当前 Demo 的两个 offset 相等，因此额外 MAC 输入为空。 */
    public VerificationResult verify(String url) throws GeneralSecurityException {
        return verify(url, new byte[0]);
    }

    /**
     * macInput 必须等于动态 File 02 的
     * [SDMMACInputOffset, SDMMACOffset) 原始字节。
     */
    public VerificationResult verify(String url, byte[] macInput)
            throws GeneralSecurityException {
        if (macInput == null) {
            throw new IllegalArgumentException("macInput 不能为空");
        }

        Map<String, String> parameters = parseUniqueTopLevelParameters(url);
        byte[] encryptedPiccData = requiredHex(parameters, "e", 16);
        byte[] receivedMac = requiredHex(parameters, "c", 8);

        // e 使用 SDMMetaReadKey（当前 Key 2）和全零 IV 解密。
        byte[] piccData = aesCbcDecrypt(
                sdmMetaReadKey,
                ZERO_IV,
                encryptedPiccData);
        requireLength(piccData, 16, "PICCData");

        int piccDataTag = piccData[0] & 0xFF;
        if ((piccDataTag & 0xC0) != 0xC0) {
            throw new VerificationException(
                    "PICCDataTag 未同时声明 UID 和 SDMReadCtr");
        }
        if ((piccDataTag & 0x30) != 0
                || (piccDataTag & 0x0F) != 0x07) {
            throw new VerificationException("PICCDataTag 不是 7-byte UID");
        }

        byte[] uid = Arrays.copyOfRange(piccData, 1, 8);
        byte[] counterLe = Arrays.copyOfRange(piccData, 8, 11);
        int readCounter = littleEndian24(counterLe);

        // SV2 = 3CC300010080 || UID || SDMReadCtr_LE，共 16 bytes。
        byte[] sessionVector = concat(SV2_PREFIX, uid, counterLe);
        byte[] sessionMacKey = aesCmac(sdmFileReadKey, sessionVector);

        // c 是完整 AES-CMAC 的奇数索引字节 1,3,...,15。
        byte[] fullMac = aesCmac(sessionMacKey, macInput);
        byte[] calculatedMac = truncateSdmMac(fullMac);

        if (!MessageDigest.isEqual(calculatedMac, receivedMac)) {
            throw new VerificationException("SDMMAC c 验证失败");
        }

        // 必须在 MAC 验证成功之后原子检查并推进计数器。
        if (!replayStore.acceptIfGreater(uid, readCounter)) {
            throw new VerificationException(
                    "SDMReadCtr 已使用或早于服务器记录，拒绝重放");
        }

        return new VerificationResult(uid, readCounter, calculatedMac);
    }

    private static Map<String, String> parseUniqueTopLevelParameters(String url) {
        final String rawQuery;
        try {
            rawQuery = URI.create(url).getRawQuery();
        } catch (IllegalArgumentException error) {
            throw new VerificationException("URL 格式错误", error);
        }
        if (rawQuery == null) {
            throw new VerificationException("URL 缺少 query");
        }

        Map<String, String> result = new HashMap<>();
        for (String parameter : rawQuery.split("&", -1)) {
            if (parameter.isEmpty()) {
                continue;
            }
            int separator = parameter.indexOf('=');
            String rawName = separator >= 0
                    ? parameter.substring(0, separator)
                    : parameter;
            String rawValue = separator >= 0
                    ? parameter.substring(separator + 1)
                    : "";
            String name = urlDecode(rawName).toLowerCase(Locale.ROOT);
            String value = urlDecode(rawValue);
            if (result.put(name, value) != null) {
                throw new VerificationException("URL 包含重复参数: " + name);
            }
        }
        return result;
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception error) {
            throw new VerificationException("URL query 解码失败", error);
        }
    }

    private static byte[] requiredHex(
            Map<String, String> parameters,
            String name,
            int byteCount) {
        String value = parameters.get(name);
        if (value == null) {
            throw new VerificationException("URL 缺少 " + name + " 参数");
        }
        if (value.length() != byteCount * 2 || !isHex(value)) {
            throw new VerificationException(
                    name + " 必须是 " + (byteCount * 2) + " 个 hex 字符");
        }
        return hex(value);
    }

    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int littleEndian24(byte[] value) {
        requireLength(value, 3, "SDMReadCtr");
        return (value[0] & 0xFF)
                | ((value[1] & 0xFF) << 8)
                | ((value[2] & 0xFF) << 16);
    }

    private static byte[] aesCbcDecrypt(byte[] key, byte[] iv, byte[] ciphertext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    /** RFC 4493 AES-CMAC，包含 zero-length message 的处理。 */
    private static byte[] aesCmac(byte[] key, byte[] message)
            throws GeneralSecurityException {
        requireLength(key, 16, "AES-CMAC key");
        byte[] firstSubKey = doubleBlock(aesEncryptBlock(key, new byte[16]));
        byte[] secondSubKey = doubleBlock(firstSubKey);

        boolean completeLastBlock = message.length > 0
                && message.length % AES_BLOCK_SIZE == 0;
        int blockCount = Math.max(
                1,
                (message.length + AES_BLOCK_SIZE - 1) / AES_BLOCK_SIZE);
        int lastOffset = (blockCount - 1) * AES_BLOCK_SIZE;
        byte[] lastBlock = new byte[AES_BLOCK_SIZE];

        if (completeLastBlock) {
            System.arraycopy(
                    message,
                    lastOffset,
                    lastBlock,
                    0,
                    AES_BLOCK_SIZE);
            xorInPlace(lastBlock, firstSubKey);
        } else {
            int remaining = message.length - lastOffset;
            if (remaining > 0) {
                System.arraycopy(message, lastOffset, lastBlock, 0, remaining);
            }
            lastBlock[remaining] = (byte) 0x80;
            xorInPlace(lastBlock, secondSubKey);
        }

        byte[] state = new byte[AES_BLOCK_SIZE];
        for (int block = 0; block < blockCount - 1; block++) {
            byte[] input = Arrays.copyOfRange(
                    message,
                    block * AES_BLOCK_SIZE,
                    (block + 1) * AES_BLOCK_SIZE);
            xorInPlace(input, state);
            state = aesEncryptBlock(key, input);
        }
        xorInPlace(lastBlock, state);
        return aesEncryptBlock(key, lastBlock);
    }

    private static byte[] aesEncryptBlock(byte[] key, byte[] block)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(block);
    }

    private static byte[] doubleBlock(byte[] input) {
        byte[] output = new byte[input.length];
        int carry = 0;
        for (int index = input.length - 1; index >= 0; index--) {
            int value = input[index] & 0xFF;
            output[index] = (byte) ((value << 1) | carry);
            carry = value >>> 7;
        }
        if (carry != 0) {
            output[output.length - 1] ^= (byte) CMAC_RB;
        }
        return output;
    }

    private static void xorInPlace(byte[] target, byte[] other) {
        for (int index = 0; index < target.length; index++) {
            target[index] ^= other[index];
        }
    }

    private static byte[] truncateSdmMac(byte[] fullMac) {
        requireLength(fullMac, 16, "full SDMMAC");
        byte[] truncated = new byte[8];
        for (int index = 0; index < truncated.length; index++) {
            truncated[index] = fullMac[index * 2 + 1];
        }
        return truncated;
    }

    private static byte[] concat(byte[]... values) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] value : values) {
            output.write(value, 0, value.length);
        }
        return output.toByteArray();
    }

    public static byte[] hex(String value) {
        if ((value.length() & 1) != 0 || !isHex(value)) {
            throw new IllegalArgumentException("非法 hex 字符串");
        }
        byte[] result = new byte[value.length() / 2];
        for (int index = 0; index < result.length; index++) {
            result[index] = (byte) Integer.parseInt(
                    value.substring(index * 2, index * 2 + 2),
                    16);
        }
        return result;
    }

    public static String toHex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) {
            result.append(String.format(Locale.ROOT, "%02X", item & 0xFF));
        }
        return result.toString();
    }

    private static void requireLength(byte[] value, int length, String name) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " 必须是 " + length + " bytes");
        }
    }

    public interface ReplayStore {
        /** 对同一 UID，只有严格大于已记录值的 counter 才能原子写入成功。 */
        boolean acceptIfGreater(byte[] uid, int readCounter);
    }

    /** 仅供单进程 Demo 使用；生产环境必须替换为数据库原子操作。 */
    public static final class InMemoryReplayStore implements ReplayStore {
        private final Map<String, Integer> latestCounters = new HashMap<>();

        @Override
        public synchronized boolean acceptIfGreater(byte[] uid, int readCounter) {
            String cardId = toHex(uid);
            Integer previous = latestCounters.get(cardId);
            if (previous != null && readCounter <= previous) {
                return false;
            }
            latestCounters.put(cardId, readCounter);
            return true;
        }
    }

    public static final class VerificationResult {
        private final byte[] uid;
        private final int readCounter;
        private final byte[] calculatedMac;

        private VerificationResult(
                byte[] uid,
                int readCounter,
                byte[] calculatedMac) {
            this.uid = uid.clone();
            this.readCounter = readCounter;
            this.calculatedMac = calculatedMac.clone();
        }

        public byte[] getUid() {
            return uid.clone();
        }

        public int getReadCounter() {
            return readCounter;
        }

        public byte[] getCalculatedMac() {
            return calculatedMac.clone();
        }
    }

    public static final class VerificationException extends RuntimeException {
        public VerificationException(String message) {
            super(message);
        }

        public VerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

## 4. 调用示例与已知卡片数据

下面的记录来自一张仍使用全零 Key 1/Key 2 的测试卡，可用于核对 Java 实现。验证结果应为 UID `04187262E51690`、计数器 `3`。

```java
public final class VerifyDemo {
    public static void main(String[] args) throws Exception {
        byte[] zeroKey = new byte[16];
        Ntag424SdmVerifier verifier = new Ntag424SdmVerifier(
                zeroKey, // SDMFileReadKey，当前 profile 的 Key 1
                zeroKey, // SDMMetaReadKey，当前 profile 的 Key 2
                new Ntag424SdmVerifier.InMemoryReplayStore());

        String url = "https://wdb-applink.weidian.com?"
                + "e=F9059AC5EB4B02AC9F6E3CB6DE0712C"
                + "&c=0B2902187FF92A61";

        Ntag424SdmVerifier.VerificationResult result = verifier.verify(url);
        System.out.println("UID="
                + Ntag424SdmVerifier.toHex(result.getUid()));
        System.out.println("SDMReadCtr=" + result.getReadCounter());
    }
}
```

当前 APK 中 version 1 的固定 Demo keys 是：

```java
byte[] demoKey1 = Ntag424SdmVerifier.hex(
        "A1B2C3D4E5F6071829303A4B5C6D7E8F");
byte[] demoKey2 = Ntag424SdmVerifier.hex(
        "000102030405060708090A0B0C0D0E0F");

Ntag424SdmVerifier verifier = new Ntag424SdmVerifier(
        demoKey1, // 验证 c
        demoKey2, // 解密 e
        replayStore);
```

这些固定值只能用于协议联调。生产密钥不得写入客户端 APK、Git、日志或文档，应保存在服务端 KMS/HSM 中。

## 5. `e` 的解密与字段结构

当前 `e` 的算法为：

```text
PICCData = AES-128-CBC-Decrypt(
    key        = SDMMetaReadKey（当前 Key 2）, 
    iv         = 00000000000000000000000000000000,
    ciphertext = hexDecode(e),
    padding    = NoPadding
)
```

解密后的 16-byte block：

```text
byte 0       PICCDataTag
byte 1..7    7-byte UID
byte 8..10   3-byte SDMReadCtr，little-endian
byte 11..15  RandomPadding
```

服务端必须先检查 `PICCDataTag`，再信任 UID 和计数器字段。当前配置同时镜像 UID 和 SDMReadCtr，7-byte UID 的正常 tag 为 `C7h`。示例代码同时校验 UID/计数器存在标志、RFU bits 和 UID 长度，避免只检查部分低位后接受不属于当前 profile 的格式。

## 6. `c` 的重新计算

首先使用从 `e` 中解出的 UID 和计数器原始字节构造 16-byte session vector：

```text
SV2 = 3CC300010080 || UID || SDMReadCtr_LE
KSesSDMFileReadMAC = AES-CMAC(SDMFileReadKey, SV2)
```

再对 File 02 配置指定的动态文件区间计算 MAC：

```text
macInput = DynamicFileData[SDMMACInputOffset, SDMMACOffset)
fullMac  = AES-CMAC(KSesSDMFileReadMAC, macInput)
cServer  = fullMac[1] || fullMac[3] || ... || fullMac[15]
```

当前 Demo 中：

```text
SDMMACInputOffset == SDMMACOffset
```

所以 `macInput` 是长度为 0 的 byte array，但仍必须执行 AES-CMAC 的空消息算法。不能把某个结果写死，因为 session key 已经绑定 UID 和本次 SDMReadCtr。

如果以后修改 offsets，让 MAC 覆盖业务 URL，后端必须取得动态 File 02 中完全相同的原始字节区间。它是 File 02 byte offset，不是 Java `String` 字符下标，也不是简单地对重建后的 URL 字符串求 MAC。

## 7. 防重放的生产实现

进程内 `HashMap` 只能用于 Demo。生产服务应在 MAC 验证成功后，通过数据库事务、行锁或条件更新实现“仅接受更大计数器”。例如：

```sql
UPDATE ntag424_counter
SET max_counter = :newCounter,
    updated_at = CURRENT_TIMESTAMP
WHERE uid = :uid
  AND max_counter < :newCounter;
```

更新行数为 0 时，必须区分首次见到该 UID 和重放：

1. UID 必须先存在于合法发行卡表，不能因为首次请求就自动信任未知 UID；
2. 已有计数器且新值不大于旧值时拒绝；
3. 首次建立计数器记录和业务受理必须位于同一事务；
4. 同一个 UID + counter 在并发、多实例和服务重启后也只能成功一次。

若业务存在合法重复请求重试，应缓存第一次成功的业务结果并做幂等返回，而不是再次推进或绕过 SDMReadCtr 检查。

SDMReadCtr 是 24-bit unsigned counter。重新执行开启 SDM 的 `ChangeFileSettings` 会将其重置，因此生产防重放主键不能只包含 UID，还应包含受控的个性化 generation/profile version。只有服务端确认卡片完成合法重新个性化后，才能创建新 generation 并接受新的低计数器；不能因为看到计数器变小就自动重置服务端状态。

## 8. 失败处理与安全边界

- 缺失、重复、非法长度或非 hex 的 `e/c`：立即拒绝；
- `PICCDataTag` 不符合当前 profile：拒绝，不提取 UID；
- `c` 不匹配：拒绝，不更新计数器；
- 计数器重复或倒退：按重放拒绝；
- 数据库或 KMS/HSM 不可用：默认拒绝，不能降级为只解密 `e`；
- 对外错误不要区分“Key 错误”“UID 已成功解密”等内部细节；
- 日志不要记录 key material、解密后的 PICCData 或完整可重放 URL。

解密 `e` 只能取得 UID 和计数器，不等于完成真伪验证。只有 `c` 验证成功并通过原子防重放检查后，才能接受该次请求。

本文代码只适用于当前 AES SDM profile。如果卡片切换为 LRP，字段长度和密码算法会变化，服务端必须按 profile 分流并拒绝用本文 AES 流程误验 LRP 请求。

## 9. 当前 Demo 与生产环境的差异

| 项目 | 当前 Demo | 生产要求 |
| --- | --- | --- |
| Key 1/Key 2 | APK 内固定值 | 服务端 KMS/HSM，支持版本与轮换 |
| profile 选择 | 固定 `F1 21` 映射 | 按发行批次/卡片记录选择 |
| MAC 输入 | 空 byte array | 严格服从卡片 File 02 offsets |
| 防重放状态 | 单进程内存 | 数据库持久化、原子更新 |
| UID 管理 | 本地测试直接返回 | 必须匹配服务端合法发行记录 |
| 失败响应 | 便于调试 | 统一外部错误，受控内部审计 |

## 10. 容易混淆的 `SDMENCFileData`

当前 URL 的 `e` 是 `PICCENCData`，使用 `SDMMetaReadKey`（当前 Key 2）直接进行 AES-CBC 解密。

`SDMENCFileData` 是另一种可选的业务文件数据加密镜像，使用 `SDMFileReadKey` 派生的 `KSesSDMFileReadENC`。当前 Demo 没有启用该功能，不能把 `SDMENCFileData` 的 Key 1 解密流程套用到当前 `e`。

## 11. 参考依据

- NXP AN12196 Rev. 2.0，§3.4.2：PICCData 加解密及 `SDMMetaReadKey`；
- NXP AN12196 Rev. 2.0，§3.4.4：SDMMAC、session vector、zero-length MAC input 和截断；
- NXP AN12196 Rev. 2.0，§5.4 Table 11：示例 `SDMAccessRights=F121` 的角色映射；
- NXP AN12196 Rev. 2.0，§8.1：后端密钥与 HSM 保护建议；
- NTAG 424 DNA datasheet Rev. 3.0，Table 69：SDM access rights 与 offsets；
- NTAG 424 DNA datasheet Rev. 3.0，§9.3、§9.3.1：verifier 防重放要求和 24-bit SDMReadCtr 行为；
- [sdm_e_c_key_mapping_research.md](sdm_e_c_key_mapping_research.md)：`e/c` 与 Key 1/Key 2 专项核对；
- [sdm_verification_flow.md](sdm_verification_flow.md)：当前 Demo 的协议流程说明；
- `app/src/main/java/com/vdian/android/lib/testforgradle/nfc/Ntag424MockServerVerifier.kt`：当前 Android Mock 实现；
- `app/src/test/java/com/vdian/android/lib/testforgradle/nfc/Ntag424MockServerVerifierTest.kt`：真实卡片捕获值与防重放测试。
