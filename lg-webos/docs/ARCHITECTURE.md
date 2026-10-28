# البنية · Architecture

## الصورة العامة · Overview

```
┌──────────────────────────── LG webOS 7.5 (Chrome 87) ────────────────────────────┐
│                                                                                   │
│  com.webos.app.home  ◄── bind-mount ──  /tmp/tvhome-as-home  (نسخة من app/)       │
│   (the page: index.html + app.js)                                                 │
│        │  HTTP  127.0.0.1:45620  (POST /method, JSON)                             │
│        ▼                                                                          │
│  com.albrhi.tvhome.service  (Node 12, root)  ──luna-send──►  LG system services   │
│        │   ├─ uefa.js  poll match.uefa.com                                        │
│        │   └─ showAlert ─► applicationManager/launch                              │
│        ▼                                                                          │
│  com.albrhi.alert  (overlay window, transparent)  draws the alert card            │
└───────────────────────────────────────────────────────────────────────────────────┘
```

## المكوّنات · Components

| المكوّن · Component | الدور · Role |
|---|---|
| `app/` | الصفحة: صفوف التطبيقات/النظام/المدخلات، بطاقة المباراة، شاشة التوقف، لوحة الإعدادات، قائمة الضغط المطوّل. ويب خالص بلا بناء. · The page itself. Plain web, no build. |
| `service/service.js` | خدمة جذرية (Node) تنفذ كل شي تحتاج صلاحياته الصفحة: تشغيل/حذف تطبيقات، صوت، صورة، تنظيف الذاكرة، قراءة مفتاح HOME، وتشغيل التنبيهات. · Root helper that does everything the page isn't allowed to. |
| `service/uefa.js` | منطق التنبيهات (بدون أي اتصال بالتلفزيون، قابل للاختبار على بيانات مسجلة). · Alert logic; testable offline. |
| `service/net.js` | `getJson` يدعم gzip. · gzip-aware JSON fetch. |
| `service/as-home.sh` | الاستبدال: يجعل واجهتنا هي الهوم. · The home swap. |
| `alert/` | تطبيق نافذة شفافة (`overlay`) يرسم البطاقة ثم يغلق نفسه. · Transparent overlay app that draws the card then closes. |

## استبدال الهوم · The home swap (`as-home.sh`)

عند الإقلاع (من `/var/lib/webosbrew/init.d/40-tvhome-as-home`):
1. ينسخ التطبيق المثبّت إلى `/tmp/tvhome-as-home` (الذاكرة، لا شي يُكتب على القرص).
2. يغيّر الـ`id` في النسخة إلى `com.webos.app.home` ويخفيه من قائمة التطبيقات.
3. `mount --bind` فوق `/usr/palm/applications/com.webos.app.home`.
4. يعيد تشغيل `sam` (مدير التطبيقات) ليقرأ التغيير (قد يتأخر ~90 ثانية).
5. يشغّل الخدمة، ويطفئ `homeAutoLaunch`، ويضبط `lastInputApp` عشان أول شاشة بعد الإقلاع تكون واجهتنا.

Undo = احذف الـlink وأعد التشغيل (`scripts/revert-home.sh`). Nothing on disk is modified.

*On boot: copy to RAM, rewrite the id to LG's home id, bind-mount over LG's home folder, restart `sam`, start the service, turn off `homeAutoLaunch`, point `lastInputApp` at us. To undo, remove the init.d link and reboot.*

### قواعد مهمة · Hard rules
- **لا تقتل عملية الصفحة ولا تغلقها/تشغلها يدوياً**: النظام يتوقف عن جلبها للأمام (يلزم إعادة تشغيل التلفزيون). *Never kill/close/launch the home page by hand; the system then stops foregrounding it (needs a reboot).*
- `handlesRelaunch` مفعّل، وكود `webOSRelaunch` يستدعي `PalmSystem.activate()`. *`webOSRelaunch` must call `PalmSystem.activate()`.*

## لماذا خدمة HTTP؟ · Why an HTTP service?

نداءات `luna://` من هوية `com.webos.app.home` عبر `PalmServiceBridge` **لا ترد أبداً**. فكل شي يمر على الخدمة الجذرية عبر `http://127.0.0.1:45620/<method>` (POST، JSON، CORS مفتوح؛ مربوطة على loopback فقط).
*Luna calls from the home app's identity never answer, so the page talks to the root service over loopback HTTP.*

## واجهة الخدمة · Service API

| الطريقة · Method | الوصف · What it does |
|---|---|
| `getApps` | كل التطبيقات (مع `system` و`removable`) · all launch points |
| `getIcon` | أيقونة تطبيق (data URI) · app icon as data URI |
| `getInputs`, `getAv` | حالة المدخلات والصوت/الصورة · inputs, picture/sound state |
| `setPicture`, `setSound` | وضع الصورة/الإضاءة، وضع الصوت · picture mode/backlight, sound mode |
| `screenOff`, `screenOn` | إطفاء الشاشة مع بقاء الصوت · panel off, audio stays |
| `launch` | تشغيل تطبيق (`{id, params}`) · launch an app |
| `sys` | شبكة، صوت، بلوتوث، مستوى الصوت، الذاكرة المتاحة · net/audio/bt/volume/free MB |
| `volume`, `mem`, `cleanMemory` | الصوت، الذاكرة، تنظيف الخلفية · volume, memory, close background apps |
| `build` | رقم نسخة الملفات (mtimes) تستخدمه الصفحة للتحديث الذاتي · files stamp used for self-update |
| `log` | الصفحة تكتب في سجل الخدمة · page → service log |
| `uninstall` | حذف تطبيق منزّل (محميّ: Homebrew, TV Home, الهوم) · remove a downloaded app (protected list) |
| `alertsGet`, `alertsSet`, `alertsTest` | حالة/تفعيل/تجربة التنبيهات · alerts status/toggle/test card |

## الإقلاع والتحديث الذاتي · Boot and self-update

1. الصفحة تحمّل، تعرض الصفوف من `localStorage` فوراً، ثم `getApps` تحدّثها (تطبيق جديد يظهر لحاله).
2. `warm()` صامت يبني طبقات الرسم مسبقاً عشان أول تنقل يكون ناعم.
3. كل 15 ثانية تسأل `build`؛ لو تغيّر الرقم → `location.reload()`. لذلك `deploy.sh` يكفي ولا يحتاج تدخل.
4. الخدمة تقرأ HOME من `evdev` كل 50 ms وتشغّل الواجهة (لأن تطبيقات مثل Netflix لا تمرر زر HOME للنظام).

*The page paints rows from cache first, then refreshes from `getApps`; a silent `warm()` pre-builds compositor layers; every 15 s it polls `build` and reloads on change; the service polls evdev every 50 ms for the HOME key.*

## خط التنبيهات · Alert pipeline

`uefa.js` (poll) → `diff` (ما الجديد؟) → طابور بفارق 7 ثوان → `showAlert` → `applicationManager/launch` للتطبيق `com.albrhi.alert` مع المعاملات → الصفحة تقرأ `PalmSystem.launchParams` وترسم البطاقة وتغلق نفسها (7 ث للهدف/الطرد/الإلغاء/VAR/الجزاء، 5.5 ث للباقي). لو رفض النظام الـoverlay يُعرض toast نصي بدل البطاقة.
تفاصيل المصدر والمنطق: [ALERTS.md](ALERTS.md).

*Poll → diff → queue (7 s gap) → launch the overlay app with params → it animates in and closes itself. If the overlay is refused a plain toast is used. Details in [ALERTS.md](ALERTS.md).*

التطبيق `com.albrhi.alert`: `defaultWindowType: overlay`, `transparent`, `noSplashOnLaunch`, `spinnerOnLaunch: false`, `handlesRelaunch`. تجربة بالمتصفح بدون تلفزيون: `alert/index.html?demo=goal|red|var|penalty|cancel|kickoff|full`.

## قيود الأداء · Performance rules

التلفزيون: رام 1.2 جيجا (~350 ميجا متاحة) وChrome 87.
- حرّك `transform` و`opacity` فقط.
- ممنوع `filter: blur`، و`box-shadow`/`background-color`/`mask` متحركة.
- حلقة التركيز والظل ثابتة على `.halo` وتظهر بـ`opacity` فقط (`will-change: opacity`).
- النتيجة المقاسة: 55–60 إطار/ثانية، ~25 ms من الضغطة للرسم.

*Animate only transform/opacity; no blur filters, no animated shadows/colours/masks; the focus ring is static on `.halo` and only fades in. Measured: 55–60 fps, ~25 ms key-to-paint.*
