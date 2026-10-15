package it.stradesumisura.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import it.stradesumisura.app.AppContainer
import it.stradesumisura.app.AppTexts
import it.stradesumisura.app.ui.components.StradeMark
import it.stradesumisura.app.ui.screens.AssessmentDetailScreen
import it.stradesumisura.app.ui.screens.AssessmentFormScreen
import it.stradesumisura.app.ui.screens.HomeScreen
import it.stradesumisura.app.ui.screens.PdfPreviewScreen
import it.stradesumisura.app.ui.screens.PeopleScreen
import it.stradesumisura.app.ui.screens.PersonDetailScreen
import it.stradesumisura.app.ui.screens.PersonEditScreen
import it.stradesumisura.app.ui.screens.PersonPickerScreen
import it.stradesumisura.app.ui.screens.SettingsScreen
import it.stradesumisura.app.ui.screens.ToolInfoScreen
import it.stradesumisura.app.ui.screens.ToolsScreen

object Routes {
    const val HOME = "home"
    const val PEOPLE = "people"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"

    const val PERSON_NEW = "people/new?toolId={toolId}"
    fun personNew(toolId: String? = null): String = if (toolId == null) "people/new" else "people/new?toolId=$toolId"

    const val PERSON_DETAIL = "person/{personId}"
    fun personDetail(personId: String): String = "person/$personId"

    const val PERSON_EDIT = "person/{personId}/edit"
    fun personEdit(personId: String): String = "person/$personId/edit"

    const val TOOL_INFO = "tool/{toolId}"
    fun toolInfo(toolId: String): String = "tool/$toolId"

    const val PICK_PERSON = "assessment/pick/{toolId}"
    fun pickPerson(toolId: String): String = "assessment/pick/$toolId"

    const val ASSESSMENT_NEW = "assessment/new/{toolId}/{personId}"
    fun assessmentNew(toolId: String, personId: String): String = "assessment/new/$toolId/$personId"

    const val ASSESSMENT_DETAIL = "assessment/{assessmentId}"
    fun assessmentDetail(assessmentId: String): String = "assessment/$assessmentId"

    const val ASSESSMENT_EDIT = "assessment/{assessmentId}/edit"
    fun assessmentEdit(assessmentId: String): String = "assessment/$assessmentId/edit"

    const val ASSESSMENT_PDF = "assessment/{assessmentId}/pdf"
    fun assessmentPdf(assessmentId: String): String = "assessment/$assessmentId/pdf"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, "Panoramica", Icons.Default.Home),
    TopLevelDestination(Routes.PEOPLE, "Persone", Icons.Default.Person),
    TopLevelDestination(Routes.TOOLS, "Strumenti", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Routes.SETTINGS, "Impostazioni", Icons.Default.Settings),
)

/** Larghezza oltre la quale la navigazione passa dalla barra in basso al pannello laterale (Windows, tablet). */
private val WideLayoutMinWidth = 840.dp

/** Larghezza massima del contenuto nelle finestre larghe, per righe di testo leggibili. */
private val WideContentMaxWidth = 1120.dp

@Composable
fun StradeNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = topLevelDestinations.any { it.route == currentRoute }
    // Sezione evidenziata nel pannello laterale, anche nelle schermate interne.
    var section by rememberSaveable { mutableStateOf(Routes.HOME) }
    LaunchedEffect(currentRoute) {
        if (isTopLevel && currentRoute != null) section = currentRoute
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideLayoutMinWidth
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!wide && isTopLevel) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navigateTopLevel(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Row(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
            ) {
                if (wide) {
                    NavigationPane(selectedRoute = section, onSelect = ::navigateTopLevel)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    StradeNavHost(
                        navController = navController,
                        navigateTopLevel = ::navigateTopLevel,
                        modifier = Modifier
                            .widthIn(max = if (wide) WideContentMaxWidth else Dp.Unspecified)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StradeNavHost(
    navController: NavHostController,
    navigateTopLevel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenPeople = { navigateTopLevel(Routes.PEOPLE) },
                onOpenSettings = { navigateTopLevel(Routes.SETTINGS) },
                onOpenTool = { navController.navigate(Routes.toolInfo(it)) },
                onStartAssessment = { navController.navigate(Routes.pickPerson(it)) },
                onOpenAssessment = { navController.navigate(Routes.assessmentDetail(it)) },
            )
        }
        composable(Routes.PEOPLE) {
            PeopleScreen(
                onOpenPerson = { navController.navigate(Routes.personDetail(it)) },
                onAddPerson = { navController.navigate(Routes.personNew()) },
            )
        }
        composable(Routes.TOOLS) {
            ToolsScreen(
                onOpenTool = { navController.navigate(Routes.toolInfo(it)) },
                onStartAssessment = { navController.navigate(Routes.pickPerson(it)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
        composable(
            route = Routes.PERSON_NEW,
            arguments = listOf(
                navArgument("toolId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val toolId = entry.stringArgument("toolId")
            PersonEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { personId ->
                    if (toolId != null) {
                        navController.navigate(Routes.assessmentNew(toolId, personId)) {
                            popUpTo(Routes.PICK_PERSON) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.personDetail(personId)) {
                            popUpTo(Routes.PERSON_NEW) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(
            route = Routes.PERSON_DETAIL,
            arguments = listOf(navArgument("personId") { type = NavType.StringType }),
        ) {
            PersonDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.personEdit(it)) },
                onNewAssessment = { toolId, personId -> navController.navigate(Routes.assessmentNew(toolId, personId)) },
                onOpenAssessment = { navController.navigate(Routes.assessmentDetail(it)) },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PERSON_EDIT,
            arguments = listOf(navArgument("personId") { type = NavType.StringType }),
        ) {
            PersonEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.TOOL_INFO,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType }),
        ) { entry ->
            val toolId = entry.stringArgument("toolId").orEmpty()
            ToolInfoScreen(
                toolId = toolId,
                onBack = { navController.popBackStack() },
                onStartAssessment = { navController.navigate(Routes.pickPerson(toolId)) },
            )
        }
        composable(
            route = Routes.PICK_PERSON,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType }),
        ) { entry ->
            val toolId = entry.stringArgument("toolId").orEmpty()
            PersonPickerScreen(
                toolId = toolId,
                onBack = { navController.popBackStack() },
                onPick = { personId ->
                    navController.navigate(Routes.assessmentNew(toolId, personId)) {
                        popUpTo(Routes.PICK_PERSON) { inclusive = true }
                    }
                },
                onCreatePerson = { navController.navigate(Routes.personNew(toolId)) },
            )
        }
        composable(
            route = Routes.ASSESSMENT_NEW,
            arguments = listOf(
                navArgument("toolId") { type = NavType.StringType },
                navArgument("personId") { type = NavType.StringType },
            ),
        ) {
            AssessmentFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { assessmentId ->
                    navController.navigate(Routes.assessmentDetail(assessmentId)) {
                        popUpTo(Routes.ASSESSMENT_NEW) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.ASSESSMENT_EDIT,
            arguments = listOf(navArgument("assessmentId") { type = NavType.StringType }),
        ) {
            AssessmentFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.ASSESSMENT_DETAIL,
            arguments = listOf(navArgument("assessmentId") { type = NavType.StringType }),
        ) {
            AssessmentDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.assessmentEdit(it)) },
                onExportPdf = { navController.navigate(Routes.assessmentPdf(it)) },
                onOpenPerson = { navController.navigate(Routes.personDetail(it)) },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.ASSESSMENT_PDF,
            arguments = listOf(navArgument("assessmentId") { type = NavType.StringType }),
        ) {
            PdfPreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Pannello di navigazione laterale delle finestre larghe, con il marchio in alto. */
@Composable
private fun NavigationPane(selectedRoute: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(248.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 20.dp),
        ) {
            StradeMark(
                height = 52.dp,
                modifier = Modifier.padding(horizontal = 14.dp),
                bandColor = MaterialTheme.colorScheme.onSurface,
                wordColor = MaterialTheme.colorScheme.surface,
                captionColor = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(28.dp))
            topLevelDestinations.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(destination.label, style = MaterialTheme.typography.labelLarge) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    selected = selectedRoute == destination.route,
                    onClick = { onSelect(destination.route) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                AppTexts.TAGLINE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Versione ${AppContainer.instance.platform.versionName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Argomento testuale della rotta, null se assente. */
private fun NavBackStackEntry.stringArgument(name: String): String? =
    arguments?.read { if (contains(name) && !isNull(name)) getString(name) else null }
