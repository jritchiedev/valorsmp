#!/usr/bin/env bash
#
# Assembles a runnable Valor SMP alpha server: builds the plugin, downloads Paper and
# Simple Voice Chat, and lays out a server directory ready to launch.
#
# Usage:  scripts/setup-alpha-server.sh [server-dir]
# Env:    JAVA_HOME must point at a Java 21 JDK (Gradle + Paper 1.21.11 require it).
#
set -euo pipefail

MC_VERSION="1.21.11"
SERVER_DIR="${1:-run}"
UA="valorsmp-alpha-builder/0.1 (+https://thevalorsmp.net)"

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

echo "==> Building the plugin JAR"
./gradlew --quiet jar
plugin_jar="$(ls -t build/libs/the-valor-smp-*.jar | head -1)"
echo "    built: $plugin_jar"

echo "==> Preparing server directory: $SERVER_DIR"
mkdir -p "$SERVER_DIR/plugins"
printf 'eula=true\n' > "$SERVER_DIR/eula.txt"
if [ ! -f "$SERVER_DIR/server.properties" ]; then
  cat > "$SERVER_DIR/server.properties" <<'PROPS'
online-mode=true
motd=Valor SMP (alpha)
view-distance=8
spawn-protection=0
PROPS
fi
cp "$plugin_jar" "$SERVER_DIR/plugins/"

echo "==> Resolving latest Paper $MC_VERSION build"
paper_url="$(curl -fsSL -A "$UA" \
  "https://fill.papermc.io/v3/projects/paper/versions/$MC_VERSION/builds/latest" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['downloads']['server:default']['url'])")"
paper_jar="$SERVER_DIR/paper-$MC_VERSION.jar"
echo "    downloading: $paper_url"
curl -fsSL -A "$UA" "$paper_url" -o "$paper_jar"

echo "==> Resolving Simple Voice Chat (paper) for $MC_VERSION"
svc_url="$(curl -fsSL -A "$UA" \
  "https://api.modrinth.com/v2/project/simple-voice-chat/version?loaders=%5B%22paper%22%5D&game_versions=%5B%22$MC_VERSION%22%5D" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print(d[0]['files'][0]['url'] if d else '')")"
if [ -n "$svc_url" ]; then
  echo "    downloading: $svc_url"
  curl -fsSL -A "$UA" "$svc_url" -o "$SERVER_DIR/plugins/$(basename "$svc_url")"
else
  echo "    WARNING: no Simple Voice Chat build found for $MC_VERSION; skipping."
fi

echo
echo "Done. Launch with:"
echo "  cd $SERVER_DIR && \"\$JAVA_HOME/bin/java\" -Xms1G -Xmx2G -jar \"$(basename "$paper_jar")\" --nogui"
