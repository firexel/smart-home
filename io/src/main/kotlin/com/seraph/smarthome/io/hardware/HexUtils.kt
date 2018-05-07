package com.seraph.smarthome.io.hardware

import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

fun byteArrayOf(vararg elements: Int): ByteArray = elements.map { it.toByte() }.toByteArray()

fun ByteArray.asHexString(): String {
    var str = ""
    this.forEach {
        var string = Integer.toHexString(it + 0)
        if (string.length == 1) string = "0" + string
        else if (string.length > 2) string = string.takeLast(2)
        str += string + " "
    }
    return str
}

fun Short.shr(shift: Int): Int = this.toInt() shr shift

class BinaryInputStream(private val wrapped: InputStream) : InputStream() {
    override fun read(): Int = wrapped.read()
    override fun read(p0: ByteArray?): Int = wrapped.read(p0)
    override fun read(p0: ByteArray?, p1: Int, p2: Int): Int = wrapped.read(p0, p1, p2)

    override fun skip(p0: Long): Long = wrapped.skip(p0)
    override fun available(): Int = wrapped.available()
    override fun reset() = wrapped.reset()
    override fun close() = wrapped.close()
    override fun mark(p0: Int) = wrapped.mark(p0)
    override fun markSupported(): Boolean = wrapped.markSupported()

    fun readByte(): Byte = read().toByte()

    fun readByteAsBits(): BooleanArray {
        val byte = readByte()
        return BooleanArray(8) { index -> byte and (1 shl index).toByte() > 0 }
    }

    fun readShort(endianness: Endianness = Endianness.MSB_LAST): Short {
        return readUshort(endianness).toShort()
    }

    fun readUshort(endianness: Endianness = Endianness.MSB_LAST): Int {
        val lsb = read()
        val msb = read()
        return when (endianness) {
            Endianness.MSB_LAST -> lsb or (msb shl 8)
            Endianness.MSB_FIRST -> msb or (lsb shl 8)
        }
    }
}

class BinaryOutputStream(private val wrapped: OutputStream) : OutputStream() {

    private val crc = Crc16()

    val checksum
        get() = crc.checksum

    override fun write(p0: Int) {
        crc.update(p0.toByte())
        wrapped.write(p0)
    }

    override fun write(p0: ByteArray?) {
        p0?.forEach { crc.update(it) }
        wrapped.write(p0)
    }

    override fun write(p0: ByteArray?, start: Int, count: Int) {
        p0?.drop(start)?.take(count)?.forEach { crc.update(it) }
        wrapped.write(p0, start, count)
    }

    override fun flush() = wrapped.flush()
    override fun close() = wrapped.close()

    fun write(b: Byte) = write(b.toInt())

    fun write(short: Short, endianness: Endianness = Endianness.MSB_LAST) {
        val msb = short.and(0xff00.toShort()).shr(8).toByte()
        val lsb = short.and(0xff).toInt().toByte()
        when (endianness) {
            Endianness.MSB_FIRST -> {
                write(msb)
                write(lsb)
            }
            Endianness.MSB_LAST -> {
                write(lsb)
                write(msb)
            }
        }
    }
}

enum class Endianness {
    MSB_FIRST, MSB_LAST
}
