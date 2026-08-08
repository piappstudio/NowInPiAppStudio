package com.piappstudio.nowinpiappstudio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piappstudio.nowinpiappstudio.ui.navigation.Screen

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "NewInPiAppStudio — API Explorations", style = MaterialTheme.typography.headlineSmall)

            val items = listOf("Contact API")
            LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                items(items) { item ->
                    Card(modifier = Modifier
                        .padding(vertical = 6.dp)
                        .clickable { if (item == "Contact API") onNavigate(Screen.Contacts) }
                        .padding(12.dp)) {
                        Text(text = item, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

