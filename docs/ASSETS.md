# المواد: أيقونات وشعارات وخطوط · Assets

## الأصل مقابل المسحوب · Own vs. fetched

| المسار · Path | المصدر · Source | في git؟ |
|---|---|---|
| `app/icons/emblem.svg` | تصميمنا (سيفان ونخلة) · own design | ✅ |
| `app/icon.png`, `largeIcon.png`, `splash.png`, `icon.svg`, `bg.jpg` | تصميمنا · own | ✅ |
| `app/fonts/` | IBM Plex Sans Arabic، Amiri (SIL OFL 1.1) + ملفات الرخصة · fonts + licences | ✅ |
| `app/icons/apps/` | أيقونات التطبيقات مسحوبة من التلفزيون نفسه · pulled from the TV | ❌ |
| `app/icons/logos/` | شعارات PlayStation وbeIN وHDMI الرسمية · official logos | ❌ |
| `app/icons/crests/`, `alert/crests/`, `tools/crests/` | شعارات الأندية · club crests | ❌ |
| `tools/ideas/bg-clasico*.jpg`, `montage.jpg` | مسودات خلفيات · drafts | ❌ |

الأسباب: الشعارات علامات تجارية لأصحابها، ولا نعيد توزيعها. *Trademarks of their owners, so not redistributed.*
المشروع كامل يعمل بدونها لكن يظهر بلا شعارات.

## إعادة بنائها · Regenerating

```bash
scripts/fetch-assets.sh              # يسحب الأيقونات من التلفزيون والشعارات من مصادرها
OUT=/tmp/try scripts/fetch-assets.sh # تجربة بدون لمس المشروع
```
> يتصل بالتلفزيون: شغّله فقط وقت ما تكون جاهز. *Needs the TV; run it when ready.*

لو المستودع **خاص** (private) وتبغى ترفع الشعارات: احذف أسطر الأصول من `.gitignore`.
*If the repo is private and you want the artwork stored, delete the asset lines in `.gitignore`.*

## الخلفيات · Wallpapers

`tools/wallpaper.html` مولّد الخلفية الأصلية (`app/bg.jpg`). `wallpaper-clasico.html` و`wallpaper-ideas.html` مسودات بديلة غير مستخدمة. صوّر الصفحة بحجم 1920×1080 واحفظها JPG.
*`wallpaper.html` generates the live wallpaper; the others are unused drafts. Screenshot at 1920×1080.*

## الأرقام · Third-party data

UEFA match API، ESPN (جدول/نتائج)، TheSportsDB (احتياط)، Open-Meteo (طقس)، ipwho.is (مدينة بالـIP): كلها عامة بدون مفتاح، تستخدم بشرط الاستخدام الشخصي وعدم الإفراط في الطلبات.
