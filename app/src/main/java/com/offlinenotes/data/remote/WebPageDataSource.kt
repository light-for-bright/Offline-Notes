
package com.offlinenotes.data.remote

import com.offlinenotes.utils.AppConfig
import com.offlinenotes.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class WebPageDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(AppConfig.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(AppConfig.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun fetchWebPage(url: String): WebPageResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.d("Fetching web page: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                    .addHeader("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .addHeader("Sec-Fetch-Dest", "document")
                    .addHeader("Sec-Fetch-Mode", "navigate")
                    .addHeader("Sec-Fetch-Site", "none")
                    .build()

                val response = client.newCall(request).execute()
                
                Logger.d("HTTP Response: ${response.code} ${response.message}")
                Logger.d("Response headers: ${response.headers}")
                
                if (!response.isSuccessful) {
                    val errorMessage = when (response.code) {
                        404 -> "Page not found"
                        403 -> "Access forbidden"
                        500 -> "Server error"
                        else -> "HTTP ${response.code}: ${response.message}"
                    }
                    throw IOException(errorMessage)
                }

                val responseBody = response.body ?: throw IOException("Empty response body")
                val contentType = response.header("Content-Type") ?: ""
                val charset = extractCharset(contentType)
                
                Logger.d("Content-Type: $contentType")
                Logger.d("Detected charset: $charset")
                Logger.d("Response body size: ${responseBody.contentLength()} bytes")
                
                // Проверяем, сжат ли контент
                val contentEncoding = response.header("Content-Encoding")
                Logger.d("Content-Encoding: $contentEncoding")
                
                // Сначала читаем как байты для правильной обработки кодировки
                val bytes = responseBody.bytes()
                Logger.d("Response bytes size: ${bytes.size}")
                
                // Если контент сжат gzip, нужно его распаковать
                val finalBytes = if (contentEncoding == "gzip") {
                    try {
                        val gzipInputStream = java.util.zip.GZIPInputStream(bytes.inputStream())
                        gzipInputStream.readBytes()
                    } catch (e: Exception) {
                        Logger.e("Failed to decompress gzip content", e)
                        bytes
                    }
                } else {
                    bytes
                }
                Logger.d("Final bytes size after decompression: ${finalBytes.size}")
                
                // Определяем кодировку из Content-Type
                val detectedCharset = charset ?: Charset.forName("UTF-8")
                Logger.d("Using charset: $detectedCharset")
                
                // Читаем HTML с правильной кодировкой
                val html = try {
                    String(finalBytes, detectedCharset)
                } catch (e: Exception) {
                    Logger.e("Failed to decode with charset $detectedCharset", e)
                    // Fallback: пробуем UTF-8
                    try {
                        String(finalBytes, Charset.forName("UTF-8"))
                    } catch (e2: Exception) {
                        Logger.e("Failed to decode with UTF-8", e2)
                        // Последний fallback: ISO-8859-1
                        String(finalBytes, Charset.forName("ISO-8859-1"))
                    }
                }
                
                // Дополнительная проверка и исправление кодировки
                val finalHtml = detectAndFixEncoding(html, finalBytes)
                
                Logger.d("Received HTML content: ${finalHtml.length} characters")
                if (finalHtml.length > 0) {
                    Logger.d("HTML preview: ${finalHtml.take(200)}...")
                } else {
                    Logger.e("Empty HTML content received!")
                }

                if (finalHtml.length > AppConfig.MAX_FILE_SIZE) {
                    throw IOException("Page too large: ${finalHtml.length} bytes")
                }

                val title = extractTitle(finalHtml)
                Logger.d("Successfully fetched page: $title")

                WebPageResult(
                    url = url,
                    title = title,
                    html = finalHtml,
                    success = true
                )

            } catch (e: Exception) {
                Logger.e("Failed to fetch web page: $url", e)
                WebPageResult(
                    url = url,
                    title = "Error",
                    html = "",
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun extractTitle(html: String): String {
        return try {
            val doc = Jsoup.parse(html, "UTF-8")
            val title = doc.select("title").first()?.text()
            
            if (title.isNullOrBlank()) {
                // Попробуем найти заголовок в h1
                val h1 = doc.select("h1").first()?.text()
                if (!h1.isNullOrBlank()) {
                    h1
                } else {
                    "Untitled Page"
                }
            } else {
                title.trim()
            }
        } catch (e: Exception) {
            Logger.e("Failed to extract title from HTML", e)
            "Untitled Page"
        }
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }

    private fun extractCharset(contentType: String): Charset? {
        return try {
            if (contentType.isBlank()) {
                Logger.d("No Content-Type header, using default charset")
                return Charset.forName("UTF-8")
            }
            
            val charsetPattern = Regex("charset=([^;\\s]+)", RegexOption.IGNORE_CASE)
            val match = charsetPattern.find(contentType)
            if (match != null) {
                val charsetName = match.groupValues[1].trim().removeSurrounding("\"", "\"")
                Logger.d("Found charset in Content-Type: $charsetName")
                
                // Нормализуем названия кодировок
                val normalizedCharset = when (charsetName.lowercase()) {
                    "utf-8", "utf8" -> "UTF-8"
                    "windows-1251", "cp1251" -> "windows-1251"
                    "iso-8859-1", "latin1" -> "ISO-8859-1"
                    "koi8-r" -> "KOI8-R"
                    else -> charsetName
                }
                
                Charset.forName(normalizedCharset)
            } else {
                Logger.d("No charset found in Content-Type, using UTF-8")
                Charset.forName("UTF-8")
            }
        } catch (e: Exception) {
            Logger.e("Failed to extract charset from: $contentType", e)
            Logger.d("Falling back to UTF-8")
            Charset.forName("UTF-8")
        }
    }

    private fun detectAndFixEncoding(html: String, originalBytes: ByteArray): String {
        return try {
            // Ищем мета-теги с кодировкой
            val metaCharsetPattern = Regex("<meta[^>]*charset\\s*=\\s*[\"']?([^\"'>\\s]+)[\"']?[^>]*>", RegexOption.IGNORE_CASE)
            val metaHttpEquivPattern = Regex("<meta[^>]*http-equiv\\s*=\\s*[\"']?content-type[\"']?[^>]*content\\s*=\\s*[\"']?[^\"'>]*charset\\s*=\\s*([^\"'>\\s;]+)[^>]*>", RegexOption.IGNORE_CASE)
            
            val metaCharsetMatch = metaCharsetPattern.find(html)
            val metaHttpEquivMatch = metaHttpEquivPattern.find(html)
            
            val detectedCharset = metaCharsetMatch?.groupValues?.get(1) ?: metaHttpEquivMatch?.groupValues?.get(1)
            
            // Проверяем, не содержит ли HTML кракозябры (признак неправильной кодировки)
            if (containsEncodingIssues(html)) {
                Logger.d("Detected encoding issues, attempting to fix")
                
                if (detectedCharset != null) {
                    Logger.d("Found charset in HTML meta tag: $detectedCharset")
                    // Пробуем перекодировать с найденной кодировкой
                    return try {
                        val correctedHtml = String(originalBytes, Charset.forName(detectedCharset))
                        Logger.d("Successfully re-encoded with charset: $detectedCharset")
                        correctedHtml
                    } catch (e: Exception) {
                        Logger.e("Failed to re-encode with detected charset: $detectedCharset", e)
                        html
                    }
                } else {
                    // Пробуем разные кодировки
                    val commonCharsets = listOf("UTF-8", "windows-1251", "ISO-8859-1", "KOI8-R")
                    for (charsetName in commonCharsets) {
                        try {
                            val testHtml = String(originalBytes, Charset.forName(charsetName))
                            if (!containsEncodingIssues(testHtml)) {
                                Logger.d("Successfully fixed encoding with charset: $charsetName")
                                return testHtml
                            }
                        } catch (e: Exception) {
                            Logger.e("Failed to test charset: $charsetName", e)
                        }
                    }
                }
            }
            
            html
        } catch (e: Exception) {
            Logger.e("Error in detectAndFixEncoding", e)
            html
        }
    }

    private fun containsEncodingIssues(text: String): Boolean {
        // Простая проверка на наличие кракозябр
        val suspiciousPatterns = listOf(
            "Ð", "Ñ", "Ð°", "Ð±", "Ð²", "Ð³", "Ð´", "Ðµ", "Ð¶", "Ð·", "Ð¸", "Ð¹", "Ðº", "Ð»", "Ð¼", "Ð½", "Ð¾", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñƒ", "Ñ„", "Ñ…", "Ñ†", "Ñ‡", "Ñˆ", "Ñ‰", "ÑŠ", "Ñ‹", "ÑŒ", "ÑŽ", "Ñ"
        )
        
        return suspiciousPatterns.any { pattern -> text.contains(pattern) }
    }
}

data class WebPageResult(
    val url: String,
    val title: String,
    val html: String,
    val success: Boolean,
    val error: String? = null
)
