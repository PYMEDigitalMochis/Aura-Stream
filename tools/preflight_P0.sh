#!/bin/sh
# Read-only preflight: never installs or downloads anything.
set -u
missing=0
for bin in java javac gradle sdkmanager; do
  if command -v "$bin" >/dev/null 2>&1; then printf 'OK %s: %s\n' "$bin" "$(command -v "$bin")";
  else printf 'PENDIENTE %s\n' "$bin"; missing=1; fi
done
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ] || [ ! -f "$SDK/platforms/android-36/android.jar" ]; then
  echo 'PENDIENTE Android SDK API 36'; missing=1
else echo 'OK Android SDK API 36'; fi
if [ -z "$SDK" ] || [ ! -f "$SDK/build-tools/36.0.0/apksigner" ]; then
  echo 'PENDIENTE Android build-tools 36.0.0'; missing=1
else echo 'OK Android build-tools 36.0.0'; fi
if [ "$missing" -ne 0 ]; then echo 'BLOQUEADO: APK no compilable en este entorno'; exit 2; fi
echo 'OK: puerta de herramientas. AÚN NO ES APK NI QA ANDROID.'
