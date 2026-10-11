#!/usr/bin/env bash
# Инструментальные UI-тесты на запущенном эмуляторе CI по готовым APK, без Gradle и пересборки:
# adb install -r обоих APK, am instrument, разбор вывода (instrumentation_report.py) в сводку и JUnit XML.
# Один повтор при падении (нестабильность видна предупреждением), затем logcat и артефакты Kaspresso
# в app/build/ci-artifacts. AdbServer не нужен.
# Использование: android-tests.sh [app.apk] [androidTest.apk]; по умолчанию — выходы Gradle
# (:app:assembleDebug :app:assembleDebugAndroidTest). Устройство — единственное подключённое или ANDROID_SERIAL.
# Приложение не удаляется и данные не стираются: install -r их сохраняет.
set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd)
app_apk=${1:-$root/app/build/outputs/apk/debug/app-debug.apk}
test_apk=${2:-$root/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk}
app_package=com.jayelmeynak.musicplayer
out=$root/app/build/ci-artifacts
kaspresso_dir=/sdcard/Android/data/$app_package/files/kaspresso
report=$root/.github/scripts/instrumentation_report.py

for apk in "$app_apk" "$test_apk"; do
  [[ -f "$apk" ]] || { echo "::error title=UI-тесты::Нет APK: $apk"; exit 1; }
done
mkdir -p "$out"
rm -rf "$out"/attempt-*  # отчёты прошлого локального прогона

adb install -r -t "$app_apk"
adb install -r -t "$test_apk"

# Runner и тестовый пакет — из установленного test APK (pm list instrumentation), а не из констант.
instrumentation=$(adb shell pm list instrumentation | tr -d '\r' \
  | sed -n "s/^instrumentation:\([^ ]*\) (target=$app_package)$/\1/p" | head -n 1)
[[ -n "$instrumentation" ]] || { echo "::error title=UI-тесты::Нет instrumentation для $app_package"; exit 1; }

# Аргументы runner'а — testInstrumentationRunnerArguments из app/build.gradle.kts (как передаёт Gradle).
runner_args=()
while IFS='=' read -r key value; do
  runner_args+=(-e "$key" "$value")
done < <(sed -n 's/^ *testInstrumentationRunnerArguments\["\([^"]*\)"\] *= *"\([^"]*\)".*/\1=\2/p' \
  "$root/app/build.gradle.kts")
echo "Instrumentation: $instrumentation, аргументы: ${runner_args[*]:-нет}"

# Буфер logcat по умолчанию мал: к концу прогона начало теряется.
adb logcat -G 16M || true
adb logcat -c || true

# am instrument возвращает 0 и при упавших тестах: итог — по разбору вывода.
run_tests() {
  local dir="$out/attempt-$1" rc=0
  mkdir -p "$dir"
  adb shell am instrument -w -r ${runner_args[@]+"${runner_args[@]}"} "$instrumentation" | tee "$dir/instrument.txt" || rc=1
  python3 "$report" "$dir/instrument.txt" "$dir/TEST-android-tests.xml" "android-tests-attempt-$1" \
    | tee "$dir/summary.txt" || rc=1
  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    { echo "### UI-тесты, попытка $1"; echo '```'; cat "$dir/summary.txt"; echo '```'; } >> "$GITHUB_STEP_SUMMARY"
  fi
  return "$rc"
}

status=0
if ! run_tests 1; then
  echo "::warning title=UI-тесты::Первый прогон упал, повтор. Отчёт первой попытки — в артефактах (attempt-1)."
  echo "flaky=true" >> "${GITHUB_OUTPUT:-/dev/null}"
  if run_tests 2; then
    echo "::warning title=Нестабильные UI-тесты::Прошли со второй попытки — тест нестабилен, разберите отчёт первой."
  else
    status=1
  fi
fi

adb logcat -d > "$out/logcat.txt" 2>/dev/null || true
adb pull "$kaspresso_dir" "$out/kaspresso" > /dev/null 2>&1 || true
exit "$status"
