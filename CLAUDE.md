# Two projects in this folder — never mix them

| Project | TV | Folders | Tools |
|---|---|---|---|
| **Google TV** | Haier MatrixTV BE, Android 14, `192.168.1.50:5555` | `albrhi-widget/`, `albrhi-tv-restore/` | `adb`, `build.sh`, `restore.sh`; this folder's git repo |
| **LG webOS** | LG webOS 7.5 rooted, address in `aljalsa-tv/scripts/tv.conf` | `aljalsa-tv/` | telnet :23, `scripts/*.sh`; its own git repo → github.com/ibrahim2100/WebOS |

Rules:
- Work only inside the folder of the project the user means. If unclear which TV, ask.
- `aljalsa-tv/` has its own `CLAUDE.md` with the webOS rules (never kill the home page, no TV scripts unless the user asks to upload, etc.). Read it before touching that project.
- The Google TV project is stable: edit it only on explicit request. `albrhi-widget/src` and `albrhi-tv-restore/source/src` are identical copies.
- Never run `adb` scripts against the LG or telnet scripts against the Google TV.
- "Albrhi TV" in old notes = the Google TV launcher; the LG project is called Aljalsa / WebOS.
