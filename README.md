Firebase Android - Authentication with Facebook, Google and custom
===

Getting started
---

- [Add Firebase to your Android Project](https://firebase.google.com/docs/android/setup).

### Google Sign In Setup

- Go to the [Firebase Console](https://console.firebase.google.com) and navigate to your project:
  - Select the **Auth** panel and then click the **Sign In Method** tab.
  - Click **Google** and turn on the **Enable** switch, then click **Save**.
- Run the sample app on your device or emulator.
  - Click the **Sign In** button with the Google-logo
  
### Facebook Login Setup

- Go to the [Facebook Developers Site](https://developers.facebook.com) and follow all
  instructions to set up a new Android app.
  - When asked for a package name, check the AndroidManifest.xml and use what's under 
  the "package" tag.
  - When asked for a main class name,
  use the same as above but append ".LoginActivity". Ex. <package-name>.LoginActivity.
- Go to the [Firebase Console](https://console.firebase.google.com) and navigate to your project:
  - Select the **Auth** panel and then click the **Sign In Method** tab.
  - Click **Facebook** and turn on the **Enable** switch, then click **Save**.
  - Enter your Facebook **App Id** and **App Secret** and click **Save**.
- Open the file `app/src/main/res/values/strings.xml` and replace the value of the `facebook_app_id` with the ID of the Facebook app you just created.
- Run the app on your device or emulator.
    - Click the **Continue with Facebook** button to begin.

### Custom Authentication Setup

- Go to the [Google Developers Console](https://console.developers.google.com/project) and navigate to your project:
    - Click on the **Service accounts** tab in the left.
    - Click on the **Create Service Account** on the top 
    or use an existing like firebase-adminsdk (only for dev-mode).
    - Enter desired service account name and click on the **Create** button.
    - Once the service account is created, click on the **Options**.
    - Choose **JSON** as the key type then click on the **Create** button.
    - You should now have a new JSON file for your service account in your Downloads directory.
- Open the file `web/auth.html` using your web browser.
    - Click **Choose File** and upload the JSON file you just downloaded.
    - Enter any User ID and click **Generate**.
    - Copy the text from the **ADB Command** section. This will be required later on.
- Run the Android application on your Android device or emulator.
    - Run the text copied from the **ADB Command** section of the web page in the steps above. This will update the Custom Token field of the running app.
    - Click **Custom Sign In** button to sign in to Firebase User Management with the generated JWT. You should
      see the User ID you entered when generating the token.