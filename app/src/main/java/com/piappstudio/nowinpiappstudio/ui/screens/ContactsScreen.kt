package com.piappstudio.nowinpiappstudio.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = Manifest.permission.READ_CONTACTS)
    val contactsState = remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    
    LaunchedEffect(permissionState.status) {
        if (permissionState.status == PermissionStatus.Granted) {
            // load contacts on IO dispatcher
            val list = withContext(Dispatchers.IO) { loadContacts(context) }
            contactsState.value = list
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Contact API") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (permissionState.status != PermissionStatus.Granted) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "This explorer needs access to your contacts to demonstrate the Contact API.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { permissionState.launchPermissionRequest() }) {
                        Text(text = "Grant Contacts Permission")
                    }
                }
            } else {
                val contacts = contactsState.value
                if (contacts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No contacts found or still loading...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(contacts) { (name, number) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text(number) },
                                leadingContent = {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun loadContacts(context: Context): List<Pair<String, String>> {
    val resolver = context.contentResolver
    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.Data.RAW_CONTACT_ID)
    val cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")
    val list = mutableListOf<Pair<String, String>>()
    cursor?.use { c ->
        val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val rawContactIdIdx = c.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID)
        while (c.moveToNext()) {
            val name = c.getString(nameIdx) ?: ""
            val number = c.getString(numIdx) ?: ""
            val rawContactId = c.getLong(rawContactIdIdx)
           // getPIIData(rawContactId, resolver) // Fetch PII data for each contact
            list.add(name to number)
        }
    }
    return list
}


fun getPIIData(rawContactId: Long, contentResolver: ContentResolver) {

        // Query RawContacts explicitly to fetch the PII account details
        val rawContactProjection = arrayOf(
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.RawContacts.DATA_SET
        )

        val rawContactCursor = contentResolver.query(
            ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
            rawContactProjection,
            null,
            null,
            null
        )

        rawContactCursor?.use { rawCursor ->
            if (rawCursor.moveToFirst()) {
                val accountName = rawCursor.getString(
                    rawCursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
                )
                val accountType = rawCursor.getString(
                    rawCursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
                )
                Log.d("PII", "Account Name: $accountName, Account Type: $accountType")
                // Process account details...
            }
        }

}

