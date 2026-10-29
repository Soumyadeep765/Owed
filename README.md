# Owed

<p align="center">
  <img src="docs/owed-logo.png" width="128" alt="Owed">
</p>

Owed is an Android app for money and things you have lent or borrowed. You can record both directions, payments, and a return date. The list stays on the phone. There is no account and no server.

The first time you open it, you pick a language, walk through a short intro, then pick a currency. After that the home screen shows the balance.

## What you can do

- See what is owed to you, what you owe, and the net
- Add money or an item, as lent or borrowed, with a note and a return date
- Record a payment, or mark the entry returned
- Search and filter entries, and group them by person
- Get a reminder before the date, on the day, and again if it is overdue
- Settle that reminder from the notification
- Pick a name from your contacts
- Save a backup file and restore it later
- Put a home screen widget on the two balances
- Lock the app with the phone’s face, fingerprint, or PIN
- Long-press the icon for Lent and Borrowed

Amounts follow the currency you choose. Words follow the language. You can change either later in Settings. Appearance can follow the phone, or stay light or dark.

## Languages

English, हिन्दी, Hinglish, Español, Français, Português, Deutsch, العربية, 中文, বাংলা.

## Currencies

INR, USD, EUR, GBP, JPY, AUD, CAD, CNY, AED, SAR, SGD, BDT, NPR, KRW, BRL, CHF.

## Requirements

- Android 8.0 or newer
- JDK 17 to build

## Build

```bash
gradle assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions builds the same APK on every push and uploads it as an artifact.

Debug and release builds are signed with `app/owed-upload.p12` (alias `owed`). That file is in the repository so every Actions build uses the same key. Raise `versionCode` in `app/build.gradle.kts` and the new APK can install over the previous one.

This key is for these updateable builds. It is not a private Play upload key. Anyone who can read the repository can sign with it.

If a phone already has an older Owed APK from a GitHub run that used a one-off debug key, uninstall that copy once. After the signed build is installed, later builds update in place.

## Privacy

Entries, payments, alerts, and settings are stored on the device. A backup is a file you save yourself. Owed does not upload it.

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
