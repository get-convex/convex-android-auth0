# Convex for Android - Auth0 Integration

This library works with the core
[Convex for Android](https://github.com/get-convex/convex-mobile/tree/main/android)
library and provides support for using Auth0 in `ConvexClientWithAuth`.

The integration uses Auth0's [Universal Login](https://auth0.com/docs/hosted-pages/login). Users are
prompted to authenticate via a browser window and then seamlessly returned to your app UI.

## Getting Started

First of all, if you haven't started a Convex application yet, head over to the
[Convex Android quickstart](https://docs.convex.dev/quickstart/android) to get the basics down. It
will get you up and running with a Convex dev deployment and a basic Android application that
communicates with it.

Once you have a working Convex + Android application, you're ready to take the following steps to
integrate with Auth0.

> [!NOTE]
> There are a lot of moving parts to getting auth set up. If you run into trouble check out the
> [Convex auth docs](https://docs.convex.dev/auth) and join our 
> [Discord community](https://convex.dev/community) to get help.

1. Follow the first two steps of the official
   [Auth0 Android quickstart](https://auth0.com/docs/quickstart/native/android) ("Configure Auth0"
   and "Install the Auth0 Android SDK").

2. Update your Convex application to support auth. Create a `convex/auth.config.ts`
   file with the following content:
    ```
    export default {
      providers: [
        {
          domain: "your-domain.us.auth0.com",
          applicationID: "yourclientid",
        },
      ]
    };
    ```
3. Run `npx convex dev` to sync the config change.

4. Add a dependency on this library to your Android project.

    ```kotlin
        // In the dependencies section of your app-level build.gradle.kts file ...
        implementation("dev.convex:android-convex-auth0:0.2.0")
    ```

5. Be sure to sync Gradle after adding that dependency. Now you should be able to import and use
   `ConvexClientWithAuth` and the `Auth0Provider` in your app.

6. You'll need to supply various bits of Auth0 config that you created in the
   [Auth0 Android quickstart](https://auth0.com/docs/quickstart/native/android) to create an
   `Auth0Provider`.

    ```kotlin
    val auth0 = Auth0Provider(
        context,
        clientId = "your auth0 client ID",
        domain = "your auth0 domain",
        scheme = "the scheme that you use in your callback and logout URLs",
    )
    ```

7. Then, wherever you have setup your Convex client with `ConvexClient`, switch to using
   `ConvexClientWithAuth` and pass the `Auth0Provider` you created.

    ```kotlin
    val convex = ConvexClientWithAuth(
        deploymentUrl = "your Convex deployment URL",
        authProvider = auth0
    )
    ```

8. Ensure that you update other references where `ConvexClient` is defined as a parameter or field
   to `ConvexClientWithAuth`.

At this point you should be able to use the `login` and `logout` methods on the client to perform
authentication with Auth0. Your Convex backend will receive the ID token from Auth0 and you'll be
able to
[use authentication details in your backend functions](https://docs.convex.dev/auth/functions-auth).

### Reacting to authentication state

The `ConvexClientWithAuth.authState` field is a `Flow` that contains the latest authentication state
from the client. You can setup your UI to react to new `authState` values and show the appropriate
screens (e.g. login/logout buttons, loading screens, authenticated content).

The `AuthState.Authenticated` value will contain the 
[`Credentials`](https://javadoc.io/doc/com.auth0.android/auth0/latest/auth0/com.auth0.android.result/-credentials/index.html)
object received from Auth0 and you can use the data that it contains to customize the user
experience in your app.

### Auto sign-in

If you would like your users to be able to launch your app directly into a signed in state after an
initial authentication, you can enable that via a flag on the `Auth0Provider` constructor.

```kotlin
val auth0 = Auth0Provider(
    // Previous values for context, clientId, domain and scheme go here ...
    enableCachedLogins = true,
)
```

This uses Auth0's
[`SecureCredentialsManager`](https://javadoc.io/doc/com.auth0.android/auth0/latest/auth0/com.auth0.android.authentication.storage/-secure-credentials-manager/index.html)
behind the scenes to store tokens after successful authentication and to retrieve previously stored
tokens.

With `enableCachedLogins` you can call the `ConvexClientWithAuth.loginFromCache` method and it will
automatically sign the user back in if prior valid credentials are available. It will update the
`authState` flow just like calls to `login` and `logout` do for interactive operations.