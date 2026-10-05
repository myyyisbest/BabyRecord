package com.babyrecord.app.data

import java.security.SecureRandom

/**
 * 简易 ULID 生成器（不引第三方库）。
 *
 * 格式：26 字符 Crockford Base32（去掉易混淆的 I L O U）
 * - 前 10 字符：48bit 毫秒时间戳（高位补 2bit 0），保证时间有序
 * - 后 16 字符：80bit 随机数，同一毫秒内不冲突
 *
 * 线程安全：generate() 加 synchronized，避免多线程生成重复随机段
 */
object Ulid {
    private val CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private val random = SecureRandom()

    @Synchronized
    fun generate(): String {
        val sb = StringBuilder(26)

        // 48bit 时间戳 → 10 个 5bit 字符（从低位往高位取，高位即最老的部分为 0）
        var t = System.currentTimeMillis() and 0xFFFFFFFFFFFFL
        repeat(10) {
            sb.insert(0, CHARS[(t and 0x1FL).toInt()])
            t = t ushr 5
        }

        // 80bit 随机 → 16 个 5bit 字符（两个 64bit 随机数拼出，各取低 40bit）
        var hi = random.nextLong()
        var lo = random.nextLong()
        repeat(8) {
            sb.append(CHARS[(hi and 0x1FL).toInt()])
            hi = hi ushr 5
        }
        repeat(8) {
            sb.append(CHARS[(lo and 0x1FL).toInt()])
            lo = lo ushr 5
        }
        return sb.toString()
    }
}
