# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An IntelliJ Platform plugin (Java, no Kotlin) that prefixes commit messages with the
current Git branch name. Published on JetBrains Marketplace as **Kolesa Commit Prefixer**.

## Commands

```bash
./gradlew buildPlugin    # -> build/distributions/idea-commit-prefixer-<version>.zip
./gradlew test
./gradlew test --tests "kz.kolesa.CommitMessagesTest.skipsStandardBranches"
./gradlew runIde         # sandbox IDE with the plugin loaded
./gradlew verifyPlugin   # IntelliJ Plugin Verifier over every IDE in verifierIdes
./gradlew verifyPlugin -PverifierIdes=PS-2026.2.1   # single IDE, notation TYPE-VERSION
./gradlew verifyPluginProjectConfiguration          # static checks, no downloads
```

`verifyPlugin` downloads a full IDE installer per entry in `verifierIdes` (~1.5 GB each,
plus roughly as much again when extracted). Check free disk before running the full list;
prefer `-PverifierIdes=...` while iterating.

## Naming: three different names, do not conflate

| | value | may it change? |
|---|---|---|
| Marketplace ID (`plugin.xml` `<id>`) | `kz.kolesa.branch-adder` | **never** — Marketplace matches updates by it |
| Artifact name (`rootProject.name`) | `idea-commit-prefixer` | yes, cosmetic |
| Display name (`plugin.xml` `<name>`) | `Kolesa Commit Prefixer` | yes |

The `branch-adder` ID is historical. A sibling directory `~/IdeaProjects/branch-adder`
holds an older, diverged copy of this same plugin — it is **not** the published source.

## Compatibility contract

All version knobs live in `gradle.properties`, never hardcoded in `plugin.xml`:

- `platformVersion` is the **oldest** supported release, not the newest. Compiling against
  it is what prevents accidentally calling API that does not exist on the floor version.
- `pluginSinceBuild` must match that floor (`2023.3` → `233`).
- `untilBuild` is deliberately `null` in `build.gradle.kts`. An upper bound is what broke
  earlier releases: every IDE update made the plugin uninstallable. Do not reintroduce one.
- `plugin.xml` has no `<idea-version>` tag — Gradle patches it in.

IntelliJ IDEA Community (`IC`) is no longer published after 2025.3; from 2026.1 IDEA is
unified under code `IU`. That is why `verifierIdes` uses `IC` for the floor and `IU` for
the ceiling. Product codes: `IC` `IU` `PS` (PhpStorm) `GO` (GoLand) `PY` (PyCharm Pro).

Build target is IntelliJ IDEA Community — the common core of every IntelliJ Platform IDE —
so one artifact installs into PhpStorm, GoLand, PyCharm, WebStorm, RubyMine, CLion,
RustRover and Rider. Keep it that way: no IDE-specific API.

## Architecture

Two independent entry points registered in `plugin.xml`, sharing all decision logic:

```
KolesaCommitMessageAction  (toolbar button)  ─┐
IssueReferenceChecker      (pre-commit gate) ─┴─> CommitMessages ──> GitBranches ──> Git4Idea
   ^ created by CommitMessageCheckinHandlerFactory
```

- **`CommitMessages`** — the only place that decides anything: which branches are skipped
  (`main`/`master`/`develop`, exact match, plus null/blank for detached HEAD), whether a
  message already mentions the branch, and how the prefix is applied. Both entry points
  must agree, otherwise the checker nags about a prefix the button refuses to insert.
  This is pure logic and is where the unit tests live.
- **Stale prefixes.** If a message already starts with a Jira-style key
  (`ISSUE_KEY` = `[A-Z][A-Z0-9]+-\d+`, uppercase and 2+ chars in the project code on
  purpose — otherwise a message starting with `utf-8 ...` would be eaten), that leading
  token is treated as a prefix left over from another branch: same issue → message is
  untouched, different issue → the token is replaced by the current branch. Never
  prepend on top of it. `mentionsBranch` accepts the branch's issue key alone for the
  same reason: on `feature/KL-123-fix` the button leaves `KL-123 fix` alone, so the
  checker must not nag about it.
- **`git/GitBranches`** — branch resolution through `GitRepositoryManager`. Never read
  `.git/HEAD` directly: that breaks worktrees and submodules (where `.git` is a file) and
  touches the filesystem from EDT. Returns `null` on detached HEAD and mid-rebase.
- **`plugin.xml`** hard-depends on `Git4Idea`, which is bundled in every target IDE.

## Platform pitfalls this code already works around

Reintroducing any of these is a regression:

- **Non-modal commit tool window** (the default since 2020.3) does not always provide
  `Refreshable.PANEL_KEY`. Actions must fall back to `VcsDataKeys.COMMIT_MESSAGE_CONTROL`
  for writing and `VcsDataKeys.COMMIT_MESSAGE_DOCUMENT` for reading, or they silently
  no-op there.
- **`AnAction.getActionUpdateThread()`** is mandatory since 2022.3.
- **`CheckinHandler.beforeCheckin()`** is not guaranteed to run on EDT in the non-modal
  flow — wrap any dialog in `invokeAndWait`.
- **Branch names are not regexes.** When editing `withBranchPrefix`, stick to
  `substring`/`startsWith` and never `replaceFirst` — branches like `release/1.2+rc`
  are legal and would blow up as a pattern. The one `Pattern` in `CommitMessages` is a
  constant applied *to* text; a branch name never becomes one.
- Action icons are 16×16 SVG (`icons/check.svg`), plugin icon is 40×40
  (`META-INF/pluginIcon.svg`).

## Tests

JUnit 5 (Jupiter). `testFramework(TestFrameworkType.JUnit5)` plus **`junit:junit:4.13.2` on
the test runtime classpath** — the platform's test runner boots through JUnit 4 even when
the tests themselves are Jupiter, and drops `NoClassDefFoundError: org/junit/runners/model/Statement`
without it.

Only `CommitMessages` is unit-testable without a running IDE; keep new decision logic there
rather than inside the action or the handler.

## Releasing

1. Bump `pluginVersion` in `gradle.properties` and add a `<change-notes>` entry in `plugin.xml`.
2. `./gradlew clean test buildPlugin verifyPlugin`
3. Upload `build/distributions/idea-commit-prefixer-<version>.zip` to
   https://plugins.jetbrains.com/plugin/edit

`./gradlew publishPlugin` also works with `PUBLISH_TOKEN` set (`CERTIFICATE_CHAIN`,
`PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` for signing). Unsigned uploads are accepted.
