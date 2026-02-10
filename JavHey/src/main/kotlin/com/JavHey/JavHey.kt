package com.JavHey

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.util.Base64

class JavHey : MainAPI() {
    override var mainUrl = "https://javhey.com"
    override var name = "JavHey"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(TvType.NSFW)

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Referer" to "$mainUrl/"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/videos/paling-dilihat/page=" to "Paling Dilihat",
        "$mainUrl/videos/top-rating/page=" to "Top Rating",
        "$mainUrl/category/21/drama/page=" to "Drama"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val document = app.get(url, headers = headers).document
        val home = document.select("article.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("div.item_content h3 a") ?: return null
        val title = titleElement.text().trim()
        val href = fixUrl(titleElement.attr("href"))
        val posterUrl = this.selectFirst("div.item_header img")?.getHighQualityImageAttr()
        return newMovieSearchResponse(title, href, TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?s=$query"
        val document = app.get(url, headers = headers).document
        return document.select("article.item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = headers).document
        val title = document.selectFirst("h1.product_title")?.text()?.trim() ?: "No Title"
        val description = document.select("p.video-description").text().replace("Description: ", "", ignoreCase = true).trim()
        val poster = document.selectFirst("div.images img")?.getHighQualityImageAttr()
        
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, headers = headers).document
        val hiddenLinksEncrypted = document.selectFirst("input#links")?.attr("value")

        if (!hiddenLinksEncrypted.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.getDecoder().decode(hiddenLinksEncrypted)
                val decodedString = String(decodedBytes)
                val urls = decodedString.split(",,,")
                
                // --- DEBUG LOGGING ---
                // Lihat ini di Logcat dengan filter "JAVHEY_DEBUG"
                System.out.println("JAVHEY_DEBUG: Decoded String: $decodedString")
                
                urls.forEach { sourceUrl ->
                    val cleanUrl = sourceUrl.trim()
                    if (cleanUrl.isNotBlank() && cleanUrl.startsWith("http")) {
                        System.out.println("JAVHEY_DEBUG: Trying to load extractor for: $cleanUrl")
                        
                        // Coba load extractor
                        loadExtractor(cleanUrl, subtitleCallback, callback)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        document.select("div.links-download a").forEach { linkTag ->
            val downloadUrl = linkTag.attr("href")
            if (downloadUrl.isNotBlank() && downloadUrl.startsWith("http")) {
                loadExtractor(downloadUrl, subtitleCallback, callback)
            }
        }
        return true
    }

    private fun Element.getHighQualityImageAttr(): String? {
        val url = when {
            this.hasAttr("data-src") -> this.attr("data-src")
            this.hasAttr("data-original") -> this.attr("data-original")
            else -> this.attr("src")
        }
        return url
    }
}
