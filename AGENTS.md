# SysAppForge Agent Guide

## Project Purpose

SysAppForge is an Android app that turns installed packages into Magisk, KernelSU, or APatch
modules. A generated module can place base and split APKs under `system/app` or
`system/priv-app`, or run `pm install-existing` during boot.

Changes to module contents and boot scripts are security-sensitive: generated scripts run as
root, and an incorrect package-management command can make apps unavailable or leave a device
in a boot loop.

## Tooling

- Use JDK 17 and Android SDK 35.
- Prefer Bash for repository commands. On Windows run `bash -lc '...'`.
- Use explicit `rtk` commands when compact output helps, but do not assume an RTK shell hook.
- Keep Markdown, Kotlin, XML, TOML, YAML, and shell content as UTF-8 without BOM.
- Preserve unrelated working-tree changes. Do not use destructive Git cleanup commands.

## Repository Map

- `app/src/main/java/com/example/sysappmodule/data/`: persisted templates, package discovery,
  and install-mode domain types.
- `app/src/main/java/com/example/sysappmodule/module/`: APK extraction, root shell generation,
  and ZIP assembly. This is the highest-risk code.
- `app/src/main/java/com/example/sysappmodule/vm/`: screen state, DataStore writes, generation,
  and one-shot UI events.
- `app/src/main/java/com/example/sysappmodule/ui/`: Compose navigation and screens.
- `app/src/main/java/com/example/sysappmodule/util/`: FileProvider intents, Downloads storage,
  and drawable conversion.
- `app/src/test/`: host-side regression tests for module generation.
- `.github/workflows/build.yml`: debug APK build; currently does not run unit tests or Lint.

## Required Validation

For normal code changes, run:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain
```

For a quick edit loop, run `:app:compileDebugKotlin` first. Do not report a module-generation
change as complete based only on compilation: add or update a test that opens the generated ZIP
and asserts its actual entries and script text.

Before handoff, also run:

```bash
git diff --check
git status --short
```

## Critical Contracts

- Every detail or preview destination must call `TemplateDetailViewModel.load(templateId)`.
- Never silently omit a selected package. If `PackageManager` cannot resolve it, block generation
  and name the unavailable package.
- A non-`INSTALL_EXISTING` package must include `base.apk` and every readable split APK.
- Treat labels, module metadata, URLs, and other user input as untrusted. `module.prop` values must
  stay on one line, and all text interpolated into root shell commands must be shell-quoted.
- Package names come from `PackageManager`; do not accept arbitrary path fragments as package
  names when constructing ZIP paths.
- ZIPs shared through intents must remain under a path declared by `file_provider_paths.xml` and
  carry `FLAG_GRANT_READ_URI_PERMISSION`.
- `externalCacheDir` is nullable. Use an internal-cache fallback when shared storage is absent.
- Keep generated-module scripts compatible with `/system/bin/sh`; do not introduce Bash syntax.
- Do not replace `pm uninstall -k --user 0` with a global uninstall without an explicit product
  decision and device evidence. The global form has materially different data and multi-user risk.

## Runtime Verification

Host tests cannot prove that an overlay becomes active on a real rooted device. For changes to
`post-fs-data.sh`, `service.sh`, paths under `system/`, or uninstall/install behavior, validate a
generated module on the supported manager family in scope and capture:

- ZIP entry listing and generated script contents.
- Manager installation output.
- `pm path <package>` before installation, after reboot, and after the service script runs.
- Whether base and split APKs resolve to `/system` module paths.
- Package enabled/installed state for user 0 and any relevant secondary user.
- A clean disable/remove-module recovery boot.

Do not describe successful APK assembly as proof that the root/runtime route works.

## Code Style

- Follow existing Kotlin and Compose patterns; keep state ownership in ViewModels and pass events
  into screens explicitly.
- Add concise comments for root-script rationale, Android-version guards, storage fallbacks, and
  non-obvious package-manager semantics.
- Keep UI strings consistent with the existing Chinese interface. Prefer string resources for new
  reusable or user-facing text.
- Avoid swallowing `Throwable`. Surface actionable failures to the UI and catch the narrowest
  exception that is expected.
- Keep changes scoped. Dependency and AGP upgrades require their own validation pass.
