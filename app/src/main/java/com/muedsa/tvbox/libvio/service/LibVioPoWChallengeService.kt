package com.muedsa.tvbox.libvio.service

import com.muedsa.tvbox.libvio.model.ChallengeParams
import com.muedsa.tvbox.tool.SerializableCookie
import com.muedsa.tvbox.tool.SharedCookieSaver
import com.muedsa.tvbox.tool.stringBody
import okhttp3.Response
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class LibVioPoWChallengeService(
    private val cookieSaver: SharedCookieSaver,
) {
    fun challenge(
        resp: Response
    ) {
        if (resp.code != 403) return
        val body = resp.stringBody()
        if (!body.contains(CHALLENGE_TITLE)) throw RuntimeException("403 $body")
        val params = parse(body)
        Timber.i("challenge $params")
        val nonce = findNonce(params)
        Timber.i("challenge nonce=$nonce")
        val cookieValue = "${params.ts}_${params.mode}_${nonce}_${params.sig}"
        cookieSaver.save(
            cookie = SerializableCookie(
                name = COOKIE_NAME,
                value = cookieValue,
                expiresAt = 253402300799999L,
                domain = resp.request.url.host,
                path = "/",
                secure = resp.request.url.isHttps,
                httpOnly = false,
                persistent = false,
                hostOnly = true,
            )
        )
    }

    companion object {
        const val CHALLENGE_TITLE = "Checking your browser"

        // \b 保证不会匹配到 xTS、MYSIG 之类的子串
        private val TS_RE = Regex("\\bTS\\s*=\\s*\"([^\"]*)\"")
        private val SIG_RE = Regex("\\bSIG\\s*=\\s*\"([0-9a-fA-F]+)\"")   // SIG 是 64 位 hex
        private val DIFF_RE = Regex("\\bDIFF\\s*=\\s*\"([^\"]*)\"")
        private val MODE_RE = Regex("\\bMODE\\s*=\\s*\"([^\"]*)\"")

        fun parse(html: String): ChallengeParams {
            fun pick(re: Regex, name: String): String =
                re.find(html)?.groupValues?.get(1)
                    ?: throw IllegalArgumentException("PoW挑战失败，未在页面中找到参数 $name")

            return ChallengeParams(
                ts = pick(TS_RE, "TS"),
                sig = pick(SIG_RE, "SIG"),
                diff = pick(DIFF_RE, "DIFF"),
                mode = pick(MODE_RE, "MODE")
            )
        }

        private const val COOKIE_NAME = "__cdn_pow"
        private const val EXPECT = 65_536L
        private const val MAX_ITER = EXPECT * 32

        private val HEX_CHARS = "0123456789abcdef".toCharArray()

        /** 判断 SHA-256 的十六进制串是否以 prefix 开头 */
        private fun matchesPrefix(digest: ByteArray, prefix: String): Boolean {
            val neededBytes = prefix.length / 2          // "0000" -> 2 字节
            for (k in 0 until neededBytes) {
                val b = digest[k].toInt() and 0xFF
                if (HEX_CHARS[b ushr 4] != prefix[k * 2]) return false
                if (HEX_CHARS[b and 0xF] != prefix[k * 2 + 1]) return false
            }
            return true
        }

        fun findNonce(params: ChallengeParams): String {
            val nThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val found = AtomicBoolean(false)
            val result = AtomicReference<String?>(null)

            val workers = (0 until nThreads).map { id ->
                thread(name = "pow-$id", isDaemon = true) {
                    val md = MessageDigest.getInstance("SHA-256")   // 每线程一个实例
                    var i = id.toLong()
                    while (!found.get() && i < MAX_ITER) {
                        val digest = md.digest((params.sig + i).toByteArray(Charsets.US_ASCII))
                        if (matchesPrefix(digest, params.diff)) {
                            if (found.compareAndSet(false, true)) {
                                result.set(i.toString())
                            }
                            return@thread
                        }
                        i += nThreads
                    }
                }
            }
            workers.forEach { it.join() }
            return result.get() ?: error("未在 $MAX_ITER 次迭代内找到 nonce")
        }

//        suspend fun findNonceCoroutine(params: ChallengeParams): String = coroutineScope {
//            val nThreads = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
//            val result   = CompletableDeferred<String>()
//
//            val jobs = (0 until nThreads).map { id ->
//                launch(Dispatchers.IO) {
//                    val md = MessageDigest.getInstance("SHA-256")
//                    var i = id.toLong()
//                    while (isActive && !result.isCompleted && i < MAX_ITER) {
//                        if (matchesPrefix(md.digest((params.sig + i).toByteArray()), params.diff)) {
//                            result.complete(i.toString())
//                            return@launch
//                        }
//                        i += nThreads
//                    }
//                }
//            }
//            try {
//                result.await()
//            } finally {
//                jobs.forEach { it.cancel() }
//            }
//        }
    }
}