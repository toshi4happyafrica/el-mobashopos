package jp.elektrolibera.mobashopos.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jp.elektrolibera.mobashopos.R
import jp.elektrolibera.mobashopos.data.PosRepository
import jp.elektrolibera.mobashopos.data.settings.SessionState
import jp.elektrolibera.mobashopos.ui.checkout.CheckoutScreen
import jp.elektrolibera.mobashopos.ui.common.LoadingBox
import jp.elektrolibera.mobashopos.ui.login.LoginScreen
import jp.elektrolibera.mobashopos.ui.main.HomeScreen
import jp.elektrolibera.mobashopos.ui.order.OrderScreen
import jp.elektrolibera.mobashopos.ui.setup.SetupScreen
import jp.elektrolibera.mobashopos.ui.tables.TablePickerScreen

/**
 * 画面の大枠は保存済みの状態で決まる：
 * 端末未登録 → 端末登録、店員未ログイン → ログイン、ログイン済み → メイン以下の画面遷移。
 * トークンが無効になると状態が変わり、自動でログイン画面へ戻る。
 */
@Composable
fun PosApp(repo: PosRepository) {
    val session by repo.session.collectAsState(initial = null)
    MaterialTheme {
        Surface {
            val s = session
            when {
                s == null -> LoadingBox()
                !s.isEnrolled -> SetupScreen(repo)
                !s.isLoggedIn -> LoginScreen(repo, s.storeName)
                else -> MainNav(repo, s)
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val ORDER_TABLES = "order/tables"
    const val ORDER = "order/{tableId}"
    const val CHECKOUT_TABLES = "checkout/tables"
    const val CHECKOUT = "checkout/{tableId}"
    fun order(tableId: String) = "order/$tableId"
    fun checkout(tableId: String) = "checkout/$tableId"
}

@Composable
private fun MainNav(repo: PosRepository, session: SessionState) {
    val nav = rememberNavController()
    val backToHome: () -> Unit = { nav.popBackStack(Routes.HOME, inclusive = false) }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repo = repo,
                session = session,
                onOrder = { nav.navigate(Routes.ORDER_TABLES) },
                onCheckout = { nav.navigate(Routes.CHECKOUT_TABLES) },
            )
        }
        composable(Routes.ORDER_TABLES) {
            TablePickerScreen(
                repo = repo,
                title = stringResource(R.string.tables_title_order),
                onBack = { nav.popBackStack() },
                onSelect = { id -> nav.navigate(Routes.order(id)) },
            )
        }
        composable(Routes.ORDER, arguments = listOf(navArgument("tableId") { type = NavType.StringType })) { entry ->
            val tableId = entry.arguments?.getString("tableId").orEmpty()
            OrderScreen(repo = repo, tableId = tableId, onBack = { nav.popBackStack() }, onDone = backToHome)
        }
        composable(Routes.CHECKOUT_TABLES) {
            TablePickerScreen(
                repo = repo,
                title = stringResource(R.string.tables_title_checkout),
                onBack = { nav.popBackStack() },
                onSelect = { id -> nav.navigate(Routes.checkout(id)) },
            )
        }
        composable(Routes.CHECKOUT, arguments = listOf(navArgument("tableId") { type = NavType.StringType })) { entry ->
            val tableId = entry.arguments?.getString("tableId").orEmpty()
            CheckoutScreen(repo = repo, tableId = tableId, onBack = { nav.popBackStack() }, onDone = backToHome)
        }
    }
}
