# T9 App Search Widget

A native Android home screen widget that lets you search installed apps using T9 (predictive text) input. Fuzzy-matches app names and displays the top 3 results for quick launching.

## Layout

```
 [App 1]  | [App 2]  | [App 3]
----------|----------|----------
   CLR    |  ABC 2   |  DEF 3
  GHI 4   |  JKL 5   |  MNO 6
 PQRS 7   |  TUV 8   | WXYZ 9
```

- **Top row** — Shows up to 3 matching apps (icon + name). Tap to launch.
- **Keypad** — T9 digit buttons. Each press appends to the search. CLR resets.

## How T9 Search Works

Each digit maps to letters (2=ABC, 3=DEF, etc.). Pressing `9-6-8` matches apps containing the subsequence matching those letter groups — e.g. "**Y**ou**T**u**b**e" won't match, but "**Y**ou**T**ube" matches `9(y)-6(o)-8(u)`.

The matching is fuzzy (subsequence-based) and scores consecutive character matches higher, so the most relevant results appear first.

## Persistence

The widget remembers your last search digits across reboots via SharedPreferences. When you add the widget or restart your phone, it restores the previous search results automatically.

## Build

Requires Android SDK with API 34.

```bash
./gradlew assembleDebug
```

## Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then long-press your home screen → Widgets → add **T9 Search**.

## Permissions

- `QUERY_ALL_PACKAGES` — Required to enumerate installed apps for search.

## Requirements

- Android 8.0+ (API 26)
- Home screen that supports app widgets
