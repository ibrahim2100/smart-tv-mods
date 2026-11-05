#!/usr/bin/env python3
"""Fetch the LOCAL KEYS of your Smart Life / Tuya devices once, so the launcher can control them on your LAN.

Needs a free Tuya IoT Cloud project that is linked to your Smart Life account (see the steps printed with --help).
Python 3 standard library only. Nothing is sent anywhere except to Tuya's own cloud API.

Two private files (permissions 600) are written OUTSIDE this repository, in ~/.albrhi/:
  tuya-devices.json    every device with its local key
  lights-import.json   what the launcher imports (devices, their buttons, and the cloud credentials for the fallback)
Keys and the Access Secret are never printed. Never commit or share those files.

  python3 tuya-keys.py
"""
import getpass, hashlib, hmac, json, os, stat, sys, time, urllib.error, urllib.request

REGIONS = {
    "1": ("Central Europe (EU)", "https://openapi.tuyaeu.com"),
    "2": ("India", "https://openapi.tuyain.com"),
    "3": ("Western America (US)", "https://openapi.tuyaus.com"),
    "4": ("China", "https://openapi.tuyacn.com"),
    "5": ("Singapore", "https://openapi-sg.iotbing.com"),
}
OUT = os.path.expanduser("~/.albrhi/tuya-devices.json")
EMPTY_SHA = hashlib.sha256(b"").hexdigest()


def sign_request(client_id, secret, method, path, token="", body=b""):
    t = str(int(time.time() * 1000))
    string_to_sign = "\n".join([method, hashlib.sha256(body).hexdigest(), "", path])
    msg = client_id + token + t + string_to_sign
    sig = hmac.new(secret.encode(), msg.encode(), hashlib.sha256).hexdigest().upper()
    headers = {"client_id": client_id, "sign": sig, "t": t, "sign_method": "HMAC-SHA256"}
    if token:
        headers["access_token"] = token
    return headers


def call(base, client_id, secret, path, token=""):
    headers = sign_request(client_id, secret, "GET", path, token)
    req = urllib.request.Request(base + path, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return {"success": False, "msg": "HTTP %s" % e.code}


def explain(res):
    code = res.get("code")
    hints = {
        1004: "sign invalid: check the Access ID / Secret and the region (data center).",
        1106: "permission denied: in the cloud project, subscribe the 'IoT Core', 'Authorization Token Management' and 'Smart Home Basic Service' APIs, and link your Smart Life account (Devices > Link App Account).",
        1010: "token invalid or expired.",
        2406: "no permission to this data: link your Smart Life account to the project first.",
    }
    return "%s (code %s). %s" % (res.get("msg"), code, hints.get(code, ""))


def main():
    if "--help" in sys.argv or "-h" in sys.argv:
        print(__doc__)
        print("""Steps (once):
 1. Go to https://iot.tuya.com and sign in / register (free).
 2. Cloud > Development > Create Cloud Project: any name, industry "Smart Home", development method "Smart Home",
    and choose the data center of your Smart Life account (Saudi Arabia is usually Central Europe or India).
 3. In the project: Service API: make sure 'IoT Core', 'Authorization Token Management' and 'Smart Home Basic Service'
    are subscribed (free trial/plan is fine).
 4. Devices > Link App Account > Add App Account: scan the QR code with the Smart Life app (Me > the scan icon at the top).
 5. Overview tab: copy the Access ID/Client ID and the Access Secret. Run this script and paste them when asked.""")
        return
    print("Data center of your Tuya cloud project:")
    for k, (name, _) in REGIONS.items():
        print("  %s) %s" % (k, name))
    choice = input("Choose [1]: ").strip() or "1"
    if choice not in REGIONS:
        sys.exit("unknown choice")
    base = REGIONS[choice][1]
    client_id = input("Access ID / Client ID: ").strip()
    secret = getpass.getpass("Access Secret (hidden): ").strip()

    res = call(base, client_id, secret, "/v1.0/token?grant_type=1")
    if not res.get("success"):
        sys.exit("Token request failed: " + explain(res))
    token = res["result"]["access_token"]

    devices, last_row = [], ""
    while True:
        path = "/v1.0/iot-01/associated-users/devices" + ("?last_row_key=" + last_row if last_row else "")
        res = call(base, client_id, secret, path, token)
        if not res.get("success"):
            sys.exit("Device list failed: " + explain(res))
        r = res["result"]
        devices += r.get("devices", [])
        if not r.get("has_more"):
            break
        last_row = r.get("last_row_key", "")
        if not last_row:
            break

    keep = [{"id": d.get("id"), "name": d.get("name"), "ip": d.get("ip"), "local_key": d.get("local_key"),
             "category": d.get("category"), "product_name": d.get("product_name"), "online": d.get("online")}
            for d in devices]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        json.dump(keep, f, indent=1, ensure_ascii=False)
    os.chmod(OUT, stat.S_IRUSR | stat.S_IWUSR)
    # Which buttons (gangs) each switch has: the cloud status lists them as switch_1, switch_2, ...
    import re
    imp = []
    for d in keep:
        gangs = []
        st = call(base, client_id, secret, "/v1.0/devices/%s/status" % d["id"], token)
        for item in (st.get("result") or []) if st.get("success") else []:
            m = re.match(r"^switch_?(\d*)$", str(item.get("code")))
            if m and isinstance(item.get("value"), bool):
                n = m.group(1) or "1"
                gangs.append({"dp": n, "code": item["code"], "name": "%s %s" % (d["name"], n)})
        gangs.sort(key=lambda g: int(g["dp"]))
        imp.append({"id": d["id"], "name": d["name"], "local_key": d["local_key"], "gangs": gangs})
    imp_path = os.path.join(os.path.dirname(OUT), "lights-import.json")
    with open(imp_path, "w") as f:
        json.dump({"cloud": {"id": client_id, "secret": secret, "base": base}, "devices": imp}, f, indent=1, ensure_ascii=False)
    os.chmod(imp_path, stat.S_IRUSR | stat.S_IWUSR)
    print("\nFound %d device(s). Saved to %s and %s (keys not shown):" % (len(keep), OUT, imp_path))
    for d in keep:
        n = [len(x["gangs"]) for x in imp if x["id"] == d["id"]][0]
        print("  - %-28s id=%s  %s  %s  buttons=%d" % (d["name"], d["id"], d["category"], "online" if d["online"] else "offline", n))


if __name__ == "__main__":
    main()
