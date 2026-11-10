# About This Fork (Touch T9)
This is a personal fork of [Traditional T9](https://github.com/sspanak/tt9). This file describes how the fork is organized and maintained. For everything else about the app, see the [README](README.md).

## Branches
| Branch | Purpose |
|---|---|
| `master` | An exact copy of the upstream `master`. **Never commit to it.** It is only updated by syncing with upstream. |
| `main` | The default branch. Contains upstream plus all changes of this fork. The APKs are built from it. |
| feature branches | One branch per change, always created from `master`, then merged into `main`. |

### Rules
1. **Never commit to `master`.** This way, syncing with upstream is always a simple fast-forward, without conflicts.
2. **Create new branches from `master`**, not from `main`. This keeps each change independent of the others, so it can be merged into `main` in any order, or sent upstream as a pull request.
3. **Merge into `main`, do not rebase it.** The history of `main` must never be rewritten.
4. **Do not modify the upstream workflows** (`.github/workflows/build.yml` and `close-stale-issues.yml`). They are disabled in the Actions tab. Changing them would only cause conflicts when merging upstream updates.
5. **Prefer new files over editing the upstream ones** for fork-only things (like this file), to keep the merges conflict-free.

## Syncing With Upstream
On GitHub:
1. Switch to the `master` branch and click "Sync fork" → "Update branch".
2. Open a pull request from `master` to `main` and merge it, or merge it locally (see below).

Or locally:
```bash
git remote add upstream https://github.com/sspanak/tt9.git # only once
git fetch upstream
git checkout master
git merge --ff-only upstream/master
git push origin master

git checkout main
git merge master
git push origin main
```

If `git merge --ff-only` fails, someone has committed to `master`. Do not force it. Move the extra commits to a feature branch first.

## Adding a New Change
```bash
git checkout master
git pull
git checkout -b my-new-feature
# ...commit the changes...
git push -u origin my-new-feature

git checkout main
git merge my-new-feature
git push origin main
```

If the feature branch gets outdated, merge `master` into it, or rebase it onto `master`, as long as it has not been merged into `main` yet.

## Fork-Only Changes
These changes exist only for this fork and must not be sent upstream:
- **Application ID suffix** (`.fork`) and app name "Touch T9". They allow installing the fork alongside the original app. See `APPLICATION_ID_SUFFIX` in `app/constants.gradle` and `app_name` in `app/src/main/res/values/strings.xml`.
- **The app icon.** All launcher icons are generated from `fork-assets/logo.jpg` with `fork-assets/make_icons.py` (see the instructions at the top of the script). The adaptive icon uses new resources (`ic_launcher_fork_*`), while the upstream ones are left unused, to avoid conflicts.
- **The "Build APK" workflow** (`.github/workflows/build-apk.yml`) and the release signing configuration in `app/build.gradle`.
- `FORK.md`, `CLAUDE.md` and the `fork-assets/` folder.

The other changes (e.g. new keyboard features) are kept in separate branches, so they could be offered upstream as pull requests, from their own branch to `sspanak/tt9:master`.

## Building
The APKs are built manually only: Actions → "Build APK" → "Run workflow". Only the "Full" variant is built, with the dictionaries included. The resulting APK can be downloaded from the "Artifacts" section of the workflow run.

To make the build faster and the APK smaller, only the languages in the "languages" field are included (default: `English Polish`). Use the file names from `app/languages/definitions`, without `.yml`, separated by spaces. English is always included, because the app falls back to it in some cases. Leave the field empty to include all languages. The other languages are only removed from the temporary copy used for the build; the repository is not modified.

Release builds are signed with a key stored in the repository secrets (Settings → Secrets and variables → Actions):
- `TT9_KEYSTORE_BASE64`: the keystore file, encoded with `base64 -w0 tt9-release.jks`
- `TT9_KEYSTORE_PASSWORD`
- `TT9_KEY_ALIAS`
- `TT9_KEY_PASSWORD`

Always use the same key. Android refuses to update an app signed with a different key, so losing it means uninstalling the app (and losing its settings and added words) before installing a new version. Keep a backup of the keystore and its passwords outside of GitHub.

To build locally, see the [building instructions](CONTRIBUTING.md#building).
