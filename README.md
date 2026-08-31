# Kolesa Commit Prefixer

Плагин для IDE на IntelliJ Platform: подставляет имя текущей git-ветки в начало
сообщения коммита и предупреждает, если перед коммитом ссылки на ветку в
сообщении нет.

Marketplace ID плагина — `kz.kolesa.branch-adder` (историческое имя, менять
нельзя: по нему Marketplace связывает обновления с уже опубликованным плагином).

## Возможности

- кнопка в панели коммита вставляет имя ветки в начало сообщения;
- ничего не делает, если ветка уже упомянута в сообщении;
- не трогает стандартные ветки — `main`, `master`, `develop`, `dev`;
- перед коммитом спрашивает подтверждение, если ссылки на ветку нет
  (отключается галочкой *Check reference to issue in message* в панели коммита).

## Совместимость

| | |
|---|---|
| Минимальная версия IDE | 2023.3 (build 233) |
| Максимальная версия | не ограничена |
| IDE | IntelliJ IDEA, PhpStorm, GoLand, PyCharm, WebStorm, RubyMine, CLion, RustRover, Rider |
| Требуется | включённый бандл-плагин **Git** (Git4Idea) |

Сборка идёт против IntelliJ IDEA Community — общего ядра всех IDE на IntelliJ
Platform, поэтому один и тот же артефакт ставится в любую из них.

## Разработка

```bash
./gradlew buildPlugin      # собрать .zip для Marketplace
./gradlew runIde           # песочница IDE с плагином
./gradlew test             # unit-тесты
./gradlew verifyPlugin     # IntelliJ Plugin Verifier по списку IDE
```

Требования: JDK 17 (Gradle подтянет сам через toolchain), Gradle Wrapper 9.7.1.

Версии платформы и список IDE для верификации — в
[`gradle.properties`](gradle.properties): `platformVersion`, `pluginSinceBuild`,
`verifierIdes`. Точечный прогон верификатора:

```bash
./gradlew verifyPlugin -PverifierIdes=PS-2026.2.1
```

## Публикация

Готовый артефакт: `build/distributions/idea-commit-prefixer-<version>.zip` —
его и заливать в [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/edit).

Перед публикацией поднять `pluginVersion` в `gradle.properties` и дописать
`<change-notes>` в `plugin.xml`.

Автоматическая публикация: `./gradlew publishPlugin` с `PUBLISH_TOKEN`
в окружении (и `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` —
для подписи).

## Лицензия

Apache 2.0.
