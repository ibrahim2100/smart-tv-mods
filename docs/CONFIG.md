# الإعدادات · Configuration

كل شي تقريباً في [`app/config.js`](../app/config.js). الخدمة (`service/`) تقرأ نفس الملف (الفرق وأسماء الأندية وخيارات التنبيه)، فتعديل واحد يكفي. بعد التعديل: `scripts/deploy.sh`.
Almost everything lives in [`app/config.js`](../app/config.js). The service reads the same file (teams, names, alert options), so one edit is enough. After editing run `scripts/deploy.sh`.

## الحقول · Fields

| الحقل · Field | العربي | English |
|---|---|---|
| `APPS` | التطبيقات المحمّلة (الترتيب الافتراضي). كل عنصر: `id` (معرّف التطبيق)، `title`، `icon` (ملف في `app/icons/apps/`)، `color` (خلفية البلاطة). أي تطبيق محمّل غير مذكور هنا **يظهر تلقائياً** آخر الصف بأيقونته من النظام. | Downloaded apps (default order). Each: `id`, `title`, `icon` (file in `app/icons/apps/`), `color` (tile background). Any installed app not listed here **appears automatically** at the end with its system icon. |
| `SYSTEM` | تطبيقات النظام الأساسية فقط. خيارات: `svg: 'gear'` أيقونة مرسومة، `popup: true` (يفتح كلوحة فوق الواجهة فبدون حركة اللون)، `always: true` (يظهر حتى لو ما ظهر في قائمة النظام)، `defaultHidden: true` (مخفي لين تظهره). | Essential built-in apps only. Options: `svg: 'gear'` (drawn icon), `popup: true` (opens as a panel over the home, no colour flood), `always: true` (shown even if the system list doesn't contain it), `defaultHidden: true` (hidden until you bring it back). |
| `INPUTS` | مدخلات HDMI. `id` = `com.webos.app.hdmiN`، `title`، `sub`، `logo` (ملف في `app/icons/logos/`)، `color`. | HDMI inputs. `id` = `com.webos.app.hdmiN`, `title`, `sub`, `logo` (file in `app/icons/logos/`), `color`. |
| `TEAMS` | الفرق اللي تظهر مبارياتها القادمة. `espn` = معرّف الفريق في ESPN، `id` = معرّف TheSportsDB (احتياط)، `ar` الاسم. لإضافة فريق: ابحث عن رقمه في `site.api.espn.com/apis/site/v2/sports/soccer/<league>/teams`. | Teams whose next match is shown. `espn` = ESPN team id, `id` = TheSportsDB id (backup), `ar` = name. To add a team find its id at `site.api.espn.com/apis/site/v2/sports/soccer/<league>/teams`. |
| `TEAM_AR`, `LEAGUE_AR`, `CITY_AR` | ترجمات الأسماء للعربي (غير الموجود يظهر بالإنجليزي). | Arabic translations (anything missing shows in English). |
| `BRAND` | السطر السفلي: `title` الكلمة، `sub` السطر اللاتيني الصغير، `emblem` ملف في `app/icons/`، `left` و`right` شعارات الأندية على الجانبين (ملفات في `app/icons/crests/`) بترتيب الشاشة. كلمة فاضية تخفيه. | The bottom lockup: `title` word, `sub` small Latin line, `emblem` file in `app/icons/`, `left` / `right` club crests (files in `app/icons/crests/`) in screen order. An empty title hides it. |
| `ALERTS` | تنبيهات المباريات (تقرأها الخدمة): `enabled`، `competitions` (معرّفات UEFA: `1` دوري الأبطال، `14` الأوروبي، `2001` المؤتمر)، ومفاتيح كل حدث: `kickoff`, `goals`, `redCards`, `penalties`, `var`, `halfTime`, `fullTime`. | Match alerts (read by the service): `enabled`, `competitions` (UEFA ids: `1` Champions League, `14` Europa, `2001` Conference) and one switch per event: `kickoff`, `goals`, `redCards`, `penalties`, `var`, `halfTime`, `fullTime`. |
| `AUTO_CLEAN_MB` | لو المتاح أقل من هالرقم والواجهة مفتوحة، تُغلق التطبيقات الخلفية تلقائياً. `0` = أبداً. | When free memory drops below this and the home is showing, background apps are closed automatically. `0` = never. |
| `SAVER_MINUTES` | دقائق الخمول قبل شاشة التوقف. | Idle minutes before the screensaver. |

## وين تنحفظ بياناتك · Where your data is kept

- **على الصفحة** (`localStorage`، مفتاحها يبدأ بـ`albrhi:`): ترتيب كل صف `appOrder/sysOrder/inputOrder`، المخفي `appHidden/sysHidden/inputHidden`، آخر تطبيق `last`، المباريات `matches` و`matchFetchedAt` و`teamsSig`، الطقس `wx` و`geo`، صوت التنقل `clicks`، وأيقونات التطبيقات الجديدة `ico:<id>`.
  *On the page (`localStorage`, keys start with `albrhi:`): row order, hidden items, last app, matches, weather/geo, nav sound, auto-discovered icons.*
- **على الخدمة:** `/media/developer/tvhome-settings.json` (مثل `{"alerts": true}`) خارج مجلد التطبيق فيبقى بعد كل `deploy.sh`.
  *In the service: `/media/developer/tvhome-settings.json`, outside the app folder so it survives deploys.*
- **تصفير ترتيبك:** الترس ← «إظهار المخفية» يرجع المخفي. لتصفير كل شي احذف مفاتيح `albrhi:*` من `localStorage` (فحص الصفحة عن بُعد، انظر [LESSONS.md](LESSONS.md)).
  *Reset: gear panel → "Show hidden items" restores hidden entries; to reset everything delete the `albrhi:*` keys from `localStorage` (see LESSONS).*
