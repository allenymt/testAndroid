package com.vdian.android.lib.testforgradle.nfc

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class HkNtag424VerifyResult(
    val ok: Boolean,
    val batchId: String?,
    val recordId: String?,
    val uid: String?,
    val physicalSerialNo: String?,
    val counter: Long?,
    val message: String
)

internal class HkNtag424VerifyClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    fun verify(url: String): HkNtag424VerifyResult {
        val body = JSONObject().put("url", url).toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder().url(VERIFY_ENDPOINT).post(body).build()
        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("验真接口 HTTP ${response.code}: $responseText")
            val root = JSONObject(responseText)
            val status = root.optJSONObject("status")
            val data = root.optJSONObject("data")
            val success = root.optBoolean("success", false) &&
                status?.optBoolean("success", false) == true && data?.optBoolean("ok", false) == true
            if (!success) {
                val message = status?.optString("message").orEmpty().ifBlank { "服务端验真失败" }
                throw IOException(message)
            }
            return HkNtag424VerifyResult(
                ok = true,
                batchId = data?.optNullableString("batchId"),
                recordId = data?.optNullableString("recordId"),
                uid = data?.optNullableString("uid"),
                physicalSerialNo = data?.optNullableString("physicalSerialNo"),
                counter = data?.takeIf { it.has("counter") && !it.isNull("counter") }?.optLong("counter"),
                message = status?.optString("message").orEmpty()
            )
        }
    }

    companion object {
        const val VERIFY_ENDPOINT = "http://10.37.98.52:8080/fusionitem/test/ntag424/verify"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    takeIf { has(name) && !isNull(name) }?.optString(name)?.takeIf { it.isNotBlank() }
