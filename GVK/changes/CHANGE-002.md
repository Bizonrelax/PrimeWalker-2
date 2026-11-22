---
id: CHANGE-002
author_role: WISH
owner: qwen-code-3c89a1e0
cause: wish
tz_rev: 3
date: 2026-11-22
status: active
---

### CHANGE-002 — Автоматическая сборка Windows-дистрибутива в CI

Основание: `РЫЧАГ: ЖЕЛАНИЕ` владельца («ну добавь воркфлов», затем «сделай пуш»). Пользователь не увидел нового `PrimeWalker-2-windows-portable.zip`, потому что в репозитории отсутствовал CI, собирающий артефакт; сам архив по FILEMAP FM3 в git не коммитится — он живёт в артефактах Actions и Releases.

Дифф по смыслу:
1. ТЗ ревизия 3, пункт 4: требование автоматической сборки портативного пакета в CI (пуш в main → артефакт Actions; тег v* → GitHub Release).
2. Файл `.github/workflows/maven.yml` (добавлен ранее, закоммичен в origin/main): job `test` (ubuntu, mvn verify + surefire-отчёты) и job `windows-portable` (windows-latest, mvn package, jpackage app-image, Compress-Archive → `PrimeWalker-2-windows-portable.zip`, upload-artifact; при теге `v*` — softprops/action-gh-release).

Влияние на фрагменты: каркас CI (`.github/workflows/**`), код приложения не затронут.

Проверка: workflow синтаксически корректен (YAML), условия запуска `push: main` / `tags v*` / `workflow_dispatch`; сборочная команда `mvn -B -ntp -DskipTests package` порождает shaded jar (`maven-shade-plugin`, `finalName=primewalker2`), совместимый с шагом jpackage. Первый прогон Actions после пуша подтвердит зелёность (вне среды этой сессии).
