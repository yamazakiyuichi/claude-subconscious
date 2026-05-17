package com.example.suicahistory.data.web

import com.example.suicahistory.data.db.Transaction
import com.example.suicahistory.domain.CategoryClassifier
import okhttp3.*
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * モバイルSuica Webサービスから利用履歴を取得する
 * 対象: https://www.mobilesuica.com/
 */
class SuicaScraper(private val cookieJar: SuicaCookieJar) {

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(true)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)

    suspend fun login(email: String, password: String): Boolean {
        // Step 1: トップページを取得してCSRFトークンを取得
        val loginPage = get("https://www.mobilesuica.com/") ?: return false
        val doc = Jsoup.parse(loginPage)
        val viewState = doc.select("input[name=__VIEWSTATE]").attr("value")
        val eventValidation = doc.select("input[name=__EVENTVALIDATION]").attr("value")

        if (viewState.isEmpty()) return false

        // Step 2: ログインPOST
        val formBody = FormBody.Builder()
            .add("__VIEWSTATE", viewState)
            .add("__EVENTVALIDATION", eventValidation)
            .add("MailAddress", email)
            .add("Password", password)
            .add("btnLogin", "ログイン")
            .build()

        val response = post("https://www.mobilesuica.com/", formBody) ?: return false
        return response.contains("ログアウト") || response.contains("マイページ")
    }

    suspend fun fetchHistory(): List<Transaction> {
        val html = get("https://www.mobilesuica.com/iq/ir/SuicaDisp.aspx?MENUID=ZG131") ?: return emptyList()
        return parseHistoryHtml(html)
    }

    private fun parseHistoryHtml(html: String): List<Transaction> {
        val doc = Jsoup.parse(html)
        val rows = doc.select("table.tbl_SfHistory tr").drop(1) // ヘッダー除外

        return rows.mapNotNull { row ->
            val cols = row.select("td")
            if (cols.size < 6) return@mapNotNull null

            val dateStr = cols[0].text().trim()
            val shopName = cols[3].text().trim()
            val amountStr = cols[4].text().replace(",", "").replace("円", "").trim()
            val balanceStr = cols[5].text().replace(",", "").replace("円", "").trim()

            val date = runCatching { dateFormat.parse(dateStr)?.time }.getOrNull() ?: return@mapNotNull null
            val amount = amountStr.toIntOrNull() ?: return@mapNotNull null
            val balance = balanceStr.toIntOrNull() ?: 0

            Transaction(
                date = date,
                shopName = shopName,
                amount = -amount, // 支払いは負値
                balance = balance,
                category = CategoryClassifier.classify(shopName),
                source = "web"
            )
        }
    }

    private fun get(url: String): String? = runCatching {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { it.body?.string() }
    }.getOrNull()

    private fun post(url: String, body: FormBody): String? = runCatching {
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { it.body?.string() }
    }.getOrNull()
}

class SuicaCookieJar : CookieJar {
    private val store = mutableMapOf<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store[url.host] ?: emptyList()

    fun clear() = store.clear()
}
