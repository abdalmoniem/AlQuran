package com.hifnawy.alquran.shared.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object QuranRepository {

    private val applicationContext by lazy { QuranApplication.applicationContext }
    private val recitersURL by lazy { "${applicationContext.getString(R.string.API_BASE_URL)}/${applicationContext.getString(R.string.API_RECITERS)}?language=ar" }
    private val surahsURL by lazy { "${applicationContext.getString(R.string.API_BASE_URL)}/${applicationContext.getString(R.string.API_SURAHS)}?language=ar" }

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
                val errorType = getErrorType(response)
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

    private fun getErrorType(response: Response): DataError.NetworkError = when (response.code) {
        401         -> DataError.NetworkError.Unauthorized(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.Unauthorized::class.simpleName}: ${response.message}"
        )

        403         -> DataError.NetworkError.Forbidden(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.Forbidden::class.simpleName}: ${response.message}"
        )

        404         -> DataError.NetworkError.NotFound(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.NotFound::class.simpleName}: ${response.message}"
        )

        408         -> DataError.NetworkError.RequestTimeout(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.RequestTimeout::class.simpleName}: ${response.message}"
        )

        413         -> DataError.NetworkError.PayloadTooLarge(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.PayloadTooLarge::class.simpleName}: ${response.message}"
        )

        429         -> DataError.NetworkError.TooManyRequests(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.TooManyRequests::class.simpleName}: ${response.message}"
        )

        in 500..599 -> DataError.NetworkError.ServerError(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.ServerError::class.simpleName}: ${response.message}"
        )

        else        -> DataError.NetworkError.Unknown(
                errorCode = response.code,
                errorMessage = "${DataError.NetworkError.Unknown::class.simpleName}: ${response.message}"
        )
    }

    suspend fun getRecitersList(): Result<List<Reciter>, DataError> = sendRESTRequest<List<Reciter>>(recitersURL) { jsonResponse ->
        val recitersJsonArray = JSONObject(jsonResponse).getJSONArray(applicationContext.getString(R.string.API_RECITERS)).toString()

        Gson().fromJson(recitersJsonArray, object : TypeToken<List<Reciter>>() {}.type)
    }

    suspend fun getSurahs(): Result<List<Surah>, DataError> = sendRESTRequest(surahsURL) { jsonResponse ->
        val surahsJsonArray = JSONObject(jsonResponse).getJSONArray(applicationContext.getString(R.string.API_SURAHS)).toString()

        Gson().fromJson(surahsJsonArray, object : TypeToken<List<Surah>>() {}.type)
    }
}
