package com.openai.tvengineering.data

enum class ProtocolType { NEC, RAW, PULSE_DISTANCE_32, PRONTO }

data class IrCommandSpec(
    val key: String,
    val label: String,
    val carrierHz: Int = 38_000,
    val protocol: ProtocolType,
    val address: Int? = null,
    val command: Int? = null,
    val dataHex: String? = null,
    val pattern: IntArray? = null,
    val pronto: String? = null,
    val lsbFirstPerByte: Boolean = true,
    val dangerous: Boolean = false,
    val engineering: Boolean = false
)

data class TvProfile(
    val id: String,
    val name: String,
    val brand: String,
    val model: String,
    val notes: String = "",
    val commands: List<IrCommandSpec>
) {
    fun command(key: String): IrCommandSpec? = commands.firstOrNull { it.key.equals(key, ignoreCase = true) }
}

data class LogEntry(val time: String, val message: String, val ok: Boolean = true)
