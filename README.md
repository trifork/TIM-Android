# Trifork Identity Manager Android

## Example

See our fully implemented example here:

https://github.com/trifork/TIM-Example-Android

## Setup

### Installation

Add maven jitpack.io to your settings.gradle file and this repository to your gradle app file
```groovy
//Necessary for gradle to locate the github repository. Should be located in project settings.gradle file
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Can also be added to your allProjects block if you are using a older gradle version
````groovy
//Necessary for gradle to locate the github repository. Can be located in app build.gradle file
allproject{ 
    repositories{
        maven { url 'https://jitpack.io' } 
    }
}
````

Then add the TIM repository as dependency in your build.gradle :app file
````groovy
//In build.gradle :app file
implementation "com.github.trifork:tim-android:$tim_version"
````

### Setup configuration
Before using any function or property from `TIM` you have to configure the framework by calling the `configure` method (typically you want to do this on app startup):

```kotlin
val config = TIMConfiguration(
    URL("TIM base URL"),
    "realm",
    "clientId",
    Uri.parse("my-app://"),
    listOf(OIDScopeOpenID, OIDScopeProfile)
)

TIM.configure(config)
```

### URL scheme

You need to add the following appAuthRedirectScheme manifest placeholder to your app's gradle file using the same redirect url used in the TIMConfiguration above. 
This will let your app catch the redirect from the chrome custom tabs when the user has finished the login.
In order for your app to differentiate between build types, each buildType could define a specific `appIdSuffix` and `appAuthRedirectScheme`. 
This will make it possible to have several apps with different build types installed and still hit the correct app after successful login.

````groovy
buildTypes {
    debug {
        applicationIdSuffix ".debug"
        manifestPlaceholders = [
                appIdSuffix          : ".debug",
                appAuthRedirectScheme: "dk.bankinvest.darwin.debug"
        ]
    }

    release {
        applicationIdSuffix ""
        manifestPlaceholders = [
                appIdSuffix          : "",
                appAuthRedirectScheme: "dk.bankinvest.darwin"
        ]
    }
}
````

Furthermore the following needs to be added to your `AndroidManifest.xml` file, in order for the chrome custom tab to work

```xml
    <!-- App Auth -->
    <activity
        android:exported="true"
        android:name="net.openid.appauth.RedirectUriReceiverActivity">
        <intent-filter>
            <action android:name="android.intent.action.VIEW"/>
            <category android:name="android.intent.category.DEFAULT"/>
            <category android:name="android.intent.category.BROWSABLE"/>
            <data android:scheme="${appId}${appIdSuffix}"/>
        </intent-filter>
    </activity>
```

You can find more information about this at the openid [AppAuth-Android repository](https://github.com/openid/AppAuth-Android)

## Common use cases

### 1. Register / OIDC Login

All users will have to register through a OpenID Connect login.

First step is to get a Open ID Connect Login Intent and send it to a resultLauncher in order for us to start a activity with chrome tabs.
The resultLauncher then retrieves the resulting intent with the login result, which we send to TIM using 'handleOpenIDConnectLoginResult'  
```kotlin
fun launchLogin() = lifecycleScope.launch {
    val intentResult = TIM.auth.getOpenIDConnectLoginIntent(this).await()
    when (intentResult) {
        is TIMResult.Success -> {
            //Calling the resultLauncher with the intent result value
            resultLauncher.launch(intentResult.value)
        }
        is TIMResult.Failure -> {
            //Failed to launch login
        }
    }
}
//We want to have this in our fragment or activity in order for us to react upon the above launched login flow finishing
val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        if (data != null) {
            lifecycleScope.launch {
                val loginResult = TIM.auth.handleOpenIDConnectLoginResult(this, data).await()

                when (loginResult) {
                    is TIMResult.Success -> {
                        //Successfully authenticated
                    }
                    is TIMResult.Failure -> {
                        //Failed to authenticate
                    }
                }
            }
        }
    }
}
```

### 2. Setting password
To avoid the OpenID Connect login everytime the user needs a valid session, you can provide a password, which will allow you to save an encrypted 
version of the refresh token, such that the user only needs to provide the password to get a valid access token.

The user must have performed a successful OpenID Connect login before setting a password, since the refresh token has to be available.

````kotlin
fun setPassword() = viewModelScope.launch {
    val refreshToken = TIM.auth.getRefreshToken()
    if (refreshToken != null) {
        val storeResult = TIM.storage.storeRefreshTokenWithNewPassword(this, refreshToken, password).await()

        when (storeResult) {
            is TIMResult.Success -> {
                //The password was successfully set
                //Send the user to the enable biometric login
            }
        }
    }
}
````

### 3. Determine biometric authentication
The entire biometric authentication flow can be completed using TIM. After the user has created a password, you can query TIM for whether the user has access to biometric authentication using the `hasBiometricCapability` method.

Besides querying TIM makes it easy to navigate the user to the settings menu using either `createBiometricSettingsIntent` or `createBiometricEnrollmentIntent` you can easily create a intent for showing the exact settings menu for configuring biometric authentication.

````kotlin
fun determineBiometricAuthentication(context: Context) = viewModelScope.launch {
    val status = TIM.hasBiometricCapability(context)

    when (status.status) {
        TIMAuthenticationStatus.BIOMETRIC_SUCCESS -> {
            // we can use biometric
        }
        TIMAuthenticationStatus.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            // ask the the user to enroll
        }
        TIMAuthenticationStatus.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
            // ask the the user to update
        }
        else -> {
            // biometric authentication is not available
        }
    }
}

//Navigate the user to the settings screen
fun navigateToBiometricSettings() {
    resultLauncher.launch(TIM.createBiometricSettingsIntent())
}

//Navigates the user to the biometric enrollment screen
fun navigateToBiometricEnrollment() {
    resultLauncher.launch(TIM.createBiometricEnrollmentIntent())
}

var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        //The user returned, update our TIMAuthenticationStatus
        determineBiometricAuthentication()
    }
}
````

### 4. Enable biometric login
After you have ensured that the user has access to and configure biometric authentication, you can enable biometric authentication. You will need the user's password and the userId from the refresh token to do this.

The `userId` can be retrieved from the refresh token: `TIM.auth.refreshToken?.userId`

```kotlin
fun enableBiometric() = viewModelScope.launch {
    val result = TIM.storage.enableBiometricAccessForRefreshToken(this, pinCode, userId, fragment).await()

    when (result) {
        is TIMResult.Failure -> {
            //Failure check result.error to figure out which error was thrown
        }
        is TIMResult.Success -> {
            //Successfully enabled biometric access
        }
    }
}
```

### 5. Login with password/biometric

You have to provide the user ID for the user, that wishes to login (this allows multiple users to login on the same device).

The user can use biometric if it was enabled previously, otherwise you will have to provide the password.
You can set a `storeNewRefreshToken` to control whether the system should update the refresh token on successful login. This is **highly recommended** to store the new refresh token, since it will keep renewing the user's session everytime they login. Although, you can set this to false, if you have cases where you don't want to update it.

```kotlin
//Login with password
fun loginPassword() = viewModelScope.launch {
    val result = TIM.auth.loginWithPassword(this, userId, pinCode, true).await()
    handleLoginResult(result)
}

fun loginBiometric(fragment: Fragment) = viewModelScope.launch {
    val result = TIM.auth.loginWithBiometricId(this, userId, fragment = fragment).await()
    handleLoginResult(result)
}

fun handleLoginResult(result: TIMResult<JWT, TIMError>) {
    when(result) {
        is TIMResult.Failure -> {
            val error = result.error
            when(error) {
                // Note that this is a simplified error handling, which uses the Bool extensions to avoid huge switch statements.
                // If you want to handle errors the right way, you should look into all error cases and decide which you need specific
                // error handling for. The ones you see here are the most common ones, which are very likely to happen.
                is TIMError.Auth -> {
                    if(error.timAuthError.isRefreshTokenExpiredError()) {
                        // Refresh Token has expired.
                    }
                }
                is TIMError.Storage -> {
                    if(error.timStorageError.isKeyLocked()) {
                        // Handle key locked (three wrong password logins)
                    }
                    else if(error.timStorageError.isWrongPassword()) {
                        // Handle wrong password
                    }
                    else if(error.timStorageError.isBiometricFailedError()) {
                        // Handle biometric failed error
                    }
                    else if(error.timStorageError.isBiometricCanceledError()) {
                        // Biometric canceled, do nothing
                    }
                    else if(error.timStorageError.isKeyServiceError()) {
                        // Something went wrong while communicating with the key service (possible network failure)
                    }
                    else {
                        // Something failed - please try again.
                    }
                }
            }
        }
        is TIMResult.Success -> {
            // Successfully logged in
        }
    }
}
```

### 6. Make use of the data and the session
#### JWT data
The tokens are of the type `JWT`. `JWT` allows you to get the following data directly from the token:
* **Expiration timestamp:** `token.expire`
* **UserId:** `token.userId`

#### Users
The framework keeps track of the user's which has created passwords and stored encrypted refresh tokens.

The `TIM.storage.availableUserIds` will return a list of identifiers from the available refresh tokens (`sub` field). Any other data related to the user and the mapping between the ID and the user's data is your responsibility. `TIM` will only keep track of the identifier from the token.

#### Refresh token

In most cases you won't have to worry about your refresh token, since the `TIM` methods are handling this for you. If you should be in a situation, where you need it, it can be accessed from the `storage`:

```kotlin
TIM.storage.getStoredRefreshToken(scope, userId, password).await()
```

#### Access token

`TIM` makes sure that your access token always is valid and refreshed automatically. This is also why the `TIM.auth.accessToken()` is a async function.

Most of the time `TIM` will complete the call immediately when the token is available, and a bit slower when the token needs to be updated.

You should avoid assigning the value of the access token to a property, and instead always use this function when you need it to make sure the token is valid.

````kotlin
fun accessToken() = viewModelScope.launch {
    val result = TIM.auth.accessToken(this).await()

    when(result) {
        is TIMResult.Failure -> {
            //Failure check: result.error
        }
        is TIMResult.Success -> {
            //Success get jwt: result.value 
        }
    }
}
````

### 7. Log out
You can log out a user, which will throw away the current access token and refresh token, such that you will have to load it again by logging in.

```kotlin
TIM.auth.logout()
```

### 8. Delete user
You can delete all data stored for a user identifier, such that the refresh token no longer will be available and the user won't exist in the `availableUserIds` set anymore. Typically you would also want to log out in this situation:

```kotlin
TIM.auth.logout() // Logout of current session
TIM.storage.clear(theUserId) // Delete the stored user data
```

### 9. Enable background timeout
You can configure TIM to monitor the time the app has been in the background and make it log out automatically if the desired duration is exceeded.
    1. The user logs in (background monitor timeout is set to 5 minutes)
    2. The user sends the app to the background
    3. The user opens the app after 6 minutes
    4. TIM automatically calls logout, which invalidates the current session and invokes the timeout callback.

```kotlin
TIM.auth.enableBackgroundTimeout {
    //Show a dialog that informs the user and navigates the user to login upon closing it
}
```

### Understanding the errors

`TIM` can throw a large set of errors, because of the different dependencies. Common for all errors it that they are wrapped in a `TIMError.auth()` or `TIMError.storage()` type depending on the area that throws the error. The errors will contain other errors coming from the stomach of the framework and there are a couple of levels in this.

Most errors are helping you as a developer to figure out, what you might have configured wrongly. Once everything is configured at setup correctly it is a small set of errors, which is important to handle as specific errors:

```kotlin
// Refresh token has expired
TIMError.auth(TIMAuthError.refreshTokenExpired)

TIMError.storage(
    TIMStorageError.encryptedStorageFailed(
        TIMEncryptedStorageError.keyServiceFailed(TIMKeyServiceError.badPassword)
    )
)

TIMError.storage(
    TIMStorageError.encryptedStorageFailed(
        TIMEncryptedStorageError.keyServiceFailed(TIMKeyServiceError.keyLocked)
    )
)
```

Since the `TIMKeyServiceError`s are so deeply into the error structure, there are short hands for this on the `TIMStorageError` type:

```kotlin
if(timStorageError.isKeyLocked()) {
    // Handle key locked (three wrong password logins)
}
if(timStorageError.isWrongPassword()) {
    // Handle wrong password
}
if(timStorageError.isKeyServiceError()) {
    // Something went wrong while communicating with the key service (possible network failure)
}
if(timStorageError.isBiometricFailedError()) {
    // Handle biometric failed error
}
if(timStorageError.isBiometricCanceledError()) {
    // Biometric canceled, do nothing
}
```

Other errors should of course still be handled, but can be handled in a more generic way, since they might be caused by network issues, server updates, or other unpredictable cases.

## Dynamic linking
If you want to support dynamically linking the app to the mitid app the following steps have to be implemented. This will make it possible to app switch between your app and the MitId app in case the user has it installed on their device.
Note that the server has to be setup correctly to support this, and accept additional parameters. Furthermore you need a server that can host a `assertlinks.json` file, making it possible for the MitId app to navigate the user back to your app.

### 1. Add additional parameters to the TIM configuration
In order for TIM to redirect correctly to the MitId app after the user has input their username the following additional parameters have to be added to the TIMConfiguration object.
Be careful when defining your base url and callback path, making sure they are mapped correctly in the intent filter defined in step 5.

```kotlin
//Only add additional parameters if we actually have the app installed
val additionalParams = if (MitIdApp.isInstalled()) {
    hashMapOf(
        Pair("app_switch_os", "android"),
        Pair("enable_app_switch", "true"),
        Pair("app_switch_url", "$YOUR_BASE_URL$YOU_CALLBACK_PATH")
    )
} else {
    mapOf()
}

val config = TIMConfiguration(
    URL("TIM base URL"),
    "realm",
    "clientId",
    Uri.parse("my-app://"),
    listOf(OIDScopeOpenID, OIDScopeProfile),
    additionalParameters
)

TIM.configure(config)

```

### 2. Add a helper object to detect whether the user has the MitId app
The MitIdApp object could be implemented as the following, here we ask the system if the user has the mitid app installed on the phone:
```kotlin
object MitIdApp {
    private const val MIT_ID_APP_PACKAGE_NAME = "dk.mitid.app.android"

    fun isSupported(): Boolean {
        //If the app is installed, then it's supported. This is exactly how the MobilePay SDK does this check
        return try {
            App.getInstance().packageManager.getApplicationInfo(MIT_ID_APP_PACKAGE_NAME, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

### 3. Add a query to the manifest file
In order for the app to ask for a ApplicationInfo, the following has to be added to the `manifest.xml` file before your `Application` tag: 

```xml
    <!-- Needed to check if MitID is installed or not -->
    <queries>
        <package android:name="dk.mitid.app.android"/>
    </queries>
```

### 4. Generate an assertlinks.json file
A `assertlinks.json` also has to be generated and located at your selected redirect server in `.well-known/assetlinks.json` 

Information about assert link files can be found here:
https://developer.android.com/training/app-links/verify-site-associations

An example of an `assertlinks.json` file:
```json
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "YOUR_PACKAGE_NAME",
      "sha256_cert_fingerprints": [
        "YOUR_SHA_256_CERT_FINGERPRINT"
      ]
    }
  }
]

```

### 5. Implement a app link receiver activity and add a intent filter
In order for your app to capture the app links from the MitId and continue the authentication flow in the open Custom Tab we define a 'dummy' receiving activity.
This activity will capture the app link and close itself, our app will regain focus and continue from where we left of (our open Custom Tab).

````kotlin
class AppLinkReceiverActivity : FragmentActivity() {

    //We catch the AppLink here, and finish the activity in order to 'Resume' the previously running activity that opened the CustomTab.
    //The issue is that android "launches" a new instance of the receiver from the intent filter, thus we "cannot" just return to the current running app.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenResumed {
            if (isTaskRoot) {
                startActivity(Intent(this@AppLinkReceiverActivity, StartActivity::class.java))
            }
            finish()
        }
    }
}
````

The intent following intent filter should be added to the activity definition in the `AndroidManifest.xml` file.
The chosen base url and callback path has to be identical to the ones defined in your additional parameters object.

````xml
<activity android:name=".ui.AppLinkReceiverActivity" android:exported="true" >
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
    
        <data android:scheme="https" />
        <data android:host="$YOUR_BASE_URL" android:path="$YOU_CALLBACK_PATH" />
    </intent-filter>
</activity>
````

## Architecture

`TIM` depends on `AppAuth` and `TIMEncryptedStorage` and wraps their use for common use cases (see sections above), such that registering, login and encrypted storage is easy to manage.

#### Storage
The `TIM.storage: TIMDataStorage` handles all storage operations in terms of encrypted and raw data to a secure storage (default is shared preferences).

This heavily depends on the `TIMEncryptedStorage` package, which communicates with the TIM KeyService, to handle encryption based on a user selected password and biometric access if enabled.

#### Auth
The `TIM.auth: TIMAuth` handles all OpenID Connect operations through the `AppAuth` framework. The main purpose of this is to handle access and refresh tokens and renewal of both. `TIMAuth` depends on the `TIMDataStorage` to store new refresh tokens.

### TIMEncryptedStorage
`TIM` depends on `TIMEncryptedStorage` for encrypted data storage and access via TouchID/FaceID:
https://github.com/trifork/TIMEncryptedStorage-Android

### AppAuth
`TIM` depends on `AppAuth` for OpenID Connect operations:
https://github.com/openid/AppAuth-Android

## Testing

`TIM` is designed to be testable, such that you can mock the parts of the framework, that you would like to. The framework contains a custom `configure` method, which allows you to fully customise the inner implementations of the framework:
```kotlin
TIM.configure(dataStorage: TIMDataStorage, auth: TIMAuth, customLogger: TIMLogger?)
```
Every dependency in `TIM` is build upon interfaces, such that you can implement your own mock-classes for testing.

⚠️ **NOTE:** This `configure` method allows you to change the `TIM` behaviour. We strongly recommend that you only use the above `configure` method for testing!

---

## For developers of TIM

### Development environment
Each project has a development build variant that makes it possible to build the projects using local copies of the repositories for faster iteration.

When selecting the development build variant for the example project or TIM, gradle will use the projects located in the same root folder as the project itself as the source for TIM and TIMEncryptedStorage.

Other build variants use the defined version from github. The debug build uses the github version in order for new developers to download the example project and just run it without needing extra work besides synchronizing and building the project.   


#### Testing locally
For testing changes to a local copy of the TIM library, add the following include in your `settings.gradle` file. (The TIM-Android folder is located in the same directory as the project files in this example)
```groovy
include ':TIM-Android'
project(':TIM-Android').projectDir = new File('../TIM-Android/TIM-Android') //Your local copy of the TIM Library

include ':TIMEncryptedStorage-Android'
project(':TIMEncryptedStorage-Android').projectDir = new File('../TIMEncryptedStorage-Android/library') //Your local copy of the TIMEncryptedStorage Library
```

And use the following implementations statement in your `app/build.gradle` file under dependencies.
```groovy
implementation project(':TIM-Android')
```

### Deployment
TIM and TIMEncryptedStorage are both distributed using a simple jitpack setup. Each project has a publish.gradle file determining the publishing configuration.

For now releasing of TIM and TIMEncryptedStorage has been achieved using the Github release interface, tagging the given commit with the version code. 
This makes it possible to refer to the version tag in gradle, as jitpack automatically creates a release using the version tag. 

There are several neat functions in jitpack making it possible to get untagged builds using gradle as well if needed.
Please refer to the official jitpack documentation for more information about how jitpack works: https://jitpack.io/docs/ or googles documentation at https://developer.android.com/studio/build/maven-publish-plugin


![Trifork Logo](https://jira.trifork.com/s/-p6q4kx/804003/9c3efa9da3fa1ef9d504f68de6c57528/_/jira-logo-scaled.png)
