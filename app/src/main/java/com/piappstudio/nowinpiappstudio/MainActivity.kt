package com.piappstudio.nowinpiappstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.piappstudio.nowinpiappstudio.ui.navigation.Screen
import com.piappstudio.nowinpiappstudio.ui.screens.ContactPickerScreen
import com.piappstudio.nowinpiappstudio.ui.screens.ContactsScreen
import com.piappstudio.nowinpiappstudio.ui.screens.HomeScreen
import com.piappstudio.nowinpiappstudio.ui.theme.NowInPiAppStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NowInPiAppStudioTheme {
                val backStack = rememberNavBackStack(Screen.Home)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavDisplay(
                            backStack = backStack,
                            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
                        ) { key ->
                            when (key) {
                                is Screen.Home -> NavEntry(key) {
                                    HomeScreen(onNavigate = { screen ->
                                        backStack.add(screen)
                                    })
                                }

                                is Screen.Contacts -> NavEntry(key) {
                                    ContactsScreen(onBack = {
                                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                                    })
                                }

                                is Screen.Contacts_Picker -> NavEntry(key) {
                                    ContactPickerScreen(onBack = {
                                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                                    })
                                }
                                
                                else -> NavEntry(key) {
                                    Box(Modifier.fillMaxSize()) {
                                        Text("Unknown destination")
                                    }
                                }
                            }
                        }
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
