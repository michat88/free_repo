package com.JavHey

import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.M3u8Helper

class Hglink : Haxloppd() {
    override var mainUrl = "https://hglink.to"
    override var name = "Hglink"
}

open class Haxloppd : ExtractorApi() {
    override var mainUrl = "https://haxloppd.com"
    override var name = "Haxloppd"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (com.lagradost.cloudstream3.SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 1. Tangani Redirect dari hglink.to ke haxloppd.com jika perlu
        var targetUrl = url
        if (url.contains("hglink.to")) {
            targetUrl = url.replace("hglink.to", "haxloppd.com")
        }

        // 2. Ambil halaman Embed
        val response = app.get(targetUrl, referer = "https://javhey.com/").text

        // 3. Cari endpoint pass_md5 (Kunci rahasia Doodstream)
        // Pola regex mencari: /pass_md5/mungkin_ada_string_acak/ID_VIDEO
        val md5Pattern = Regex("""/pass_md5/[^']*""")
        val md5Match = md5Pattern.find(response)?.value

        if (md5Match != null) {
            val trueUrl = "$mainUrl$md5Match"
            
            // 4. Request ke pass_md5 untuk mendapatkan Token Stream awal
            val tokenResponse = app.get(trueUrl, referer = targetUrl).text

            // 5. Buat String acak (Doodstream butuh string acak di akhir URL agar unik)
            val randomString = generateRandomString()
            val videoUrl = "$tokenResponse$randomString?token=${md5Match.substringAfterLast("/")}&expiry=${System.currentTimeMillis()}"

            // 6. Dapatkan kualitas & kirim link (Biasanya m3u8)
            M3u8Helper.generateM3u8(
                name,
                videoUrl,
                targetUrl // Referer wajib saat memutar video
            ).forEach(callback)
        } else {
            // Fallback jika pola pass_md5 berubah (jarang terjadi)
            // Coba cari redirect langsung jika server sedang 'longgar'
            val redirectMatch = Regex("""window\.location\.replace\('([^']*)'\)""").find(response)
            if (redirectMatch != null) {
                callback.invoke(
                    ExtractorLink(
                        name,
                        name,
                        redirectMatch.groupValues[1],
                        targetUrl,
                        Qualities.Unknown.value,
                        INFER_TYPE
                    )
                )
            }
        }
    }

    // Fungsi pembantu untuk membuat string acak (10 karakter)
    private fun generateRandomString(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
