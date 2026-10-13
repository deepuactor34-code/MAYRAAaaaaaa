package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.viewmodel.MainViewModel

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
    const val ABOUT = "about"
}

@Composable
fun MayraaNavGraph(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(
            route = Routes.SPLASH,
            exitTransition = { fadeOut(animationSpec = tween(400)) }
        ) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.HOME,
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(350))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(350))
            }
        ) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToActivity = { navController.navigate(Routes.ACTIVITY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToPermissions = { navController.navigate(Routes.PERMISSIONS) }
            )
        }

        composable(
            route = Routes.ACTIVITY,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(350))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(350))
            }
        ) {
            ActivityScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SETTINGS,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(350))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(350))
            }
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(
            route = Routes.PERMISSIONS,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(350))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(350))
            }
        ) {
            PermissionsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ABOUT,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(350))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(350))
            }
        ) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
