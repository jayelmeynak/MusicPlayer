#!/usr/bin/env bash
# Инструментальные UI-тесты на запущенном эмуляторе CI по готовым APK, без Gradle и пересборки:
# adb install -r обоих APK, am instrument, разбор вывода (instrumentation_report.py) в сводку и JUnit XML.
# Один повтор при падении (нестабильность видна предупреждением); после каждой попытки её logcat и артефакты
# Kaspresso — в app/build/ci-artifacts/attempt-<N>. AdbServer не нужен.
# Использование: android-tests.sh [app.apk] [androidTest.apk]; по умолчанию — выходы Gradle
# (:app:assembleDebug :app:assembleDebugAndroidTest). Устройство — единственное подключённое или ANDROID_SERIAL.
# Приложение не удаляется и данные не стираются: install -r их сохраняет, am force-stop только останавливает
# процесс. На устройстве меняются лишь буфер лога (logcat -G — размер, logcat -c — очистка) и каталог
# артефактов Kaspresso (удаляется перед каждой попыткой). Таймаут am instrument — INSTRUMENT_TIMEOUT
# (по умолчанию 20m; нужен timeout или gtimeout, иначе без таймаута с предупреждением).
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
# Отчёты прошлого локального прогона.
rm -rf "$out"/attempt-* "$out/kaspresso" "$out/logcat.txt"

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
# Аргумент в другом виде скрипт не разберёт, а Gradle передаст: такое расхождение — ошибка, а не тихий пропуск.
declared=$(grep -v '^ *//' "$root/app/build.gradle.kts" | grep -c 'testInstrumentationRunnerArguments' || true)
if (( declared != ${#runner_args[@]} / 2 )); then
  echo "::error title=UI-тесты::testInstrumentationRunnerArguments в app/build.gradle.kts: строк $declared," \
    "разобрано $(( ${#runner_args[@]} / 2 )). Поддержан только вид testInstrumentationRunnerArguments[\"k\"] = \"v\"."
  exit 1
fi
echo "Instrumentation: $instrumentation, аргументы: ${runner_args[*]:-нет}"

# Общий таймаут: зависание при старте процесса (до таймаута на тест) не съедает таймаут job'а, артефакты
# собираются. Без timeout/gtimeout (macOS без coreutils) — без таймаута.
instrument_timeout=${INSTRUMENT_TIMEOUT:-20m}
timeout_cmd=()
if command -v timeout > /dev/null; then
  timeout_cmd=(timeout "$instrument_timeout")
elif command -v gtimeout > /dev/null; then
  timeout_cmd=(gtimeout "$instrument_timeout")
else
  echo "::warning title=UI-тесты::Нет timeout/gtimeout — am instrument без общего таймаута."
fi

# Буфер logcat по умолчанию мал: к концу прогона начало теряется.
adb logcat -G 16M || true

# am instrument возвращает 0 и при упавших тестах: итог — по разбору вывода.
run_tests() {
  local dir="$out/attempt-$1" rc=0 instrument_rc
  mkdir -p "$dir"
  # Каталог Kaspresso на устройстве — только артефакты прошлых прогонов; данные приложения не трогаются.
  adb shell rm -rf "$kaspresso_dir" || true
  adb logcat -c || true
  set +e
  ${timeout_cmd[@]+"${timeout_cmd[@]}"} adb shell am instrument -w -r ${runner_args[@]+"${runner_args[@]}"} \
    "$instrumentation" | tee "$dir/instrument.txt"
  instrument_rc=${PIPESTATUS[0]}
  set -e
  if (( instrument_rc == 124 )); then
    echo "::error title=UI-тесты::am instrument не завершился за $instrument_timeout (попытка $1)."
    adb shell am force-stop "$app_package" || true
  fi
  (( instrument_rc == 0 )) || rc=1
  python3 "$report" "$dir/instrument.txt" "$dir/TEST-android-tests.xml" "android-tests-attempt-$1" \
    | tee "$dir/summary.txt" || rc=1
  adb logcat -d > "$dir/logcat.txt" 2>/dev/null || true
  adb pull "$kaspresso_dir" "$dir/kaspresso" > /dev/null 2>&1 || true
  if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    { echo "### UI-тесты, попытка $1"; echo '```'; cat "$dir/summary.txt"; echo '```'; } >> "$GITHUB_STEP_SUMMARY"
  fi
  return "$rc"
}

status=0
if ! run_tests 1; then
  echo "::warning title=UI-тесты::Первый прогон упал, повтор. Отчёт первой попытки — в артефактах (attempt-1)."
  # Процесс прошлой попытки (возможно, зависший) — остановить; force-stop данные не стирает.
  adb shell am force-stop "$app_package" || true
  if run_tests 2; then
    # flaky — только когда повтор прошёл: при двух падениях job и так failure, артефакты выгружаются.
    echo "flaky=true" >> "${GITHUB_OUTPUT:-/dev/null}"
    echo "::warning title=Нестабильные UI-тесты::Прошли со второй попытки — тест нестабилен, разберите отчёт первой."
  else
    status=1
  fi
fi

exit "$status"
