package com.example.gagalmuluyaallah.connection

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.dicoding.picodiploma.mycamera.reduceFileImage
import com.example.gagalmuluyaallah.ResultSealed
import com.example.gagalmuluyaallah.model.ApiService
import com.example.gagalmuluyaallah.model.GeneralResponse
import com.example.gagalmuluyaallah.model.LoginResponse
import com.example.gagalmuluyaallah.model.LoginResult
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File

class GeneralRepository private constructor(
        private var apiService: ApiService,
        private val userPreference: UserPreference,
) {
    // set token variable
    private var token: String? = null

    // get token from user preference
    //the method is called suspend because its need to wait until the token is ready
    // so it can run in the background
    private suspend fun getToken(): String? = token ?: runBlocking {
        userPreference.getToken().first()
    }.also { token = it }


    // this function is to send the register user to dicoding API Database.
    //@PARAM name
    //@PARAM email
    //@Param password
    fun register(name: String, email: String, password: String): LiveData<ResultSealed<GeneralResponse>> = liveData {
        emit(ResultSealed.Loading)
        Log.d("GeneralRepository", "register: $name, $email, $password")
        try {
            val response = apiService.register(name, email, password)
            if (!response.error!!) {
                emit(ResultSealed.Success(response))
                Log.d("GeneralRepository", "register sukses: ${response.message}")
            }
            else {
                emit(ResultSealed.Error(response.message.toString()))
                Log.e("GeneralRepository error 1 :", "register 01 : ${response.message}")
            }
            emit(ResultSealed.Success(response))
        } catch (e: HttpException) {
            // this one is for the error response from the server <<<<< (●'◡'●)
            val errorBody = e.response()?.errorBody()?.string()
            val response = Gson().fromJson(errorBody, GeneralResponse::class.java)
            emit(ResultSealed.Error(response.message.toString()))
            Log.e("GeneralRepository error 2 :", "register 02 : ${response.message}")
        } catch (e: Exception) {
            // and this one is for the error response from the app <<<<<<< ☜(ﾟヮﾟ☜)
            emit(ResultSealed.Error(e.message.toString()))
            Log.e("GeneralRepository error 3", "register 03: ${e.message}")
        }
    }

    fun login(email: String, password: String): LiveData<ResultSealed<LoginResult>> = liveData {
        emit(ResultSealed.Loading)
        try {
            val response = apiService.login(email, password)
            val loginResult = response.loginResult

            if (loginResult != null) {
                emit(ResultSealed.Success(loginResult)) //if sukses
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, LoginResponse::class.java)
            Log.e("Login Server error", "Login HTTP : ${e.message}")
            emit(ResultSealed.Error(errorResponse.message.toString()))

        } catch (e: Exception) {
            Log.d("Login Result error 3", "Register 03 : ${e.message}")
            emit(ResultSealed.Error(e.message.toString()))

        }
    }

    fun uploadNewStory(file: File?, description: String, lat: Double?, lon: Double?): LiveData<ResultSealed<GeneralResponse>> = liveData {
        emit(ResultSealed.Loading)
        try {
            val imageFile = reduceFileImage(file!!)
            Log.d("Image File", "showImage: ${imageFile.path}")

            val descriptionBody = description.toRequestBody("text/plain".toMediaType())
            val requestImageFile = imageFile.asRequestBody("image/jpeg".toMediaType())

            val multipartBody = MultipartBody.Part.createFormData(
                    "photo",
                    imageFile.name,
                    requestImageFile
            )

            val response = apiService.addNewStory(multipartBody, descriptionBody, lat, lon)

            emit((ResultSealed.Success(response)))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBody, GeneralResponse::class.java)

            emit(ResultSealed.Error(errorResponse.message.toString()))
        } catch (e: Exception) {
            Toast.makeText(null, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            emit(ResultSealed.Error(e.message.toString()))
        }
    }

    fun getAllStories(viewModelScope: CoroutineScope) {

    }

    //    fun getAllStories(): LiveData<ResultSealed<PagingData<ListStoryItem>>> = liveData {
    //        emit(ResultSealed.Loading)
    //        try {
    //            val response = apiService.getStories()
    //            val pagingData = listOf(response)!!.map { it }
    //            emit(ResultSealed.Success(PagingData.from(pagingData)))
    //        } catch (e: HttpException) {
    //            val errorBody = e.response()?.errorBody()?.string()
    //            val errorResponse = Gson().fromJson(errorBody, StoriesResponse::class.java)
    //            emit(ResultSealed.Error(errorResponse.message.toString()))
    //        } catch (e: Exception) {
    //            emit(ResultSealed.Error(e.message.toString()))
    //        }
    //    }

//    fun getAllStories(coroutineScope: CoroutineScope): LiveData<ResultSealed<List<StoryItems>>> = liveData {
//        emit(ResultSealed.Loading)
//        try {
//            val token = getToken()
//            apiService = ApiConfig.getApiService(token.toString())
//
//            val response = apiService.getStories()
//        } catch (e: HttpException) {
//
//            val errorBody = e.response()?.errorBody()?.string()
//            val errorResponse = Gson().fromJson(errorBody, StoriesResponse::class.java)
//
//            emit(ResultSealed.Error(errorResponse.message.toString()))
//        } catch (e: Exception) {
//            Log.e("GeneralRepository", "getAllStories: ${e.message}")
//            emit(ResultSealed.Error(e.message.toString()))
//        }
//    }




    companion object {
        //set instance to GeneralRepository
        private var instance: GeneralRepository? = null

        // this one is to set the instance so it can be use somewhere else
        fun getInstance(
                // instance for api and userpreference to save the token
                apiService: ApiService, //from ApiService
                userPreference: UserPreference,
        ): GeneralRepository {
            return instance ?: synchronized(this) {
                instance ?: GeneralRepository(apiService, userPreference).also { instance = it }
            }
        }
    }
}