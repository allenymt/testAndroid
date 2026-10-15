# 星愿卡-Ntag424验真流程改造
- 新建2个Activity，一个负责写，一个负责读+验真

# 写数据页面
- 提供mock数据包含key0、key1、key2、ndef数据，key0和key2固定，key1每张卡不一样
- ndef数据和现在保持一致，第一是URL：https://wdb-applink.weidian.com?targetUrl=weidianbuyer://wdb/nfc%3Fversion%3D1%26encryptId%3DN424.%26e%3D00000000000000000000000000000000%26c%3D0000000000000000，第二是packageName，默认都是com.koudai.weidian.buyer
- 写数据操作增加：每张卡需要生成一个递增的SN码，例如001、002，一直递增；写完数据后在原有的mock数据上新增uid、SN码，产出完整的json数据

# 验真页面 
- 增加http接口，碰卡读取ndef数据请求http接口验真
```java
curl -X POST \
    'http://10.37.98.52:8080/fusionitem/test/ntag424/verify' \
    -H 'Content-Type: application/json' \
    -d '{
      "url": "https://wdb-applink.weidian.com?targetUrl=weidianbuyer://wdb/nfc%3Fversion%3D1%26encryptId%3DN424.%26e%3D实际32位HEX%26c%3D实际16位HEX"
    }'

  例如参数格式：

  {
    "url": "https://wdb-applink.weidian.com?targetUrl=weidianbuyer://wdb/nfc%3Fversion%3D1%26encryptId%3DN424.%26e%3D0123456789ABCDEF0123456789ABCDEF%26c%3D0123456789ABCDEF"
  }

  校验成功会返回：

  {
    "status": {
      "code": 0,
      "message": "success",
      "success": true,
      "failed": false
    },
    "data": {
      "ok": true,
      "batchId": "1",
      "recordId": "v1-0001",
      "uid": "04DE5F1EACC040",
      "physicalSerialNo": "No.001",
      "counter": 1
    },
    "success": true,
    "failed": false
  }

  注意：示例中的 e 和 c 只是格式占位，无法通过验真。必须先调用 /bind 将对应 recordId 和 UID、卡面编码绑定，再提交芯片实际生成的 e、c
```

# 疑问点
- key0固定，key1可变，是否需要建立本地数据库存储，通过uid映射,存储key0、key1
- 如何保障SN码递增不重复

# mock的数据
```java
[
        {
        "batchId": "1",
        "recordId": "v1-0001",
        "version": 1,
        "key0": "9E099ADFB67F851D3A935C9D1D1BD7A5",
        "key1": "C6337F8419A95E1CE6CDF21A322179E7",
        "key2": "5D0FAB01C1543751F084C27B6C5644D7",
        "url": "https://wdb-applink.weidian.com?targetUrl=weidianbuyer://wdb/nfc%3Fversion%3D1%26encryptId%3DN424.&e=00000000000000000000000000000000&c=0000000000000000",
        "packageName": "com.koudai.weidian.buyer",
        "uid": null,
        "physicalSerialNo": null,
        "lastCounter": null
        },
        {
        "batchId": "1",
        "recordId": "v1-0002",
        "version": 1,
        "key0": "9E099ADFB67F851D3A935C9D1D1BD7A5",
        "key1": "96885C62C17C28F0124069E0B8567F07",
        "key2": "5D0FAB01C1543751F084C27B6C5644D7",
        "url": "https://wdb-applink.weidian.com?targetUrl=weidianbuyer://wdb/nfc%3Fversion%3D1%26encryptId%3DN424.&e=00000000000000000000000000000000&c=0000000000000000",
        "packageName": "com.koudai.weidian.buyer",
        "uid": null,
        "physicalSerialNo": null,
        "lastCounter": null
        }
        ]

```