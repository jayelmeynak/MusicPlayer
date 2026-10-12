# MusicPlayer

Модульное Android-приложение на Kotlin и Jetpack Compose: музыкальный плеер с 30-секундными превью треков из Deezer API и воспроизведением аудиофайлов устройства. Воспроизведение — ExoPlayer (Media3) в `MediaSessionService`, UI управляет им через `MediaController`.

## Функциональность

- **Чарт и поиск Deezer.** Список треков чарта с повтором при ошибке и поиск с debounce.
- **Аудио устройства.** Список аудиофайлов из MediaStore с поиском.
- **Очередь.** Тап по треку ставит в очередь весь видимый список (чарт, выдача поиска или локальная вкладка); next/prev идут по нему.
- **Фоновое воспроизведение.** Стандартное уведомление Media3 с обложкой, управление из шторки и с Bluetooth/гарнитуры. Сессия доступна только своему приложению и доверенным системным контроллерам.
- **Экран плеера.** Обложка, перемотка, play/pause с индикатором буферизации; для превью Deezer — подпись «Превью 30 с» и полная длительность трека.
- **Мини-плеер.** Виден на экранах списков, пока очередь не пуста; нажатие открывает экран плеера.
- **Навигация.** Navigation3: вкладки «Remote» (Deezer) и «Local» (устройство) с отдельными back stack; вкладка и стеки переживают поворот и пересоздание Activity.
- **Тема.** Material 3, светлая и тёмная.

## Модули

```
app/                      # точка входа, навигация (Navigation3), маппинг списков в очередь плеера
build-logic/convention/   # convention-плагины Gradle (included build)
util/
  result/                 # Result, DataError (JVM)
  coroutines/             # квалификаторы диспетчеров и application scope (JVM)
  testing/                # MainDispatcherRule для тестов (JVM)
lib/
  network/                # Deezer API: Retrofit, OkHttp, Gson
  database/               # Room-кэш обложек локальных треков
  mediastore/             # чтение MediaStore, кэш обложек локальных треков
  designsystem/           # тема Material 3, общие Compose-компоненты, UiText
  navigation/             # контракт навигации: Navigator, EntryInstaller (JVM)
feature/player/
  api/                    # контракты воспроизведения: PlaybackController, QueueItem, PlayerDestination (JVM)
  impl/                   # сервис, ExoPlayer/MediaSession, контроллер, экран плеера
  ui/                     # мини-плеер
  di/                     # Hilt-привязки контрактов
feature/catalog/
  api/                    # ключ экрана каталога ChartDestination (JVM)
  impl/                   # чарт и поиск Deezer, резолвер ссылок превью для плеера
  di/                     # Hilt-привязки экрана и резолвера
feature/local-music/
  api/                    # ключ вкладки «Local» LocalMusicDestination (JVM)
  impl/                   # аудиофайлы устройства, поиск, резолвер локальных треков для плеера
  di/                     # Hilt-привязки экрана и резолвера
```

Зависимости: `app` → `feature:player:api|ui|di`, `feature:catalog:api|di`, `feature:local-music:api|di`, `lib:designsystem`, `lib:navigation`, `util:coroutines`; `feature:*:impl` → `lib:*` → `util:*`; `feature:<домен>:impl` подключается только из своего `:di` (это проверяет сборка), фичи друг от друга не зависят. Архитектура — Clean Architecture и MVVM на Compose; все фичи — модули `feature:<домен>:api|impl|ui|di`.

## Технологии

- Kotlin 2.3.20, Jetpack Compose (BOM 2026.04.01), Material 3
- Navigation3 1.1.1
- Hilt 2.59.2 (KSP)
- Retrofit 3.0.0, OkHttp, Gson
- Room 2.8.4
- Media3 1.10.0 (ExoPlayer, MediaSession, MediaController)
- Coil — изображения
- Корутины и Flow
- Тесты: JUnit4, kotlinx-coroutines-test, Turbine, Robolectric, MockWebServer

Версии — в `gradle/libs.versions.toml`; SDK и JVM — в `build-logic` (`MusicPlayerBuild.kt`): minSdk 29, targetSdk 36, compileSdk 37, JVM 17. Сборка — AGP 9.1.1.

## Сборка

Нужны JDK 17 и Android SDK 37.

```bash
git clone https://github.com/jayelmeynak/MusicPlayer.git
cd MusicPlayer
./gradlew assembleDebug                              # debug APK
./gradlew installDebug                               # установка на устройство или эмулятор
./gradlew lintDebug testDebugUnitTest assembleDebug  # проверки, как в CI
```

Все тесты локальные (`src/test`), инструментальных тестов нет. CI (GitHub Actions) запускает lint, unit-тесты и сборку debug APK на каждый PR и push в `main`.

Для вкладки «Local» на устройстве должны быть аудиофайлы и выдано разрешение на чтение аудио (`READ_MEDIA_AUDIO` на Android 13+, `READ_EXTERNAL_STORAGE` ниже). На Android 13+ уведомлению плеера нужно разрешение `POST_NOTIFICATIONS`.
