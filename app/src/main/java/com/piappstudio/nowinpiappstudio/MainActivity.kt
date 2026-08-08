package com.piappstudio.nowinpiappstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.ui.Modifier
import com.piappstudio.nowinpiappstudio.ui.navigation.Screen
import com.piappstudio.nowinpiappstudio.ui.screens.ContactsScreen
import com.piappstudio.nowinpiappstudio.ui.screens.HomeScreen
import com.piappstudio.nowinpiappstudio.ui.theme.NowInPiAppStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NowInPiAppStudioTheme {
                val current = rememberSaveable { mutableStateOf<Screen>(Screen.Home) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // simple in-app navigation driven by a state value
                    when (current.value) {
                        Screen.Home -> HomeScreen(onNavigate = { screen -> current.value = screen })
                        Screen.Contacts -> ContactsScreen(onBack = { current.value = Screen.Home })
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewEntryPoint() {
    NowInPiAppStudioTheme {
        HomeScreen(onNavigate = {})
    }
}