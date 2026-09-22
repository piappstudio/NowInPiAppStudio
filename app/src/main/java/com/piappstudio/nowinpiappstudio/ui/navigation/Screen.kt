package com.piappstudio.nowinpiappstudio.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    val displayName: String

    @Serializable
    data object Home : Screen {
        override val displayName: String = "Home"
    }
    
    @Serializable
    data object Contacts : Screen {
        override val displayName: String = "Contact API"
    }
    
    @Serializable
    data object Contacts_Picker : Screen {
        override val displayName: String = "Contact Picker"
    }
}
