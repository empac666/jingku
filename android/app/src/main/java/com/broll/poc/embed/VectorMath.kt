package com.broll.poc.embed

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

fun truncateNormalize(vector: FloatArray, dim: Int): FloatArray {
    val n = minOf(dim, vector.size)
    val out = FloatArray(n)
    var sum = 0.0
    for (i in 0 until n) {
        val v = vector[i].toDouble()
        out[i] = vector[i]
        sum += v * v
    }
    val norm = sqrt(sum)
    if (norm > 1e-12) {
        for (i in 0 until n) {
            out[i] = (out[i] / norm).toFloat()
        }
    }
    return out
}

fun cosine(a: FloatArray, b: FloatArray): Float {
    val n = minOf(a.size, b.size)
    var dot = 0.0
    var na = 0.0
    var nb = 0.0
    for (i in 0 until n) {
        val x = a[i].toDouble()
        val y = b[i].toDouble()
        dot += x * y
        na += x * x
        nb += y * y
    }
    if (na <= 1e-12 || nb <= 1e-12) return 0f
    return (dot / (sqrt(na) * sqrt(nb))).toFloat()
}

fun floatsToBytes(vector: FloatArray): ByteArray {
    val buf = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (v in vector) buf.putFloat(v)
    return buf.array()
}

fun bytesToFloats(bytes: ByteArray): FloatArray {
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val out = FloatArray(bytes.size / 4)
    for (i in out.indices) out[i] = buf.float
    return out
}
