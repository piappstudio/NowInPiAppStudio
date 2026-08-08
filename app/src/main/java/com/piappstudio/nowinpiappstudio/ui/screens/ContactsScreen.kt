package com.piappstudio.nowinpiappstudio.ui.screens

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = Manifest.permission.READ_CONTACTS)
    val contactsState = remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(permissionState.hasPermission) {
        if (permissionState.hasPermission) {
            // load contacts on IO dispatcher
            val list = withContext(Dispatchers.IO) { loadContacts(context) }
            contactsState.value = list
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {
        // Simple header to avoid experimental TopAppBar API
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Back", modifier = Modifier
                .clickable { onBack() }
                .padding(12.dp))
            Text(text = "Contact API", modifier = Modifier.padding(start = 8.dp))
        }

        if (!permissionState.hasPermission) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "This explorer needs access to your contacts.")
                Button(onClick = { permissionState.launchPermissionRequest() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(text = "Grant Contacts Permission")
                }
            }
        } else {
            val contacts = contactsState.value
            if (contacts.isEmpty()) {
                Text(text = "No contacts found or still loading...", modifier = Modifier.padding(12.dp))
            } else {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(contacts) { (name, number) ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                            Text(text = number, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun loadContacts(context: Context): List<Pair<String, String>> {
    val resolver = context.contentResolver
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
    val cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")
    val list = mutableListOf<Pair<String, String>>()
    cursor?.use { c ->
        val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (c.moveToNext()) {
            val name = c.getString(nameIdx) ?: ""
            val number = c.getString(numIdx) ?: ""
            list.add(name to number)
        }
    }
    return list
}

