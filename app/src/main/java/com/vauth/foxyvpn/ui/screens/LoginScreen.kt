package com.vauth.foxyvpn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.vauth.foxyvpn.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vauth.foxyvpn.data.FxaAuthRepository
import com.vauth.foxyvpn.data.model.BatchImportItemResult
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: FxaAuthRepository,
    onSignedIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var awaitingTwoFactor by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun doSubmitLogin() {
        val cleanEmail = FxaAuthRepository.normalizeEmail(email)
        if (cleanEmail.isBlank() || password.isBlank() || isLoading) return
        errorMessage = null
        isLoading = true
        scope.launch {
            val result = authRepository.startLogin(cleanEmail, password)
            isLoading = false
            result.onSuccess { needsVerification ->
                if (needsVerification) awaitingTwoFactor = true else onSignedIn()
            }.onFailure { errorMessage = it.message ?: "Sign-in failed" }
        }
    }

    fun doSubmitTwoFactor() {
        val cleanCode = FxaAuthRepository.sanitizeCode(code)
        if (cleanCode.isBlank() || isLoading) return
        errorMessage = null
        isLoading = true
        scope.launch {
            val result = authRepository.submitTwoFactorCode(cleanCode)
            isLoading = false
            result.onSuccess { onSignedIn() }
                .onFailure { errorMessage = it.message ?: "Verification failed" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Sign in with Firefox", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            "FoxyVPN runs on the free 50 GB of monthly VPN traffic Mozilla includes with a Firefox account. No subscription is required.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 24.dp))

        if (!awaitingTwoFactor) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "").lowercase()
                },
                label = { Text("Firefox account email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        doSubmitLogin()
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 20.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    doSubmitLogin()
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp)) else Text("Continue")
            }
        } else {
            Text("Enter the confirmation code sent to your email", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "")
                },
                label = { Text("Confirmation code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        doSubmitTwoFactor()
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 20.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    doSubmitTwoFactor()
                },
                enabled = !isLoading && code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp)) else Text("Verify")
            }
        }

        errorMessage?.let {
            Spacer(Modifier.padding(top = 12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )
        }

        Spacer(Modifier.padding(top = 20.dp))
        BatchImportSection(
            authRepository = authRepository,
            onImported = onSignedIn,
        )
    }
}

@Composable
private fun BatchImportSection(
    authRepository: FxaAuthRepository,
    onImported: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BatchImportItemResult>>(emptyList()) }

    if (!expanded) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Batch import accounts")
        }
        return
    }

    Text(
        "Batch import",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.padding(top = 8.dp))
    Text(
        "Paste one entry per line (email:password) or a JSON array of " +
            "{email, password} / {email, accessToken, refreshToken} objects.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.padding(top = 8.dp))
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        placeholder = { Text("name@example.com:password") },
    )
    if (progress.isNotBlank()) {
        Spacer(Modifier.padding(top = 8.dp))
        Text(progress, style = MaterialTheme.typography.labelMedium)
    }
    if (results.isNotEmpty()) {
        Spacer(Modifier.padding(top = 8.dp))
        val okCount = results.count { it.success }
        Text(
            "Imported $okCount of ${results.size}",
            style = MaterialTheme.typography.titleSmall,
        )
        results.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.identifier,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (r.success) "✓" else "✗ ${r.error.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (r.success) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    Spacer(Modifier.padding(top = 12.dp))
    Button(
        onClick = {
            importing = true
            results = emptyList()
            scope.launch {
                val out = authRepository.batchImportAccounts(input) { current, total, label ->
                    progress = "$current/$total · $label"
                }
                results = out
                progress = ""
                importing = false
                if (out.any { it.success }) onImported()
            }
        },
        enabled = !importing && input.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (importing) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
        else Text("Import")
    }
}
