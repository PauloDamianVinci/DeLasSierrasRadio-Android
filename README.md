# DE LAS SIERRAS RADIO — v12 Live Stop (from stable v10)

Replaces in-app pause with STOP (discard buffered audio), and PLAY with a fresh live-stream connection. A pause command from lock-screen/headset/media notification also discards buffered audio in RadioService, so it cannot resume stale audio. Existing Media3 service, metadata, bilingual UI, app icon and background playback remain. Android may still display a standard pause icon in system media controls, but its behavior is stop-and-reset.

Build on GitHub Actions from a new branch `version-12-live-stop` created from `version-10`. This code has not been compiled or tested on a device.
