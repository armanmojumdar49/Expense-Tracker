# Expense Tracker (Android)

A simple offline Android expense tracker built with Kotlin and native Android views.

## Features
- Record income and expenses
- Summary of total income, expenses, and balance
- Categories for common transactions
- Local on-device storage using SharedPreferences
- Long-press a transaction to delete it
- Currency display in Bangladeshi taka (৳)

## Build an APK on GitHub
1. Create a new GitHub repository named `expense-tracker-android`.
2. Upload the contents of this project to the repository root (including the `.github` folder).
3. Open the repository's **Actions** tab.
4. Select **Build Expense Tracker APK** and click **Run workflow**, or push a commit to `main`.
5. Open the completed workflow run and download the **ExpenseTracker-debug-apk** artifact.
6. Extract the ZIP file; the installable debug APK is `app-debug.apk`.

## Local build
Requires JDK 17, Android SDK (API 35), and Gradle 8.9:
```bash
gradle assembleDebug
```

## Privacy
Transactions are stored locally on the device. The app does not request internet or bank permissions.
