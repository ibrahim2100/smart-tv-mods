# دروس وأعطال · Lessons and troubleshooting

اقرأها قبل أي تعديل. كل نقطة كلّفتنا ساعات. *Read before changing anything; each line cost hours.*

## قواعد ذهبية · Golden rules

1. **لا تقتل عملية الصفحة الرئيسية ولا تشغّلها/تغلقها يدوياً.** النظام يتوقف عن إحضارها للأمام (شاشة سوداء) ولا يصلحها إلا إعادة التشغيل. *Never kill/close/launch the home page by hand: black screen until reboot.*
2. **`handlesRelaunch` + `PalmSystem.activate()`** في معالج `webOSRelaunch`، وإلا ما تظهر الواجهة عند الضغط على HOME.
3. **نداءات luna من هوية الهوم لا ترد**: مرّ كل شي على الخدمة الجذرية.
4. **`getAppLoadStatus` من الصفحة يتجاوز المهلة (5 ث)** فتظهر تطبيقات محذوفة؛ استخدم `getApps` من الخدمة.
5. **الحركة**: `transform`/`opacity` فقط. `filter: blur`، `box-shadow` متحرك، `background-color`، `mask` = تقطيع على هذا الجهاز.
6. **Node 12 على التلفزيون**: لا `?.` ولا `??`. كود الخدمة بصيغة ES5/ES2015 بسيط.
7. **JSON في معاملات الإطلاق**: علامة `'` تكسر الأمر؛ بسّط النصوص.
8. **css بـ`background:` داخل `html,body` تلغي الخلفية**؛ استخدم `background-color`.
9. **`filmMaker` وضع صورة غير مدعوم** على هذا الجهاز؛ قائمة الأوضاع مصححة.
10. **أي ملف جديد في `app/` لازم يدخل قائمة `deploy.sh`** (نسينا `bg.jpg` مرة فما ظهرت الخلفية).

## استكشاف الأعطال · Troubleshooting

| العرض · Symptom | السبب/الحل · Cause / fix |
|---|---|
| لا تنبيهات | `alertsGet` يبيّن `enabled`؟ السجل `/tmp/tvhome-svc.log`. لا مباراة جارية؟ الخدمة تنام لين قبل الانطلاقة بدقيقتين. |
| بطاقة تنبيه تتأخر ~ثانية | طبيعي (إطلاق تطبيق). مقبول. |
| بطاقة التنبيه تظهر كنص صغير (toast) | النظام رفض الـoverlay؛ أعد تثبيت `alert/` بـ`install-alert-app.sh`. |
| تطبيق جديد لا يظهر | انتظر 15 ث؛ تأكد الخدمة تعمل (`check.sh`). |
| الصفحة قديمة | `deploy.sh` ثم انتظر إعادة التحميل التلقائية (15 ث). |
| ثقل | شغّل تنظيف الذاكرة (أيقونة المكنسة)، وراقب `AUTO_CLEAN_MB`. |
| الهوم لا يظهر بعد إقلاع | `sam` يتأخر حتى ~90 ث؛ سجل `/tmp/tvhome-as-home.log`. |
| الأيقونات/الشعارات ناقصة | `scripts/fetch-assets.sh` (غير مخزنة بـgit). |

## أماكن السجلات · Logs

```
/tmp/tvhome-svc.log          خدمة الواجهة والتنبيهات · service + alerts
/tmp/tvhome-as-home.log      سجل الاستبدال عند الإقلاع · swap at boot
/var/log/messages            سجل النظام · system log
```

لقطة شاشة من التلفزيون (960×540) · capture:
```bash
luna-send -n 1 luna://com.webos.service.capture/executeOneShot '{"path":"/tmp/s.jpg","method":"DISPLAY","width":960,"height":540,"format":"JPG"}'
```

## فحص الصفحة عن بُعد · Remote inspection

الصفحة تكتب `log` للخدمة (`api.log`). لفحص أعمق شغّل الواجهة كتطبيق عادي (غير الهوم) وافتح أدوات المطوّر عبر `ares-inspect` (webOS CLI، لم نجربه هنا).
*The page logs through the service. For deeper inspection run it as a normal app and use `ares-inspect`.*

## لا تعمل · Dead ends we tried

- SSH على هذا التلفزيون مغلق؛ `tools/tv` يعتمد SSH فلا يشتغل هنا (السكربتات تستخدم telnet).
- SofaScore/FotMob لا تعطي بيانات؛ Livescore يعمل لكن غير رسمي.
- `filter: blur` وظلال متحركة: ثقيلة جداً.

## تفاصيل إضافية · More notes

- **PlayStation على HDMI1** تحوّل مدخل التلفزيون لحالها عند التشغيل (HDMI-CEC) وتغطي الواجهة. هذا من إعدادات LG (SIMPLINK / تبديل المدخل التلقائي) وليس من الواجهة. *A console on HDMI1 switches the input by itself via CEC; that's an LG setting (SIMPLINK / auto input switch), not the app.*
- **شعار beIN** مرسوم تقريباً؛ استبدله بالملف الرسمي في `app/icons/logos/bein.png` متى ما توفر.
- **قياس الأداء** على التلفزيون: `app/perf.js`. شغّل مستقبل القياس على الكمبيوتر (بورت 9100) وافتح التطبيق بالمعاملات `{"perf":"<ip الكمبيوتر>"}`؛ يضغط الأسهم تلقائياً ويرسل الإطارات وزمن الاستجابة. *Run a receiver on your computer (port 9100) and launch with `{"perf":"<ip>"}`.*
- **معاينة على الكمبيوتر:** `python3 -m http.server 8765` داخل `app/` وافتح نافذة 1920×1080؛ أوامر النظام لا تعمل خارج التلفزيون فتظهر رسالة "Would open".
- لوحة الترس ← «تنبيهات الأهداف»: تشغيل/إيقاف وإرسال بطاقة تجربة.
