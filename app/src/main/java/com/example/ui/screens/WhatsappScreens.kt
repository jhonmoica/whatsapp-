package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import com.example.BuildConfig
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- Helper Functions and Custom Drawings ---

@Composable
fun ChatWallpaper(modifier: Modifier = Modifier, isDark: Boolean = isSystemInDarkTheme()) {
    val tintColor = if (isDark) Color(0xFF0F1C24) else Color(0xFFEFEAE2)
    val patternColor = if (isDark) Color(0xFF16252F) else Color(0xFFE6E0D5)

    Box(modifier = modifier.background(tintColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacingX = 140.dp.toPx()
            val spacingY = 140.dp.toPx()
            val cols = (size.width / spacingX).toInt() + 2
            val rows = (size.height / spacingY).toInt() + 2

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = c * spacingX + (if (r % 2 == 0) spacingX / 2f else 0f)
                    val y = r * spacingY

                    val symbolIndex = (r + c) % 5
                    when (symbolIndex) {
                        0 -> { // Cloud
                            drawCircle(patternColor, radius = 5.dp.toPx(), center = Offset(x, y))
                            drawCircle(patternColor, radius = 3.dp.toPx(), center = Offset(x - 4.dp.toPx(), y + 1.dp.toPx()))
                            drawCircle(patternColor, radius = 3.dp.toPx(), center = Offset(x + 4.dp.toPx(), y + 1.dp.toPx()))
                        }
                        1 -> { // Heart
                            val path = Path().apply {
                                moveTo(x, y + 3.dp.toPx())
                                cubicTo(x - 5.dp.toPx(), y - 3.dp.toPx(), x - 8.dp.toPx(), y + 1.dp.toPx(), x, y + 8.dp.toPx())
                                cubicTo(x + 8.dp.toPx(), y + 1.dp.toPx(), x + 5.dp.toPx(), y - 3.dp.toPx(), x, y + 3.dp.toPx())
                            }
                            drawPath(path, color = patternColor)
                        }
                        2 -> { // Small branch
                            drawLine(patternColor, Offset(x - 4.dp.toPx(), y + 4.dp.toPx()), Offset(x + 4.dp.toPx(), y - 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                            drawCircle(patternColor, radius = 2.dp.toPx(), center = Offset(x, y - 2.dp.toPx()))
                            drawCircle(patternColor, radius = 2.dp.toPx(), center = Offset(x - 2.dp.toPx(), y + 2.dp.toPx()))
                        }
                        3 -> { // Smiley face
                            drawCircle(patternColor, radius = 7.dp.toPx(), center = Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                            drawCircle(patternColor, radius = 1.dp.toPx(), center = Offset(x - 2.5.dp.toPx(), y - 1.5.dp.toPx()))
                            drawCircle(patternColor, radius = 1.dp.toPx(), center = Offset(x + 2.5.dp.toPx(), y - 1.5.dp.toPx()))
                            val path = Path().apply {
                                arcTo(
                                    rect = Rect(x - 3.dp.toPx(), y - 2.dp.toPx(), x + 3.dp.toPx(), y + 3.dp.toPx()),
                                    startAngleDegrees = 0f,
                                    sweepAngleDegrees = 180f,
                                    forceMoveTo = false
                                )
                            }
                            drawPath(path, color = patternColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                        }
                        4 -> { // Phone
                            drawRoundRect(
                                color = patternColor,
                                topLeft = Offset(x - 3.dp.toPx(), y - 5.dp.toPx()),
                                size = Size(6.dp.toPx(), 10.dp.toPx()),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(patternColor, radius = 0.8.dp.toPx(), center = Offset(x, y + 3.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageStatusDoubleTick(status: String, modifier: Modifier = Modifier) {
    val isRead = status == "read"
    val tickColor = if (isRead) Color(0xFF34B7F1) else Color(0xFF8696A0) // WhatsApp blue ticks vs grey ticks

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (status == "sent") {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                modifier = Modifier.size(13.dp),
                tint = Color(0xFF8696A0)
            )
        } else {
            Box(contentAlignment = Alignment.CenterStart) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delivered",
                    modifier = Modifier.size(13.dp),
                    tint = tickColor
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Delivered",
                    modifier = Modifier
                        .size(13.dp)
                        .offset(x = 4.dp),
                    tint = tickColor
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun ContactAvatar(
    name: String,
    avatarColorHex: String,
    size: Double = 46.0,
    modifier: Modifier = Modifier
) {
    val color = remember(avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(avatarColorHex))
        } catch (e: Exception) {
            Color(0xFF075E54)
        }
    }
    val initial = if (name.isNotEmpty()) name.first().uppercase() else "C"

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = (size * 0.42).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatStatusTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val hrs = (diff / (1000 * 60 * 60)).toInt()
    return when {
        hrs == 0 -> {
            val mins = (diff / (1000 * 60)).toInt()
            if (mins <= 1) "Agora mesmo" else "Há $mins minutos"
        }
        hrs < 24 -> "Há $hrs horas"
        else -> {
            val days = hrs / 24
            "Há $days dias"
        }
    }
}

// --- Main Container with Scaffold and Bottom Navigation Bar ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsappMainContainer(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) } // 0: Chats, 1: Status, 2: Calls, 3: Settings
    
    val activeChatId by viewModel.activeChatId.collectAsState()
    val activeCallState by viewModel.activeCallState.collectAsState()
    
    val chats by viewModel.chats.collectAsState()
    val unreadChatsCount = remember(chats) {
        chats.sumOf { it.unreadCount }
    }

    var showContactSelectionDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showProfileMenuDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (activeChatId == null) {
                TopAppBar(
                    title = {
                        Text(
                            text = "WhatsApp",
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF075E54),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* Search Placeholder */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = if (isSystemInDarkTheme()) Color.White else Color(0xFF075E54))
                        }
                        Box {
                            IconButton(onClick = { showProfileMenuDropdown = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = if (isSystemInDarkTheme()) Color.White else Color(0xFF075E54))
                            }
                            DropdownMenu(
                                expanded = showProfileMenuDropdown,
                                onDismissRequest = { showProfileMenuDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Novo Grupo") },
                                    onClick = {
                                        showProfileMenuDropdown = false
                                        showCreateGroupDialog = true
                                    }
                                )
                                if (currentTab == 2) {
                                    DropdownMenuItem(
                                        text = { Text("Limpar Registro") },
                                        onClick = {
                                            showProfileMenuDropdown = false
                                            viewModel.clearAllCallLogs()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Meu Perfil") },
                                    onClick = {
                                        showProfileMenuDropdown = false
                                        currentTab = 3 // Settings / Profile tab
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (activeChatId == null) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadChatsCount > 0) {
                                        Badge(containerColor = WhatsappGreenAccent) {
                                            Text(unreadChatsCount.toString(), color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == 0) Icons.Default.Chat else Icons.Outlined.Chat,
                                    contentDescription = "Conversas"
                                )
                            }
                        },
                        label = { Text("Conversas") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Default.CameraAlt else Icons.Outlined.CameraAlt,
                                contentDescription = "Status"
                            )
                        },
                        label = { Text("Status") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Default.Call else Icons.Outlined.Call,
                                contentDescription = "Chamadas"
                            )
                        },
                        label = { Text("Chamadas") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                                contentDescription = "Configurações"
                            )
                        },
                        label = { Text("Ajustes") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeChatId == null && currentTab != 3) {
                FloatingActionButton(
                    onClick = {
                        if (currentTab == 0) {
                            showContactSelectionDialog = true
                        } else if (currentTab == 1) {
                            // Show Quick Status Posting Dialog
                            showPostStatusDialog = true
                        } else if (currentTab == 2) {
                            showContactSelectionDialog = true
                        }
                    },
                    containerColor = WhatsappGreenAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = when (currentTab) {
                            0 -> Icons.Default.Message
                            1 -> Icons.Default.Create
                            else -> Icons.Default.Call
                        },
                        contentDescription = "Ação"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ChatListTab(viewModel = viewModel, onChatClicked = { chatId -> viewModel.openChat(chatId) })
                1 -> StatusTab(viewModel = viewModel)
                2 -> CallTab(viewModel = viewModel)
                3 -> ProfileSettingsTab(viewModel = viewModel)
            }

            // Chat detail screen slides in when activeChatId is set
            AnimatedVisibility(
                visible = activeChatId != null,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                activeChatId?.let { chatId ->
                    ChatDetailScreen(
                        chatId = chatId,
                        viewModel = viewModel,
                        onBack = { viewModel.closeChat() }
                    )
                }
            }

            // Call overlay screen rises up when a call state is active
            AnimatedVisibility(
                visible = activeCallState != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ActiveCallOverlay(viewModel = viewModel)
            }
        }
    }

    // --- Overlay Dialogs ---

    if (showContactSelectionDialog) {
        ContactSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showContactSelectionDialog = false },
            onContactSelected = { contactId ->
                showContactSelectionDialog = false
                viewModel.openChat(contactId)
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            viewModel = viewModel,
            onDismiss = { showCreateGroupDialog = false }
        )
    }

    if (showPostStatusDialog) {
        PostStatusDialog(
            onDismiss = { showPostStatusDialog = false },
            onPost = { text, color ->
                showPostStatusDialog = false
                viewModel.postTextStatus(text, color)
            }
        )
    }
}

// Variables for showing popups
private var showPostStatusDialog by mutableStateOf(false)

// --- Tab Content Composable: Chat List (Conversas) ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListTab(
    viewModel: ChatViewModel,
    onChatClicked: (String) -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val typingContactId by viewModel.typingContactId.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    if (chats.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhuma conversa ainda",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Toque no botão abaixo para iniciar uma nova conversa com seus contatos.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chat_list")
        ) {
            items(chats, key = { it.id }) { chat ->
                var showOptionsDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onChatClicked(chat.id) },
                            onLongClick = { showOptionsDialog = true }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactAvatar(
                        name = chat.title,
                        avatarColorHex = chat.avatarColorHex,
                        size = 52.0
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.title,
                                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatTime(chat.lastMessageTimestamp),
                                fontSize = 12.sp,
                                color = if (chat.unreadCount > 0) WhatsappGreenAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isTyping = typingContactId != null && (chat.id == typingContactId || (chat.isGroup && contacts.any { it.id == typingContactId }))
                            if (isTyping) {
                                val typerName = if (chat.isGroup) {
                                    val contact = contacts.find { it.id == typingContactId }
                                    "${contact?.name ?: "Alguém"}: "
                                } else ""
                                Text(
                                    text = "$typerName" + "digitando...",
                                    color = WhatsappGreenAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = chat.lastMessage,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (chat.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(WhatsappGreenAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                if (showOptionsDialog) {
                    AlertDialog(
                        onDismissRequest = { showOptionsDialog = false },
                        title = { Text(chat.title) },
                        text = { Text("Deseja apagar esta conversa e todo o seu histórico de mensagens?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteChat(chat.id)
                                    showOptionsDialog = false
                                }
                            ) {
                                Text("Apagar", color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOptionsDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- Tab Content Composable: Status (Updates) ---

@Composable
fun StatusTab(
    viewModel: ChatViewModel
) {
    val statusUpdates by viewModel.statusUpdates.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var activeStatusViewerItem by remember { mutableStateOf<StatusUpdate?>(null) }

    val userStatuses = remember(statusUpdates) {
        statusUpdates.filter { it.contactId == "user" }
    }
    val contactStatuses = remember(statusUpdates) {
        statusUpdates.filter { it.contactId != "user" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("status_tab")
    ) {
        // My Status Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (userStatuses.isNotEmpty()) {
                            activeStatusViewerItem = userStatuses.first()
                        } else {
                            showPostStatusDialog = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    ContactAvatar(
                        name = userName,
                        avatarColorHex = "#128C7E",
                        size = 54.0
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(WhatsappGreenAccent)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar Status",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Meu Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (userStatuses.isNotEmpty()) "Toque para ver suas atualizações" else "Toque para adicionar uma atualização de status",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = "Atualizações recentes",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF128C7E),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (contactStatuses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma atualização recente",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Group statuses by contact
            val groupedStatuses = contactStatuses.groupBy { it.contactId }

            items(groupedStatuses.keys.toList()) { contactId ->
                val contactList = groupedStatuses[contactId] ?: emptyList()
                val latestStatus = contactList.maxByOrNull { it.timestamp } ?: return@items
                val contact = contacts.find { it.id == contactId }
                val hasUnread = contactList.any { !it.isViewed }

                // Circle Status Border Indicator
                val strokeColor = if (hasUnread) WhatsappGreenAccent else Color(0xFF8696A0).copy(alpha = 0.5f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextUnread = contactList.find { !it.isViewed } ?: contactList.first()
                            activeStatusViewerItem = nextUnread
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .drawBehind {
                                if (contactList.size == 1) {
                                    drawCircle(
                                        color = strokeColor,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                    )
                                } else {
                                    // Draw dashed status indicator circles
                                    val count = contactList.size
                                    val sweep = (360f / count) - 10f
                                    for (i in 0 until count) {
                                        val start = (i * (360f / count)) - 90f
                                        val itemHasUnread = !contactList[i].isViewed
                                        drawArc(
                                            color = if (itemHasUnread) WhatsappGreenAccent else Color(0xFF8696A0).copy(alpha = 0.5f),
                                            startAngle = start,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ContactAvatar(
                            name = latestStatus.contactName,
                            avatarColorHex = contact?.avatarColorHex ?: "#25D366",
                            size = 48.0
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = latestStatus.contactName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatStatusTime(latestStatus.timestamp),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 84.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    }

    // Story Status Viewer overlay
    activeStatusViewerItem?.let { currentStatus ->
        val group = remember(currentStatus, statusUpdates) {
            statusUpdates.filter { it.contactId == currentStatus.contactId }.sortedBy { it.timestamp }
        }
        val currentIndex = group.indexOfFirst { it.id == currentStatus.id }

        StatusStoryViewer(
            statuses = group,
            startIndex = if (currentIndex != -1) currentIndex else 0,
            onDismiss = { activeStatusViewerItem = null },
            onStatusViewed = { id -> viewModel.viewStatus(id) }
        )
    }
}

// --- Tab Content Composable: Call Log (Ligações) ---

@Composable
fun CallTab(
    viewModel: ChatViewModel
) {
    val callLogs by viewModel.callLogs.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    if (callLogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhum registro de chamada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Suas chamadas recentes de voz e vídeo aparecerão aqui.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("call_tab")
        ) {
            items(callLogs, key = { it.id }) { log ->
                val contact = contacts.find { it.id == log.contactId }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.startCall(
                                contactName = log.contactName,
                                contactId = log.contactId,
                                isVideo = log.isVideo
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactAvatar(
                        name = log.contactName,
                        avatarColorHex = contact?.avatarColorHex ?: "#075E54",
                        size = 50.0
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.contactName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (log.isMissed && log.isIncoming) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    !log.isIncoming -> Icons.Default.CallMade
                                    log.isMissed -> Icons.Default.CallMissed
                                    else -> Icons.Default.CallReceived
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = when {
                                    log.isMissed && log.isIncoming -> Color.Red
                                    !log.isIncoming -> Color(0xFF8696A0)
                                    else -> Color(0xFF25D366)
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatStatusTime(log.timestamp),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.startCall(
                                contactName = log.contactName,
                                contactId = log.contactId,
                                isVideo = log.isVideo
                            )
                        }
                    ) {
                        Icon(
                            imageVector = if (log.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Ligar",
                            tint = if (isSystemInDarkTheme()) Color(0xFF25D366) else Color(0xFF075E54)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    }
}

// --- Tab Content Composable: Profile & Settings (Ajustes) ---

@Composable
fun ProfileSettingsTab(
    viewModel: ChatViewModel
) {
    val userName by viewModel.userName.collectAsState()
    val userPhone by viewModel.userPhone.collectAsState()
    val userStatus by viewModel.userStatus.collectAsState()
    val isSmartReplyEnabled by viewModel.isSmartReplyEnabled.collectAsState()

    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsState()
    val firebaseLogs by viewModel.firebaseLogs.collectAsState()
    val isFirebaseReal by viewModel.isFirebaseReal.collectAsState()
    val firebaseStatusMessage by viewModel.firebaseStatusMessage.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showFirebaseLogs by remember { mutableStateOf(false) }
    var showFirebaseSetupHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("settings_tab")
    ) {
        // Quick Profile Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { showEditProfileDialog = true },
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) DarkSurfaceLighter else Color.White
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(
                    name = userName,
                    avatarColorHex = "#128C7E",
                    size = 64.0
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userStatus,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userPhone,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        // Section header
        Text(
            text = "Recursos Inteligentes IA",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF128C7E),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // AI Smart Reply settings toggle
        ListItem(
            headlineContent = { Text("Smart Replies Inteligentes (IA)") },
            supportingContent = {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val activeModeText = if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                    "Ativo com Gemini 3.5-Flash (Nuvem)"
                } else {
                    "Ativo com Simulador Offline"
                }
                Text("Simula respostas com base na personalidade de cada contato. Status: $activeModeText")
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = WhatsappGreenAccent
                )
            },
            trailingContent = {
                Switch(
                    checked = isSmartReplyEnabled,
                    onCheckedChange = { viewModel.setSmartReplyEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WhatsappGreenAccent
                    )
                )
            },
            modifier = Modifier.clickable { viewModel.setSmartReplyEnabled(!isSmartReplyEnabled) }
        )

        // --- Firebase Integration Section ---
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        Text(
            text = "Sincronização & Backup Firebase",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF128C7E),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) DarkSurfaceLighter else Color(0xFFF8F9FA)
            ),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Connection Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isFirebaseReal) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isFirebaseReal) WhatsappGreenAccent else Color(0xFFE67E22),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFirebaseReal) "Firebase Conectado" else "Firebase Simulator Ativo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = firebaseStatusMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sincronize ou restaure suas conversas locais de forma segura usando o Firebase Firestore em tempo real.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sync controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.performFirebaseBackup() },
                        enabled = firebaseSyncStatus != "SYNCING_BACKUP" && firebaseSyncStatus != "SYNCING_RESTORE",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsappGreenAccent,
                            disabledContainerColor = WhatsappGreenAccent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f).testTag("firebase_backup_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup", fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.performFirebaseRestore() },
                        enabled = firebaseSyncStatus != "SYNCING_BACKUP" && firebaseSyncStatus != "SYNCING_RESTORE",
                        border = BorderStroke(1.dp, WhatsappGreenAccent),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WhatsappGreenAccent,
                            disabledContentColor = WhatsappGreenAccent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f).testTag("firebase_restore_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar", fontSize = 13.sp)
                        }
                    }
                }

                // Progress Indicator
                if (firebaseSyncStatus == "SYNCING_BACKUP" || firebaseSyncStatus == "SYNCING_RESTORE") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        Text(
                            text = if (firebaseSyncStatus == "SYNCING_BACKUP") "Enviando dados..." else "Buscando dados...",
                            fontSize = 12.sp,
                            color = WhatsappGreenAccent,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = WhatsappGreenAccent,
                            trackColor = WhatsappGreenAccent.copy(alpha = 0.15f)
                        )
                    }
                }

                // Expandable Logs Console
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFirebaseLogs = !showFirebaseLogs }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver console de depuração Firebase",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showFirebaseLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showFirebaseLogs) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.Black, shape = RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        if (firebaseLogs.isEmpty()) {
                            Text(
                                text = "Nenhuma operação Firebase executada.",
                                color = Color(0xFF8696A0),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(firebaseLogs.size) { index ->
                                    val log = firebaseLogs[index]
                                    Text(
                                        text = log,
                                        color = if (log.contains("Erro") || log.contains("exceção", true)) Color.Red else if (log.contains("[SIMULAÇÃO]")) Color(0xFF3498DB) else Color.Green,
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { viewModel.clearFirebaseLogs() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Limpar Console", fontSize = 11.sp)
                    }
                }

                // Help accordion for physical setup
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFirebaseSetupHelp = !showFirebaseSetupHelp }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Como habilitar Firebase na nuvem real?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showFirebaseSetupHelp) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showFirebaseSetupHelp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), shape = RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Para conectar com seu console Firebase real:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Crie um projeto no Console do Firebase (firebase.google.com)\n" +
                                   "2. Adicione um app Android com o ID de pacote:\n" +
                                   "   com.aistudio.whatsappclone.*\n" +
                                   "3. Faça o download do arquivo google-services.json\n" +
                                   "4. Adicione o arquivo na pasta 'app' do seu projeto.\n" +
                                   "5. Ative o Firestore Database e Authentication anônimo no console do Firebase.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        Text(
            text = "Preferências Gerais",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF128C7E),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        // General list settings
        ListItem(
            headlineContent = { Text("Conversas") },
            supportingContent = { Text("Papel de parede, histórico de conversas") },
            leadingContent = { Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF8696A0)) }
        )
        ListItem(
            headlineContent = { Text("Notificações") },
            supportingContent = { Text("Sons de mensagens, grupos e chamadas") },
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF8696A0)) }
        )
        ListItem(
            headlineContent = { Text("Armazenamento e Dados") },
            supportingContent = { Text("Uso de rede, download automático") },
            leadingContent = { Icon(Icons.Default.DataUsage, contentDescription = null, tint = Color(0xFF8696A0)) }
        )
        ListItem(
            headlineContent = { Text("Ajuda") },
            supportingContent = { Text("Fale conosco, termos e políticas") },
            leadingContent = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF8696A0)) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        ListItem(
            headlineContent = { Text("Sair da Conta", color = Color.Red, fontWeight = FontWeight.Bold) },
            supportingContent = { Text("Desconectar seu número de telefone deste dispositivo") },
            leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red) },
            modifier = Modifier
                .clickable { viewModel.logoutUser() }
                .testTag("logout_button")
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "from Meta",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentPhone = userPhone,
            currentStatus = userStatus,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, status ->
                showEditProfileDialog = false
                viewModel.updateProfile(name, phone, status)
            }
        )
    }
}

// --- Composable: Chat Detail Screen (Tela de Conversa) ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val activeChatDetails by viewModel.activeChatDetails.collectAsState()
    val activeChatMessages by viewModel.activeChatMessages.collectAsState()
    val typingContactId by viewModel.typingContactId.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    val chatDetails = activeChatDetails ?: return
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var textInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }

    // Scroll to the bottom of message list whenever new messages arrive
    LaunchedEffect(activeChatMessages.size) {
        if (activeChatMessages.isNotEmpty()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(activeChatMessages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Show Info Dialog */ }
                    ) {
                        ContactAvatar(
                            name = chatDetails.title,
                            avatarColorHex = chatDetails.avatarColorHex,
                            size = 38.0
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = chatDetails.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val isTyping = typingContactId != null && (chatId == typingContactId || (chatDetails.isGroup && contacts.any { it.id == typingContactId }))
                            Text(
                                text = if (isTyping) {
                                    val contactName = contacts.find { it.id == typingContactId }?.name ?: "Contato"
                                    if (chatDetails.isGroup) "$contactName digitando..." else "digitando..."
                                } else "online",
                                color = if (isTyping) WhatsappGreenAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = if (isTyping) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.startCall(
                                contactName = chatDetails.title,
                                contactId = chatId,
                                isVideo = true
                            )
                        }
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Chamada de vídeo")
                    }
                    IconButton(
                        onClick = {
                            viewModel.startCall(
                                contactName = chatDetails.title,
                                contactId = chatId,
                                isVideo = false
                            )
                        }
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Chamada de voz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Doodle WhatsApp Wallpapers custom canvas background
            ChatWallpaper(modifier = Modifier.fillMaxSize())

            Column(modifier = Modifier.fillMaxSize()) {
                // Messages container
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                ) {
                    items(activeChatMessages, key = { it.id }) { message ->
                        val isUser = message.senderId == "user"
                        var showMsgOptions by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                        )
                                    )
                                    .background(
                                        if (isUser) {
                                            if (isSystemInDarkTheme()) WhatsappDarkGreenBubble else WhatsappLightGreen
                                        } else {
                                            if (isSystemInDarkTheme()) DarkSurfaceLighter else Color.White
                                        }
                                    )
                                    .combinedClickable(
                                        onClick = { replyingToMessage = message },
                                        onLongClick = { showMsgOptions = true }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Column {
                                    // Reply layout if replying to a message
                                    if (message.replyToId != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.06f))
                                                .height(IntrinsicSize.Min)
                                        ) {
                                            // Left border accent line
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .fillMaxHeight()
                                                    .background(if (isUser) WhatsappTealDark else WhatsappTealLight)
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isUser) "Você" else chatDetails.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (isUser) WhatsappTealDark else WhatsappTealLight
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = message.replyToText ?: "Mensagem original",
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    if (chatDetails.isGroup && !isUser && message.senderId != "system") {
                                        Text(
                                            text = message.senderName,
                                            fontWeight = FontWeight.Bold,
                                            color = WhatsappTealLight,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.widthIn(min = 60.dp)
                                    ) {
                                        Text(
                                            text = message.text,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = formatTime(message.timestamp),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                textAlign = TextAlign.End
                                            )
                                            if (isUser && message.senderId != "system") {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                MessageStatusDoubleTick(status = message.status)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showMsgOptions) {
                            AlertDialog(
                                onDismissRequest = { showMsgOptions = false },
                                title = { Text("Mensagem") },
                                text = { Text("O que você deseja fazer com esta mensagem?") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteMessage(message.id)
                                            showMsgOptions = false
                                        }
                                    ) {
                                        Text("Apagar para Todos", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showMsgOptions = false }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }
                }

                // Input bar area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    // Active reply display card
                    AnimatedVisibility(
                        visible = replyingToMessage != null,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        replyingToMessage?.let { replyMsg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSystemInDarkTheme()) DarkSurfaceLighter else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(WhatsappTealDark)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (replyMsg.senderId == "user") "Você" else chatDetails.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = WhatsappTealDark
                                            )
                                            Text(
                                                text = replyMsg.text,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(onClick = { replyingToMessage = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Text Input Field Styled exactly like WhatsApp bubble
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(26.dp))
                                .background(if (isSystemInDarkTheme()) DarkSurface else Color.White)
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertEmoticon,
                                contentDescription = "Emojis",
                                tint = Color(0xFF8696A0),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = { Text("Mensagem", color = Color(0xFF8696A0)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("message_input_field"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 4
                            )
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Anexar",
                                tint = Color(0xFF8696A0),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Câmera",
                                tint = Color(0xFF8696A0),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Green Circular Action Send/Mic button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WhatsappTealLight)
                                .clickable {
                                    if (textInput.isNotBlank()) {
                                        viewModel.sendMessage(
                                            text = textInput,
                                            replyToId = replyingToMessage?.id,
                                            replyToText = replyingToMessage?.text
                                        )
                                        textInput = ""
                                        replyingToMessage = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (textInput.isBlank()) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Status Stories Viewer (Visualizador de Status) ---

@Composable
fun StatusStoryViewer(
    statuses: List<StatusUpdate>,
    startIndex: Int,
    onDismiss: () -> Unit,
    onStatusViewed: (String) -> Unit
) {
    var currentIndex by remember { mutableStateOf(startIndex) }
    val currentStatus = statuses[currentIndex]

    var progress by remember { mutableStateOf(0f) }
    val durationMs = 5000f
    val tickStepMs = 40L

    // Reset progress and trigger "viewed" callbacks when story changes
    LaunchedEffect(currentIndex) {
        progress = 0f
        onStatusViewed(currentStatus.id)
        
        while (progress < 1f) {
            delay(tickStepMs)
            progress += tickStepMs / durationMs
        }

        // Auto advance or dismiss on story complete
        if (currentIndex < statuses.size - 1) {
            currentIndex += 1
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val bgBrush = remember(currentStatus.backgroundColorHex) {
            val bgColor = try {
                Color(android.graphics.Color.parseColor(currentStatus.backgroundColorHex ?: "#128C7E"))
            } catch (e: Exception) {
                Color(0xFF128C7E)
            }
            Brush.verticalGradient(listOf(bgColor.copy(alpha = 0.95f), bgColor))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Tap right side of screen to skip, left side to go back
                    if (currentIndex < statuses.size - 1) {
                        currentIndex += 1
                    } else {
                        onDismiss()
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, bottom = 24.dp)
            ) {
                // Stories Progress Indicators Bar at the top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in statuses.indices) {
                        val indicatorProgress = when {
                            i < currentIndex -> 1f
                            i == currentIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { indicatorProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Header info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                    ContactAvatar(
                        name = currentStatus.contactName,
                        avatarColorHex = currentStatus.contactColorHex,
                        size = 40.0
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentStatus.contactName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = formatStatusTime(currentStatus.timestamp),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Text status content centered
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStatus.textContent ?: "",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                }

                Text(
                    text = "Deslize para cima para responder",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }
}

// --- Active Call Overlay Composable (Pulsando Voice Call / Video Call) ---

@Composable
fun ActiveCallOverlay(
    viewModel: ChatViewModel
) {
    val contactName by viewModel.activeCallContactName.collectAsState()
    val isVideo by viewModel.activeCallIsVideo.collectAsState()
    val isIncoming by viewModel.activeCallIsIncoming.collectAsState()
    val callState by viewModel.activeCallState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    // Call Background theme
    val bgBrush = remember {
        Brush.verticalGradient(
            listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
    }

    // Pulsating animation scale for voice call avatar background radar waves
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Name and Call State Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = contactName ?: "Desconhecido",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (callState) {
                        "calling" -> "Chamando..."
                        "ringing" -> "Tocando..."
                        "connected" -> "Conectado ($formattedDuration)"
                        else -> "Desconectando..."
                    },
                    color = WhatsappGreenAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Body: Pulsating Radar Circle during Voice Call OR Mock Video layout
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isVideo && callState == "connected") {
                    // Video Call layout preview mockup (Simulating video)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(80.dp)
                        )
                        // Mini Picture-in-picture window for user
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(60.dp, 80.dp)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                        )
                    }
                } else {
                    // Voice Call pulsating radar background
                    if (callState == "ringing" || callState == "connected" || callState == "calling") {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = pulseAlpha))
                        )
                    }
                    ContactAvatar(
                        name = contactName ?: "C",
                        avatarColorHex = "#128C7E",
                        size = 110.0
                    )
                }
            }

            // Bottom Buttons: Answer, Reject, End Call, Mute, Speaker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                if (isIncoming && callState == "ringing") {
                    // Incoming Call Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Answer green button
                        Button(
                            onClick = { viewModel.answerCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreenAccent),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Atender", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Decline red button
                        Button(
                            onClick = { viewModel.rejectOrMissCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Recusar", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                } else {
                    // In-Call Controller Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        IconButton(
                            onClick = { viewModel.toggleMute() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mudo",
                                tint = if (isMuted) Color.Black else Color.White
                            )
                        }

                        // Large red End Call Button
                        Button(
                            onClick = { viewModel.rejectOrMissCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Desligar", tint = Color.White, modifier = Modifier.size(30.dp))
                        }

                        // Speaker button
                        IconButton(
                            onClick = { viewModel.toggleSpeaker() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Alto-falante",
                                tint = if (isSpeakerOn) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Overlay Helper Views & Dialogs ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelectionDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onContactSelected: (String) -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Contatos", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactSelected(contact.id) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactAvatar(
                                name = contact.name,
                                avatarColorHex = contact.avatarColorHex,
                                size = 44.0
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contact.statusText,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 80.dp, end = 20.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    var groupTitle by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<Contact>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Novo Grupo", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar")
                            }
                        },
                        actions = {
                            TextButton(
                                enabled = groupTitle.isNotBlank() && selectedMembers.isNotEmpty(),
                                onClick = {
                                    viewModel.createGroupChat(groupTitle, groupDesc, selectedMembers.toList())
                                    onDismiss()
                                }
                            ) {
                                Text("Criar", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = groupTitle,
                        onValueChange = { groupTitle = it },
                        label = { Text("Nome do Grupo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Descrição (Opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Selecionar Participantes (${selectedMembers.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(contacts, key = { it.id }) { contact ->
                            val isSelected = selectedMembers.contains(contact)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedMembers.remove(contact)
                                        else selectedMembers.add(contact)
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedMembers.remove(contact)
                                        else selectedMembers.add(contact)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                ContactAvatar(
                                    name = contact.name,
                                    avatarColorHex = contact.avatarColorHex,
                                    size = 40.0
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(contact.name, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentPhone: String,
    currentStatus: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf(currentPhone) }
    var status by remember { mutableStateOf(currentStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Seu Nome") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Recado / Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, phone, status) }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun PostStatusDialog(
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val colors = listOf("#128C7E", "#075E54", "#25D366", "#EC407A", "#AB47BC", "#00838F", "#1A237E")
    var selectedColorIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Status de Texto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Digite sua atualização de status...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Column {
                    Text("Selecione uma cor de fundo:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        colors.forEachIndexed { index, colorHex ->
                            val color = remember(colorHex) { Color(android.graphics.Color.parseColor(colorHex)) }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColorIndex == index) 3.dp else 0.dp,
                                        color = if (selectedColorIndex == index) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = index }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onPost(text, colors[selectedColorIndex]) }
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// --- Composable: WhatsappLoginFlow ---
@Composable
fun WhatsappLoginFlow(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) } // 1: Welcome, 2: Phone Input, 3: OTP Verification, 4: Profile Registration
    var countryCode by remember { mutableStateOf("+55") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userStatus by remember { mutableStateOf("No WhatsApp") }
    var selectedAvatarColorIndex by remember { mutableStateOf(0) }
    
    val avatarColors = listOf(
        "#128C7E", // Teal
        "#075E54", // Dark Teal
        "#34B7F1", // Blue
        "#25D366", // Green
        "#E53935", // Red
        "#EC407A", // Pink
        "#8E24AA", // Purple
        "#FF9800", // Orange
        "#000000"  // Black
    )
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isCompleting by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Countdown timer for OTP Screen
    var timerSeconds by remember { mutableStateOf(60) }
    LaunchedEffect(step) {
        if (step == 3) {
            timerSeconds = 60
            while (timerSeconds > 0) {
                delay(1000)
                timerSeconds--
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) Color(0xFF0B141A) else Color.White)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (step) {
            1 -> {
                // STEP 1: WELCOME SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Bem-vindo ao WhatsApp",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF075E54),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        // Custom Drawn High-fidelity WhatsApp Logo
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "WhatsApp Logo",
                                    modifier = Modifier.size(54.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Leia nossa Política de Privacidade. Toque em \"Concordar e continuar\" para aceitar os Termos de Serviço.",
                            fontSize = 13.sp,
                            color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("agree_and_continue_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A884),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Concordar e continuar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            2 -> {
                // STEP 2: PHONE NUMBER INPUT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        IconButton(onClick = { step = 1 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = if (isSystemInDarkTheme()) Color.White else Color(0xFF1F2C34)
                            )
                        }
                        Text(
                            text = "Insira seu número de telefone",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1F2C34),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "O WhatsApp precisará confirmar seu número de telefone celular. Tarifas de SMS podem ser aplicadas.",
                        fontSize = 14.sp,
                        color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Country and Code Selection
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xFF1F2C34) else Color(0xFFF0F2F5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Brasil",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF111B21)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = countryCode,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00A884)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar País",
                                    tint = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Phone Number Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            // Clean digits only and limit size
                            val cleaned = input.filter { it.isDigit() }
                            if (cleaned.length <= 11) {
                                phoneNumber = cleaned
                            }
                        },
                        label = { Text("Número de telefone") },
                        placeholder = { Text("(11) 91234-5678") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Telefone",
                                tint = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A884),
                            focusedLabelColor = Color(0xFF00A884)
                        )
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            if (phoneNumber.length >= 10) {
                                showConfirmDialog = true
                            }
                        },
                        enabled = phoneNumber.length >= 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("next_step_phone_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A884),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF00A884).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Avançar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Confirm Phone Dialog
                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("O número está correto?") },
                        text = {
                            Text("Enviaremos um SMS para confirmar o número:\n\n$countryCode $phoneNumber\n\nDeseja continuar ou editar o número?")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showConfirmDialog = false
                                    step = 3
                                }
                            ) {
                                Text("OK", color = Color(0xFF00A884), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("EDITAR", color = Color(0xFF00A884))
                            }
                        }
                    )
                }
            }
            3 -> {
                // STEP 3: OTP VERIFICATION
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        IconButton(onClick = { step = 2 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = if (isSystemInDarkTheme()) Color.White else Color(0xFF1F2C34)
                            )
                        }
                        Text(
                            text = "Confirmando seu número",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1F2C34),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Enviamos um SMS com um código de confirmação de 6 dígitos para o número $countryCode $phoneNumber. Insira-o abaixo.",
                        fontSize = 14.sp,
                        color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // OTP digit boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (0..5).forEach { index ->
                            val digit = otpCode.getOrNull(index)?.toString() ?: ""
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSystemInDarkTheme()) Color(0xFF1F2C34) else Color(0xFFF0F2F5)
                                    )
                                    .border(
                                        width = if (otpCode.length == index) 2.dp else 1.dp,
                                        color = when {
                                            otpError -> Color.Red
                                            otpCode.length == index -> Color(0xFF00A884)
                                            else -> if (isSystemInDarkTheme()) Color(0xFF2C3E50) else Color(0xFFD1D7DB)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        // Trigger typing easily
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSystemInDarkTheme()) Color.White else Color(0xFF111B21)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Styled field to input the OTP code
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { input ->
                            val cleaned = input.filter { it.isDigit() }
                            if (cleaned.length <= 6) {
                                otpCode = cleaned
                                otpError = false
                                
                                if (cleaned.length == 6) {
                                    // Start verification animation
                                    coroutineScope.launch {
                                        isVerifying = true
                                        delay(1500) // Simulated Verification Delay
                                        isVerifying = false
                                        step = 4
                                    }
                                }
                            }
                        },
                        label = { Text("Código de 6 dígitos") },
                        placeholder = { Text("000000") },
                        modifier = Modifier
                            .width(200.dp)
                            .testTag("otp_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A884),
                            focusedLabelColor = Color(0xFF00A884)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isVerifying) {
                        CircularProgressIndicator(
                            color = Color(0xFF00A884),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Verificando...",
                            color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781),
                            fontSize = 14.sp
                        )
                    } else {
                        // Countdown Timer / Resend Button
                        if (timerSeconds > 0) {
                            Text(
                                text = "Aguarde ${timerSeconds}s para reenviar o código",
                                color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781),
                                fontSize = 14.sp
                            )
                        } else {
                            Button(
                                onClick = {
                                    timerSeconds = 60
                                    otpCode = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color(0xFF00A884)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reenviar",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reenviar SMS", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            4 -> {
                // STEP 4: PROFILE SETUP
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dados do Perfil",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1F2C34),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Por favor, insira o seu nome de usuário e personalize o seu avatar para que outras pessoas vejam você.",
                        fontSize = 14.sp,
                        color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Interactive Avatar customization
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContactAvatar(
                            name = if (userName.isBlank()) "V" else userName,
                            avatarColorHex = avatarColors[selectedAvatarColorIndex],
                            size = 110.0
                        )
                        
                        // Edit pen badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00A884))
                                .align(Alignment.BottomEnd)
                                .border(2.dp, if (isSystemInDarkTheme()) Color(0xFF0B141A) else Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Avatar",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Avatar Color Selector
                    Text(
                        text = "Escolha a cor do seu perfil:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        avatarColors.take(5).forEachIndexed { index, hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedAvatarColorIndex == index) 3.dp else 0.dp,
                                        color = if (selectedAvatarColorIndex == index) (if (isSystemInDarkTheme()) Color.White else Color.Black) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatarColorIndex = index }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        avatarColors.drop(5).forEachIndexed { dropIndex, hex ->
                            val index = dropIndex + 5
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedAvatarColorIndex == index) 3.dp else 0.dp,
                                        color = if (selectedAvatarColorIndex == index) (if (isSystemInDarkTheme()) Color.White else Color.Black) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatarColorIndex = index }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // User Name Input
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Nome (visível para contatos)") },
                        placeholder = { Text("Ex: João Silva") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Nome",
                                tint = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A884),
                            focusedLabelColor = Color(0xFF00A884)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // User Status Input
                    OutlinedTextField(
                        value = userStatus,
                        onValueChange = { userStatus = it },
                        label = { Text("Recado / Status") },
                        placeholder = { Text("Ex: No WhatsApp") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Status",
                                tint = if (isSystemInDarkTheme()) Color(0xFF8696A0) else Color(0xFF667781)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A884),
                            focusedLabelColor = Color(0xFF00A884)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    if (isCompleting) {
                        CircularProgressIndicator(color = Color(0xFF00A884))
                    } else {
                        Button(
                            onClick = {
                                if (userName.isNotBlank()) {
                                    coroutineScope.launch {
                                        isCompleting = true
                                        delay(1200) // Simulated Profile Initialization
                                        isCompleting = false
                                        viewModel.registerUser(
                                            name = userName.trim(),
                                            phone = "$countryCode $phoneNumber",
                                            status = userStatus.trim(),
                                            avatarColorHex = avatarColors[selectedAvatarColorIndex]
                                        )
                                    }
                                }
                            },
                            enabled = userName.isNotBlank() && !isCompleting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("finish_registration_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A884),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF00A884).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = "Começar a Conversar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
