package com.example.healthx.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun AuthNavGraph(onAuthSuccess: (String) -> Unit, onBackToAccounts: () -> Unit) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    val error by authViewModel.authError.collectAsState()
    if (error != null) {
        AlertDialog(
            onDismissRequest = { authViewModel.clearError() },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Authentication Failed", color = Color.White) },
            text = { Text(error!!, color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = { authViewModel.clearError() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignup = { navController.navigate("signup") },
                onNavigateToForgotPwd = { navController.navigate("forgot_pwd") },
                onLoginSuccess = onAuthSuccess,
                onBackToAccounts = onBackToAccounts
            )
        }
        composable("signup") {
            SignupScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onOtpSent = { navController.navigate("verify_otp") }
            )
        }
        composable("verify_otp") {
            VerifyOtpScreen(viewModel = authViewModel, onVerifySuccess = onAuthSuccess)
        }
        composable("forgot_pwd") {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOtpSent = { navController.navigate("reset_pwd") }
            )
        }
        composable("reset_pwd") {
            ResetPasswordScreen(
                viewModel = authViewModel,
                onResetSuccess = { navController.navigate("login") { popUpTo("login") { inclusive = true } } }
            )
        }
    }
}

// ==========================================
// LOGIN SCREEN
// ==========================================
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPwd: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    onBackToAccounts: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    // Check if device has any saved accounts
    val savedAccounts by viewModel.sessionManager.savedAccountsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to HealthX", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

        Spacer(modifier = Modifier.height(8.dp))
        Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.End).clickable { onNavigateToForgotPwd() })
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(email, password, onLoginSuccess) },
            modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { viewModel.loginAsGuest(onLoginSuccess) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Continue as Guest")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Text("Don't have an account? ", color = Color.Gray)
            Text("Sign Up", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigateToSignup() })
        }

        // Show back button ONLY if they have accounts to go back to
        if (savedAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onBackToAccounts) {
                Text("Back to Saved Accounts", color = Color.Gray)
            }
        }
    }
}

// ==========================================
// SIGNUP SCREEN
// ==========================================
@Composable
fun SignupScreen(viewModel: AuthViewModel, onNavigateToLogin: () -> Unit, onOtpSent: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profileImageUri = uri
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create an Account", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        // Image Picker UI
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
                .clickable { galleryLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = Color.Gray, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Pass the local URI string to the ViewModel
                viewModel.signup(name, email, password, profilePhotoUri = profileImageUri, onOtpSent = onOtpSent)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Text("Already have an account? ", color = Color.Gray)
            Text("Login", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigateToLogin() })
        }
    }
}

// ... Keep VerifyOtpScreen, ForgotPasswordScreen, and ResetPasswordScreen exactly as they were ...
@Composable
fun VerifyOtpScreen(viewModel: AuthViewModel, onVerifySuccess: (String) -> Unit) {
    var otp by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Black) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text("Verify Your Email", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("We sent a 6-digit code to:\n${viewModel.pendingVerificationEmail}", color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("Enter OTP") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.verifyOtp(otp, onVerifySuccess) },
                modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && otp.length >= 4
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Verify")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Didn't receive the code? Resend OTP", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
                viewModel.resendSignupOtp { coroutineScope.launch { snackbarHostState.showSnackbar("OTP Resent Successfully!") } }
            })
        }
    }
}

@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel, onNavigateBack: () -> Unit, onOtpSent: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text("Reset Password", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enter your email address and we'll send you an OTP to reset your password.", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.forgotPassword(email, onOtpSent) },
            modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && email.isNotBlank()
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Send OTP")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Back to Login", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNavigateBack() })
    }
}

@Composable
fun ResetPasswordScreen(viewModel: AuthViewModel, onResetSuccess: () -> Unit) {
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Black) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text("Create New Password", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enter the OTP sent to ${viewModel.resetEmail} and choose a new password.", color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("Enter OTP") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("New Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.resetPassword(otp, newPassword, onResetSuccess) },
                modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !isLoading && otp.length >= 4 && newPassword.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Reset Password")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Didn't receive the code? Resend OTP", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
                viewModel.forgotPassword(viewModel.resetEmail) { coroutineScope.launch { snackbarHostState.showSnackbar("OTP Resent Successfully!") } }
            })
        }
    }
}