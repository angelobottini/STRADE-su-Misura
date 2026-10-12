package it.stradesumisura.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import it.stradesumisura.app.ui.navigation.StradeNavigation
import it.stradesumisura.app.ui.theme.StradeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StradeTheme {
                StradeNavigation()
            }
        }
    }
}
