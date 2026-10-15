package it.stradesumisura.app.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/** true quando il pannello di navigazione laterale è visibile (finestre larghe: Windows, tablet). */
val LocalNavigationPaneVisible = staticCompositionLocalOf { false }
