package com.familyos.core.data.auth

import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Maps Firebase Auth throwables into user-facing [AppError] messages.
 */
object FirebaseAuthErrorMapper {

    private const val SETUP_HINT =
        "Firebase не настроен. Замените app/google-services.json на файл из Firebase Console " +
            "и включите Authentication → Email/Password (+ Google). Подробности в README."

    /** Converts any auth-related [Throwable] into a domain [AppException]. */
    fun toAppException(throwable: Throwable): AppException {
        val message = when (throwable) {
            is FirebaseAuthWeakPasswordException ->
                "Пароль слишком слабый. Минимум 6 символов."

            is FirebaseAuthInvalidCredentialsException ->
                when {
                    throwable.errorCode.contains("email", ignoreCase = true) ->
                        "Некорректный email."
                    throwable.message?.contains("API key", ignoreCase = true) == true ->
                        SETUP_HINT
                    else ->
                        "Неверный email или пароль."
                }

            is FirebaseAuthInvalidUserException ->
                "Пользователь не найден. Зарегистрируйтесь."

            is FirebaseAuthUserCollisionException ->
                "Этот email уже зарегистрирован. Войдите или восстановите пароль."

            is FirebaseNetworkException ->
                "Нет сети. Проверьте интернет и повторите."

            is FirebaseAuthException -> mapAuthCode(throwable.errorCode, throwable.message)

            else -> {
                val raw = throwable.message.orEmpty()
                when {
                    raw.contains("API key", ignoreCase = true) ||
                        raw.contains("API_KEY", ignoreCase = true) ||
                        raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                        raw.contains("app not authorized", ignoreCase = true) ||
                        raw.contains("DEVELOPER_ERROR", ignoreCase = true) ->
                        SETUP_HINT

                    raw.isBlank() -> "Ошибка авторизации. Проверьте Firebase-конфигурацию (README)."
                    else -> raw
                }
            }
        }
        return AppException(AppError.Unauthorized(message, throwable))
    }

    private fun mapAuthCode(code: String, fallback: String?): String = when (code) {
        "ERROR_INVALID_API_KEY",
        "ERROR_APP_NOT_AUTHORIZED",
        "ERROR_API_KEY_NOT_VALID",
        "ERROR_CONFIGURATION_NOT_FOUND",
        -> SETUP_HINT

        "ERROR_OPERATION_NOT_ALLOWED" ->
            "В Firebase Console включите Authentication → Sign-in method → Email/Password " +
                "(и Google, если используете вход через Google)."

        "ERROR_EMAIL_ALREADY_IN_USE" ->
            "Этот email уже зарегистрирован."

        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_INVALID_EMAIL",
        -> "Неверный email или пароль."

        "ERROR_USER_DISABLED" -> "Аккаунт отключён."
        "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток. Подождите и попробуйте снова."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Нет сети. Проверьте интернет."
        else -> fallback ?: "Ошибка Firebase Auth ($code). См. README."
    }
}
