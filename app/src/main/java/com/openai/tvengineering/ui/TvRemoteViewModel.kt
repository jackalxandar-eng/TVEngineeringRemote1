package com.openai.tvengineering.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openai.tvengineering.core.IrProtocols
import com.openai.tvengineering.core.IrTransmitter
import com.openai.tvengineering.data.IrCommandSpec
import com.openai.tvengineering.data.LogEntry
import com.openai.tvengineering.data.ProfileRepository
import com.openai.tvengineering.data.ProtocolType
import com.openai.tvengineering.data.TvProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray

class TvRemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val transmitter = IrTransmitter(app)
    private val prefs = app.getSharedPreferences("tv_engineer_profiles", Application.MODE_PRIVATE)
    private val starterIds = ProfileRepository.starterProfiles().map { it.id }.toSet()

    private fun loadStoredProfiles(): List<TvProfile> = runCatching {
        val raw = prefs.getString("profiles_json", "[]") ?: "[]"
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) add(ProfileRepository.parse(arr.getString(i)))
        }
    }.getOrDefault(emptyList())

    private fun initialProfiles(): List<TvProfile> {
        val starters = ProfileRepository.starterProfiles()
        val stored = loadStoredProfiles()
        return starters + stored.filterNot { saved -> starters.any { it.id == saved.id } }
    }

    var hardware by mutableStateOf(transmitter.status())
        private set
    var profiles by mutableStateOf(initialProfiles())
        private set
    var activeProfile by mutableStateOf(
        profiles.firstOrNull { it.id == prefs.getString("active_profile_id", null) } ?: profiles.first()
    )
        private set
    var engineerMode by mutableStateOf(false)
        private set
    var logs by mutableStateOf(listOf<LogEntry>())
        private set
    var pendingDangerous by mutableStateOf<IrCommandSpec?>(null)
        private set

    init {
        log("TV Engineer Remote ready")
        log(if (hardware.emitterPresent) "IR emitter detected" else "No IR emitter reported by this device", hardware.emitterPresent)
    }

    private fun time(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    fun log(message: String, ok: Boolean = true) {
        logs = (listOf(LogEntry(time(), message, ok)) + logs).take(300)
    }

    fun refreshHardware() {
        hardware = transmitter.status()
        log("Hardware status refreshed: emitter=${hardware.emitterPresent}", hardware.emitterPresent)
    }

    fun setEngineerMode(enabled: Boolean) {
        engineerMode = enabled
        log("Engineer mode ${if (enabled) "enabled" else "disabled"}")
    }

    fun selectProfile(profile: TvProfile) {
        activeProfile = profile
        prefs.edit().putString("active_profile_id", profile.id).apply()
        log("Selected profile: ${profile.brand} ${profile.model}")
    }

    fun importProfile(json: String) {
        runCatching { ProfileRepository.parse(json) }
            .onSuccess { parsed ->
                val p = if (parsed.id in starterIds) parsed.copy(id = "imported-${System.currentTimeMillis()}") else parsed
                profiles = (profiles.filterNot { it.id == p.id } + p)
                activeProfile = p
                persistProfiles()
                log("Imported profile: ${p.name}")
            }
            .onFailure { log("Profile import failed: ${it.message}", false) }
    }


    private fun persistProfiles() {
        val arr = JSONArray()
        profiles.filterNot { it.id in starterIds }.forEach { arr.put(ProfileRepository.toJson(it)) }
        prefs.edit()
            .putString("profiles_json", arr.toString())
            .putString("active_profile_id", activeProfile.id)
            .apply()
    }

    fun saveCommand(spec: IrCommandSpec) {
        val base = if (activeProfile.id in starterIds) {
            activeProfile.copy(
                id = "custom-${System.currentTimeMillis()}",
                name = "${activeProfile.name} (Custom)"
            )
        } else activeProfile
        val updated = base.copy(commands = base.commands.filterNot { it.key.equals(spec.key, true) } + spec)
        profiles = if (base.id == activeProfile.id) {
            profiles.map { if (it.id == base.id) updated else it }
        } else {
            profiles + updated
        }
        activeProfile = updated
        persistProfiles()
        log("Saved command ${spec.key} to ${updated.name}")
    }

    fun createProfile(name: String, brand: String, model: String) {
        if (name.isBlank() || brand.isBlank()) {
            log("Profile name and brand are required", false)
            return
        }
        val p = TvProfile(
            id = "custom-${System.currentTimeMillis()}",
            name = name.trim(),
            brand = brand.trim(),
            model = model.trim().ifBlank { "Unknown" },
            notes = "Created on device. Add verified IR commands from Engineer Mode.",
            commands = emptyList()
        )
        profiles = profiles + p
        activeProfile = p
        persistProfiles()
        log("Created profile: ${p.name}")
    }

    fun deleteActiveProfile() {
        if (activeProfile.id in starterIds) {
            log("Built-in templates cannot be deleted", false)
            return
        }
        val deleted = activeProfile.name
        profiles = profiles.filterNot { it.id == activeProfile.id }
        activeProfile = profiles.first()
        persistProfiles()
        log("Deleted profile: $deleted")
    }

    fun exportActiveProfile(): String = ProfileRepository.toJson(activeProfile)

    fun requestKey(key: String) {
        val spec = activeProfile.command(key)
        if (spec == null) {
            log("No command mapped for $key in ${activeProfile.name}", false)
            return
        }
        requestCommand(spec)
    }

    fun requestCommand(spec: IrCommandSpec) {
        if (spec.engineering && !engineerMode) {
            log("Blocked engineering command '${spec.label}' while Engineer Mode is off", false)
            return
        }
        if (spec.dangerous) {
            pendingDangerous = spec
            return
        }
        send(spec)
    }

    fun confirmDangerous() {
        val spec = pendingDangerous ?: return
        pendingDangerous = null
        send(spec, confirmed = true)
    }

    fun cancelDangerous() { pendingDangerous = null }

    fun sendRaw(carrierHz: Int, pattern: IntArray, label: String = "RAW") {
        if (!engineerMode) {
            log("RAW transmission requires Engineer Mode", false)
            return
        }
        transmit(carrierHz, pattern, label)
    }

    fun sendNec(carrierHz: Int, address: Int, command: Int) {
        if (!engineerMode) {
            log("NEC laboratory transmission requires Engineer Mode", false)
            return
        }
        runCatching { IrProtocols.nec(address, command) }
            .onSuccess { transmit(carrierHz, it, "NEC addr=0x${address.toString(16)} cmd=0x${command.toString(16)}") }
            .onFailure { log(it.message ?: "NEC encode error", false) }
    }

    fun sendHex32(carrierHz: Int, hex: String, lsbFirst: Boolean) {
        if (!engineerMode) {
            log("32-bit pulse generator requires Engineer Mode", false)
            return
        }
        runCatching { IrProtocols.pulseDistance32(hex, lsbFirstPerByte = lsbFirst) }
            .onSuccess { transmit(carrierHz, it, "Pulse32 $hex") }
            .onFailure { log(it.message ?: "Pulse32 encode error", false) }
    }

    fun sendPronto(pronto: String) {
        if (!engineerMode) {
            log("Pronto transmission requires Engineer Mode", false)
            return
        }
        runCatching { IrProtocols.pronto0000(pronto) }
            .onSuccess { (carrier, pattern) -> transmit(carrier, pattern, "Pronto 0000") }
            .onFailure { log(it.message ?: "Pronto parse error", false) }
    }

    fun runMacro(keys: List<String>, delayMs: Long) {
        if (keys.isEmpty()) return
        viewModelScope.launch {
            log("Macro started: ${keys.joinToString(" → ")}")
            for ((index, key) in keys.withIndex()) {
                val spec = activeProfile.command(key)
                if (spec == null) {
                    log("Macro skipped unmapped key: $key", false)
                } else if (spec.dangerous || spec.engineering) {
                    log("Macro blocked protected command: ${spec.label}", false)
                } else {
                    val encoded = runCatching { encode(spec) }
                    if (encoded.isSuccess) {
                        val (carrier, pattern) = encoded.getOrThrow()
                        val result = withContext(Dispatchers.IO) { transmitter.transmit(carrier, pattern) }
                        log("Macro TX ${spec.label}: ${result.fold({ "OK" }, { it.message ?: "failed" })}", result.isSuccess)
                    } else log("Macro encode failed for ${spec.label}", false)
                }
                if (index != keys.lastIndex) delay(delayMs.coerceIn(50, 10_000))
            }
            log("Macro finished")
        }
    }

    private fun send(spec: IrCommandSpec, confirmed: Boolean = false) {
        if (spec.dangerous && !confirmed) return
        runCatching { encode(spec) }
            .onSuccess { (carrier, pattern) -> transmit(carrier, pattern, spec.label) }
            .onFailure { log("Encode failed for ${spec.label}: ${it.message}", false) }
    }

    private fun encode(spec: IrCommandSpec): Pair<Int, IntArray> = when (spec.protocol) {
        ProtocolType.NEC -> spec.carrierHz to IrProtocols.nec(
            requireNotNull(spec.address) { "NEC address missing" },
            requireNotNull(spec.command) { "NEC command missing" }
        )
        ProtocolType.RAW -> spec.carrierHz to requireNotNull(spec.pattern) { "RAW pattern missing" }
        ProtocolType.PULSE_DISTANCE_32 -> spec.carrierHz to IrProtocols.pulseDistance32(
            requireNotNull(spec.dataHex) { "32-bit hex missing" },
            lsbFirstPerByte = spec.lsbFirstPerByte
        )
        ProtocolType.PRONTO -> IrProtocols.pronto0000(requireNotNull(spec.pronto) { "Pronto data missing" })
    }

    private fun transmit(carrierHz: Int, pattern: IntArray, label: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { transmitter.transmit(carrierHz, pattern) }
            result.onSuccess { log("TX $label @ $carrierHz Hz (${pattern.size} durations)") }
                .onFailure { log("TX failed: ${it.message}", false) }
        }
    }
}
