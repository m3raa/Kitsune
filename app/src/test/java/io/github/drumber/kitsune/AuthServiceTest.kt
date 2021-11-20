package io.github.drumber.kitsune

import io.github.drumber.kitsune.data.service.auth.AuthService
import io.github.drumber.kitsune.di.serviceModule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Test

class AuthServiceTest : BaseTest() {

    override val koinModules = listOf(serviceModule)

    @Ignore("Requires working credentials.")
    @Test
    fun obtainAccessToken() = runBlocking {
        val authService = getKoin().get<AuthService>()
        val response = authService.obtainAccessToken(username = "<username/email>", password = "<password>")
        assertNotNull(response)
        println("Access token created at '${response.createdAt}'")
    }

}