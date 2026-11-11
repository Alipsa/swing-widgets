#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

for tool in mvn groovy; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Required command not found: $tool. Install Maven and Groovy and add them to PATH." >&2
    exit 1
  fi
done

mvn -q compile

# Ignore the demo's published dependency and use the freshly compiled local code.
exec groovy -cp "$PWD/target/classes" -Dgroovy.grape.enable=false \
  examples/AllControlsDemo.groovy "$@"
