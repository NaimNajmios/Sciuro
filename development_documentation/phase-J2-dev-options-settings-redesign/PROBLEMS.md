# Phase J2 — Developer Options & Settings Redesign — Problems

## Issues Encountered

### 1. SheetList modifier: fillParentMaxWidth vs fillParentMaxHeight (2026-07-28)
- **Symptom:** `fillParentMaxWidth()` used for `SheetList` inside LazyColumn — incorrect modifier, should be `fillParentMaxHeight()` which is a LazyListScope item-scope modifier
- **Cause:** Confusion between Column/Row scope modifiers and LazyListScope modifiers
- **Resolution:** Replaced `fillParentMaxWidth()` with `fillParentMaxHeight()`

### 2. Build cache not recompiling modified files (2026-07-28)
- **Symptom:** `compileDebugKotlinAndroid` reported UP-TO-DATE or FROM-CACHE even after file edits
- **Cause:** Gradle incremental compilation detected no changes (fast writes preserved timestamps)
- **Resolution:** Used `--no-build-cache --rerun-tasks` or `clean` to force recompilation

### 3. item { Spacer } outside LazyColumn scope in PipelineTrace (2026-07-28)
- **Symptom:** LazyListScope extension function `item()` called outside the LazyColumn builder block
- **Cause:** Restructured file moved the spacer `item { }` outside the closing brace of the LazyColumn
- **Resolution:** Moved `item { Spacer }` inside the LazyColumn block
