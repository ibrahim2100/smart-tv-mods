# ملاحظات للمطوّر · Developer notes (for humans and Claude)

مشروع: واجهة رئيسية لـLG webOS 7.5 (Chrome 87، رام 1.2 جيجا، روت عبر Homebrew). الوثائق في `docs/`. *Custom home for a rooted LG webOS TV.*

## قبل أي شي · Before anything
- **لا تشغّل سكربت يتصل بالتلفزيون** (`deploy`, `check`, `install-*`, `fetch-assets`, `backup`, `enable/revert-home`) إلا لو المستخدم طلب رفعاً صراحةً. كل شي آخر محلي. *Don't run TV-connecting scripts unless asked to upload.*
- `npm test` آمن (بيانات UEFA مسجلة).
- Node على التلفزيون 12: لا `?.`/`??`. المتصفح Chrome 87.

## قواعد حرجة · Critical rules (details: docs/LESSONS.md)
1. لا تقتل/تغلق/تشغّل عملية الهوم يدوياً (شاشة سوداء حتى إعادة التشغيل).
2. الصفحة لا تستدعي luna: كل شي عبر الخدمة `127.0.0.1:45620`.
3. حرّك `transform`/`opacity` فقط؛ لا blur ولا ظلال/ألوان متحركة.
4. ملف جديد في `app/` = أضفه إلى قائمة `scripts/deploy.sh`.
5. التنبيهات: **دوري الأبطال فقط** (`ALERTS.competitions: [1]`) ومصدر UEFA دائم.
6. الخلفية `bg.jpg` الأولى تبقى. الشعارات/الأيقونات المسحوبة غير مخزنة بـgit.
7. لا تعدّل مشروع Android القديم.

## خريطة الملفات · Map
```
app/index.html, app.js (VERSION في أعلاه), style.css, config.js, perf.js
service/service.js (API + إعدادات + تنظيف الذاكرة + HOME)   uefa.js (منطق التنبيهات)   net.js   as-home.sh
alert/index.html (بطاقة التنبيه)   scripts/*.sh   tools/uefa-test.js + uefa-sample-*.json
```

## سير العمل · Workflow
عدّل محلياً ← `npm test` ← للمعاينة افتح `app/index.html` أو `alert/index.html?demo=goal` بالمتصفح ← عند الإذن بالرفع: `scripts/deploy.sh` (الصفحة تعيد تحميل نفسها خلال 15 ث).
*Edit locally, test, preview in a browser, and only when told to upload run `deploy.sh`.*

## معلق · Open items
انظر [docs/ROADMAP.md](docs/ROADMAP.md): اختبار تنبيهات الطرد/VAR/الجزاء على مباراة حقيقية، واختبار التثبيت من الصفر.
