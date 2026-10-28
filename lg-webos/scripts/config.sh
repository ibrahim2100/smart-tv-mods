#!/bin/bash
# Shared config for the LG webOS restore tools. Source this, don't run it directly.
# The TV's address: export TV_IP=192.168.x.y, or put TV_IP=192.168.x.y in scripts/tv.conf (that file is not committed).
_here="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
[ -f "$_here/tv.conf" ] && . "$_here/tv.conf"
TV_IP="${TV_IP:?set TV_IP (export TV_IP=192.168.x.y) or create scripts/tv.conf, see tv.conf.example}"
TELNET_PORT=23

# Runs a shell command on the TV's root telnet shell and prints its output.
# Usage: tv_sh "command; command2"
tv_sh() {
  local cmd="$1"
  ( printf '%s\r\n' "$cmd"; sleep 2; printf 'exit\r\n' ) | nc -w 10 "$TV_IP" "$TELNET_PORT" 2>&1 \
    | sed '1,2d'   # drop the banner + blank line
}

# Pushes a local file to the TV at a given remote path.
# The TV's busybox nc has no listen mode (-l), only outbound client mode - so for a push,
# the Mac listens and feeds the file, and the TV connects out and writes what it reads.
# Usage: tv_push local/file /remote/path
tv_push() {
  local local_file="$1" remote_path="$2" port=9193
  local mac_ip
  mac_ip=$(ipconfig getifaddr en0)
  [ -f "$local_file" ] || { echo "no such local file: $local_file"; return 1; }
  [ -n "$mac_ip" ] || { echo "could not find this Mac's IP on en0"; return 1; }
  ( nc -l "$port" < "$local_file" ) &
  local pid=$!
  sleep 1
  tv_sh "nc $mac_ip $port > $remote_path" >/dev/null
  sleep 1
  wait "$pid" 2>/dev/null
}

# Pulls a remote file on the TV down to a local path, via a one-shot TCP listener on this Mac.
# Usage: tv_pull /remote/path local/file
tv_pull() {
  local remote_path="$1" local_file="$2" port=9192
  local mac_ip
  mac_ip=$(ipconfig getifaddr en0)
  [ -n "$mac_ip" ] || { echo "could not find this Mac's IP on en0"; return 1; }
  rm -f "$local_file"
  ( nc -l "$port" > "$local_file" ) &
  local pid=$!
  sleep 1
  tv_sh "nc $mac_ip $port < $remote_path" >/dev/null
  sleep 1
  wait "$pid" 2>/dev/null
}
