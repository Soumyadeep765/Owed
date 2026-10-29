# Owed

Owed is an Android app for money and things you have lent or borrowed. It stays on the phone. Nothing is uploaded.

You can track both directions, record payments, group people, and get a reminder before a return date, on the day, and again if it is overdue.

## Features

- Home balance for what is owed to you and what you owe
- Entries for money and items, with search and filters
- People, grouped from the names on your entries
- In-app alerts and system notifications
- Multiple currencies
- Languages, including Hinglish
- Light, dark, and system appearance
- Data kept in on-device storage
- Backup file you can save and restore
- Home screen widget for the two balances
- Phone lock before the balances show
- Pick a person from contacts
- Settle a reminder from the notification
- Home screen shortcuts for lent and borrowed

## Requirements

- Android 8.0 (API 26) or newer
- JDK 17

## Build

```bash
gradle assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Debug and release builds are signed with `app/owed-upload.p12` (alias `owed`). GitHub Actions uses that same key, so raising `versionCode` in `app/build.gradle.kts` lets a new APK install over the previous one. This key is in the repository so the builds stay updatable. It is not a private Play upload key.

If a phone already has an older Owed APK from a GitHub run that used a one-off debug key, uninstall that copy once. After that, later builds update in place.

## Privacy

Entries, payments, and alerts are stored on the device. Owed does not have an account or a server.

## Developer

Soumyadeep Das

- Email: [soumyadeepdas765@gmail.com](mailto:soumyadeepdas765@gmail.com)
- GitHub: [Soumyadeep765/Owed](https://github.com/Soumyadeep765/Owed)
- Telegram: [@devxy](https://t.me/devxy)

The same contact is in the app under Settings, About.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE)
