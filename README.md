# TV Engineering Remote — Android IR Blaster Console

تطبيق Android احترافي للتحكم بالشاشات عبر **Consumer IR / IR Blaster** مع وضع فني صيانة منفصل.

## ما الذي يعمل فعلياً؟

- اكتشاف وجود IR Blaster من Android وعرض نطاقات التردد التي يبلغ عنها الجهاز.
- إرسال أنماط IR بواسطة `ConsumerIrManager.transmit()`.
- Remote كامل: Power / Input / Home / Menu / D-pad / OK / Back / Info / Exit / Volume / Channel / digits / media.
- ملفات TV Profiles بصيغة JSON، قابلة للإنشاء داخل التطبيق والاستيراد والتصدير، وتُحفظ محلياً بعد الإغلاق.
- Engineering Mode محمي لأوامر RAW / NEC / Pulse-distance 32-bit / Pronto 0000.
- Macro Sequencer للأوامر العادية فقط؛ يمنع الأوامر الحساسة/الهندسية تلقائياً.
- Service / Hidden Commands تظهر فقط عندما تكون موجودة في Profile خاص بالموديل.
- تأكيد إضافي قبل إرسال أي أمر موسوم `dangerous`.
- سجل إرسال وتشخيص داخل التطبيق.

## لماذا لا توجد “أكواد مخفية لكل التلفزيونات” مضمّنة؟

Service Menu codes تختلف حسب الشركة، الموديل، المنطقة وأحياناً اللوحة الأم/الفيرموير. إرسال تسلسل غير صحيح قد يغير Panel Type أو إعدادات مصنع مهمة. لذلك التطبيق مصمم كأداة مهندس: تستورد Profile موثوق للموديل، وبعدها تظهر أوامر الصيانة الخاصة به.

## حدود IR

IR عادةً اتصال أحادي الاتجاه: الهاتف يرسل، لكنه لا يحصل على ACK أو telemetry. لذلك قراءة حرارة اللوحة، ساعات التشغيل، EDID الفعلي، أخطاء اللوحة أو حالة HDMI تحتاج قناة أخرى يدعمها المصنع (LAN/API/service port). التطبيق لا يدعي قراءة هذه البيانات عبر IR.

## Learn IR

واجهة Android `ConsumerIrManager` هي للإرسال. الهاتف العادي لا يلتقط ريموت IR بهذه الواجهة. للتعلم استخدم مستقبل IR خارجي، ثم استورد الناتج كـ RAW أو Pronto 0000.

## فتح المشروع

1. افتح مجلد `TVEngineeringRemote` في Android Studio.
2. استخدم JDK 17 أو أحدث متوافق مع AGP.
3. اسمح لـ Gradle Sync بتنزيل Gradle/Android dependencies.
4. ثبّت Android SDK API 37 / Build Tools المطلوبة إذا طلب Android Studio ذلك.
5. شغّل التطبيق على هاتف حقيقي يحتوي IR Blaster. المحاكي لن يرسل IR فعلياً.

### Build من الطرفية

Windows:

```bat
gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

الناتج المعتاد:

`app/build/outputs/apk/debug/app-debug.apk`

> ملفات `gradlew` المرفقة تقوم بتنزيل `gradle-wrapper.jar` الرسمي لأول مرة إذا لم يكن موجوداً، ثم Gradle Wrapper ينزل Gradle 9.6.0 مع SHA-256 مثبت في الإعدادات.

## صيغة Profile

انظر:

`app/src/main/assets/profiles/profile_schema_example.json`

المفاتيح الشائعة التي تستخدمها واجهة الريموت:

`POWER`, `INPUT`, `HOME`, `MENU`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `OK`, `BACK`, `INFO`, `EXIT`, `VOL_UP`, `VOL_DOWN`, `MUTE`, `CH_UP`, `CH_DOWN`, `GUIDE`, `NUM_0` ... `NUM_9`, `PLAY_PAUSE`, `REW`, `FF`.

### Protocol = NEC

```json
{
  "key": "POWER",
  "label": "Power",
  "carrierHz": 38000,
  "protocol": "NEC",
  "address": 32,
  "command": 16,
  "dangerous": false,
  "engineering": false
}
```

### Protocol = RAW

```json
{
  "key": "MY_COMMAND",
  "label": "My verified command",
  "carrierHz": 38000,
  "protocol": "RAW",
  "pattern": [9000, 4500, 560, 560],
  "dangerous": false,
  "engineering": true
}
```

### Protocol = PULSE_DISTANCE_32

```json
{
  "key": "KEY_NAME",
  "label": "Documented 32-bit code",
  "carrierHz": 38000,
  "protocol": "PULSE_DISTANCE_32",
  "dataHex": "20DF10EF",
  "dangerous": false,
  "engineering": false
}
```

### Protocol = PRONTO

```json
{
  "key": "KEY_NAME",
  "label": "Learned command",
  "protocol": "PRONTO",
  "pronto": "0000 ....",
  "dangerous": false,
  "engineering": false
}
```

## Security / maintenance design

- الأوامر ذات `engineering=true` لا تعمل إلا في Engineer Mode.
- الأوامر ذات `dangerous=true` تحتاج Confirmation Dialog كل مرة.
- Macro لا ينفذ `engineering` أو `dangerous` حتى لو كان Engineer Mode مفتوحاً.
- لا توجد أكواد مصنع سرية أو عشوائية داخل المصدر.

## المشروع

- Kotlin + Jetpack Compose
- `minSdk 23`
- `compileSdk 37`
- `targetSdk 36`
- AGP 9.4.0
- Gradle 9.6.0
- Compose BOM 2026.09.00
