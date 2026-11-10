# Notes for Claude
This repository is a personal fork of [sspanak/tt9](https://github.com/sspanak/tt9). Read [FORK.md](FORK.md) for the full details. The essential rules:

- **Never commit to `master`.** It mirrors upstream and is only updated with "Sync fork".
- **Create new branches from `master`** (`git checkout -b <name> origin/master`), never from `main`.
- **`main` is the default branch** and contains everything. Merge feature branches into it; never rebase or force-push it.
- **Do not edit `.github/workflows/build.yml` or `close-stale-issues.yml`.** They are upstream files, disabled in the Actions tab. The fork's build is `.github/workflows/build-apk.yml`, started manually only. Do not add automatic triggers (push, pull request, schedule) to any workflow.
- **Prefer new files over editing upstream ones** for fork-only changes, to avoid merge conflicts when syncing.
- Keep fork-only changes (application ID suffix, app name, app icon, build workflow, signing config, `FORK.md`, `CLAUDE.md`, `fork-assets/`) separate from features that could be sent upstream.
- New user-facing strings go in `app/src/main/res/values/strings.xml` (English). Missing translations are only lint warnings.
- The project has `full` and `lite` flavors. Only `full` is used by this fork.
