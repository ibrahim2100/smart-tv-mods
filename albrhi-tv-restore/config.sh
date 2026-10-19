# Albrhi TV - shared settings for restore.sh / revert.sh / check.sh
DEV_DEFAULT="192.168.1.50:5555"

APP_PKG="com.albrhi.tvwidget"
HOME_ACT="com.albrhi.tvwidget/.HomeActivity"
DREAM="com.albrhi.tvwidget/.BarcaDream"

# Google TV home + its fallback home. Disabling both is what lets Albrhi Home take the HOME button.
GTV_LAUNCHER="com.google.android.apps.tv.launcherx"
GTV_HOME_ACT="com.google.android.apps.tv.launcherx/.home.HomeActivity"
GTV_SETUP="com.google.android.tungsten.setupwraith"
GTV_DREAM="com.google.android.apps.tv.dreamx/.service.Backdrop"

# Haier / MediaTek apps we switch off (disable-user = reversible with `pm enable`)
DISABLE_PKGS="com.haier.emanual com.haierubic.usmartdevice com.haier.apps com.haier.tv.hmagicboard com.haier.gamingbar com.haier.bt_pair_tips com.haier.launcher.haiermatrix com.haier.tvhousekeeper com.mediatek.androidbox"

# System apps removed for the current user (come back with `pm install-existing`)
REMOVE_SYSTEM_PKGS="com.google.android.videos com.google.android.play.games com.google.android.youtube.tvmusic"

# Apps you installed that we removed (they need Play Store / the original APK to come back)
REMOVE_USER_PKGS="com.tcl.browser com.ionitech.airscreen com.app.SpeedTest nextapp.fx"
