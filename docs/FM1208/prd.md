# 目标
- 可以对 FM1208卡进行nfc读写， 协议是ISO/IEC 7816-4 compatible 、ISO/IEC 14443-4 (Type A) compatible 、ISO/IEC 14443-3 (Type A) compatible
- 在nfc目录新增代码，当前目录都是对ndef协议的读写
- 目标数据: https协议的数据，第二能确保唤起指定APP


# 当前业务协议数据
- 第一条URL：• Record #1: URI record
  Type Name Format: NFC Forum well-known type
  Short Record type: "U"
  protocol field: https://
  URI field: wdb-applink.weidian.com?
  targetUrl=https%3A%2F%2Fvmspub.weidian.com%2Fgaia
  %2F79497%2F78f86a5b_bbk.html%3FshowTitle%3D0%2
  6transparentStatusBar%3D1
  Payload length: 145 bytes

- 第二条，Android包名，用户唤起指定APP：  Record #2: Android Application record
  Type Name Format: NFC Forum external type
  Short Record type: "android.com:pkg"
  package name: com.koudai.weidian.buyer
  Payload length: 24 bytes

- 当前demo只是为了测试读写 ，可以参考代码理解实际业务
  - 用户手机系统唤起APP的承接页： /Users/yulun/vdian_merge1/vdian/app/src/main/java/com/koudai/weidian/buyer/activity/WDNfcActivity.java
  - APP内主动读取识别nfc卡内容：/Users/yulun/vdian_merge1/vdian/bundles/business-support/compat/src/main/java/com/vdian/android/wdb/business/common/WdbNFCReaderActivity.kt

- FM1208卡扫描的完整信息
- agInfo
  <
  IC INFO
  NDEF
  EXTRA
  FULL SCAN
  •
  • Technologies Supported
  ISO/IEC 7816-4 compatible
  ISO/IEC 14443-4 (Type A) compatible
  ISO/IEC 14443-3 (Type A) compatible
  • Android Technology Information
  Tag description:
  • TAG: Tech [android.nfc.tech.lsoDep, android.nfc.tech.NfcAJ
  • Maximum transceive length: 65279 bytes
  • Default maximum transceive time-out: 1500 ms
  • Extended length APDUs supported
  • Maximum transceive length: 253 bytes
  • Default maximum transceive time-out: 618 ms
  • Detailed Protocol Information
  ID: 77:E9:CC:7D
  ATQA: 0x0800
  SAK: 0x20
  ATS: 0x10788090022090000000000077E9CC7D
  • Max. accepted frame size: 256 bytes (FSCI: 8)
  • Supported receive rates:
  • 106 kbit/s (DR: 1)
  • Supported send rates:
  • 106 kbit/s (DS: 1)
  • SFGT: 302.0us (SFGI: 0)
  • FWT: 154.7ms (FWI: 9)
  • NAD not supported
  • CID supported
  • Historical bytes: 0x2090000000000077E9CC7D | .....•w..y

