package com.openai.tvengineering.core

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager

data class CarrierRange(val minHz: Int, val maxHz: Int)

data class IrHardwareStatus(
    val featureDeclaredByDevice: Boolean,
    val emitterPresent: Boolean,
    val ranges: List<CarrierRange>
)

class IrTransmitter(context: Context) {
    private val appContext = context.applicationContext
    private val manager: ConsumerIrManager? =
        appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun status(): IrHardwareStatus {
        val feature = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
        val present = try { manager?.hasIrEmitter() == true } catch (_: Throwable) { false }
        val ranges = try {
            manager?.carrierFrequencies?.map { CarrierRange(it.minFrequency, it.maxFrequency) }.orEmpty()
        } catch (_: Throwable) { emptyList() }
        return IrHardwareStatus(feature, present, ranges)
    }

    fun transmit(carrierHz: Int, pattern: IntArray): Result<Unit> = runCatching {
        val m = manager ?: error("Consumer IR service is unavailable on this device")
        check(m.hasIrEmitter()) { "This Android device does not report an IR emitter" }
        require(carrierHz in 20_000..100_000) { "Carrier frequency looks invalid: $carrierHz Hz" }
        require(pattern.isNotEmpty()) { "IR pattern is empty" }
        require(pattern.all { it > 0 }) { "Every mark/space duration must be positive" }
        // Android's API documents a maximum pattern duration shorter than 2 seconds.
        require(pattern.sumOf { it.toLong() } < 2_000_000L) { "IR pattern must be shorter than 2 seconds" }

        val ranges = try { m.carrierFrequencies?.toList().orEmpty() } catch (_: Throwable) { emptyList() }
        if (ranges.isNotEmpty()) {
            require(ranges.any { carrierHz in it.minFrequency..it.maxFrequency }) {
                "Carrier $carrierHz Hz is outside the emitter's reported ranges"
            }
        }
        m.transmit(carrierHz, pattern)
    }
}
