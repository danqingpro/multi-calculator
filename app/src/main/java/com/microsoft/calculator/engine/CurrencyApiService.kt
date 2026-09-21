package com.microsoft.calculator.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 汇率 API 服务,使用免费无需注册的 open.er-api.com 接口。
 * 数据每日更新,支持 160+ 货币,返回 JSON 格式。
 * 备用接口: api.frankfurter.app (ECB 数据,33 种主要货币)。
 *
 * 由于 Android 禁止主线程网络请求,所有方法须在协程中调用。
 */
object CurrencyApiService {

    private const val PRIMARY_API = "https://open.er-api.com/v6/latest/CNY"
    private const val FALLBACK_API = "https://api.frankfurter.app/latest?from=CNY"

    /** 获取汇率,以 CNY 为基准(1 CNY = X 目标货币) */
    suspend fun fetchRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        try {
            fetchFromPrimary()
        } catch (e: Exception) {
            try {
                fetchFromFallback()
            } catch (e2: Exception) {
                emptyMap()
            }
        }
    }

    private fun fetchFromPrimary(): Map<String, Double> {
        val url = URL(PRIMARY_API)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            if (json.optString("result") != "success") throw Exception("API error")
            val ratesJson = json.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            rates["CNY"] = 1.0
            ratesJson.keys().forEach { code ->
                rates[code] = ratesJson.getDouble(code)
            }
            rates
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchFromFallback(): Map<String, Double> {
        val url = URL(FALLBACK_API)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val ratesJson = json.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            rates["CNY"] = 1.0
            ratesJson.keys().forEach { code ->
                rates[code] = ratesJson.getDouble(code)
            }
            rates
        } finally {
            conn.disconnect()
        }
    }
}
