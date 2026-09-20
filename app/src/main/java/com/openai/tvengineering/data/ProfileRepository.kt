package com.openai.tvengineering.data

import org.json.JSONArray
import org.json.JSONObject

object ProfileRepository {
    fun starterProfiles(): List<TvProfile> = listOf(
        TvProfile(
            id = "blank-safe",
            name = "Universal / Empty Profile",
            brand = "Any",
            model = "Map your own codes",
            notes = "No vendor-specific service codes are preloaded. Import or create codes verified for the exact model.",
            commands = emptyList()
        ),
        TvProfile(
            id = "nec-lab-template",
            name = "NEC Engineering Template",
            brand = "Generic",
            model = "NEC protocol",
            notes = "Template values are examples for bench testing, not guaranteed TV codes.",
            commands = listOf(
                IrCommandSpec("TEST_NEC", "NEC Example", 38_000, ProtocolType.NEC, address = 0x00, command = 0x00)
            )
        )
    )

    fun parse(json: String): TvProfile {
        val root = JSONObject(json)
        val array = root.optJSONArray("commands") ?: JSONArray()
        val commands = buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val protocol = ProtocolType.valueOf(o.optString("protocol", "RAW").uppercase())
                val raw = o.optJSONArray("pattern")?.let { a -> IntArray(a.length()) { idx -> a.getInt(idx) } }
                add(
                    IrCommandSpec(
                        key = o.getString("key"),
                        label = o.optString("label", o.getString("key")),
                        carrierHz = o.optInt("carrierHz", 38_000),
                        protocol = protocol,
                        address = if (o.has("address")) o.getInt("address") else null,
                        command = if (o.has("command")) o.getInt("command") else null,
                        dataHex = o.optString("dataHex").takeIf { it.isNotBlank() },
                        pattern = raw,
                        pronto = o.optString("pronto").takeIf { it.isNotBlank() },
                        lsbFirstPerByte = o.optBoolean("lsbFirstPerByte", true),
                        dangerous = o.optBoolean("dangerous", false),
                        engineering = o.optBoolean("engineering", false)
                    )
                )
            }
        }
        return TvProfile(
            id = root.optString("id", "imported-${System.currentTimeMillis()}"),
            name = root.optString("name", "Imported profile"),
            brand = root.optString("brand", "Unknown"),
            model = root.optString("model", "Unknown"),
            notes = root.optString("notes", ""),
            commands = commands
        )
    }

    fun toJson(profile: TvProfile): String {
        val root = JSONObject()
            .put("id", profile.id)
            .put("name", profile.name)
            .put("brand", profile.brand)
            .put("model", profile.model)
            .put("notes", profile.notes)
        val commands = JSONArray()
        profile.commands.forEach { c ->
            val o = JSONObject()
                .put("key", c.key)
                .put("label", c.label)
                .put("carrierHz", c.carrierHz)
                .put("protocol", c.protocol.name)
                .put("dangerous", c.dangerous)
                .put("engineering", c.engineering)
            c.address?.let { o.put("address", it) }
            c.command?.let { o.put("command", it) }
            c.dataHex?.let { o.put("dataHex", it) }
            c.pronto?.let { o.put("pronto", it) }
            if (c.protocol == ProtocolType.PULSE_DISTANCE_32) o.put("lsbFirstPerByte", c.lsbFirstPerByte)
            c.pattern?.let { p -> o.put("pattern", JSONArray(p.toList())) }
            commands.put(o)
        }
        root.put("commands", commands)
        return root.toString(2)
    }
}
