# Two projects, two top-level folders — never mix them

```
google-tv/   Haier MatrixTV BE (Google TV, Android 14)  — albrhi-widget/ (source), albrhi-tv-restore/ (restore kit)
lg-webos/    LG webOS 7.5, rooted                       — own git repo → github.com/ibrahim2100/WebOS
```

| | `google-tv/` | `lg-webos/` |
|---|---|---|
| Tools | `adb` at 192.168.1.50:5555, `build.sh`, `restore.sh` | telnet :23, `scripts/*.sh`, `npm test` |
| Git | this folder's repo (local) | its own repo; the outer repo ignores it |
| Docs | `google-tv/README.md` | `lg-webos/CLAUDE.md` (read first), `lg-webos/docs/` |
| Status | stable: edit only on explicit request | active |

Rules:
- Work only inside the folder of the project the user means; if unclear which TV, ask.
- `lg-webos/CLAUDE.md` holds the webOS rules (never kill the home page, no TV-connecting scripts unless the user asks to upload, etc.).
- Never run `adb` scripts against the LG TV or telnet scripts against the Google TV.
- Keep `albrhi-widget/` and `albrhi-tv-restore/` side by side (`pack.sh` writes to `../albrhi-tv-restore`).
- "Albrhi TV" in old notes = the Google TV launcher; the LG project is Aljalsa / WebOS.
