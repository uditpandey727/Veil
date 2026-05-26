package com.veil

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.veil.data.repository.VeilRepository
import com.veil.di.ChatViewModelFactory
import com.veil.di.VeilViewModelFactory
import com.veil.ui.appearance.AppearancePreferences
import com.veil.ui.appearance.AppearanceScreen
import com.veil.ui.appearance.AppearanceViewModel
import com.veil.ui.chat.ChatScreen
import com.veil.ui.chat.ChatViewModel
import com.veil.ui.home.HomeScreen
import com.veil.ui.home.HomeViewModel
import com.veil.ui.onboarding.OnboardingScreen
import com.veil.ui.onboarding.OnboardingViewModel
import com.veil.ui.qr.QrScreen
import com.veil.ui.qr.QrViewModel
import com.veil.ui.requests.MessageRequestsScreen
import com.veil.ui.requests.MessageRequestsViewModel
import com.veil.ui.settings.SettingsScreen
import com.veil.ui.settings.SettingsViewModel
import com.veil.ui.theme.VeilColors
import com.veil.ui.theme.VeilTheme
import com.veil.util.BiometricHelper

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME       = "home"
    const val CHAT       = "chat/{contactId}"
    const val SETTINGS   = "settings"
    const val QR         = "qr"
    const val APPEARANCE = "appearance"
    const val REQUESTS   = "requests"

    fun chat(contactId: String) = "chat/$contactId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        val container  = (application as VeilApp).container
        val repository = container.repository
        val appearance = container.appearance

        setContent {
            val config by appearance.config.collectAsStateWithLifecycle()
            VeilTheme(appTheme = config.theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = VeilColors.Background) {
                    AppLockGate(isLockEnabled = repository.isAppLockEnabled()) {
                        VeilNavGraph(
                            repository = repository,
                            appearance = appearance,
                            context    = applicationContext,
                            startRoute = if (repository.isFirstLaunch()) Routes.ONBOARDING
                                         else Routes.HOME
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLockGate(isLockEnabled: Boolean, content: @Composable () -> Unit) {
    var unlocked by remember { mutableStateOf(!isLockEnabled) }
    if (unlocked) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize().background(VeilColors.Background))
        val activity = LocalContext.current as? FragmentActivity
        LaunchedEffect(Unit) {
            activity?.let {
                BiometricHelper.authenticate(activity = it, onSuccess = { unlocked = true })
            } ?: run { unlocked = true }
        }
    }
}

@Composable
fun VeilNavGraph(
    repository : VeilRepository,
    appearance : AppearancePreferences,
    context    : android.content.Context,
    startRoute : String
) {
    val navController = rememberNavController()
    val factory       = remember { VeilViewModelFactory(repository, appearance, context) }

    NavHost(
        navController       = navController,
        startDestination    = startRoute,
        enterTransition     = { fadeIn(tween(220)) },
        exitTransition      = { fadeOut(tween(180)) },
        popEnterTransition  = { fadeIn(tween(220)) },
        popExitTransition   = { fadeOut(tween(180)) }
    ) {
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = viewModel(factory = factory)
            OnboardingScreen(
                viewModel         = vm,
                onIdentityCreated = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel           = vm,
                onConversationClick = { navController.navigate(Routes.chat(it)) },
                onSettingsClick     = { navController.navigate(Routes.SETTINGS) },
                onAddContactClick   = { navController.navigate(Routes.QR) },
                onRequestsClick     = { navController.navigate(Routes.REQUESTS) }
            )
        }

        composable(
            route     = Routes.CHAT,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { back ->
            val contactId = back.arguments?.getString("contactId") ?: return@composable
            val vm: ChatViewModel = viewModel(
                key     = "chat_$contactId",
                factory = ChatViewModelFactory(repository, contactId)
            )
            ChatScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel         = vm,
                onBackClick       = { navController.popBackStack() },
                onAppearanceClick = { navController.navigate(Routes.APPEARANCE) },
                onQrClick         = { navController.navigate(Routes.QR)},
                onWiped           = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.APPEARANCE) {
            val vm: AppearanceViewModel = viewModel(factory = factory)
            AppearanceScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }

        composable(Routes.QR) {
            val vm: QrViewModel = viewModel(factory = factory)
            QrScreen(
                viewModel      = vm,
                onBackClick    = { navController.popBackStack() },
                onContactAdded = { contactId ->
                    navController.navigate(Routes.chat(contactId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.REQUESTS) {
            val vm: MessageRequestsViewModel = viewModel(factory = factory)
            MessageRequestsScreen(
                viewModel   = vm,
                onBackClick = { navController.popBackStack() },
                onOpenChat  = { contactId ->
                    navController.navigate(Routes.chat(contactId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }
    }
}
