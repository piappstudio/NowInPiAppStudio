package com.piappstudio.nowinpiappstudio.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsPickerSessionContract.ACTION_PICK_CONTACTS
import android.provider.ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class representing a parsed Contact with selected details.
data class Contact(
    val lookupKey: String,
    val name: String,
    val emails: List<String>,
    val phones: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedName by remember { mutableStateOf<String?>(null) }
    var selectedPhone by remember { mutableStateOf<String?>(null) }
    var selectedContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }

    // 1. Launcher to pick a SPECIFIC Phone Number directly from system UI
    val phonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = result.data?.data
        uri?.let { phoneUri ->
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(phoneUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    selectedName = cursor.getString(nameIdx)
                    selectedPhone = cursor.getString(numberIdx)
                }
            }
        }
    }

    // 2. Launcher to pick a whole Contact entry
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    selectedName = cursor.getString(nameIdx)
                    selectedPhone = "N/A (Full Contact Picked)"
                }
            }
        }
    }


    // [START android_contact_picker_result_uri_processing]

    // Helper function to query the content resolver with the URI returned by the Contact Picker.
// Parses the cursor to extract contact details such as name, email, and phone number.
    suspend fun processContactPickerResultUri(
        sessionUri: Uri,
        context: Context
    ): List<Contact> = withContext(Dispatchers.IO) {
        // Define the columns we want to retrieve from the ContactPicker ContentProvider
        val projection = arrayOf(
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.MIMETYPE, // Type of data (e.g., email or phone)
            ContactsContract.Data.DATA1, // The actual data (Phone number / Email string)
        )

        // We use `LOOKUP_KEY` as a unique ID to aggregate all contact info related to a same person
        val contactsMap = mutableMapOf<String, Contact>()

        // Note: The Contact Picker Session Uri doesn't support custom selection & selectionArgs.
        // We query the URI directly to get the results chosen by the user.
        context.contentResolver.query(sessionUri, projection, null, null, null)?.use { cursor ->
            // Get the column indices for our requested projection
            val lookupKeyIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val mimeTypeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)

            while (cursor.moveToNext()) {
                val lookupKey = cursor.getString(lookupKeyIdx)
                val mimeType = cursor.getString(mimeTypeIdx)
                val name = cursor.getString(nameIdx) ?: ""
                val data1 = cursor.getString(data1Idx) ?: ""

                val email =
                    if (mimeType == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) data1 else null
                val phone =
                    if (mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) data1 else null

                val existingContact = contactsMap[lookupKey]
                if (existingContact != null) {
                    contactsMap[lookupKey] = existingContact.copy(
                        emails = if (email != null) existingContact.emails + email else existingContact.emails,
                        phones = if (phone != null) existingContact.phones + phone else existingContact.phones
                    )
                } else {
                    contactsMap[lookupKey] = Contact(
                        lookupKey = lookupKey,
                        name = name,
                        emails = if (email != null) listOf(email) else emptyList(),
                        phones = if (phone != null) listOf(phone) else emptyList()
                    )
                }
            }
        }

        return@withContext contactsMap.values.toList()
    }

    // Launcher for the Contact Picker intent
    val coroutine = rememberCoroutineScope()
    val pickContact =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            if (it.resultCode == Activity.RESULT_OK) {
                val resultUri = it.data?.data ?: return@rememberLauncherForActivityResult

                // Process the result URI in a background thread to fetch all selected contacts
                coroutine.launch {
                    val contacts = processContactPickerResultUri(resultUri, context)
                    coroutine.launch(Dispatchers.Main) {
                        selectedContacts = contacts
                        selectedName = contacts.joinToString { it.name }
                        selectedPhone = contacts.flatMap { it.phones }.joinToString()
                    }
                }
            }
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Contact Picker") },
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
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Use system UI to pick a contact. This method does not require READ_CONTACTS permission.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Option A: Opens direct phone picker
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(Intent.ACTION_PICK).apply {
                        type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                    }
                    phonePickerLauncher.launch(intent)
                }
            ) {
                Text("Pick Phone Number")
            }

            // Option D: Opens multiple contacts picker (Android 17+)
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                        val requestedFields = arrayListOf(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                        )
                        val pickMultipleIntent = Intent(ACTION_PICK_CONTACTS).apply {
                            putExtra(EXTRA_USE_SYSTEM_CONTACTS_PICKER, true)
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            putStringArrayListExtra(
                                EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                                requestedFields
                            )
                        }
                        pickContact.launch(pickMultipleIntent)
                    } else {
                        // Fallback: Use standard single contact picker
                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        }
                        phonePickerLauncher.launch(intent)
                    }
                }
            ) {
                Text("Pick Multiple Contact")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option B: Opens full contact picker
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {

                    contactPickerLauncher.launch(null)
                }
            ) {
                Text("Pick Full Contact")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            // Option C: Opens full contact picker
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // [START android_contact_picker_single_contact_selection_intent]
                    // Define the specific contact data fields you need
                    val requestedFields = arrayListOf(
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    )

                    // Set up the intent for the Contact Picker
                    val pickContactIntent = Intent(ACTION_PICK_CONTACTS).apply {
                        putExtra(EXTRA_USE_SYSTEM_CONTACTS_PICKER, true)
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        putStringArrayListExtra(
                            EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                            requestedFields
                        )
                    }
                    pickContact.launch(pickContactIntent)
                }
            ) {
                Text("Pick Contact with Android 17")
            }
                }

            Spacer(modifier = Modifier.height(32.dp))

            if (selectedContacts.isNotEmpty()) {
                Text(
                    text = "Picked Contacts List",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                selectedContacts.forEach { contact ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Name: ${contact.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (contact.phones.isNotEmpty()) {
                                Text(
                                    text = "Phone: ${contact.phones.joinToString()}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (contact.emails.isNotEmpty()) {
                                Text(
                                    text = "Email: ${contact.emails.joinToString()}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            } else if (selectedName != null || selectedPhone != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Selected Contact",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                        )
                        selectedName?.let {
                            Text(
                                text = "Name: $it",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        selectedPhone?.let {
                            Text(
                                text = "Phone: $it",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
