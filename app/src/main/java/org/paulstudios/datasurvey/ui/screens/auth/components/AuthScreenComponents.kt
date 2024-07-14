package org.paulstudios.datasurvey.ui.screens.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.paulstudios.datasurvey.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    email: MutableState<String>,
    password: MutableState<String>,
    buttonText: String,
    onSubmit: () -> Unit,
    secondaryButtonText: String,
    onSecondaryButtonClick: () -> Unit,
    passwordStrengthComposable: @Composable (() -> Unit)? = null,
    errorMessage: String = "",
    isLoading: Boolean = false,
    onGithubLogin: () -> Unit,
    onGoogleLogin: () -> Unit
) {
    var emailError by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(100.dp)
                )

                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email.value,
                    onValueChange = {
                        email.value = it
                        emailError = if (isValidEmail(it)) "" else "Invalid Email"
                    },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = emailError.isNotEmpty()
                )
                if (emailError.isNotEmpty()) {
                    Text(emailError, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password.value,
                    onValueChange = { password.value = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                passwordStrengthComposable?.invoke()

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isValidEmail(email.value)) {
                            onSubmit()
                        } else {
                            emailError = "Invalid Email"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Text(buttonText)
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onSecondaryButtonClick) {
                    Text(secondaryButtonText)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Login Button
                Button(onClick = onGoogleLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in with Google")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // GitHub Login Button
                Button(onClick = onGithubLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in with GitHub")
                }
            }
        }
    }
}
