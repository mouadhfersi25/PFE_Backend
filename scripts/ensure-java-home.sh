#!/usr/bin/env bash
# Corrige JAVA_HOME si l'outil Jenkins JDK est mal configure.
# A sourcer : . scripts/ensure-java-home.sh

if [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
  if command -v java >/dev/null 2>&1; then
    JAVA_BIN="$(readlink -f "$(command -v java)")"
    export JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
  fi
fi

if [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
  echo "ERREUR : JAVA_HOME invalide (${JAVA_HOME:-non defini}). Configure le JDK dans Manage Jenkins > Tools."
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
echo "JAVA_HOME=$JAVA_HOME"
java -version
