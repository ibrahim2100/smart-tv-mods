# تنبيهات المباريات · Match alerts

## المصدر · Source

مصدر واحد دائم: **واجهة UEFA الرسمية** (عامة، بدون مفتاح):

| الطلب · Request | الاستخدام · Use |
|---|---|
| `https://match.uefa.com/v5/matches?competitionId=1&status=LIVE` | المباريات الجارية (نتيجة، دقيقة، مرحلة) · live matches |
| `…&status=UPCOMING` | أقرب انطلاقة (للنوم لين قبلها بدقيقتين) · next kick-off |
| `/matches/{id}` | حالة نهائية (FINISHED) · final status |
| `/matches/{id}/events?filter=ALL` | الأهداف (مع المسجّل)، البطاقات، VAR، ركلات الجزاء · events |

الكاش عندهم `max-age=4, s-maxage=7` فهو أسرع مصدر مجاني ورسمي. الأسماء تأتي بالعربي (`translations.displayName.AR` للفرق و`translations.name.AR` للاعبين).
*Cache is `max-age=4, s-maxage=7`: the fastest free official source. Names come in Arabic.*

## المنطق · Logic (`service/uefa.js`)

- **الإيقاع:** مباراة جارية → قراءة القائمة كل 5 ثوان وأحداث كل مباراة كل 10. لا مباراة جارية → يسأل عن أقرب انطلاقة وينام لين قبلها بدقيقتين. خطأ شبكة → إعادة بعد 30 ث.
- **أول ظهور لمباراة = خط أساس فقط** (لا تنبيهات متأخرة لو شغّلت التلفزيون في نص المباراة).
- **بداية:** فقط لو المرحلة FIRST_HALF، الدقيقة ≤ 3، والنتيجة 0-0.
- **الأهداف:** تُمنع التكرارات بمعرّف المسجّل؛ لو جاءت أهداف عدة بقراءة وحدة تظهر بطاقة لكل هدف بالنتيجة المتتابعة؛ هدف ملغي يُنبَّه عليه.
- **نهاية المباراة:** عندما تخرج من قائمة LIVE ويرجع `/matches/{id}` بحالة FINISHED.
- **الطابور:** فاصل 7 ثوان بين بطاقتين. تأخير الإطلاق ~ثانية (مقبول).
- مفاتيح كل حدث في `CONFIG.ALERTS` (انظر [CONFIG.md](CONFIG.md))، وتفعيل/إيقاف عام من الخدمة (`alertsSet`).

*Cadence: 5 s on the live list, 10 s on events; idle → sleeps until 2 min before the next kick-off. First sight of a match only baselines. Kick-off only if FIRST_HALF, minute ≤ 3, 0-0. Goals de-duplicated by scorer id; multiple goals in one poll → one card each with a running score. Full time = left the LIVE list and `/matches/{id}` says FINISHED. Cards are queued 7 s apart; launch latency ≈ 1 s.*

## ألوان البطاقات · Card kinds

ذهبي: هدف · أحمر: طرد · أزرق: VAR · برتقالي: ركلة جزاء · `#de8660`: إلغاء · ذهبي: بداية/استراحة/نهاية.

## مقارنة المصادر · Source comparison

| المصدر · Source | النتيجة · Verdict |
|---|---|
| **UEFA match API** | ✅ رسمي، مجاني، بدون مفتاح، كاش 4–7 ث، مسجّلو الأهداف + ترجمة عربية · official, free, keyless |
| ESPN | ✅ يبقى لبطاقة المباراة في الصفحة (كاش 1 ث) لكن بلا تفاصيل أحداث كافية للتنبيهات · kept for the page's match card |
| TheSportsDB | احتياط فقط (مفتاح مجاني، محدود المعدل) · fallback only |
| Livescore CDN | يعمل لكنه غير رسمي · works, unofficial |
| SofaScore / FotMob | 403 / 404 · blocked |
| football-data.org | يحتاج مفتاح · needs a key |

## اختبار · Testing

```bash
npm test      # tools/uefa-test.js على بيانات مسجّلة (tools/uefa-sample-*.json) — بدون تلفزيون
```
تجربة البطاقة على التلفزيون (لاحقاً): `curl -s -X POST -d '{"kind":"goal"}' http://127.0.0.1:45620/alertsTest` من داخل التلفزيون، أو بالمتصفح بدون تلفزيون: `alert/index.html?demo=goal`.

## ما لم يُختبر بعد · Known gaps

- صياغة الطرد/VAR/ركلة الجزاء **لم تُجرَّب على مباراة حقيقية**؛ أسماء نتائج VAR (مثل `CONFIRMED`) تخمين وتُعدَّل من `VAR_RESULT_AR` في `uefa.js` بعد أول مباراة.
- Red-card / VAR / penalty wording is untested on a live match; the VAR result names are guesses (see `VAR_RESULT_AR`).
