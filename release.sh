#!/usr/bin/env bash
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"

if command -v jdk11 >/dev/null 2>&1; then
  . jdk11
fi
mvn -Prelease clean site deploy