package com.trifork.timandroid.biometric

typealias TIMBiometricAuthentication = Int

val TIMBiometricAuthentication.status: TIMAuthenticationStatus
    get() = TIMAuthenticationStatus.getAuthenticationStatus(this)

enum class TIMAuthenticationStatus(val status: Int) {
    BIOMETRIC_STATUS_UNKNOWN(-1),
    BIOMETRIC_SUCCESS(0),
    BIOMETRIC_ERROR_NONE_ENROLLED(11),
    BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED(15),
    BIOMETRIC_ERROR_NO_HARDWARE(12),
    BIOMETRIC_ERROR_UNSUPPORTED(-2),
    BIOMETRIC_ERROR_HW_UNAVAILABLE(1);

    companion object {
        fun getAuthenticationStatus(status: Int) : TIMAuthenticationStatus = values()
            .firstOrNull { it.status == status }
            .let { it ?: BIOMETRIC_STATUS_UNKNOWN }
    }
}