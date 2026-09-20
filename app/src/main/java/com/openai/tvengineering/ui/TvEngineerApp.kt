package com.openai.tvengineering.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.openai.tvengineering.core.IrProtocols
import com.openai.tvengineering.data.IrCommandSpec
import com.openai.tvengineering.data.ProtocolType
import com.openai.tvengineering.data.TvProfile

private val Bg = Color(0xFF070B13)
private val Panel = Color(0xFF0D1524)
private val Panel2 = Color(0xFF111D31)
private val Accent = Color(0xFF5EA2FF)
private val Good = Color(0xFF4AD998)
private val Warn = Color(0xFFFFC857)
private val Danger = Color(0xFFFF6178)
private val Muted = Color(0xFF9AACCA)

private enum class MainTab(val title: String, val icon: String) {
    REMOTE("الريموت", "▣"),
    DEVICES("الأجهزة", "TV"),
    ENGINEER("الهندسة", "⚙"),
    LOGS("السجل", "≡")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvEngineerApp(vm: TvRemoteViewModel) {
    var tab by rememberSaveable { mutableStateOf(MainTab.REMOTE) }
    val colors = darkColorScheme(
        primary = Accent,
        secondary = Color(0xFF8B78FF),
        background = Bg,
        surface = Panel,
        surfaceVariant = Panel2,
        error = Danger,
        onBackground = Color(0xFFF3F6FF),
        onSurface = Color(0xFFF3F6FF)
    )

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            Scaffold(
                containerColor = Bg,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Panel),
                        title = {
                            Column {
                                Text("TV Engineering Remote", fontWeight = FontWeight.Bold)
                                Text(
                                    if (vm.hardware.emitterPresent) "IR Blaster متوفر" else "لا يوجد IR Blaster مُعلن",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (vm.hardware.emitterPresent) Good else Warn
                                )
                            }
                        },
                        actions = {
                            StatusChip(if (vm.engineerMode) "ENGINEER" else "USER", vm.engineerMode)
                            Spacer(Modifier.width(12.dp))
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = Panel) {
                        MainTab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Text(item.icon) },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when (tab) {
                        MainTab.REMOTE -> RemoteScreen(vm)
                        MainTab.DEVICES -> DevicesScreen(vm)
                        MainTab.ENGINEER -> EngineerScreen(vm)
                        MainTab.LOGS -> LogsScreen(vm)
                    }
                }
            }
        }

        vm.pendingDangerous?.let { cmd ->
            AlertDialog(
                onDismissRequest = vm::cancelDangerous,
                title = { Text("تأكيد أمر صيانة حساس") },
                text = {
                    Text("الأمر: ${cmd.label}\n\nقد يغير إعدادات خدمة أو إعدادات مصنع. نفّذه فقط على شاشة تملكها أو لديك تصريح صيانتها، وبعد التحقق من كود الموديل الصحيح.")
                },
                confirmButton = {
                    Button(
                        onClick = vm::confirmDangerous,
                        colors = ButtonDefaults.buttonColors(containerColor = Danger)
                    ) { Text("تنفيذ") }
                },
                dismissButton = { TextButton(onClick = vm::cancelDangerous) { Text("إلغاء") } }
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, enabled: Boolean) {
    Surface(
        color = if (enabled) Color(0x2239D98A) else Color(0x221A2940),
        border = BorderStroke(1.dp, if (enabled) Good else Muted),
        shape = RoundedCornerShape(50)
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RemoteScreen(vm: TvRemoteViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            PanelCard {
                Text("الملف النشط", color = Muted, style = MaterialTheme.typography.labelMedium)
                Text("${vm.activeProfile.brand} • ${vm.activeProfile.model}", fontWeight = FontWeight.Bold)
                if (vm.activeProfile.commands.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("هذا الملف فارغ. استورد Profile خاصاً بالشاشة من تبويب الأجهزة.", color = Warn, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            PanelCard {
                Button(
                    onClick = { vm.requestKey("POWER") },
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(76.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("⏻", style = MaterialTheme.typography.headlineMedium) }
                Spacer(Modifier.height(14.dp))
                RemoteRow(
                    listOf("INPUT" to "المصدر", "HOME" to "الرئيسية", "MENU" to "القائمة"), vm
                )
                Spacer(Modifier.height(12.dp))
                DPad(vm)
                Spacer(Modifier.height(12.dp))
                RemoteRow(listOf("BACK" to "رجوع", "INFO" to "معلومات", "EXIT" to "خروج"), vm)
            }
        }
        item {
            PanelCard {
                Text("الصوت والقنوات", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                RemoteRow(listOf("VOL_DOWN" to "VOL −", "MUTE" to "كتم", "VOL_UP" to "VOL +"), vm)
                Spacer(Modifier.height(8.dp))
                RemoteRow(listOf("CH_DOWN" to "CH −", "GUIDE" to "دليل", "CH_UP" to "CH +"), vm)
            }
        }
        item {
            PanelCard {
                Text("لوحة الأرقام", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                for (row in listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"))) {
                    RemoteRow(row.map { "NUM_$it" to it }, vm)
                    Spacer(Modifier.height(8.dp))
                }
                RemoteRow(listOf("DASH" to "—", "NUM_0" to "0", "LAST" to "آخر قناة"), vm)
            }
        }
        item {
            PanelCard {
                Text("وسائط", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                RemoteRow(listOf("REW" to "⏪", "PLAY_PAUSE" to "⏯", "FF" to "⏩"), vm)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DPad(vm: TvRemoteViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        RemoteKey("▲", { vm.requestKey("UP") }, Modifier.width(90.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteKey("◀", { vm.requestKey("LEFT") }, Modifier.width(90.dp))
            Button(onClick = { vm.requestKey("OK") }, modifier = Modifier.size(88.dp), shape = CircleShape) { Text("OK", fontWeight = FontWeight.Bold) }
            RemoteKey("▶", { vm.requestKey("RIGHT") }, Modifier.width(90.dp))
        }
        RemoteKey("▼", { vm.requestKey("DOWN") }, Modifier.width(90.dp))
    }
}

@Composable
private fun RemoteRow(items: List<Pair<String, String>>, vm: TvRemoteViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (key, label) ->
            RemoteKey(label, { vm.requestKey(key) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RemoteKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(50.dp), shape = RoundedCornerShape(14.dp)) {
        Text(label, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DevicesScreen(vm: TvRemoteViewModel) {
    val context = LocalContext.current
    var newName by rememberSaveable { mutableStateOf("") }
    var newBrand by rememberSaveable { mutableStateOf("") }
    var newModel by rememberSaveable { mutableStateOf("") }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read selected file")
        }.onSuccess(vm::importProfile).onFailure { vm.log("Read failed: ${it.message}", false) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.exportActiveProfile()) }
                ?: error("Unable to write selected file")
        }.onSuccess { vm.log("Profile exported") }.onFailure { vm.log("Export failed: ${it.message}", false) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PanelCard {
                Text("Profiles للشاشات", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "IR لا يحتاج Pairing. تختار ملف الأكواد الصحيح للشركة والموديل ثم توجّه الهاتف إلى مستقبل الأشعة في الشاشة.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) { Text("استيراد JSON") }
                    OutlinedButton(onClick = { exportLauncher.launch("${vm.activeProfile.id}.json") }, modifier = Modifier.weight(1f)) { Text("تصدير") }
                }
            }
        }
        item {
            PanelCard {
                Text("إضافة شاشة / Profile جديد", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                SmallField("اسم الشاشة، مثال: Living Room", newName, { newName = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallField("الشركة", newBrand, { newBrand = it }, Modifier.weight(1f))
                    SmallField("الموديل", newModel, { newModel = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    vm.createProfile(newName, newBrand, newModel)
                    if (newName.isNotBlank() && newBrand.isNotBlank()) { newName = ""; newBrand = ""; newModel = "" }
                }, modifier = Modifier.fillMaxWidth()) { Text("إنشاء Profile") }
            }
        }

        items(vm.profiles, key = { it.id }) { profile ->
            ProfileCard(profile, selected = profile.id == vm.activeProfile.id) { vm.selectProfile(profile) }
        }
        item {
            PanelCard {
                Text("الملف النشط", fontWeight = FontWeight.Bold)
                Text("${vm.activeProfile.name} • ${vm.activeProfile.commands.size} أمر", color = Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                val normal = vm.activeProfile.commands.count { !it.engineering }
                val service = vm.activeProfile.commands.count { it.engineering }
                Text("Remote: $normal   |   Service/Engineer: $service", color = Muted)
                if (!vm.activeProfile.id.startsWith("blank-") && !vm.activeProfile.id.startsWith("nec-lab")) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = vm::deleteActiveProfile, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Text("حذف هذا Profile") }
                }
            }
        }

        item {
            PanelCard {
                Text("ملاحظات توافق", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• الشاشة العادية والذكية يمكن التحكم بأساسياتها عبر IR إذا كانت تستقبل IR.", color = Muted)
                Text("• البيانات المتقدمة مثل الحرارة، ساعات التشغيل أو حالة HDMI لا يمكن قراءتها من IR أحادي الاتجاه وحده.", color = Muted)
                Text("• ملفات Service يجب أن تطابق الموديل/المنطقة/اللوحة الأم قبل الاستخدام.", color = Muted)
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: TvProfile, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF173052) else Panel),
        border = BorderStroke(1.dp, if (selected) Accent else Color(0xFF263955)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(profile.name, fontWeight = FontWeight.Bold)
                    Text("${profile.brand} • ${profile.model}", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("${profile.commands.size} cmds", color = if (selected) Good else Muted, style = MaterialTheme.typography.labelSmall)
            }
            if (profile.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp)); Text(profile.notes, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            val serviceCount = profile.commands.count { it.engineering }
            if (serviceCount > 0) {
                Spacer(Modifier.height(8.dp)); Text("Service/Engineering: $serviceCount", color = Warn, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EngineerScreen(vm: TvRemoteViewModel) {
    var carrier by rememberSaveable { mutableStateOf("38000") }
    var necAddress by rememberSaveable { mutableStateOf("00") }
    var necCommand by rememberSaveable { mutableStateOf("00") }
    var raw by rememberSaveable { mutableStateOf("9000, 4500, 560, 560") }
    var hex32 by rememberSaveable { mutableStateOf("00000000") }
    var lsbFirst by rememberSaveable { mutableStateOf(true) }
    var pronto by rememberSaveable { mutableStateOf("") }
    var macro by rememberSaveable { mutableStateOf("POWER, INPUT, OK") }
    var macroDelay by rememberSaveable { mutableStateOf("700") }
    var saveKey by rememberSaveable { mutableStateOf("CUSTOM_KEY") }
    var saveLabel by rememberSaveable { mutableStateOf("Custom command") }
    var saveEngineering by rememberSaveable { mutableStateOf(true) }
    var saveDangerous by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PanelCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Engineer Mode", fontWeight = FontWeight.Bold)
                    Text("يفتح أدوات RAW/NEC/Pronto وأوامر الصيانة المحمية.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = vm.engineerMode, onCheckedChange = vm::setEngineerMode)
            }
        }

        PanelCard {
            SectionTitle("Command Builder")
            Text("اختبر الكود أولاً، ثم احفظه داخل Profile الحالي. عند تعديل Template ينشئ التطبيق نسخة Custom تلقائياً.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            SmallField("Key مثل POWER أو SERVICE_MENU", saveKey, { saveKey = it.uppercase().replace(" ", "_") }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SmallField("Label", saveLabel, { saveLabel = it }, Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = saveEngineering, onCheckedChange = { saveEngineering = it })
                Text("Engineering command")
                Spacer(Modifier.width(10.dp))
                Checkbox(checked = saveDangerous, onCheckedChange = { saveDangerous = it })
                Text("Sensitive")
            }
        }

        PanelCard {
            SectionTitle("تشخيص عتاد IR")
            Metric("FEATURE_CONSUMER_IR", if (vm.hardware.featureDeclaredByDevice) "YES" else "NO", vm.hardware.featureDeclaredByDevice)
            Metric("IR emitter", if (vm.hardware.emitterPresent) "DETECTED" else "NOT DETECTED", vm.hardware.emitterPresent)
            val ranges = if (vm.hardware.ranges.isEmpty()) "غير مُعلنة" else vm.hardware.ranges.joinToString { "${it.minHz}-${it.maxHz} Hz" }
            Text("Carrier ranges: $ranges", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = vm::refreshHardware, modifier = Modifier.fillMaxWidth()) { Text("إعادة فحص العتاد") }
        }

        PanelCard {
            SectionTitle("NEC Generator")
            NumericField("Carrier Hz", carrier) { carrier = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallField("Address HEX", necAddress, { necAddress = it }, Modifier.weight(1f))
                SmallField("Command HEX", necCommand, { necCommand = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = vm.engineerMode,
                    onClick = {
                        val c = carrier.toIntOrNull() ?: 38000
                        val a = necAddress.removePrefix("0x").toIntOrNull(16)
                        val cmd = necCommand.removePrefix("0x").toIntOrNull(16)
                        if (a == null || cmd == null) vm.log("Invalid NEC HEX values", false) else vm.sendNec(c, a, cmd)
                    }, modifier = Modifier.weight(1f)
                ) { Text("إرسال NEC") }
                OutlinedButton(
                    onClick = {
                        val a = necAddress.removePrefix("0x").toIntOrNull(16)
                        val cmd = necCommand.removePrefix("0x").toIntOrNull(16)
                        if (a == null || cmd == null || saveKey.isBlank()) vm.log("Invalid command metadata/HEX", false)
                        else vm.saveCommand(IrCommandSpec(saveKey, saveLabel, carrier.toIntOrNull() ?: 38000, ProtocolType.NEC, address = a, command = cmd, dangerous = saveDangerous, engineering = saveEngineering))
                    }, modifier = Modifier.weight(1f)
                ) { Text("حفظ") }
            }
        }

        PanelCard {
            SectionTitle("RAW Pulse Console")
            NumericField("Carrier Hz", carrier) { carrier = it }
            OutlinedTextField(
                value = raw,
                onValueChange = { raw = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("mark/space µs مفصولة بفواصل") },
                minLines = 3
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = vm.engineerMode,
                    onClick = {
                        val p = IrProtocols.parseRawMicros(raw)
                        if (p.isEmpty()) vm.log("RAW pattern is empty", false) else vm.sendRaw(carrier.toIntOrNull() ?: 38000, p)
                    }, modifier = Modifier.weight(1f)
                ) { Text("إرسال RAW") }
                OutlinedButton(
                    onClick = {
                        val p = IrProtocols.parseRawMicros(raw)
                        if (p.isEmpty() || saveKey.isBlank()) vm.log("RAW pattern/key missing", false)
                        else vm.saveCommand(IrCommandSpec(saveKey, saveLabel, carrier.toIntOrNull() ?: 38000, ProtocolType.RAW, pattern = p, dangerous = saveDangerous, engineering = saveEngineering))
                    }, modifier = Modifier.weight(1f)
                ) { Text("حفظ") }
            }
        }

        PanelCard {
            SectionTitle("32-bit Pulse Distance")
            Text("لأدلة الصيانة التي توثّق قيمة 32-bit كاملة مع pulse-distance timings.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            SmallField("HEX 32-bit", hex32, { hex32 = it }, Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = lsbFirst, onCheckedChange = { lsbFirst = it })
                Text("LSB first داخل كل byte")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = vm.engineerMode, onClick = { vm.sendHex32(carrier.toIntOrNull() ?: 38000, hex32, lsbFirst) }, modifier = Modifier.weight(1f)) { Text("إرسال 32-bit") }
                OutlinedButton(onClick = {
                    if (saveKey.isBlank() || hex32.removePrefix("0x").length != 8) vm.log("Key or 32-bit HEX invalid", false)
                    else vm.saveCommand(IrCommandSpec(saveKey, saveLabel, carrier.toIntOrNull() ?: 38000, ProtocolType.PULSE_DISTANCE_32, dataHex = hex32, lsbFirstPerByte = lsbFirst, dangerous = saveDangerous, engineering = saveEngineering))
                }, modifier = Modifier.weight(1f)) { Text("حفظ") }
            }
        }

        PanelCard {
            SectionTitle("Pronto Learned Code")
            Text("يدعم النوع 0000. هذا هو المسار العملي لاستيراد كود تم التقاطه بمستقبل IR خارجي.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = pronto, onValueChange = { pronto = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("0000 ....") })
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = vm.engineerMode && pronto.isNotBlank(), onClick = { vm.sendPronto(pronto) }, modifier = Modifier.weight(1f)) { Text("إرسال Pronto") }
                OutlinedButton(enabled = pronto.isNotBlank(), onClick = {
                    if (saveKey.isBlank()) vm.log("Command key is required", false)
                    else vm.saveCommand(IrCommandSpec(saveKey, saveLabel, protocol = ProtocolType.PRONTO, pronto = pronto, dangerous = saveDangerous, engineering = saveEngineering))
                }, modifier = Modifier.weight(1f)) { Text("حفظ") }
            }
        }

        PanelCard {
            SectionTitle("Macro Sequencer")
            Text("الأوامر المحمية أو Engineering لا تعمل داخل Macro تلقائياً.", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = macro, onValueChange = { macro = it }, modifier = Modifier.fillMaxWidth(), label = { Text("KEY1, KEY2, KEY3") })
            NumericField("Delay ms", macroDelay) { macroDelay = it }
            Button(
                onClick = {
                    val keys = macro.split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                    vm.runMacro(keys, macroDelay.toLongOrNull() ?: 700)
                }, modifier = Modifier.fillMaxWidth()
            ) { Text("تشغيل Macro") }
        }

        PanelCard {
            SectionTitle("Service / Hidden Commands")
            Text(
                "يعرض التطبيق فقط أوامر الصيانة الموجودة في Profile الذي استوردته للموديل المحدد. لا يتم تخمين Service Menu codes لأن الكود الخطأ قد يغير Panel Type أو EEPROM أو إعدادات المصنع.",
                color = Warn,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))
            val commands = vm.activeProfile.commands.filter { it.engineering }
            if (commands.isEmpty()) {
                Text("لا توجد أوامر Engineering في الملف الحالي.", color = Muted)
            } else {
                commands.forEach { cmd ->
                    ServiceCommandRow(cmd, vm)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        PanelCard {
            SectionTitle("حدود IR المهمة")
            Text("الهاتف يرسل الأوامر ولا يحصل عادةً على ACK أو telemetry من التلفزيون. لذلك معلومات الحرارة/EDID/ساعات التشغيل تحتاج بروتوكول شبكة أو منفذ خدمة خاص بالمصنّع، وليس IR وحده.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ServiceCommandRow(cmd: IrCommandSpec, vm: TvRemoteViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1220)), border = BorderStroke(1.dp, if (cmd.dangerous) Danger else Color(0xFF2B405F))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cmd.label, fontWeight = FontWeight.SemiBold)
                Text("${cmd.protocol} • ${cmd.carrierHz} Hz${if (cmd.dangerous) " • SENSITIVE" else ""}", color = if (cmd.dangerous) Warn else Muted, style = MaterialTheme.typography.labelSmall)
            }
            Button(enabled = vm.engineerMode, onClick = { vm.requestCommand(cmd) }) { Text("إرسال") }
        }
    }
}

@Composable
private fun LogsScreen(vm: TvRemoteViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            PanelCard {
                Text("Event / IR Log", fontWeight = FontWeight.Bold)
                Text("آخر ${vm.logs.size} حدث. لا يتم تخزين السجل خارج ذاكرة الجلسة في هذا الإصدار.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(vm.logs) { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF09111D))) {
                Row(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(entry.time, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
                    Text(entry.message, color = if (entry.ok) Color(0xFFDCE8FA) else Color(0xFFFF9BAB), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Color(0xFF24364F)),
        shape = RoundedCornerShape(20.dp)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Metric(name: String, value: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, color = Muted)
        Text(value, color = if (ok) Good else Warn, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NumericField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SmallField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = modifier, label = { Text(label) }, singleLine = true)
}
