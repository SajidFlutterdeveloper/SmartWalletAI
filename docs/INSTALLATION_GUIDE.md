# Installation & Developer Guide

### Prerequisites
*   Android Studio Iguana | 2023.2.1 or newer.
*   Android SDK 34 (Upside Down Cake).
*   Java Development Kit (JDK) 17.

### 1. Developer Setup (From Source)
1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/SajidFlutterdeveloper/SmartWalletAI.git
    ```
2.  **Firebase Configuration:**
    *   Go to the [Firebase Console](https://console.firebase.google.com/).
    *   Create a new project named "SmartWalletAI".
    *   Add an Android App using the package name `com.smartwallet.ai`.
    *   Download `google-services.json` and place it in the `app/` folder.
    *   Enable **Authentication (Email/Password)** and **Cloud Firestore**.
3.  **Build the Project:**
    *   Open the project in Android Studio.
    *   Wait for Gradle Sync to finish.
    *   Run the app on an Emulator or Physical Device.

### 2. APK Installation (For Users)
1.  Locate the file in `apk/SmartWalletAI_v1.0.apk`.
2.  Transfer the APK to your Android device.
3.  Enable "Install from Unknown Sources" in your phone's security settings.
4.  Open the file and tap "Install".

### 3. Future Enhancements
*   [ ] Bank SMS Auto-Detection.
*   [ ] Export data to PDF/Excel.
*   [ ] Multi-currency support.
*   [ ] Dark Mode optimization.
