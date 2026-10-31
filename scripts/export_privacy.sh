#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
if [[ $# != 1 ]]; then
  printf 'Aufruf: scripts/export_privacy.sh neuer-ausgabeordner\n' >&2
  exit 1
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$HOME/.local/share/garnbox/jdk-21.0.13+9"
fi
export PATH="$JAVA_HOME/bin:$PATH"
cd "$ROOT"
./mvnw --batch-mode --no-transfer-progress test-compile
"$JAVA_HOME/bin/java" -cp target/classes:target/test-classes \
  de.appfabrik.garnbox.i18n.PrivacyTextExport "$1"
