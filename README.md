<p align="center">
  <img src="branding/logo.png" width="160" alt="Cold Cat logo">
</p>

<h1 align="center">Cold Cat for Android</h1>

<p align="center">
  Лёгкий Android-браузер на движке GeckoView с поддержкой расширений Firefox.<br>
  <a href="https://github.com/m4xjk3/cold-cat-android/releases/latest">Скачать APK</a> ·
  <a href="#сборка">Собрать самому</a> ·
  <a href="CHANGELOG.md">Изменения</a>
</p>

<p align="center">
  <img alt="Android 8+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="GeckoView" src="https://img.shields.io/badge/engine-GeckoView-2B9BD6">
  <img alt="License" src="https://img.shields.io/badge/license-MPL--2.0-7FD3FF">
</p>

---

## Что это

Cold Cat — независимый браузер для Android. Он использует **GeckoView** — тот же
движок, на котором работают Firefox для Android и Waterfox для Android. Поэтому
в нём работают **подписанные расширения Firefox** с
[addons.mozilla.org](https://addons.mozilla.org/android/): блокировщики рекламы,
тёмные темы, менеджеры паролей и другие.

Версия **0.1.0** — первый рабочий Android-билд. Версия для ПК появится позже,
отдельным репозиторием.

## Возможности

- 🔒 **Строгая защита от трекинга** (ETP Strict) включена по умолчанию.
- 🧩 **Расширения Firefox**: каталог рекомендуемых, установка по ссылке на `.xpi`,
  удаление, запрос разрешений перед установкой.
- 🗂 **Вкладки**: переключение, закрытие, бейдж количества, восстановление после
  поворота экрана.
- 🔍 **Умная строка**: URL или поиск в DuckDuckGo.
- 🖥 **Версия для ПК** одним переключателем.
- 🏠 Брендовая стартовая страница, иконка, splash screen.
- 🌙 Светлая и тёмная тема системного интерфейса.
- 🇷🇺 🇬🇧 Русский и английский языки.
- 🪶 Минимум зависимостей: AndroidX, Material Components и GeckoView.

## Установка

1. Скачайте APK из
   [Releases](https://github.com/m4xjk3/cold-cat-android/releases/latest):
   - `ColdCat-0.1.0-arm64-v8a.apk` — для современных телефонов (почти все
     устройства после 2016 года);
   - `ColdCat-0.1.0-armeabi-v7a.apk` — для старых 32-битных ARM-устройств.
2. Разрешите установку из неизвестного источника для файлового менеджера.
3. Откройте APK и установите.

Требования: Android 8.0 (API 26) или новее. APK подписан ключом проекта;
обновления ставятся поверх без удаления данных.

> Первый запуск может занять несколько секунд: GeckoView инициализирует профиль.

## Расширения

Откройте **Меню → Дополнения**. В каталоге уже есть uBlock Origin, Dark Reader,
Bitwarden, Privacy Badger, ClearURLs и SponsorBlock. Можно установить любое
подписанное расширение по прямой ссылке на `.xpi` с addons.mozilla.org.

Ограничения, общие с Firefox для Android:

- неподписанные `.xpi` отклоняются движком;
- часть десктопных WebExtension API на Android недоступна;
- после установки расширения открытые вкладки перезагружаются при возврате в
  браузер.

## Сборка

Нужны **JDK 17**, **Android SDK 37.2** (`sdkmanager "platforms/android-37.2"
"build-tools;37.0.0"`, плюс `ANDROID_HOME`), **Gradle 9.6** через wrapper и
**AGP 9.5**. Проверено локально: JDK 17.0.20, Gradle 9.6.0, AGP 9.5.0.

```bash
git clone <this-repo> cold-cat-android
cd cold-cat-android

# unit-тесты
./gradlew testDebugUnitTest

# debug-сборка
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk

# release-сборка (нужен ключ, см. ниже) — два APK по ABI
./gradlew assembleRelease
# app/build/outputs/apk/release/app-arm64-v8a-release.apk
# app/build/outputs/apk/release/app-armeabi-v7a-release.apk
```

### Подпись release

```bash
keytool -genkeypair -v \
  -keystore ~/.coldcat/coldcat-release.jks \
  -alias coldcat -keyalg RSA -keysize 4096 -validity 10000

cp keystore.properties.example keystore.properties
# укажите путь к keystore и пароли
./gradlew assembleRelease
```

`keystore.properties` и `*.jks` исключены из git. **Сохраните ключ и пароли**:
без них нельзя выпускать обновления поверх установленной версии.

### Структура

```
app/src/main/java/com/coldcat/browser/
  ColdCatApp.kt       инициализация GeckoRuntime и ETP
  MainActivity.kt     вкладки, навигация, меню, стартовая страница
  AddonsActivity.kt   менеджер расширений и запросы разрешений
  AddonCatalog.kt     рекомендуемые расширения AMO
  UrlUtils.kt         URL/поиск, покрыт unit-тестами
app/src/main/assets/
  home.html           стартовая страница
branding/
  logo-source.jpg     исходный логотип
  make-icons.ps1      генерация иконок Android из логотипа
```

Движок: `org.mozilla.geckoview:geckoview:159.0.20261022150525` из
`maven.mozilla.org`. `compileSdk`/`targetSdk` — 37 (minor 2), `minSdk` — 26.

> GeckoView 159 собран с Kotlin 2.4, а встроенный в AGP 9.5 компилятор — 2.2.
> Поэтому в `app/build.gradle.kts` включён
> `-Xskip-metadata-version-check`: проверка метаданных отключена, но runtime-
> библиотека 2.4.20 упакована в APK.

## Почему не полное дерево Waterfox?

[BrowserWorks/waterfox-android](https://github.com/BrowserWorks/waterfox-android) —
это форк mozilla-central: сборка Gecko требует Linux, отдельного toolchain и
десятков гигабайт. Для мобильного продукта здесь используется тот же
GeckoView-движок из официального Maven-репозитория Mozilla, а фронтенд написан
с нуля под Cold Cat. Это даёт настоящие Firefox-расширения и быстрые сборки на
обычной машине с Android SDK.

## Планы

- [ ] APK для ПК-версии (отдельный репозиторий)
- [ ] Загрузки и история
- [ ] Приватные вкладки
- [ ] Поиск по странице
- [ ] Настройки поисковика и стартовой страницы
- [ ] Синхронизация Firefox Account (по возможности GeckoView)
- [ ] Патчи и настройки из Waterfox

## Лицензия и благодарности

Код Cold Cat распространяется по **Mozilla Public License 2.0** (см. [LICENSE](LICENSE)).
GeckoView © Mozilla и участники проекта, MPL-2.0. AndroidX и Material Components —
Apache-2.0. Подробнее в [NOTICE.md](NOTICE.md).

Cold Cat не связан с Mozilla или BrowserWorks. Спасибо проектам
[Waterfox](https://github.com/BrowserWorks/waterfox-android),
[Firefox for Android](https://github.com/mozilla-mobile/firefox-android) и
[GeckoView](https://mozilla.github.io/geckoview/) за движок и вдохновение.
