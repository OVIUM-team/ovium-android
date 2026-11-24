package com.ovium.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ovium.app.data.remote.ApiClient
import com.ovium.app.data.remote.LoginRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text("Iniciar sesión en Ovium", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !cargando,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            enabled = !cargando && usuario.isNotBlank() && contrasena.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                cargando = true
                error = null
                scope.launch {
                    try {
                        val respuesta = ApiClient.service.login(
                            LoginRequest(usuario.trim(), contrasena)
                        )
                        val token = respuesta.body()?.accessToken
                        when {
                            respuesta.isSuccessful && !token.isNullOrBlank() -> {
                                ApiClient.guardarToken(token)
                                onLoginSuccess()
                            }
                            respuesta.code() == 401 ->
                                error = "Usuario o contraseña incorrectos."
                            else ->
                                error = "No se pudo iniciar sesión. Intenta de nuevo."
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: IOException) {
                        error = "No se pudo conectar con la API."
                    } catch (_: Exception) {
                        error = "No se pudo procesar la respuesta de la API."
                    } finally {
                        cargando = false
                    }
                }
            }
        ) {
            Text(if (cargando) "Iniciando sesión…" else "Iniciar sesión")
        }
    }
}
