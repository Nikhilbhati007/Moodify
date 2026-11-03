package com.bhati.moodify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.bhati.moodify.data.local.TokenManager
import com.bhati.moodify.data.local.room.AppDatabase
import com.bhati.moodify.ui.auth.AuthState
import com.bhati.moodify.ui.auth.AuthViewModel
import com.bhati.moodify.ui.music.MusicViewModel
import com.bhati.moodify.ui.auth.LoginScreen
import com.bhati.moodify.ui.auth.SignupScreen
import com.bhati.moodify.ui.dashboard.DashboardScreen
import com.bhati.moodify.ui.theme.MoodifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(this)
        com.bhati.moodify.data.api.RetrofitClient.tokenProvider = { tokenManager.getToken() }
        val database = AppDatabase.getDatabase(this)

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return AuthViewModel(tokenManager) as T
                }
                if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MusicViewModel(tokenManager, database.trackDao(), database.moodDao(), database.historyDao(), database.playlistDao()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel(factory = factory)
            val isDarkTheme by authViewModel.isDarkTheme.collectAsState()

            MoodifyTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoodifyApp(factory, authViewModel)
                }
            }
        }
    }
}

@Composable
fun MoodifyApp(factory: ViewModelProvider.Factory, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    
    val authState by authViewModel.authState.collectAsState()
    val globalMusicViewModel: com.bhati.moodify.ui.music.MusicViewModel = viewModel(factory = factory)

    // Determine start destination
    val startDestination = if (authState is AuthState.Success) "dashboard" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            // Auto navigate if successful
            LaunchedEffect(authState) {
                if (authState is AuthState.Success) {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            LoginScreen(
                authState = authState,
                onLoginClick = { user, pass -> authViewModel.login(user, pass) },
                onNavigateToSignup = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            LaunchedEffect(authState) {
                if (authState is AuthState.Success) {
                    navController.navigate("dashboard") {
                        popUpTo("signup") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            SignupScreen(
                authState = authState,
                onSignupClick = { user, pass -> authViewModel.register(user, pass) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                musicViewModel = globalMusicViewModel,
                username = authViewModel.username.collectAsState().value,
                onProfileClick = { navController.navigate("profile") },
                onCameraClick = { navController.navigate("camera") },
                onSearchClick = { navController.navigate("search") },
                onLibraryClick = { navController.navigate("library") },
                onHistoryClick = { navController.navigate("history") },
                onPlaylistsClick = { navController.navigate("playlists") },
                onCategoryClick = { vibe -> navController.navigate("recommendations/$vibe/English") },
                onToggleTheme = { authViewModel.toggleTheme() },
                isDarkTheme = authViewModel.isDarkTheme.collectAsState().value
            )
        }
        composable("search") {
            com.bhati.moodify.ui.music.SearchScreen(
                viewModel = globalMusicViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("library") {
            com.bhati.moodify.ui.music.LibraryScreen(
                viewModel = globalMusicViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("history") {
            com.bhati.moodify.ui.music.HistoryScreen(
                viewModel = globalMusicViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("playlists") {
            com.bhati.moodify.ui.music.PlaylistsScreen(
                viewModel = globalMusicViewModel,
                onPlaylistClick = { id, name -> navController.navigate("playlistDetail/$id/$name") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "playlistDetail/{id}/{name}",
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val name = backStackEntry.arguments?.getString("name") ?: "Playlist"
            com.bhati.moodify.ui.music.PlaylistDetailScreen(
                playlistId = id,
                playlistName = name,
                viewModel = globalMusicViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("profile") {
            com.bhati.moodify.ui.dashboard.ProfileScreen(
                username = authViewModel.username.collectAsState().value,
                musicViewModel = globalMusicViewModel,
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("camera") {
            com.bhati.moodify.ui.camera.CameraScreen(
                onMoodCaptured = { mood, lang ->
                    globalMusicViewModel.logMood(mood)
                    navController.navigate("recommendations/$mood/$lang") {
                        popUpTo("dashboard")
                    }
                }
            )
        }
        composable(
            route = "recommendations/{mood}/{lang}",
            arguments = listOf(
                navArgument("mood") { type = NavType.StringType },
                navArgument("lang") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mood = backStackEntry.arguments?.getString("mood") ?: "Neutral"
            val lang = backStackEntry.arguments?.getString("lang") ?: "English"
            com.bhati.moodify.ui.music.RecommendationScreen(
                mood = mood,
                lang = lang,
                viewModel = globalMusicViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}