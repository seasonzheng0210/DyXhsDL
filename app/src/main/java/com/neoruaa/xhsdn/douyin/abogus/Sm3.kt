package com.neoruaa.xhsdn.douyin.abogus

/**
 * 国密 SM3 哈希（GB/T 32905-2016），纯 Kotlin 无依赖。
 *
 * 用途：a_bogus 对参数串做"双重 SM3"（先 SM3(message+"cus")，再把 32 字节摘要
 * 作为输入再 SM3 一次），与 Python 侧 gmssl.sm3_hash 输出必须逐字节一致。
 */
internal object Sm3 {

    private val IV = intArrayOf(
        0x7380166f.toInt(), 0x4914b2b9.toInt(), 0x172442d7.toInt(), 0xda8a0600.toInt(),
        0xa96f30bc.toInt(), 0x163138aa.toInt(), 0xe38dee4d.toInt(), 0xb0fb0e4e.toInt()
    )

    /** 计算 SM3 摘要，返回 32 字节大端结果。 */
    fun hash(data: ByteArray): ByteArray {
        val padded = pad(data)
        val v = IV.copyOf()

        for (base in padded.indices step 64) {
            // 1) 消息扩展
            val w = IntArray(68)
            for (t in 0 until 16) {
                w[t] = ((padded[base + t * 4].toInt() and 0xff) shl 24) or
                        ((padded[base + t * 4 + 1].toInt() and 0xff) shl 16) or
                        ((padded[base + t * 4 + 2].toInt() and 0xff) shl 8) or
                        (padded[base + t * 4 + 3].toInt() and 0xff)
            }
            for (t in 16 until 68) {
                val x = w[t - 16] xor w[t - 9] xor rotl(w[t - 3], 15)
                w[t] = p1(x) xor rotl(w[t - 13], 7) xor w[t - 6]
            }
            val wp = IntArray(64)
            for (t in 0 until 64) wp[t] = w[t] xor w[t + 4]

            // 2) 压缩
            var a = v[0]; var b = v[1]; var c = v[2]; var d = v[3]
            var e = v[4]; var f = v[5]; var g = v[6]; var h = v[7]
            for (t in 0 until 64) {
                val tj = if (t < 16) 0x79cc4519 else 0x7a879d8a
                val ss1 = rotl(rotl(a, 12) + e + rotl(tj, t), 7)
                val ss2 = ss1 xor rotl(a, 12)
                val tt1 = ff(t, a, b, c) + d + ss2 + wp[t]
                val tt2 = gg(t, e, f, g) + h + ss1 + w[t]
                d = c
                c = rotl(b, 9)
                b = a
                a = tt1
                h = g
                g = rotl(f, 19)
                f = e
                e = p0(tt2)
            }
            v[0] = v[0] xor a; v[1] = v[1] xor b; v[2] = v[2] xor c; v[3] = v[3] xor d
            v[4] = v[4] xor e; v[5] = v[5] xor f; v[6] = v[6] xor g; v[7] = v[7] xor h
        }

        // 3) 输出 32 字节
        val out = ByteArray(32)
        for (i in 0 until 8) {
            out[i * 4] = (v[i] ushr 24).toByte()
            out[i * 4 + 1] = (v[i] ushr 16).toByte()
            out[i * 4 + 2] = (v[i] ushr 8).toByte()
            out[i * 4 + 3] = v[i].toByte()
        }
        return out
    }

    private fun pad(data: ByteArray): ByteArray {
        val bitLen = data.size.toLong() * 8
        // 填充后 (total - 8) % 64 == 56
        var tail = (data.size + 1) % 64
        val zeroBytes = if (tail <= 56) 56 - tail else 120 - tail
        val out = ByteArray(data.size + 1 + zeroBytes + 8)
        System.arraycopy(data, 0, out, 0, data.size)
        out[data.size] = 0x80.toByte()
        for (i in 0 until 8) {
            out[out.size - 1 - i] = (bitLen ushr (8 * i)).toByte()
        }
        return out
    }

    private fun rotl(x: Int, r: Int): Int {
        val n = r and 31
        if (n == 0) return x
        return (x shl n) or (x ushr (32 - n))
    }

    private fun p0(x: Int) = x xor rotl(x, 9) xor rotl(x, 17)

    private fun p1(x: Int) = x xor rotl(x, 15) xor rotl(x, 23)

    private fun ff(t: Int, x: Int, y: Int, z: Int): Int =
        if (t < 16) x xor y xor z
        else (x and y) or (x and z) or (y and z)

    private fun gg(t: Int, x: Int, y: Int, z: Int): Int =
        if (t < 16) x xor y xor z
        else (x and y) or (x.inv() and z)
}
