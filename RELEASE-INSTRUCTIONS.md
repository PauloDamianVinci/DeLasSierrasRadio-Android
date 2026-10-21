# Public APK signing — step by step

This project is based on **version-12-live-stop**, the user-validated baseline. No playback changes.

## Important

A permanent signing key is essential. Losing it may prevent future in-place updates. **Do not publish the keystore or passwords.** GitHub Actions artifacts containing the keystore are private to authorized repository users, but treat them as sensitive and delete the key-generation run after downloading.

## Step 1: Create a branch

Create `release-prep` from `version-12-live-stop` on GitHub. Upload all project files from this ZIP **including the two NEW workflow files** in `.github/workflows/`. The existing `android.yml` stays unchanged. The release APK uses the same `applicationId` and `versionCode` as v12.

## Step 2: Set two repository secrets

Repository → Settings → Secrets and variables → Actions → New repository secret:
- `RADIO_KEYSTORE_PASSWORD`: choose a strong unique password, at least 16 characters.
- `RADIO_KEY_PASSWORD`: use the SAME password (PKCS12 often requires this).

Save these passwords in a secure password manager. Do not send them to ChatGPT.

## Step 3: Generate signing keystore ONCE

Actions → Create release signing key (one time) → Run workflow, branch `release-prep`.
Download the `PRIVATE-radio-signing-key-KEEP-SAFE` artifact. Unzip it to obtain `radio-release.p12`.
Store an offline backup (ideally two encrypted backups). Never commit this file to GitHub.

## Step 4: Convert the keystore to base64 with built-in Windows PowerShell

Run in PowerShell, replacing the path with your actual downloaded location:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\Users\YOUR_USER\Downloads\radio-release.p12')) | Set-Clipboard
```

No software installation needed. Add repository secret `RADIO_KEYSTORE_BASE64` and paste the clipboard value. Do not paste it into GitHub source files or chat.

Once backed up, delete the one-time generation workflow run (and its artifact) from GitHub Actions.

## Step 5: Build the signed release APK

Actions → Build signed release APK → Run workflow, branch `release-prep`.
Download artifact `DeLasSierrasRadio-signed-release-APK`. Extract `app-release.apk`.

## Step 6: Test before publishing

The existing debug APK likely uses a different signing certificate, so Android may require uninstalling it before installing the release APK. Back up any needed app preferences first.

Test playback, live stop/restart, metadata, ES/EN, background/lock screen, Bluetooth controls and sharing. Keep the keystore safe and use the SAME keystore for every future release.

**Never run the one-time key generation again for future releases.**
