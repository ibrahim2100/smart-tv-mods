# تلفزيونان، مشروعان · Two TVs, two projects

```
untitled folder 3/
├── google-tv/   🟢 Haier Google TV (Android 14)   ← لانشر Android · Android launcher
│   ├── albrhi-widget/        المصدر والبناء · source + build
│   └── albrhi-tv-restore/    حزمة الاسترجاع · restore kit
└── lg-webos/    🔴 LG webOS (rooted)               ← شاشة رئيسية ويب · web home screen
                              مستودعه الخاص · own repo: github.com/ibrahim2100/WebOS
```

**القاعدة:** كل مشروع في مجلده الرئيسي، ولا شي مشترك بينهما. · **Rule:** each project lives in its own top-level folder and shares nothing with the other.

| | 🟢 `google-tv/` | 🔴 `lg-webos/` |
|---|---|---|
| **التلفزيون · TV** | Haier MatrixTV BE — Google TV / Android 14 | LG webOS 7.5، مروّت · rooted (Homebrew Channel) |
| **ما هو · What** | لانشر Android (Albrhi Home) + شاشة انتظار، APK | واجهة ويب تحل محل هوم LG + خدمة جذرية + بطاقات تنبيه دوري الأبطال |
| **الأدوات · Tooling** | `adb` (`192.168.1.50:5555`)، `build.sh`, `restore.sh`, `check.sh` | telnet بورت 23، `scripts/deploy.sh`, `check.sh`, `npm test` |
| **التوثيق · Docs** | [google-tv/README.md](google-tv/README.md) | [lg-webos/README.md](lg-webos/README.md) و`lg-webos/docs/` |
| **Git** | مستودع هذا المجلد (محلي) · this folder's repo (local) | مستودع مستقل داخل `lg-webos/` · its own repo (public) |
| **الحالة · Status** | مستقر · stable | قيد التطوير · active |

## قواعد الفصل · Separation rules
1. شغّل أي سكربت من داخل مجلد مشروعه فقط. · Run scripts only from inside their own project folder.
2. سكربتات `adb` تخص Google TV فقط، وسكربتات telnet تخص LG فقط. · `adb` scripts = Google TV only; telnet scripts = LG only.
3. `lg-webos/` مستودع git مستقل يتجاهله المستودع الخارجي؛ التزامات كل منهما لا تدخل الآخر. · Separate git histories; the outer repo ignores `lg-webos/`.
4. «Albrhi TV» قديماً = لانشر Google TV. مشروع LG اسمه **الجلسة / WebOS**. · "Albrhi TV" in old notes = the Google TV launcher; the LG project is Aljalsa / WebOS.
5. مشروع Google TV مستقر: لا يُعدَّل إلا بطلب صريح. · The Google TV project is stable: change it only on request.
