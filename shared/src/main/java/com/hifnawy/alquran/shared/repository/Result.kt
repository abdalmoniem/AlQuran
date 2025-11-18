package com.hifnawy.alquran.shared.repository

sealed interface Error
typealias RootError = Error

sealed interface Result<out D, out E : RootError> {
    data class Success<out D, out E : RootError>(val data: D) : Result<D, E>
    data class Error<out D, out E : RootError>(val error: E) : Result<D, E>
}

sealed interface DataError : Error {

    val errorMessage: String

    sealed interface NetworkError : DataError {

        val errorCode: Int

        data class TooManyRequests(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class PayloadTooLarge(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class RequestTimeout(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class Unreachable(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class Unauthorized(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class Forbidden(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class NotFound(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class ServerError(override val errorCode: Int, override val errorMessage: String) : NetworkError
        data class Unknown(override val errorCode: Int, override val errorMessage: String) : NetworkError
    }

    sealed interface LocalError : DataError {
        data class DiskFull(override val errorMessage: String) : LocalError
    }

    sealed interface ParseError : DataError {
        data class JsonSyntaxException(override val errorMessage: String) : ParseError
    }
}
