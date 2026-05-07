package com.codeancy.metroui.route

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.codeancy.metroui.common.components.SnackBar
import com.codeancy.metroui.common.utils.MetroUiColor
import com.codeancy.metroui.domain.models.LiveLocationUi
import com.codeancy.metroui.domain.models.ifExistInThisInterChange
import com.codeancy.metroui.firebase.AnalyticsEvents
import com.codeancy.metroui.firebase.ScreenName
import com.codeancy.metroui.firebase.logEvent
import com.codeancy.metroui.route.components.InterchangeRoute
import com.codeancy.metroui.route.components.LiveLocationSettingRow
import com.codeancy.metroui.route.components.MetroRouteHeader
import com.codeancy.metroui.route.components.MetroRouteStations
import com.codeancy.metroui.route.components.MetroRouteSubInfo
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.book_ticket
import kotlinx.coroutines.launch
import org.corexero.sutradhar.analytics.FirebaseAnalyticsTracker
import org.corexero.sutradhar.location.models.LocationProviderStatus
import org.corexero.sutradhar.remoteConfig.ConfigKey
import org.corexero.sutradhar.remoteConfig.FirebaseRemoteConfig
import org.jetbrains.compose.resources.vectorResource

private data object LiveLocationConfigKey : ConfigKey<Boolean>(
    key = "LIVE_LOCATION",
    defaultValue = false
)

@Composable
fun RouteScreen(
    state: RoutScreenState,
    liveLocationUi: LiveLocationUi?,
    locationAccess: LocationProviderStatus?,
    onBack: () -> Unit,
    onAction: (RouteScreenUiAction) -> Unit,
    onShare: (ImageBitmap) -> Unit,
    showInAppReview: () -> Unit,
) {

    LaunchedEffect(Unit) {
        onAction(RouteScreenUiAction.Init)
    }

    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var showCallout by remember { mutableStateOf(false) }

    LaunchedEffect(state.showBookTicketTooltip) {
        if (state.showBookTicketTooltip) {
            showCallout = true
            delay(4000)
            showCallout = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MetroUiColor.background),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            MetroRouteHeader(
                onBack = onBack,
                onShare = {
                    scope.launch {
                        onAction(RouteScreenUiAction.ShareScreenShot)
                        val imageBitmap = graphicsLayer.toImageBitmap()
                        onShare(imageBitmap)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            val routeResultUi = state.routeResultUi ?: run {
                RouteScreenShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState())
                )
                return@Column
            }

            MetroRouteSubInfo(
                time = routeResultUi.interchange.last().let {
                    it.inBetweenStations.lastOrNull() ?: it.destinationStation
                }.time,
                fare = routeResultUi.fare,
                stations = routeResultUi.stations,
                interchanges = routeResultUi.interchanges,
                startStationName = routeResultUi.sourceStation.name.asString(),
                destinationStationName = routeResultUi.destinationStation.name.asString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                isInterChangeRouteOpen = state.isInterChangeFormatOpen,
                onToggleRoute = {
                    onAction(RouteScreenUiAction.ToggleInterChangeFormat)
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isInterChangeFormatOpen
                        && FirebaseRemoteConfig.getBoolean(LiveLocationConfigKey)
                    ) {
                        LiveLocationSettingRow(
                            checked = state.isLiveLocationEnabled,
                            enabled = liveLocationUi !is LiveLocationUi.Initializing && liveLocationUi !is LiveLocationUi.NotInMetro,
                            hasLocation = liveLocationUi is LiveLocationUi.Location,
                            onCheckedChange = { isChecked ->
                                onAction(RouteScreenUiAction.ToggleLiveLocation(isChecked))
                            },
                            locationAccess = locationAccess,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                    AnimatedContent(state.isInterChangeFormatOpen) {
                        if (it) {
                            val liveLocationUis = remember(
                                liveLocationUi,
                                routeResultUi.interchange,
                                state.isLiveLocationEnabled
                            ) {
                                if (
                                    state.isLiveLocationEnabled
                                    && liveLocationUi != null
                                    && liveLocationUi is LiveLocationUi.Location
                                ) {
                                    routeResultUi.interchange.map { interchange ->
                                        liveLocationUi.ifExistInThisInterChange(interchange)
                                    }
                                } else {
                                    List(routeResultUi.interchange.size) { null }
                                }
                            }

                            LaunchedEffect(liveLocationUis) {
                                if (
                                    liveLocationUi is LiveLocationUi.Location
                                    && state.isLiveLocationEnabled &&
                                    liveLocationUis.filterNotNull().isEmpty()
                                ) {
                                    onAction(RouteScreenUiAction.ShowNotInsideMetroError)
                                }
                            }

                            InterchangeRoute(
                                routeResultUi = routeResultUi,
                                liveLocationUis = liveLocationUis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawWithContent {
                                        graphicsLayer.record {
                                            this@drawWithContent.drawContent()
                                        }
                                        drawLayer(graphicsLayer)
                                    }
                            )
                        } else {
                            MetroRouteStations(
                                routeResultUi = routeResultUi,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawWithContent {
                                        graphicsLayer.record {
                                            this@drawWithContent.drawContent()
                                        }
                                        drawLayer(graphicsLayer)
                                    }
                            )
                        }
                    }
                }
                SnackBar(
                    state.showError,
                    message = state.errorMessage,
                    onDismiss = {
                        onAction(RouteScreenUiAction.DismissError)
                    },
                    backgroundColor = MaterialTheme.colorScheme.error,
                )
            }

            if (state.showInAppReview) {
                showInAppReview()
            }

        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            AnimatedVisibility(
                visible = showCallout,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it },
                exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it },
            ) {
                BookTicketCallout()
            }
            FloatingActionButton(
                onClick = {
                    showCallout = false
                    try {
                        uriHandler.openUri("https://wa.me/+919650855800?text=Hi")
                    } catch (_: IllegalArgumentException) {
                        onAction(RouteScreenUiAction.ShowNotInsideMetroError)
                    } finally {
                        FirebaseAnalyticsTracker.logEvent(
                            eventName = AnalyticsEvents.BOOK_TICKET,
                            screenName = ScreenName.ROUTE_SCREEN,
                            eventParams = emptyMap()
                        )
                    }
                },
                containerColor = Color(0xFFDEFEE7),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.book_ticket),
                    contentDescription = "Book Ticket via WhatsApp",
                    tint = Color(0xFF16A34A)
                )
            }
        }
    }
}

private val BookTicketGreen = Color(0xFF16A34A)

@Composable
private fun BookTicketCallout() {
    Column(horizontalAlignment = Alignment.End) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BookTicketGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Book Ticket",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Send 'Hi' on WhatsApp\nto book your ticket!",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Canvas(
            modifier = Modifier
                .padding(end = 18.dp)
                .size(width = 14.dp, height = 8.dp),
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, BookTicketGreen)
        }
    }
}
