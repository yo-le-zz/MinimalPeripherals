#!/usr/bin/env bash
# Compiles the peripherals against small stubs of the CC: Tweaked API, then runs the Lua test programs
# against them. No Minecraft, no Gradle, no network needed.
#
#   tools/offline-test/run.sh                      # runs both test programs
#   tools/offline-test/run.sh examples/engines_test.lua -v
#
# Needs: a JDK 21, BouncyCastle (apt install libbcprov-java, or set BCPROV=/path/to/bcprov.jar),
#        python3 with `pip install jpype1 lupa`.
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(cd ../.. && pwd)"
BC="${BCPROV:-/usr/share/java/bcprov.jar}"
OUT="$ROOT/build/offline-test"
J="$ROOT/common/src/main/java/cryptographyperipherals"

rm -rf "$OUT" && mkdir -p "$OUT"
javac -cp "$BC" -d "$OUT" $(find stubs -name '*.java') "$J"/core/*.java "$J"/utils/*.java "$J/Limits.java" "$J"/peripheral/*.java

if [ "$#" -gt 0 ]; then
    file="$1"; shift
    python3 run.py "$OUT" "$BC" "$ROOT/$file" "$@"
else
    for file in examples/crypto_test.lua examples/engines_test.lua; do
        echo "### $file"
        python3 run.py "$OUT" "$BC" "$ROOT/$file"
    done
fi
