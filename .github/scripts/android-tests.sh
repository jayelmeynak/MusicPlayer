#!/usr/bin/env bash
# Инструментальные UI-тесты на запущенном эмуляторе CI: один повтор при падении (нестабильность видна
# предупреждением), затем logcat и артефакты Kaspresso в app/build/ci-artifacts. AdbServer не нужен.
set -euo pipefail

out=app/build/ci-artifacts
kaspresso_dir=/sdcard/Android/data/com.jayelmeynak.musicplayer/files/kaspresso
mkdir -p "$out"
# Буфер logcat по умолчанию мал: к концу прогона начало теряется.
adb logcat -G 16M || true
adb logcat -c || true

# APK остаются после прогона: иначе Gradle удалит приложение вместе с артефактами Kaspresso до adb pull.
run_tests() {
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
}

status=0
if ! run_tests; then
  echo "::warning title=UI-тесты::Первый прогон упал, повтор. Отчёт первой попытки — в артефактах."
  cp -R app/build/outputs/androidTest-results "$out/first-attempt-results" 2>/dev/null || true
  cp -R app/build/reports/androidTests "$out/first-attempt-reports" 2>/dev/null || true
  echo "flaky=true" >> "${GITHUB_OUTPUT:-/dev/null}"
  if run_tests; then
    echo "::warning title=Нестабильные UI-тесты::Прошли со второй попытки — тест нестабилен, разберите отчёт первой."
  else
    status=1
  fi
fi

adb logcat -d > "$out/logcat.txt" 2>/dev/null || true
adb pull "$kaspresso_dir" "$out/kaspresso" > /dev/null 2>&1 || true
exit "$status"
