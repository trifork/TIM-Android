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
     * // TODO: Missing docs - MFJ (14/09/2021)
     */
    fun hasBiometricAccessForRefreshToken(userId: String): Boolean

    /**
     *  // TODO: Missing docs - MFJ (14/09/2021)
     */
    fun disableBiometricAccessForRefreshToken(userId: String)

    /**
     * Clears all securely stored data for [userId]
     */
    fun clear(userId: String)

    /**
     * Gets a stored refresh token for a [userId] and [password] combination, if such exists
     * @param userId The user id associated with the refresh token
     * @param password The password that was used to store the refresh token
     */
    fun getStoredRefreshToken(scope: CoroutineScope, userId: String, password: String): Deferred<TIMResult<JWT, TIMError>>

    /**
     * // TODO: Missing docs - MFJ (14/09/2021)
     */
    fun storeRefreshTokenWithExistingPassword(scope: CoroutineScope, refreshToken: JWT, password: String): Deferred<TIMResult<Unit, TIMError>>

    /**
     * Stores a refresh
     */
    fun storeRefreshTokenWithNewPassword(scope: CoroutineScope, refreshToken: JWT, password: String): Deferred<TIMResult<TIMESKeyCreationResult, TIMError>>

    /**
     * // TODO: Missing docs - MFJ (14/09/2021)
     */
    fun getStoredRefreshTokenViaBiometric(scope: CoroutineScope, userId: String, fragment: Fragment): Deferred<TIMResult<BiometricRefreshToken, TIMError>>

    /**
     * // TODO: Missing docs - MFJ (14/09/2021)
     */
    fun enableBiometricAccessForRefreshToken(scope: CoroutineScope, password: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>>

    /**
     *  // TODO: Missing docs - MFJ (14/09/2021)
     */
    fun storeRefreshTokenWithLongSecret(scope: CoroutineScope, refreshToken: JWT, longSecret: String): Deferred<TIMResult<Unit, TIMError>>

    /**
     * Enables biometric protection access for refresh token using longSecret.
     * @param longSecret The long secret that was created upon creation of the password.
     * @param userId The `userId` for the refresh token.
     */
    fun enableBiometricAccessForRefreshTokenLongSecret(scope: CoroutineScope, longSecret: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>>
}