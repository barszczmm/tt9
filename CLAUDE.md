# Notes for Claude
This repository is a personal fork of [sspanak/tt9](https://github.com/sspanak/tt9). Read [FORK.md](FORK.md) for the full details. The essential rules:

- **Never commit to `master`.** It mirrors upstream and is only updated with "Sync fork".
- **Create new branches from `master`** (`git checkout -b <name> origin/master`), never from `main`.
- **`main` is the default branch** and contains everything. Merge feature branches into it; never rebase or force-push it.
- **Do not edit `.github/workflows/build.yml` or `close-stale-issues.yml`.** They are upstream files, disabled in the Actions tab. The fork's build is `.github/workflows/build-apk.yml`, started manually only. Do not add automatic triggers (push, pull request, schedule) to any workflow.
- **Prefer new files over editing upstream ones** for fork-only changes, to avoid merge conflicts when syncing.
- Keep fork-only changes (application ID suffix, app name, app icon, build workflow, signing config, `FORK.md`, `CLAUDE.md`, `fork-assets/`) separate from features that could be sent upstream.
- The project has `full` and `lite` flavors. Only `full` is used by this fork.

## Code style (from upstream review feedback)
Code meant for upstream is reviewed by the upstream owner. Follow these rules from the start:

- **Translate every new or changed user-facing string.** Add it to `app/src/main/res/values/strings.xml` (English), and also to all 12 translated languages: `values-bg`, `de`, `es`, `fr`, `it`, `iw`, `lt`, `nl`, `pt-rBR`, `ru`, `tr`, `uk`. When the meaning of an English string changes, update its translations too. Match the quotation marks each language file already uses.
- **No inline styling in layouts.** Put view attributes (sizes, paddings, gravity, colors, text sizes) in named styles in `res/values/styles.xml`, and reuse them across elements and across the normal and `_large` layouts. Do not hardcode colors. Use theme attributes, or set the colors in the styles. Android 12+ overrides go in `res/values-v31/styles.xml`.
- **Test UI changes on Android 11 and older, too.** The legacy (AppCompat) and Material 3 (`values-v31`) themes differ. `minSdk` is 24, so avoid attributes added later (e.g. `android:paddingHorizontal` needs API 26; use `paddingStart`/`paddingEnd`).
- **Reuse existing helpers instead of repeating code**, e.g. `HardwareInfo.getScreenPixelDensity(context)` instead of `context.getResources().getDisplayMetrics().density`.
- **Do not alias constants.** Use the original constant directly (e.g. `SettingsStore.SOME_LIMIT`). If its name is unclear for the new use, rename it at the source.
- **Never swallow exceptions silently.** Log unexpected cases with `Logger` (`Logger.w`/`Logger.e`, with `LOG_TAG`), including the relevant data, so they can be investigated.
- **Annotate nullability** with `@NonNull`/`@Nullable` on public method parameters and return values, matching what the method actually accepts.
