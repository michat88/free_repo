package com.JavHey

import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.newExtractorLink

// --- DAFTAR SERVER ---
// Kita buat kelas untuk setiap domain yang muncul di Logcat
class Hglink : JavHeyDood("https://hglink.to", "Hglink")
class Haxloppd : JavHeyDood("https://haxloppd.com", "Haxloppd")
class Minochinos : JavHeyDood("https://minochinos.com", "Minochinos")
class Bysebuho : JavHeyDood("https://bysebuho.com", "Bysebuho")
class GoTv : JavHeyDood("https://go-tv.lol", "GoTv")

// --- LOGIKA UTAMA (PARENT CLASS) ---
// Semua server di atas mewarisi logika dari kelas ini
open class JavHeyDood(override var mainUrl: String, override var name: String) : ExtractorApi() {
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (com.lagradost.cloudstream3.SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 1. Standarisasi URL (ubah /v/ menjadi /e/ agar konsisten)
        val targetUrl = url.replace("/v/", "/e/")
        
        // 2. Ambil halaman Embed
        val response = app.get(targetUrl, referer = "https://javhey.com/").text

        // 3. Cari endpoint pass_md5 (Kunci rahasia Doodstream)
        val md5Pattern = Regex("""/pass_md5/[^']*""")
        val md5Match = md5Pattern.find(response)?.value

        if (md5Match != null) {
            val trueUrl = "https://dood.li$md5Match" // Gunakan domain dood.li untuk handshake agar lebih stabil
            
            // 4. Request ke pass_md5
            val tokenResponse = app.get(trueUrl, referer = targetUrl).text

            // 5. Buat String acak
            val randomString = generateRandomString()
            val videoUrl = "$tokenResponse$randomString?token=${md5Match.substringAfterLast("/")}&expiry=${System.currentTimeMillis()}"

            // 6. Dapatkan kualitas & kirim link
            M3u8Helper.generateM3u8(
                name,
                videoUrl,
                targetUrl 
            ).forEach(callback)
        } else {
            // Fallback: Cari redirect langsung
            val redirectMatch = Regex("""window\.location\.replace\('([^']*)'\)""").find(response)
            if (redirectMatch != null) {
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = name,
                        url = redirectMatch.groupValues[1],
                        type = INFER_TYPE
                    ) {
                        this.referer = targetUrl
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
        }
    }

    private fun generateRandomString(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
