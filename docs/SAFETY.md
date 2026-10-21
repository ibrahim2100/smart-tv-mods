# الأمان والتراجع · Safety and reverting

> المشروع غير رسمي. أنت تعدّل تلفزيونك على مسؤوليتك. *Unofficial: you modify your TV at your own risk.*

## وش يتغير على التلفزيون · What changes on the TV

| التغيير · Change | وين · Where | التراجع · Undo |
|---|---|---|
| تطبيق الواجهة `com.albrhi.tvhome` + الخدمة | `/media/developer/apps/...` | `scripts/uninstall-custom-home.sh` |
| تطبيق البطاقة `com.albrhi.alert` | نفس المكان · same | حذف من `luna://com.webos.appInstallService/remove` أو `uninstall-custom-home.sh` |
| استبدال الهوم (bind-mount في الذاكرة) | `/var/lib/webosbrew/init.d/40-tvhome-as-home` | `scripts/revert-home.sh` (لا يكتب شي على القرص) |
| `general.homePromotion` → off | إعدادات LG | يرجعها `revert-home.sh` |
| `general.screenSaverEnabled` / `screenSaverAd` → off | إعدادات LG | يرجعها `revert-home.sh` |
| `homeAutoLaunch` → off، `lastInputApp`/`physicalLastInputApp` → تطبيقنا | إعدادات LG | يرجعها `revert-home.sh` (قيمتها `livetv`) |
| ملف إعدادات الخدمة | `/media/developer/tvhome-settings.json` | يُحذف مع الإزالة |

اللقطات الأصلية في `scripts/backups/` (غير مرفوعة على git) قبل أي تعديل. شغّل `scripts/backup.sh` قبل أي خطوة كبيرة.
*Original snapshots are in `scripts/backups/` (git-ignored). Run `backup.sh` before big steps.*

## الروت · Root

- الروت نفسه (Homebrew Channel) ما له زر تراجع برمجي. الحل المضمون: **Factory Reset** من إعدادات التلفزيون (يمسح كل شي بما فيه الروت).
  *Root itself has no software undo; a factory reset from the TV's settings removes everything including root.*
- **telnet المنفذ 23** يعطي صلاحيات روت **بلا كلمة سر** لأي جهاز على شبكتك. لا تفتحه للإنترنت، ويفضّل إغلاقه إذا ما تستخدم السكربتات (Homebrew Channel ← إعدادات). *Port 23 is an unauthenticated root shell for anyone on your LAN.*
- خدمة الواجهة تسمع على `127.0.0.1:45620` فقط (CORS مفتوح لكن لا تُرى من الشبكة). *The service binds to loopback only.*

## حماية من الأخطاء · Guards

- **الحذف**: من الضغط المطوّل فقط، بتأكيد، ولا يحذف تطبيقات النظام ولا غير القابل للحذف، ولا Homebrew Channel ولا واجهتنا ولا الهوم.
- لا تشغّل `revert-home.sh` و`uninstall-custom-home.sh` بالعكس: ارجع للهوم الأصلي أولاً، ثم احذف التطبيق، وإلا ما يبقى هوم شغّال لين تعيد التثبيت.
- الخدمة فيها `uncaughtException` guard فلا تموت من خطأ شبكة.
- **ملاحظة تشغيلية:** لا تشغّل أي سكربت يتصل بالتلفزيون أثناء مباراة أو مشاهدة: `deploy.sh` يعيد تشغيل الخدمة (ثانيتان بدون تنبيهات) والصفحة تُعيد تحميل نفسها.

*Deletion only via long-press with confirmation; protected ids can't be removed. Revert the home swap before uninstalling the app. Avoid running deploy while watching: it restarts the service and the page reloads.*

## لو تعطّل شي · If something breaks

1. شاشة سوداء بعد الإقلاع: انتظر دقيقتين (`sam` يعيد التشغيل)، ثم أعد تشغيل التلفزيون من القابس.
2. الهوم ما يظهر عند الضغط: لا تقتل العمليات؛ أعد التشغيل (انظر [LESSONS.md](LESSONS.md)).
3. أردت الرجوع: `scripts/revert-home.sh` (يعيد التشغيل) أو، لو التلفزيون ما يستجيب للسكربتات، احذف `/var/lib/webosbrew/init.d/40-tvhome-as-home` من Homebrew Channel ← Terminal وأعد التشغيل.
4. آخر حل: Factory Reset.
