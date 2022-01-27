# Trifork Identity Manager Android

## Example

See our fully implemented example here:

https://github.com/trifork/TIM-Example-Android

## Setup

### Installation

Add maven jit to your settings.gradle file and this repository to your gradle app file
```groovy
//Necessary in order for gradle to locate the github repository. Can be located in settings.gradle file
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

//In build.gradle :app file
implementation "com.github.trifork:TIM-Android:1.0.0"
```

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
This will let your app catch the redirect from the chrome custom tabs when the user has finished the login:
````groovy
android.defaultConfig.manifestPlaceholders = ['appAuthRedirectScheme': 'my-app://']
````

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

// The user pressed cancel in the safari view controller during the OpenID Connect login
TIMError.auth(TIMAuthError.safariViewControllerCancelled)

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

![Trifork Logo](https://jira.trifork.com/s/-p6q4kx/804003/9c3efa9da3fa1ef9d504f68de6c57528/_/jira-logo-scaled.png)
