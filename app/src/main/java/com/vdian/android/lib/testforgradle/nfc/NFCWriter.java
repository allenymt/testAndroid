package com.vdian.android.lib.testforgradle.nfc;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NFCWriter {

    /**
     * 创建NDEF消息并写入NFC标签
     * @param tag NFC标签对象
     * @return 是否写入成功
     */
    public static boolean writeNdefMessage(Tag tag) {
        try {
            // 创建文本类型的NDEF记录
            String content = "com.koudai";
            byte[] languageBytes = "en".getBytes(StandardCharsets.US_ASCII);
            byte[] textBytes = content.getBytes(StandardCharsets.UTF_8);

            int languageLength = languageBytes.length;
            int textLength = textBytes.length;

            byte[] payload = new byte[1 + languageLength + textLength];
            payload[0] = (byte) languageLength;  // 设置语言代码长度

            // 复制语言代码
            System.arraycopy(languageBytes, 0, payload, 1, languageLength);
            // 复制文本内容
            System.arraycopy(textBytes, 0, payload, 1 + languageLength, textLength);

            // 创建NDEF记录
            NdefRecord textRecord = new NdefRecord(
                    NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT,
                    new byte[0],
                    payload);

            // 创建NDEF消息
            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{textRecord});

            // 尝试写入NFC标签
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                // 标签支持NDEF
                ndef.connect();
                if (ndef.isWritable()) {
                    ndef.writeNdefMessage(ndefMessage);
                    ndef.close();
                    return true;
                }
                ndef.close();
            } else {
                // 标签不支持NDEF，尝试格式化
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    format.connect();
                    format.format(ndefMessage);
                    format.close();
                    return true;
                }
            }
        } catch (IOException | android.nfc.FormatException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取NDEF消息的十六进制字符串
     * @return 十六进制字符串
     */
    public static String getNdefMessageHex() {
        try {
            String content = "com.koudai";
            byte[] languageBytes = "en".getBytes(StandardCharsets.US_ASCII);
            byte[] textBytes = content.getBytes(StandardCharsets.UTF_8);

            int languageLength = languageBytes.length;
            int textLength = textBytes.length;

            byte[] payload = new byte[1 + languageLength + textLength];
            payload[0] = (byte) languageLength;

            System.arraycopy(languageBytes, 0, payload, 1, languageLength);
            System.arraycopy(textBytes, 0, payload, 1 + languageLength, textLength);

            NdefRecord textRecord = new NdefRecord(
                    NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT,
                    new byte[0],
                    payload);

            NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{textRecord});

            // 将NDEF消息转换为十六进制字符串
            byte[] messageBytes = ndefMessage.toByteArray();
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageBytes) {
                hexString.append(String.format("%02X", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}