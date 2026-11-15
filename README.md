# FluxSU

FluxSU is a fork of [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra), a kernel-based Android root solution derived from KernelSU.

Changes over upstream:
- Rebranded to FluxSU, with its own launcher icon
- Home dashboard info row (mode, SELinux, kernel)
- Quick toggles on the home screen (su, module umount, hide mounts)

See `NOTICE-FluxSU.md` for licensing. FluxSU is distributed under the same GPL terms as upstream.

## Building

Every push to `main` runs the **Build Manager** GitHub Actions workflow, which builds the kernel module (LKM), `ksud` and the manager APK together. Open the finished run and download the artifacts.

- Without any secrets, each run signs the APK with a fresh temporary key and builds the kernel module for that same key, so always use the module and APK **from the same run**. Updating over an older install needs an uninstall first.
- For stable updates, add the repository secrets `KEYSTORE` (base64 of your .jks), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. The workflow then binds the kernel module to your key.
