# Release candidate 0.2

- [ ] Latest GitHub Actions build and regression fixtures green.
- [ ] Verify APK signing certificate continuity with installed app; ephemeral CI debug keys may differ. Do not promise in-place updates until stable signing is configured.
- [ ] Install candidate on Android and test Wi-Fi discovery and manual IPP connection to HP Smart Tank 210.
- [ ] Print PDF and JPEG; verify actual A4/F4/Legal/4R dimensions where supported.
- [ ] Test quality, color, paper type, margins, copies, page ranges and explicit unsupported setting errors.
- [ ] Test PWG fallback on a printer advertising the required format and resolution.
- [ ] Test single-photo and multi-photo passport layout, rotation, crop, ordering, preview and overflow rejection.
- [ ] Verify preferences persist and reset works.
- [ ] Verify diagnostics; maintenance commands remain unavailable until protocol verified.
- [ ] Configure stable release signing and publish verified APK before enabling automatic update.
- [ ] Verify upgrade without data loss and no ads; local printing works without internet on LAN.
