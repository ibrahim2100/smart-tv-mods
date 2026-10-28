// Aljalsa TV - what shows on the home screen. Order here is the default order; the TV remembers your own.
// icon: file in icons/apps/ (pulled from the TV itself), logo: file in icons/logos/, svg: built-in drawing.
window.CONFIG = {
  // Downloaded apps (LG Content Store / sideloaded)
  APPS: [
    {id: 'netflix', title: 'Netflix', icon: 'netflix.png', color: '#000000'},
    {id: 'youtube.leanback.v4', title: 'YouTube', icon: 'youtube.leanback.v4.png', color: '#ffffff'},
    {id: 'net.mbc.shahid-lgapp', title: 'شاهد', icon: 'net.mbc.shahid-lgapp.png', color: '#0a8f6b'},
    {id: 'amazon', title: 'Prime Video', icon: 'amazon.png', color: '#00a8e1'},
    {id: 'com.intigral.webtv', title: 'stc tv', icon: 'com.intigral.webtv.png', color: '#4b2e83'},
    {id: 'com.disney.disneyplus-prod', title: 'Disney+', icon: 'com.disney.disneyplus-prod.png', color: '#113ccf'},
    {id: 'dazn', title: 'DAZN', icon: 'dazn.png', color: '#17171a'},
    {id: 'com.apple.appletv', title: 'Apple TV', icon: 'com.apple.appletv.png', color: '#000000'},
    {id: 'webos-tv', title: 'ثمانية', icon: 'webos-tv.png', color: '#14110c'},
    {id: 'com.sharqtv.tv', title: 'NOW الشرق', icon: 'com.sharqtv.tv.png', color: '#0b3d91'},
    {id: 'com.9xtream.webos', title: '9Xtream', icon: 'com.9xtream.webos.png', color: '#111'},
    {id: 'com.iptvsmarters.app', title: 'IPTV Smarters Pro', icon: 'com.iptvsmarters.app.png', color: '#4f37e6'},
    {id: 'com.smarterstvplayers.net', title: 'Smarters Player', icon: 'com.smarterstvplayers.net.png', color: '#4e37e5'}
  ],
  // Only the essential built-in apps. (Add defaultHidden: true to an entry to keep it hidden until you bring it back from the gear panel: "Show hidden items".)
  SYSTEM: [
    {id: 'com.palm.app.settings', title: 'الإعدادات', svg: 'gear', color: '#2a2f3a', popup: true, always: true},
    {id: 'com.webos.app.browser', title: 'المتصفح', icon: 'com.webos.app.browser.png', color: '#1b2a4a'},
    {id: 'com.webos.app.mediadiscovery', title: 'الوسائط', icon: 'com.webos.app.mediadiscovery.png', color: '#1d3b34'},
    {id: 'com.webos.app.discovery', title: 'المتجر', icon: 'com.webos.app.discovery.png', color: '#a50034'},
    {id: 'org.webosbrew.hbchannel', title: 'Homebrew', icon: 'org.webosbrew.hbchannel.png', color: '#1a1a1a'}
  ],
  // HDMI inputs: a logo or a name. glow = the colour that lights up behind the card when it's selected.
  INPUTS: [
    {id: 'com.webos.app.hdmi1', title: 'PlayStation', sub: 'HDMI 1', logo: 'playstation.png', color: '#1b2b57', glow: '#2f6bff'},
    {id: 'com.webos.app.hdmi3', title: 'الرسيفر', sub: 'HDMI 3', logo: 'bein.png', color: '#2a1744', glow: '#8a4bff'},
    {id: 'com.webos.app.hdmi2', title: 'HDMI 2', sub: 'المدخل 2', logo: 'hdmi.png', color: '#16323a', glow: '#2bb7c4'}
  ],
  // Next-match card: each team's next match, from ESPN (no key; every competition, live score) with TheSportsDB as the
  // backup if ESPN is down. refreshed every hour (every 2 minutes while a match is live).
  // espn = team id in ESPN's URLs (site.api.espn.com/.../soccer/<league>/teams), id = TheSportsDB team id.
  TEAMS: [
    {espn: '83', id: '133739', ar: 'برشلونة'},
    {espn: '86', id: '133738', ar: 'ريال مدريد'},
    {espn: '929', id: '136013', ar: 'الهلال'},
    {espn: '817', id: '136022', ar: 'النصر'}
  ],
  // Arabic names for matches' teams and cities (anything missing shows in English).
  TEAM_AR: {'Barcelona': 'برشلونة', 'Real Madrid': 'ريال مدريد', 'Atletico Madrid': 'أتلتيكو مدريد', 'Sevilla': 'إشبيلية',
    'Valencia': 'فالنسيا', 'Villarreal': 'فياريال', 'Athletic Bilbao': 'أتلتيك بلباو', 'Real Sociedad': 'ريال سوسيداد',
    'Real Betis': 'ريال بيتيس', 'Girona': 'جيرونا', 'Getafe': 'خيتافي', 'Osasuna': 'أوساسونا', 'Celta Vigo': 'سلتا فيغو',
    'Bayern Munich': 'بايرن ميونخ', 'Paris SG': 'باريس سان جيرمان', 'Paris Saint-Germain': 'باريس سان جيرمان',
    'Manchester City': 'مانشستر سيتي', 'Manchester United': 'مانشستر يونايتد', 'Liverpool': 'ليفربول', 'Arsenal': 'أرسنال',
    'Chelsea': 'تشيلسي', 'Tottenham': 'توتنهام', 'Inter': 'إنتر', 'Inter Milan': 'إنتر', 'AC Milan': 'ميلان', 'Juventus': 'يوفنتوس',
    'Napoli': 'نابولي', 'Borussia Dortmund': 'دورتموند', 'Benfica': 'بنفيكا', 'Porto': 'بورتو', 'Ajax': 'أياكس',
    'Newcastle United': 'نيوكاسل', 'Aston Villa': 'أستون فيلا', 'Atalanta': 'أتالانتا', 'Monaco': 'موناكو',
    'Al-Hilal': 'الهلال', 'Al Hilal': 'الهلال', 'Al-Nassr': 'النصر', 'Al Nassr': 'النصر', 'Al-Ittihad': 'الاتحاد', 'Al Ittihad': 'الاتحاد',
    'Al-Ahli': 'الأهلي', 'Al Ahli': 'الأهلي', 'Al-Ahli Jeddah': 'الأهلي', 'Al-Qadsiah': 'القادسية', 'Al-Shabab': 'الشباب', 'Al-Ettifaq': 'الاتفاق',
    'Al-Fateh': 'الفتح', 'Al-Taawoun': 'التعاون', 'Al-Fayha': 'الفيحاء', 'Al-Raed': 'الرائد', 'Damac': 'ضمك', 'Al-Khaleej': 'الخليج',
    'Al-Okhdood': 'الأخدود', 'Al-Riyadh': 'الرياض', 'Al-Wehda': 'الوحدة', 'Neom': 'نيوم', 'Al-Hazem': 'الحزم', 'Al-Orobah': 'العروبة',
    'Al Qadsiah': 'القادسية', 'Al Fateh': 'الفتح', 'Al Ettifaq': 'الاتفاق', 'Al Shabab': 'الشباب', 'Al Taawoun': 'التعاون', 'Al Fayha': 'الفيحاء',
    'Al Raed': 'الرائد', 'Al Khaleej': 'الخليج', 'Al Okhdood': 'الأخدود', 'Al Riyadh': 'الرياض', 'Al Wehda': 'الوحدة', 'Al Hazem': 'الحزم',
    'Al Orobah': 'العروبة', 'Al Najma': 'النجمة', 'Al Diriyah': 'الدرعية', 'Abha': 'أبها', 'Al-Shamal': 'الشمال', 'Al Sadd': 'السد',
    'Al Duhail': 'الدحيل', 'Persepolis': 'برسبوليس', 'Sepahan': 'سباهان', 'Pakhtakor': 'باختاكور', 'Al Ain': 'العين', 'Shabab Al Ahli': 'شباب الأهلي',
    'RB Leipzig': 'لايبزيغ', 'Alavés': 'ألافيس', 'Racing Santander': 'راسينغ سانتاندير', 'Atlético Madrid': 'أتلتيكو مدريد', 'Celta Vigo': 'سلتا فيغو',
    'Real Sociedad': 'ريال سوسيداد', 'Athletic Club': 'أتلتيك بلباو', 'Levante': 'ليفانتي', 'Elche': 'إلتشي', 'Málaga': 'ملقا', 'Deportivo': 'ديبورتيفو',
    'Mallorca': 'مايوركا', 'Rayo Vallecano': 'رايو فاليكانو', 'Espanyol': 'إسبانيول', 'Real Oviedo': 'ريال أوفييدو', 'Sporting Gijón': 'سبورتينغ خيخون',
    'Al-Kholood': 'الخلود', 'Al Faisaly': 'الفيصلي', 'Al-Faisaly': 'الفيصلي', 'Al Wasl': 'الوصل', 'Al Kholood': 'الخلود', 'Al-Najma': 'النجمة', 'Al-Akhdoud': 'الأخدود', 'Al-Zulfi': 'الزلفي'},
  LEAGUE_AR: {'Spanish La Liga': 'الدوري الإسباني', 'UEFA Champions League': 'دوري أبطال أوروبا', 'Copa del Rey': 'كأس الملك',
    'Spanish Super Cup': 'السوبر الإسباني', 'FIFA Club World Cup': 'كأس العالم للأندية', 'UEFA Europa League': 'الدوري الأوروبي',
    'UEFA Super Cup': 'السوبر الأوروبي', 'International Friendlies': 'ودية',
    'Saudi-Arabian Pro League': 'دوري روشن السعودي', 'Spanish LALIGA': 'الدوري الإسباني', 'Spanish Copa del Rey': 'كأس ملك إسبانيا', 'Spanish Supercopa': 'السوبر الإسباني', 'AFC Champions League Two': 'دوري أبطال آسيا 2', "King's Cup": 'كأس الملك', 'Saudi Pro League': 'دوري روشن السعودي', 'AFC Champions League': 'دوري أبطال آسيا',
    'AFC Champions League Elite': 'دوري أبطال آسيا للنخبة', "King's Cup": 'كأس الملك', 'Saudi King Cup': 'كأس الملك',
    'Saudi Super Cup': 'السوبر السعودي', 'FIFA Club World Cup': 'كأس العالم للأندية'},
  CITY_AR: {'Dammam': 'الدمام', 'Riyadh': 'الرياض', 'Jeddah': 'جدة', 'Mecca': 'مكة المكرمة', 'Makkah': 'مكة المكرمة',
    'Medina': 'المدينة المنورة', 'Al Khobar': 'الخبر', 'Khobar': 'الخبر', 'Dhahran': 'الظهران', 'Tabuk': 'تبوك', 'Abha': 'أبها',
    'Buraydah': 'بريدة', 'Hail': 'حائل', 'Jazan': 'جازان', 'Najran': 'نجران', 'Taif': 'الطائف', 'Yanbu': 'ينبع', 'Jubail': 'الجبيل',
    'Al Hofuf': 'الهفوف', 'Hofuf': 'الهفوف', 'Qatif': 'القطيف', 'Al Qatif': 'القطيف', 'Khamis Mushait': 'خميس مشيط',
    'Kuwait City': 'مدينة الكويت', 'Doha': 'الدوحة', 'Manama': 'المنامة', 'Dubai': 'دبي', 'Abu Dhabi': 'أبوظبي', 'Muscat': 'مسقط'},
  // The line of writing at the bottom of the home screen. Empty title hides it.
  // left / right: club crests (files in icons/crests/) shown on each side of the title, in screen order.
  BRAND: {title: 'الجلسة', sub: 'ALJALSA', emblem: 'emblem.svg', left: ['barcelona.png', 'hilal.png'], right: ['nassr.png', 'madrid.png']},
  // Match alerts: run by the root service from UEFA's own match API and shown as system notifications over any app
  // (kick-off, goals with the scorer, red card, penalty, VAR, half time, extra time, full time). No match list, no cost for the home page.
  // competitions = UEFA competition ids: 1 = Champions League, 14 = Europa League, 2001 = Conference League. Each switch can be turned off.
  ALERTS: {enabled: true, competitions: [1], kickoff: true, goals: true, redCards: true, penalties: true, var: true, halfTime: true, fullTime: true},
  // Memory: when the home screen is open and less than this many MB are free, background apps are closed (0 = never).
  AUTO_CLEAN_MB: 150,
  SAVER_MINUTES: 3
};
