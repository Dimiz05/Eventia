package com.example.eventia.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Account
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.components.EventArtwork
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.ui.screens.explore.exploreColors
import com.example.eventia.ui.screens.session.SessionUiState
import com.example.eventia.ui.theme.EventiaTheme
import java.time.OffsetDateTime

@Composable
internal fun ProfileRoute(
    session: SessionUiState.Active?,
    onRetryAccount: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onEventClick: (Long) -> Unit,
    onSessionExpired: () -> Unit
) {
    if (session == null || session.signingOut) {
        ProfileScreen(session, null, onRetryAccount, onSignIn, onSignOut, {}, onEventClick)
        return
    }

    val viewModel: ProfileViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    LaunchedEffect(state.sessionRejected) {
        if (state.sessionRejected) onSessionExpired()
    }
    ProfileScreen(session, state, onRetryAccount, onSignIn, onSignOut, viewModel::refresh, onEventClick)
}

@Composable
internal fun ProfileScreen(
    session: SessionUiState.Active?,
    state: ProfileUiState?,
    onRetryAccount: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRetryAttendances: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    var showUpcoming by rememberSaveable { mutableStateOf(true) }
    val groups = state?.attendances
    val events = if (showUpcoming) groups?.upcoming else groups?.past
    MaterialTheme(colorScheme = exploreColors()) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                ProfileHeader()
                if (session?.signingOut == true) {
                    LoadingScreen(stringResource(R.string.auth_loading))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { ProfileAccount(session, onRetryAccount, onSignIn, onSignOut) }
                        if (state != null) {
                            item {
                                AttendanceTabs(groups, showUpcoming) { showUpcoming = it }
                            }
                            if (state.loading) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(R.string.profile_attendances_loading))
                                        LinearProgressIndicator(Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            if (state.failed) {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            stringResource(R.string.profile_attendances_error),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        OutlinedButton(onClick = onRetryAttendances) {
                                            Text(stringResource(R.string.retry))
                                        }
                                    }
                                }
                            }
                            if (events != null) {
                                if (events.isEmpty() && !state.loading && !state.failed) {
                                    item {
                                        val message = if (showUpcoming) R.string.profile_upcoming_empty
                                        else R.string.profile_past_empty
                                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                            Text(
                                                stringResource(message),
                                                Modifier.fillMaxWidth().padding(16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                items(events, key = Event::id) { event ->
                                    AttendanceTicket(event, showUpcoming) { onEventClick(event.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader() {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painterResource(R.drawable.eventia_app_icon), null,
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
        )
        Text(
            stringResource(R.string.tab_profile),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(color = colors.outlineVariant)
}

@Composable
private fun ProfileAccount(
    session: SessionUiState.Active?,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val account = session?.account
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painterResource(R.drawable.profile_avatar_duck),
                    stringResource(R.string.profile_avatar),
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val name = account?.name?.takeIf { it.isNotBlank() }
                        ?: stringResource(if (session == null) R.string.profile_guest_title else R.string.tab_profile)
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (account != null) {
                        Text(account.email, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    } else if (session == null) {
                        Text(
                            stringResource(R.string.profile_guest_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
            if (session == null) {
                Button(onClick = onSignIn, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.auth_sign_in))
                }
            } else {
                if (session.loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if ((account != null && session.refreshFailed) || (account == null && !session.loading)) {
                    Text(stringResource(R.string.auth_account_error), color = colors.error)
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
                if (session.signOutFailed) {
                    Text(stringResource(R.string.auth_sign_out_error), color = colors.error)
                }
                FilledTonalButton(
                    onClick = onSignOut,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    )
                ) { Text(stringResource(R.string.auth_sign_out)) }
            }
        }
    }
}

@Composable
private fun AttendanceTabs(groups: AttendanceGroups?, showUpcoming: Boolean, onSelect: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.profile_attendances),
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            Modifier.fillMaxWidth().background(colors.primaryContainer, CircleShape).padding(4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(true, false).forEach { upcoming ->
                val selected = upcoming == showUpcoming
                val count = if (upcoming) groups?.upcoming?.size else groups?.past?.size
                val label = stringResource(if (upcoming) R.string.profile_upcoming else R.string.profile_past)
                val contentColor = if (selected) colors.onPrimary else colors.onPrimaryContainer
                Row(
                    Modifier.weight(1f).clip(CircleShape)
                        .background(if (selected) colors.primary else colors.primaryContainer)
                        .selectable(selected, role = Role.Tab, onClick = { onSelect(upcoming) })
                        .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    Icon(
                        painterResource(if (upcoming) R.drawable.ic_event_calendar else R.drawable.ic_catalog_clock),
                        null,
                        Modifier.size(16.dp),
                        tint = contentColor
                    )
                    Text(
                        if (count == null) label else stringResource(R.string.profile_attendance_tab_count, label, count),
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceTicket(event: Event, upcoming: Boolean, onOpen: () -> Unit) {
    val paper = Color(0xFFF5F5F4)
    val ink = Color(0xFF192C33)
    val accent = Color(0xFF007A85)
    val border = Color(0xFF9ABACB)
    var footerHeight by remember { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxWidth().drawWithContent {
            val inset = 0.5.dp.toPx()
            val radius = 14.dp.toPx()
            val notch = 8.dp.toPx()
            val seam = size.height - footerHeight
            val outline = Path().apply {
                addRoundRect(
                    RoundRect(Rect(inset, inset, size.width - inset, size.height - inset), CornerRadius(radius))
                )
            }
            val cutouts = Path().apply {
                addOval(Rect(Offset(inset, seam), notch))
                addOval(Rect(Offset(size.width - inset, seam), notch))
            }
            val ticket = Path.combine(PathOperation.Difference, outline, cutouts)
            clipPath(ticket) {
                this@drawWithContent.drawContent()
                drawLine(
                    border, Offset(notch, seam), Offset(size.width - notch, seam),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                )
            }
            drawPath(ticket, border, style = Stroke(1.dp.toPx()))
        }.background(paper).clickable(onClick = onOpen)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val landscapeHeight = maxWidth * (509f / 1645f)
            Box(Modifier.fillMaxWidth().heightIn(min = landscapeHeight)) {
                Image(
                    painterResource(R.drawable.attendance_misty_mountains),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(landscapeHeight),
                    contentScale = ContentScale.Fit
                )
                Row(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventArtwork(event.imageUrl, Modifier.size(62.dp).clip(RoundedCornerShape(10.dp)))
                    Column(
                        Modifier.weight(1f).background(Color.White.copy(alpha = 0.74f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            eventLocalDateTime(event),
                            style = MaterialTheme.typography.bodySmall,
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            event.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = ink,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(if (upcoming) R.drawable.ic_session_check else R.drawable.ic_catalog_clock),
                                null, Modifier.size(16.dp), tint = if (upcoming) accent else ink
                            )
                            Text(
                                stringResource(
                                    if (upcoming) R.string.profile_attendance_confirmed else R.string.profile_event_past
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (upcoming) accent else ink,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().onSizeChanged { footerHeight = it.height }.padding(horizontal = 12.dp)
        ) {
            val stacked = maxWidth < 250.dp || LocalDensity.current.fontScale > 1.25f
            if (upcoming && stacked) {
                Column(Modifier.fillMaxWidth()) {
                    TicketVenue(event.venue.name, ink, accent, Modifier.padding(top = 8.dp))
                    ManageAttendance(onOpen, accent, Modifier.align(Alignment.End))
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TicketVenue(event.venue.name, ink, accent, Modifier.weight(1f))
                    if (upcoming) ManageAttendance(onOpen, accent)
                }
            }
        }
    }
}

@Composable
private fun TicketVenue(name: String, ink: Color, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (name.isNotBlank()) {
            Icon(painterResource(R.drawable.ic_catalog_pin), null, Modifier.size(17.dp), tint = accent)
            Text(name, style = MaterialTheme.typography.bodySmall, color = ink)
        }
    }
}

@Composable
private fun ManageAttendance(onOpen: () -> Unit, accent: Color, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onOpen,
        modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(
            stringResource(R.string.profile_manage_attendance),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 850)
@Preview(showBackground = true, widthDp = 390, heightDp = 850, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfilePreview() {
    val event = Event(
        id = 1,
        title = "Noche de jazz en la plaza",
        summary = "",
        description = null,
        startsAt = OffsetDateTime.parse("2030-11-14T20:00:00-04:00"),
        endsAt = null,
        category = Category(1, "Música"),
        imageUrl = null,
        venue = Venue(1, "Plaza Mayor", "Av. Principal"),
        capacity = 50,
        availableSpots = 36,
        isAttending = true
    )
    val groups = AttendanceGroups(
        upcoming = listOf(event, event.copy(id = 2, title = "Taller de Robótica y Kotlin", startsAt = event.startsAt.plusDays(4))),
        past = listOf(event.copy(id = 3, title = "Festival Indie", startsAt = event.startsAt.minusYears(5)))
    )
    ProfilePreviewFrame(
        SessionUiState.Active(Account("camila@example.com", "Camila Rodríguez"), loading = false),
        ProfileUiState(groups, loading = false)
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileGuestPreview() {
    ProfilePreviewFrame(null, null)
}

@Composable
private fun ProfilePreviewFrame(session: SessionUiState.Active?, state: ProfileUiState?) {
    EventiaTheme {
        Box(Modifier.fillMaxSize().background(exploreColors().surface).statusBarsPadding().navigationBarsPadding()) {
            ProfileScreen(session, state, {}, {}, {}, {}, {})
        }
    }
}
