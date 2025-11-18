package com.hifnawy.alquran.shared.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class QuranRepository(private val context: Context) {

    val recitersURL = "${context.getString(R.string.API_BASE_URL)}/${context.getString(R.string.API_RECITERS)}?language=ar"
    val surahsURL = "${context.getString(R.string.API_BASE_URL)}/${context.getString(R.string.API_SURAHS)}?language=ar"

    /**
     * Sends a generic GET REST request and attempts to parse the response body.
     *
     * @param url The URL to request.
     * @param parser A function to convert the raw String response body into the expected type D.
     * @return A Result<D, DataError> indicating success with parsed data or a DataError.
     */
    private suspend fun <D> sendRESTRequest(url: String, parser: (String) -> D): Result<D, DataError> = withContext(Dispatchers.IO) {
        val client: OkHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            // .retryOnConnectionFailure(true)
            .build()
        val request: Request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Accept", "application/json")
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()

            // Ensure the body is consumed and closed, getting the raw string
            val responseBody = response.body.string()

            if (response.isSuccessful) {
                try {
                    // Success: Parse the body string into the generic type D
                    val data = parser(responseBody)
                    Result.Success(data)
                } catch (ex: Exception) {
                    // Error: Parsing failed (e.g., malformed JSON)
                    Result.Error(
                            DataError.ParseError.JsonSyntaxException(
                                    errorMessage = "${DataError.ParseError.JsonSyntaxException::class.simpleName}: ${ex.message ?: "Json Syntax Exception"}"
                            )
                    ) // Using ParseError
                }
            } else {
                // Error: Map HTTP status code to specific NetworkError enum
                val errorType = when (response.code) {
                    401 -> DataError.NetworkError.Unauthorized(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.Unauthorized::class.simpleName}: ${response.message}"
                    )

                    403 -> DataError.NetworkError.Forbidden(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.Forbidden::class.simpleName}: ${response.message}"
                    )

                    404 -> DataError.NetworkError.NotFound(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.NotFound::class.simpleName}: ${response.message}"
                    )

                    408 -> DataError.NetworkError.RequestTimeout(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.RequestTimeout::class.simpleName}: ${response.message}"
                    )

                    413 -> DataError.NetworkError.PayloadTooLarge(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.PayloadTooLarge::class.simpleName}: ${response.message}"
                    )

                    429 -> DataError.NetworkError.TooManyRequests(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.TooManyRequests::class.simpleName}: ${response.message}"
                    )

                    in 500..599 -> DataError.NetworkError.ServerError(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.ServerError::class.simpleName}: ${response.message}"
                    )

                    else -> DataError.NetworkError.Unknown(
                            errorCode = response.code,
                            errorMessage = "${DataError.NetworkError.Unknown::class.simpleName}: ${response.message}"
                    )
                }
                Result.Error(errorType)
            }
        } catch (ex: IOException) {
            // Handle low-level IO/network exceptions (connection failed, DNS issue)
            // Check for specific connection issues, otherwise default to UNREACHABLE
            val errorType = when (ex) {
                is SocketTimeoutException -> DataError.NetworkError.RequestTimeout(
                        errorCode = -1,
                        errorMessage = "${DataError.NetworkError.RequestTimeout::class.simpleName}: ${ex.message ?: " Request Timeout"}"
                )

                else                      -> DataError.NetworkError.Unreachable(
                        errorCode = -1,
                        errorMessage = "${DataError.NetworkError.Unreachable::class.simpleName}: ${ex.message ?: " Unreachable "}"
                )
            }
            Result.Error(errorType)
        } catch (ex: Exception) {
            // Catch all other unexpected exceptions
            Result.Error(
                    DataError.NetworkError.Unknown(
                            errorCode = -1,
                            errorMessage = "${DataError.NetworkError.Unknown::class.simpleName}: ${ex.message ?: " Unknown Error Occurred !"}"
                    )
            )
        }
    }

    private suspend fun sendRESTRequest(
            url: String,
            responseHandler: ((error: Boolean, errorType: KClass<out Exception>?, responseMessage: String) -> Unit)?
    ) {
        val client: OkHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            // .retryOnConnectionFailure(true)
            .build()
        val request: Request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Accept", "application/json")
            .build()

        withContext(Dispatchers.IO) {
            try {
                async { client.newCall(request).execute() }.await().apply {
                    responseHandler?.invoke(false, null, body.string())
                }
            } catch (ex: Exception) {
                Timber.warn(ex.message, ex)
                responseHandler?.invoke(true, ex::class, "Connection failed with error: $ex")
            }
        }
    }

    suspend fun getRecitersList(context: Context): Result<List<Reciter>, DataError> = sendRESTRequest<List<Reciter>>(recitersURL) { jsonResponse ->
        val recitersJsonArray = JSONObject(jsonResponse).getJSONArray(context.getString(R.string.API_RECITERS)).toString()

        Gson().fromJson(recitersJsonArray, object : TypeToken<List<Reciter>>() {}.type)
    }

    suspend fun getSurahs(context: Context): List<Surah> {
        var surahs = emptyList<Surah>()

        sendRESTRequest(surahsURL) { error, _, responseMessage ->
            if (error) {
                Timber.warn(responseMessage)
                return@sendRESTRequest
            }
            val surahsJsonArray = JSONObject(responseMessage).getJSONArray(context.getString(R.string.API_SURAHS)).toString()

            surahs = Gson().fromJson(surahsJsonArray, object : TypeToken<List<Surah>>() {}.type)
        }

        Timber.debug(surahs.joinToString(separator = "\n") { it.toString() })

        return surahs
    }
}
