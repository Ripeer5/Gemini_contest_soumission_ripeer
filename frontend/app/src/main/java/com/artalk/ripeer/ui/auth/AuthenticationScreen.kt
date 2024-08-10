package com.artalk.ripeer.ui.auth

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.artalk.ripeer.ui.conversations.ui.theme.ArtalkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

@Composable
fun AuthenticationApp() {
    val navController = rememberNavController()
    ArtalkTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(navController, startDestination = "signIn") {
                composable("signIn") { SignInScreen(navController) }
                composable("signUp") { SignUpScreen(navController) }
            }
        }
    }
}

@Composable
fun SignInScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val authState = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign In",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        TextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                Log.d("SignInScreen", "Attempting to sign in with email: ${email.value}")
                auth.signInWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("SignInScreen", "Sign in successful")
                            // Handle successful sign-in
                        } else {
                            Log.e("SignInScreen", "Authentication failed", task.exception)
                            authState.value = "Authentication failed: ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (authState.value.isNotEmpty()) {
            ErrorMessage(message = authState.value)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("signUp") }) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun SignUpScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val userName = remember { mutableStateOf("") }
    val authState = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        TextField(
            value = userName.value,
            onValueChange = { userName.value = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                Log.d("SignUpScreen", "Attempting to create account with email: ${email.value}")
                auth.createUserWithEmailAndPassword(email.value, password.value)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(userName.value)
                                    .build()
                                user.updateProfile(profileUpdates)
                                    .addOnCompleteListener { profileTask ->
                                        if (profileTask.isSuccessful) {
                                            Log.d("SignUpScreen", "User profile updated.")
                                            // Handle successful sign-up
                                        } else {
                                            Log.e("SignUpScreen", "Profile update failed", profileTask.exception)
                                            authState.value = "Profile update failed: ${profileTask.exception?.message}"
                                        }
                                    }
                            }
                        } else {
                            Log.e("SignUpScreen", "Account creation failed", task.exception)
                            authState.value = "Account creation failed: ${task.exception?.message}"
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (authState.value.isNotEmpty()) {
            ErrorMessage(message = authState.value)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("signIn") }) {
            Text("Already have an account? Sign In")
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Red,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
