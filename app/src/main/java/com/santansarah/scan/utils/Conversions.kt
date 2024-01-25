package com.santansarah.scan.utils

import android.os.SystemClock
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.BitSet
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit


/**
 * Use “%02x” to convert the given byte to its corresponding hex value.
 * Moreover, it pads the hex value with a leading zero if necessary.
 */
fun ByteArray.toHex(): String =
    "0x" + joinToString(separator = "") { eachByte -> "%02X".format(eachByte).uppercase() }

fun ByteArray.print(): String =
    joinToString(separator = ",") { eachByte -> eachByte.toInt().toString() }

fun Int.toHex(): String =
    "0x%04X".format(this)

fun Int.toHex2(): String =
    "%02X".format(this)

fun ByteArray.bitsToHex(): String {
    val bitSet = BitSet.valueOf(this)
    Timber.d(bitSet.toString())
    Timber.d(bitSet[bitSet.size()].toString())
    return bitSet.toLongArray().toHex()
}

fun ByteArray.bits(): String {
    val bitSet = BitSet.valueOf(this)
    val stringBuilder = StringBuilder()
    val bitRange = 0..15
    for (index in bitRange) {
        val curBit = if (index <= bitSet.size())
            bitSet.get(index)
        else
            false

        stringBuilder.append(if (curBit) '1' else '0')
    }
    return stringBuilder.reverse().toString()
}

fun ByteArray.toBinaryString(): String {
    return this.joinToString(" ") {
        //Integer.toBinaryString(it.toInt())
        toBinary(it.toInt(), 8)
    }
}

fun ByteArray.toFloat(): String {
    return ByteBuffer.wrap(this)
        .order(ByteOrder.LITTLE_ENDIAN).
    float.toString()
}

private fun toBinary(number: Int, length: Int): String {
    var number = number
    val stringBuilder = StringBuilder()
    for (index in 0 until length) {
        stringBuilder.append(if (number and 1 == 1) '1' else '0')
        number = number shr 1
    }
    return stringBuilder.reverse().toString()
}

fun LongArray.toHex(): String =
    joinToString(separator = "") { words -> "0x%04X".format(words.toShort()) }

fun Byte.toHex(): String = "%02X".format(this)

fun String.decodeHex(): ByteArray {
    with (this.substringAfter("0x")) {
        require(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { textHexadecimal : String -> textHexadecimal.toInt(16).toByte() }
            .toByteArray()
        //.toString(Charsets.ISO_8859_1)
    }
}

fun ByteArray.decodeSkipUnreadable(): String {
    val badChars = '\uFFFD'

    this.forEach { byte ->
        Timber.d(this.indexOf(byte).toString() + ": " + byte.toInt().toString())
    }

    val newString = this.decodeToString().filter {
        it != badChars
    }

        /*.filter {
        it.code > 0
    }*/

    return newString
}


// "^([A-F0-9]{4}|[A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12})$"
// (4 or 8 in beginning) yyyyxxxx-0000-1000-8000-00805f9b34fb
// only replace first four 0's if padded.
fun UUID.toGss() =
    this.toString()
        .replaceFirst(Regex("^0+(?!$)"), "")
        .replace("-0000-1000-8000-00805f9b34fb", "")
        .uppercase()

fun Long.toMillis() =
    System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(
        SystemClock.elapsedRealtimeNanos() - this, TimeUnit.NANOSECONDS
    )

fun Long.toDate() =
    SimpleDateFormat("MM/dd/yy h:mm:ss ", Locale.US).format(Date(this))




