package io.github.drumber.kitsune.data.service

import io.github.drumber.kitsune.data.model.AccessToken
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthService {

    @FormUrlEncoded
    @POST("token")
    suspend fun obtainAccessToken(
        @Field("grant_type") grantType: String = "password",
        @Field("username") username: String,
        @Field("password") password: String
    ): AccessToken

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): AccessToken

}