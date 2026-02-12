package com.bzcards.scan

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bzcards.scan.model.BusinessCard
import com.bzcards.scan.ui.screens.CameraScreen
import com.bzcards.scan.ui.screens.ResultScreen
import com.bzcards.scan.ui.screens.SavedCardsScreen
import com.bzcards.scan.ui.theme.BZCardScanTheme
import com.bzcards.scan.util.BusinessCardParser
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BZCardScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BZCardScanNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BZCardScanNavigation() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as BZCardScanApp
    val dao = remember { app.database.businessCardDao() }
    var scannedCard by remember { mutableStateOf<BusinessCard?>(null) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    NavHost(navController = navController, startDestination = "camera") {
        composable("camera") {
            if (cameraPermissionState.status.isGranted) {
                CameraScreen(
                    onTextRecognized = { rawText ->
                        scannedCard = BusinessCardParser.parse(rawText)
                        navController.navigate("result")
                    },
                    onNavigateToSavedCards = {
                        navController.navigate("saved")
                    }
                )
            } else {
                PermissionRequest(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
        }

        composable("result") {
            scannedCard?.let { card ->
                val context = androidx.compose.ui.platform.LocalContext.current
                ResultScreen(
                    initialCard = card,
                    onSave = { editedCard ->
                        scope.launch {
                            dao.insert(editedCard)
                            Toast.makeText(context, "Card saved!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack("camera", false)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("saved") {
            SavedCardsScreen(
                cardsFlow = dao.getAllCards(),
                onDelete = { card ->
                    scope.launch { dao.delete(card) }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "BZ Card Scan needs camera access to photograph and scan business cards.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Camera Permission")
        }
    }
}
