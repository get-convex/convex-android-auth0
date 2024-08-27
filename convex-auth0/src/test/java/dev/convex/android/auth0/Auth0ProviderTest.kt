package dev.convex.android.auth0

import kotlinx.coroutines.test.runTest
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

class Auth0ProviderTest {
    @Test
    fun `login success`() = runTest {
        //TODO: Add actual tests (https://github.com/auth0-samples/auth0-android-sample/issues/170).
        expectThat(true).isTrue()
    }
}