// Hecho con IA (Claude), siguiendo la GUIAAPPKOTLIN del profe
package cl.inacap.monitorestructural.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

// Pantalla de Login: correo (usuario) y contraseña con Firebase Authentication
@Composable
fun LoginScreen(
    state: LoginUiState,
    onIngresar: (correo: String, clave: String) -> Unit,
    onCrearCuenta: (correo: String, clave: String) -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    // La contraseña solo vive en memoria mientras se escribe (no se guarda)
    var clave by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Monitor Estructural", style = MaterialTheme.typography.headlineMedium)
            Text("Ingresa con tu cuenta para ver y enviar datos")
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo (usuario)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),   // se ven puntos
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            if (state.cargando) {
                CircularProgressIndicator()
            }
            Button(
                onClick = { onIngresar(correo, clave) },
                enabled = !state.cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }
            OutlinedButton(
                onClick = { onCrearCuenta(correo, clave) },
                enabled = !state.cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear cuenta")
            }
        }
    }
}
