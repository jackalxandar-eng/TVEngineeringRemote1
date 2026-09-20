package com.openai.tvengineering.core

import kotlin.math.roundToInt

/** Encoders convert protocol-level values to Android ConsumerIrManager mark/space arrays (microseconds). */
object IrProtocols {
    /** Standard 8-bit NEC address + inverted address + 8-bit command + inverted command. */
    fun nec(address: Int, command: Int): IntArray {
        require(address in 0..255) { "NEC address must be 0..255" }
        require(command in 0..255) { "NEC command must be 0..255" }
        val bytes = intArrayOf(address, address xor 0xFF, command, command xor 0xFF)
        val out = ArrayList<Int>(67)
        out += 9000
        out += 4500
        for (b in bytes) {
            for (bit in 0 until 8) {
                out += 560
                out += if (((b shr bit) and 1) == 1) 1690 else 560
            }
        }
        out += 560
        return out.toIntArray()
    }

    /** NEC repeat frame, useful for long-press behavior on devices that support it. */
    fun necRepeat(): IntArray = intArrayOf(9000, 2250, 560)

    /**
     * Generic pulse-distance 32-bit generator for service manuals that document a full 32-bit value.
     * [lsbFirstPerByte] matches many consumer IR conventions; disable when a manual explicitly shows MSB-first.
     */
    fun pulseDistance32(
        hex: String,
        leaderMark: Int = 9000,
        leaderSpace: Int = 4500,
        bitMark: Int = 560,
        zeroSpace: Int = 560,
        oneSpace: Int = 1690,
        trailerMark: Int = 560,
        lsbFirstPerByte: Boolean = true
    ): IntArray {
        val normalized = hex.removePrefix("0x").replace(" ", "").padStart(8, '0')
        require(normalized.length == 8) { "Expected exactly 32 bits / 8 hex digits" }
        val bytes = normalized.chunked(2).map { it.toInt(16) }
        val out = ArrayList<Int>(67)
        out += leaderMark
        out += leaderSpace
        for (b in bytes) {
            val bits = if (lsbFirstPerByte) (0 until 8) else (7 downTo 0)
            for (bit in bits) {
                out += bitMark
                out += if (((b shr bit) and 1) == 1) oneSpace else zeroSpace
            }
        }
        out += trailerMark
        return out.toIntArray()
    }

    /** Convert Pronto learned code (type 0000) to carrier + alternating microsecond durations. */
    fun pronto0000(pronto: String): Pair<Int, IntArray> {
        val words = pronto.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.map { it.toInt(16) }
        require(words.size >= 6) { "Pronto code is too short" }
        require(words[0] == 0x0000) { "Only learned Pronto type 0000 is supported" }
        val freqWord = words[1]
        require(freqWord > 0) { "Invalid Pronto frequency word" }
        val sequencePairs = words[2] + words[3]
        val expectedDurations = sequencePairs * 2
        require(words.size >= 4 + expectedDurations) { "Pronto duration count does not match header" }
        val periodMicros = freqWord * 0.241246
        val carrier = (1_000_000.0 / periodMicros).roundToInt()
        val pattern = words.drop(4).take(expectedDurations)
            .map { (it * periodMicros).roundToInt().coerceAtLeast(1) }
            .toIntArray()
        return carrier to pattern
    }

    fun parseRawMicros(text: String): IntArray = text
        .split(',', ' ', '\n', '\t', ';')
        .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
        .filter { it > 0 }
        .toIntArray()
}
