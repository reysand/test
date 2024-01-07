/*
 * Copyright 2023 Andrey Slyusar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reysand.files.ui.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException
import com.reysand.files.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class OneDriveService(val context: Context) {

    var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    var mAccount: IAccount? = null
    private val scopes: List<String> = listOf("User.Read")

    @OptIn(DelicateCoroutinesApi::class)
    private val msalPublicClient: Deferred<ISingleAccountPublicClientApplication> by lazy {
        GlobalScope.async(Dispatchers.IO) {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.auth_config_single_account
            )
        }
    }

    suspend fun signIn(callback: (String?) -> Unit) {
        val signInParameters = SignInParameters.builder()
            .withActivity(context as Activity)
            .withScopes(scopes)
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                    mAccount = authenticationResult?.account
                    Log.d("TAG", "signIn: ${mAccount?.username}")
                    callback(mAccount?.username)
                }

                override fun onError(exception: MsalException?) {
                    exception?.printStackTrace()
                    Log.e("TAG", "onError: ", exception)
                }

                override fun onCancel() {
                    Log.d("TAG", "onCancel: ")
                }
            })
            .build()

        val client = msalPublicClient.await()
        client.signIn(signInParameters)
    }

    suspend fun isSignedIn(): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = msalPublicClient.await()
            val account = client.currentAccount
            account.currentAccount != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val client = msalPublicClient.await()
        client.signOut()
    }
}