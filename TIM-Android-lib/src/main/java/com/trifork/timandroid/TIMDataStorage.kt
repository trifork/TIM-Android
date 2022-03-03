package com.trifork.timandroid

import androidx.fragment.app.Fragment
import com.trifork.timandroid.helpers.BiometricRefreshToken
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.models.errors.TIMError
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.keyservice.TIMESKeyCreationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface TIMDataStorage {

    /**
     * The set of user ids which there are refresh tokens stored for.
     */
    val availableUserIds: Set<String>

    /**
     * Checks whether a [userId] has a stored refresh token
     */
    fun hasRefreshToken(userId: String): Boolean

    /**
     * Checks whether a [userId] has stored a refresh token with biometric protection access
     * @param userId the [userId] from the refresh token
     */
    fun hasBiometricAccessForRefreshToken(userId: String): Boolean

    /**
     *  Disables biometric protection access for refresh token
     *  @param userId the [userId] for the refresh token
     */
    fun disableBiometricAccessForRefreshToken(userId: String)

    /**
     * Clears all securely stored data for [userId]
     * @param userId the [userId]
     */
    fun clear(userId: String)

    /**
     * Gets a stored refresh token for a [userId] and [password] combination, if such exists
     * @param userId The user id associated with the refresh token
     * @param password The password that was used to store the refresh token
     */
    fun getStoredRefreshToken(scope: CoroutineScope, userId: String, password: String): Deferred<TIMResult<JWT, TIMError>>

    /**
     * Stores refresh token with existing password.
     * @param scope the [CoroutineScope] eg. a viewModelScope
     * @param refreshToken the refresh token
     * @param password the password that already has a encryption key
     * @return deferred TIMResult containing a Unit or TIMError class when the operation fails
     */
    fun storeRefreshTokenWithExistingPassword(scope: CoroutineScope, refreshToken: JWT, password: String): Deferred<TIMResult<Unit, TIMError>>

    /**
     * Stores refresh token with a new password and removes current biometric access for potential previous refresh token
     * @param scope the [CoroutineScope] eg. a viewModelScope
     * @param refreshToken the refresh token
     * @param password a new password that needs a new encryption key
     * @return deferred TIMResult containing a TIMESKeyCreationResult or TIMError class when the operation fails
     */
    fun storeRefreshTokenWithNewPassword(scope: CoroutineScope, refreshToken: JWT, password: String): Deferred<TIMResult<TIMESKeyCreationResult, TIMError>>

    /**
     * Gets a stored refresh token with biometric protection for a [userId]
     * @param scope the [CoroutineScope] eg. a viewModelScope
     * @param userId the [userId] from the refresh token
     * @param fragment a fragment for displaying the biometric authentication prompt
     * @return deferred TIMResult containing a BiometricRefreshToken with the `longSecret`, which was used as secret from the biometric secure store. A TIMError class is returned if the operation fails
     */
    fun getStoredRefreshTokenViaBiometric(scope: CoroutineScope, userId: String, fragment: Fragment): Deferred<TIMResult<BiometricRefreshToken, TIMError>>

    /**
     * Enable biometric access for refresh token using password.
     * @param scope the [CoroutineScope] eg. a viewModelScope
     * @param password the defined password for the provided [userId]
     * @param userId the users [userId]
     * @param fragment a fragment for displaying the biometric authentication prompt
     * @return deferred TIMResult containing a Unit or TIMError class when the operation fails
     */
    fun enableBiometricAccessForRefreshToken(scope: CoroutineScope, password: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>>

    /**
     * Enables biometric protection access for refresh token using longSecret.
     * @param scope the [CoroutineScope] eg. a viewModelScope
     * @param longSecret The long secret that was created upon creation of the password.
     * @param userId The [userId] for the refresh token.
     * @param fragment a fragment for displaying the biometric authentication prompt
     * @return deferred TIMResult containing a Unit or TIMError class when the operation fails
     */
    fun enableBiometricAccessForRefreshTokenLongSecret(scope: CoroutineScope, longSecret: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>>

    /**
     * Stores a refresh token using long secret instead of password.
     * It is unlikely, that you will need to use this method, unless you are doing something custom. TIM does use this method internally to keep refresh tokens up-to-date even when logging in with biometric access.
     * @param scope the [CoroutineScope] eg. a viewModelScope
     * @param refreshToken The refresh token
     * @param longSecret The long secret (can be obtained via biometric access)
     * @return deferred TIMResult containing a Unit or TIMError class when the operation fails
     */
    fun storeRefreshTokenWithLongSecret(scope: CoroutineScope, refreshToken: JWT, longSecret: String): Deferred<TIMResult<Unit, TIMError>>


}