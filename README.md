# MusicPlayer

Модульное Android-приложение на Kotlin и Jetpack Compose: музыкальный плеер с превью треков из Deezer API и воспроизведением аудиофайлов устройства. Воспроизведение — ExoPlayer (Media3) в foreground-сервисе с `MediaSession`.

## Функциональность

- **Чарт и поиск Deezer.** Список треков чарта и поиск с debounce.
- **Аудио устройства.** Список аудиофайлов из MediaStore с поиском. При воспроизведении локального трека в очереди весь список, между треками можно переключаться.
- **Фоновое воспроизведение.** Foreground-сервис и `MediaSession`; уведомление с названием, исполнителем и обложкой.
- **Мини-плеер.** Виден на экранах списков, пока очередь не пуста. Нажатие открывает полноэкранный плеер.
- **Навигация.** Navigation3: вкладки «Remote» (Deezer) и «Local» (устройство) с отдельными back stack, экран плеера открывается поверх вкладки через `NavDisplay`.
- **Тема.** Material 3, светлая и тёмная.

## Модули

```
app/                      # точка входа, навигация (Navigation3), мини-плеер
core/
  network/                # Deezer API: Retrofit, OkHttp, Gson; Result/DataError
  local/                  # MediaStore и Room-кэш обложек
  ui/                     # тема Material 3, UiText
features/
  player/                 # ExoPlayer/Media3, сервис, уведомление, экран плеера
  search-tracks/          # чарт и поиск Deezer
  download-tracks/        # аудиофайлы устройства
```

Зависимости направлены `app` → `features:*` → `core:*`; фичи друг от друга не зависят. Архитектура — Clean Architecture и MVVM; идёт поэтапная миграция на модули `feature:*:api|impl|ui|di`, `lib:*`, `util:*`.

## Технологии

- Kotlin, Jetpack Compose, Material 3
- Navigation3
- Hilt (KSP)
- Retrofit, OkHttp, Gson
- Room
- Media3 (ExoPlayer, MediaSession)
- Coil — изображения в UI; Glide — обложка в уведомлении
- Корутины и Flow

Версии — в `gradle/libs.versions.toml`. minSdk 29, targetSdk 36.

## Сборка

Нужны JDK 17 и Android SDK 36.

```bash
git clone https://github.com/jayelmeynak/MusicPlayer.git
cd MusicPlayer
./gradlew assembleDebug     # debug APK
./gradlew installDebug      # установка на устройство или эмулятор
./gradlew lint test         # проверки
```

Для вкладки «Local» на устройстве должны быть аудиофайлы и выдано разрешение на чтение аудио (`READ_MEDIA_AUDIO` на Android 13+, `READ_EXTERNAL_STORAGE` ниже).
