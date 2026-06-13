# Registering & Signing LFG for Google Developer Sideload Verification

This guide outlines how to configure the **LFG** app, generate a unique signing key (keystore), and register your app's package identity with Google so you can bypass the upcoming sideloading restrictions (e.g., the "advanced flow" warning and the 24-hour cooling-off period).

---

## 1. How the Sideloading Protections Work

Starting in late 2026 and rolling out globally in 2027, Android devices running Google Play Services will introduce new protections for apps installed outside the Play Store:
* **Verified Developers:** If the app is signed with a key registered to a verified Google Developer account (even if the APK is distributed off-Store), it can be sideloaded instantly with standard, familiar prompts.
* **Unverified Developers:** Sideloading an unregistered APK requires enabling Developer Options, performing a security check, and waiting through a **24-hour cooling-off period** before installation.

To keep using **LFG** friction-free, you can register your own personal build under your Google Developer account.

---

## 2. Choosing Your Google Developer Console Path

Google offers two primary ways to verify your developer identity:

| Feature | Standard Play Console Account | Hobbyist / Student Account |
| :--- | :--- | :--- |
| **Cost** | One-time $25 USD registration fee | **Free** |
| **Verification** | Government ID & proof of address | Minimal (linked to Google Account) |
| **Distribution** | Unlimited public devices | Limited to a small number of personal/test devices |
| **Usage Console** | [Google Play Console](https://play.google.com/console) | [Android Developer Console](https://developer.android.com) |

> [!TIP]
> If you are only building LFG for yourself and a few friends or family members, the **Free Hobbyist / Student account** is perfect and avoids the government-ID check.

---

## 3. Step-by-Step Key Generation & Registration

### Step A: Choose a Unique Package Name (Application ID)
Because Google Play and Developer Consoles require package names (Application IDs) to be **globally unique**, you cannot register the default `com.bradflaugher.lfg` (which belongs to the original project creator).

Instead, pick a unique identifier like:
`com.yourusername.lfg`

Update the `applicationId` in `android/src/app/build.gradle.kts`. You do **not** need to rename directories or package declarations in the source files, because the Android Gradle Plugin separates the package namespace from the application ID.

---

### Step B: Generate a Private Release Key (Keystore)
You need to generate a secure keystore file. Run the following command in your terminal:

```bash
keytool -genkey -v -keystore lfg-release.jks -alias lfg-alias -keyalg RSA -keysize 2048 -validity 10000
```

During this prompt:
1. Enter a secure keystore password and key password (keep these safe!).
2. Fill out the certificate details (e.g., your name/organizational unit).
3. This creates a file named `lfg-release.jks`.

> [!CAUTION]
> Store your `lfg-release.jks` file securely and back it up. If you lose this key, you will not be able to update your installed app without uninstalling it first and losing your app data.

---

### Step C: Retrieve the SHA-256 Fingerprint
To register your key with Google, you need to extract its SHA-256 fingerprint. Run:

```bash
keytool -list -v -keystore lfg-release.jks -alias lfg-alias
```

Locate the line labeled **SHA256:**. It will look like this:
`SHA256: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00`

Copy this fingerprint string.

---

### Step D: Register the App Package with Google
1. Log into your **Google Play Console** or **Android Developer Console**.
2. Navigate to the **App Registration** or **Packages** section.
3. Register a new application package:
   * **Package Name (Application ID):** Enter the unique package name you chose in Step A (e.g., `com.yourusername.lfg`).
   * **Certificate Fingerprint:** Paste the SHA-256 fingerprint from Step C.
4. (Optional) You may be asked to upload an APK signed with this key to prove ownership.

---

## 4. Configuring GitHub Actions for Automatic Release Signing

LFG's release pipeline is pre-configured to automatically sign your APK when building in GitHub Actions. You do not need to modify any code. Simply configure **Repository Secrets** in your GitHub repository.

### Steps to configure:

1. **Convert your Keystore file to Base64:**
   Run the following command on your local machine to get the Base64 representation of your `lfg-release.jks` file:
   
   ```bash
   base64 -w 0 lfg-release.jks
   ```
   *On macOS, use:*
   ```bash
   base64 -i lfg-release.jks
   ```
   Copy the entire output string.

2. **Add Repository Secrets to your GitHub Fork:**
   Go to your fork's settings: **Settings → Secrets and variables → Actions → New repository secret** and add the following four secrets:

   * `LFG_RELEASE_KEYSTORE_BASE64`: Paste the entire base64 string you copied in Step 1.
   * `LFG_RELEASE_STORE_PASSWORD`: The password for your keystore.
   * `LFG_RELEASE_KEY_ALIAS`: The alias for your key (e.g., `lfg-alias`).
   * `LFG_RELEASE_KEY_PASSWORD`: The password for your key.

Once these secrets are configured, every time the `Release` workflow runs, it will decode your keystore, sign the release APK with your developer credentials, and attach the verified, sideload-ready APK directly to your GitHub Releases page!

---

## Appendix: Under-the-Hood Gradle Configuration

To keep keystore credentials out of public git repositories, the project is configured to read signing parameters dynamically from project properties. The **LFG** Gradle build has been updated in `android/src/app/build.gradle.kts` with the following configuration:

```kotlin
  signingConfigs {
    getByName("debug") {
      storeFile = file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }

    // Create the release signing configuration
    create("release") {
      if (project.hasProperty("LFG_RELEASE_STORE_FILE")) {
        storeFile = file(project.property("LFG_RELEASE_STORE_FILE") as String)
        storePassword = project.property("LFG_RELEASE_STORE_PASSWORD") as String
        keyAlias = project.property("LFG_RELEASE_KEY_ALIAS") as String
        keyPassword = project.property("LFG_RELEASE_KEY_PASSWORD") as String
      } else {
        // Fallback to debug configuration if credentials aren't present
        storeFile = file("debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Use the newly defined release signing configuration
      signingConfig = signingConfigs.getByName("release")
    }
  }
```

This configuration ensures that:
1. **Security:** Keystore passwords and alias values are never hardcoded inside git files.
2. **Robustness:** If the project properties are not provided (e.g. for standard contributors or local dev compilation), the release build falls back seamlessly to the `debug` signing configurations, preventing compile failures.
