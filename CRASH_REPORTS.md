# Crash reports

After a crash, FFA writes reports to:

- Visible (file manager): `Android/data/com.ffa.flashforalert/files/crash-reports/`
- Internal fallback: `/data/data/com.ffa.flashforalert/files/crash-reports/`
- Cache fallback: `/data/data/com.ffa.flashforalert/cache/crash-reports/`

Files:
- `crash-<timestamp>.txt`
- `latest-crash.txt`

If you use adb:
`adb shell run-as com.ffa.flashforalert ls files/crash-reports`
`adb shell run-as com.ffa.flashforalert cat files/crash-reports/latest-crash.txt`