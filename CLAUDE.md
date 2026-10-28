# Read AGENTS.md first

The canonical guide for AI agents is [`AGENTS.md`](AGENTS.md) (prerequisites, how to run, how to build your own version, safety). The notes below are the short version.

# Two projects, two top-level folders — never mix them

```
google-tv/   Haier MatrixTV BE (Google TV, Android 14)  — albrhi-widget/ (source), albrhi-tv-restore/ (restore kit)
lg-webos/    LG webOS 7.5, rooted
```

| | `google-tv/` | `lg-webos/` |
|---|---|---|
| Tools | `adb` at 192.168.1.50:5555, `build.sh`, `restore.sh` | telnet :23, `scripts/*.sh`, `npm test` |
| Git | one repo for both: github.com/ibrahim2100/smart-tv-mods (public) | same repo |
| Docs | `google-tv/README.md` | `lg-webos/CLAUDE.md` (read first), `lg-webos/docs/` |
| Status | stable: edit only on explicit request | active |

Rules:
- Work only inside the folder of the project the user means; if unclear which TV, ask.
- `lg-webos/CLAUDE.md` holds the webOS rules (never kill the home page, no TV-connecting scripts unless the user asks to upload, etc.).
- Never run `adb` scripts against the LG TV or telnet scripts against the Google TV.
- Keep `albrhi-widget/` and `albrhi-tv-restore/` side by side (`pack.sh` writes to `../albrhi-tv-restore`).
- "Albrhi TV" in old notes = the Google TV launcher; the LG project is Aljalsa / WebOS.

## Public repo hygiene (the repo is PUBLIC)
- Never commit: real IPs (use the `*.example` files), `*.keystore`, `backups/`, `*.apk`, tokens, personal email. Commits use the GitHub noreply address `31652152+ibrahim2100@users.noreply.github.com`.
- Before every push run `git ls-files | grep -iE 'keystore|\.apk|backups|tv\.conf|device\.conf'` (must be empty) and `git grep -nE '192\.168\.(1\.54|8\.112)'` (must be empty).
- Local machine files stay ignored: `lg-webos/scripts/tv.conf`, `google-tv/albrhi-tv-restore/device.conf`.
