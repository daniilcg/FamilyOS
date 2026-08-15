package com.familyos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.app.navigation.FamilyOsNavHost
import com.familyos.app.ui.MainViewModel
import com.familyos.core.domain.model.ThemeMode
import com.familyos.core.ui.theme.FamilyOsTheme
import com.familyos.feature.auth.google.GoogleSignInHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity Compose host with edge-to-edge system bars.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var googleSignInHelper: GoogleSignInHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val signInHelper = remember { googleSignInHelper }
            FamilyOsTheme(darkTheme = darkTheme) {
                FamilyOsNavHost(
                    googleSignInHelper = signInHelper,
                    mainViewModel = mainViewModel,
                )
            }
        }
    }
}
