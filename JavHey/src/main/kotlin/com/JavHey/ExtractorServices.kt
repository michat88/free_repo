package com.JavHey

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.lagradost.cloudstream3.network.WebViewResolver
import org.jsoup.nodes.Document
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*

object JavHeyExtractorManager {

    suspend fun invoke(
        html: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val rawUrls = mutableSetOf<String>()

        // 1. Ambil Link Tersembunyi (Base64 biasanya berisi server VIP/Cepat)
        try {
            val regexBase64 = Regex("""id="links"\s+value="([^"]+)"""")
            regexBase64.find(html)?.groupValues?.get(1)?.let { encrypted ->
                if (encrypted.isNotEmpty()) {
                    val decoded = String(Base64.getDecoder().decode(encrypted))
                    decoded.split(",,,").forEach { url ->
                        val clean = url.trim()
                        if (isValidLink(clean)) rawUrls.add(clean)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 2. Ambil Link Tombol
        try {
            val regexLink = Regex("""href="(https?://[^"]+)"""")
            regexLink.findAll(html).forEach { match ->
                val href = match.groupValues[1].trim()
                if (isValidLink(href) && !rawUrls.contains(href)) {
                    rawUrls.add(href)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // --- RAHASIA KECEPATAN (SORTING) ---
        // Kita urutkan agar server 'VidHide' dan 'StreamWish' dipanggil paling awal.
        // DoodStream (yang sering lemot) ditaruh belakangan.
        val sortedUrls = rawUrls.sortedBy { url ->
            val u = url.lowercase()
            when {
                u.contains("vidhide") || u.contains("filelions") -> 0 // Prioritas 1 (Tercepat)
                u.contains("streamwish") || u.contains("mwish") -> 1  // Prioritas 2
                u.contains("mixdrop") -> 2                            // Prioritas 3
                else -> 99                                            // Sisanya (Dood, dll)
            }
        }

        // 3. EKSEKUSI
        coroutineScope {
            sortedUrls.forEach { url ->
                launch(Dispatchers.IO) {
                    try {
                        // Begitu server cepat (VidHide) selesai, 'callback' langsung dipanggil.
                        // Player langsung memutar video TANPA menunggu DoodStream selesai loading.
                        loadExtractor(url, subtitleCallback, callback)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun isValidLink(url: String): Boolean {
        if (!url.startsWith("http")) return false
        val u = url.lowercase()
        return (u.contains("vidhide") || u.contains("filelions") || u.contains("kinoger") ||
                u.contains("mixdrop") || u.contains("streamwish") || u.contains("mwish") ||
                u.contains("wishembed") || u.contains("dood") || u.contains("ds2play") ||
                u.contains("lulustream") || u.contains("swdyu") || u.contains("earn")) &&
               !u.contains("emturbovid") && 
               !u.contains("bestx")
    }
}

// ... (Sisa kode Extractor Class di bawah tetap sama) ...
