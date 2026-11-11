#!/usr/bin/env bash
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"

# The build plugins (spotless-maven-plugin 3.x) need Java 17 or newer to run. The published jar
# still targets Java 11 because pom.xml sets maven.compiler.release=11.
# Check the JDK Maven will use: JAVA_HOME when set (SDKMAN sets it), otherwise java on PATH.
java_major() {
  "${JAVA_HOME:+$JAVA_HOME/bin/}java" -version 2>&1 | awk -F'"' '/version/ { split($2, v, "."); print (v[1] == "1" ? v[2] : v[1]); exit }'
}

if [[ -z "$(java_major)" || "$(java_major)" -lt 17 ]]; then
  if command -v sdk >/dev/null 2>&1; then
    for candidate in "$HOME"/.sdkman/candidates/java/21.* "$HOME"/.sdkman/candidates/java/17.* "$HOME"/.sdkman/candidates/java/25.*; do
      if [[ -d "$candidate" ]]; then
        sdk use java "$(basename "$candidate")" >/dev/null
        break
      fi
    done
  fi
fi

major="$(java_major)"
if [[ -z "$major" || "$major" -lt 17 ]]; then
  echo "Releasing requires JDK 17 or newer (found: ${major:-none}). Install one, e.g. with 'sdk install java 21-librca'." >&2
  exit 1
fi
echo "Releasing with Java $major"
mvn -Prelease clean site deploy
