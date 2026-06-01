package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.JobApplication
import com.example.data.RawEmail
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecruitmentApp(viewModel: RecruitmentViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val adminEmail by viewModel.adminEmail.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(targetState = isLoggedIn, label = "ScreenTransition") { loggedIn ->
            if (loggedIn) {
                MainRecruitmentPortal(viewModel = viewModel, adminEmail = adminEmail)
            } else {
                RecruitmentLoginScreen(viewModel = viewModel)
            }
        }
    }
}

// ---------------- AUTHENTICATION SCREEN ----------------
@Composable
fun RecruitmentLoginScreen(viewModel: RecruitmentViewModel) {
    var authTabState by remember { mutableIntStateOf(0) } // 0 for Login/Connect, 1 for Save/Register
    
    var emailInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regStatusText by remember { mutableStateOf<String?>(null) }
    var isRegSuccess by remember { mutableStateOf(false) }

    val tokenNotif by viewModel.generatedTokenNotification.collectAsStateWithLifecycle()
    val accounts by viewModel.registeredAccounts.collectAsStateWithLifecycle()

    // Dialog showing the sent security token
    if (tokenNotif != null) {
        val extractedToken = tokenNotif!!.substringAfterLast(": ").trim()
        AlertDialog(
            onDismissRequest = { viewModel.clearTokenNotification() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Token Dispatched",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Security Access Token Sent", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column {
                    Text(
                        text = "A simulated security access token has been sent to your administrative email mailbox address.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ACCESS CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = extractedToken,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Copy this code and enter it into the 'Security Access Token' input field on the Connect tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tokenInput = extractedToken
                        if (regEmail.isNotEmpty()) {
                            emailInput = regEmail
                        }
                        viewModel.clearTokenNotification()
                        authTabState = 0
                    }
                ) {
                    Text("Copy & Connect")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .testTag("login_card"),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Admin Shield / Mail icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Admin Security Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AI Recruiter Portal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Secure Admin Mailbox Synchronizer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Beautiful custom Tab layout for navigation between login and register
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    listOf("Connect Mailbox", "Register Mailbox").forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (authTabState == index) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable {
                                    authTabState = index
                                    errorText = null
                                    regStatusText = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (authTabState == index) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (authTabState == 0) {
                    // TAB 0: LOGIN & SECURITY ACCESS TOKEN CONNECT
                    
                    // Quick profiles list of available accounts
                    Text(
                        text = "Active Profiles (Select to Autofill):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        accounts.keys.forEach { email ->
                            val label = when (email) {
                                "recruit@techcorp.com" -> "TechCorp (Enterprise)"
                                "careers@innovate.ai" -> "Innovate AI (AI Lab)"
                                "hiring@futuretech.com" -> "FutureTech (Scaleup)"
                                else -> "Custom (Self Registered)"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (emailInput.lowercase() == email.lowercase()) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable {
                                        emailInput = email
                                        val token = accounts[email]?.securityToken ?: ""
                                        tokenInput = token
                                        errorText = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Mailbox",
                                    tint = if (emailInput.lowercase() == email.lowercase()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            errorText = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        label = { Text("Recruitment Email Address") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, "Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            errorText = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        label = { Text("Security Access Token") },
                        leadingIcon = { Icon(Icons.Default.Lock, "Security Access Token") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Request token shortcut for selected/entered email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Need a security token?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(
                            onClick = {
                                if (emailInput.isBlank()) {
                                    errorText = "Enter or select an email first to receive a code."
                                } else {
                                    val token = viewModel.sendSecurityToken(emailInput)
                                    if (token == null) {
                                        errorText = "This email is not registered yet. Switch to 'Register Mailbox' to save it first!"
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Request Token")
                        }
                    }

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (emailInput.isBlank() || !emailInput.contains("@")) {
                                errorText = "Please enter a valid administrative mailbox address."
                            } else if (tokenInput.isBlank()) {
                                errorText = "Please enter your Security Access Token. You can request one above."
                            } else {
                                val success = viewModel.login(emailInput, tokenInput)
                                if (!success) {
                                    errorText = "Verification failed. Check token or register this mailbox."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Lock Open")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synchronize & Connect", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    // TAB 1: REGISTER NEW MAILBOX / SAVE PASSWORD
                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = {
                            regEmail = it
                            regStatusText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Set Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, "Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = {
                            regPassword = it
                            regStatusText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Set Mailbox Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (regStatusText != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRegSuccess) MaterialTheme.colorScheme.primaryContainer 
                                                 else MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRegSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isRegSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = regStatusText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isRegSuccess) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (regEmail.isBlank() || !regEmail.contains("@")) {
                                    isRegSuccess = false
                                    regStatusText = "Please enter a valid administrative email."
                                } else if (regPassword.isBlank()) {
                                    isRegSuccess = false
                                    regStatusText = "Please enter a secure password."
                                } else {
                                    val saved = viewModel.registerAccount(regEmail, regPassword)
                                    if (saved) {
                                        isRegSuccess = true
                                        regStatusText = "Credentials saved securely. Now send security token below!"
                                    } else {
                                        isRegSuccess = false
                                        regStatusText = "Failed to save. Review inputs."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Account")
                        }

                        ElevatedButton(
                            onClick = {
                                val clean = regEmail.lowercase().trim()
                                val isCreated = accounts.containsKey(clean)
                                if (!isCreated) {
                                    isRegSuccess = false
                                    regStatusText = "Save the account above first before requesting a Token."
                                } else {
                                    viewModel.sendSecurityToken(clean)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Token")
                        }
                    }
                }
            }
        }
    }
}

// ---------------- MAIN RECRUITMENT PORTAL DASHBOARD ----------------
@Composable
fun MainRecruitmentPortal(viewModel: RecruitmentViewModel, adminEmail: String) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncText by viewModel.syncProgressText.collectAsStateWithLifecycle()
    
    val tabs = listOf(
        "📬 Inbox Sync" to Icons.Default.Email,
        "📂 Categories" to Icons.Default.List,
        "📊 Analytics" to Icons.Default.Info,
        "📨 Outbox" to Icons.Default.CheckCircle
    )

    Scaffold(
        topBar = {
            Column {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI Recruitment Manager",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Live Gemini AI",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.resetSystemState() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, "Reset DB", tint = MaterialTheme.colorScheme.outline)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.logout() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountCircle, "Admin Avatar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = adminEmail.split("@").firstOrNull() ?: "HR Admin",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Linear syncing progress overlay
                if (isSyncing) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 4.dp, horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (syncText.isNotEmpty()) syncText else "Connecting to server...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label.split(" ").last()) },
                        icon = { Icon(icon, contentDescription = label) }
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
            when (selectedTab) {
                0 -> InboxSyncTab(viewModel = viewModel)
                1 -> CategoriesTab(viewModel = viewModel)
                2 -> AnalyticsTab(viewModel = viewModel)
                3 -> LogsAndOutboxTab(viewModel = viewModel)
            }
        }
    }
}

// ---------------- TAB 1: MAILBOX SYNCHRONIZER ----------------
@Composable
fun InboxSyncTab(viewModel: RecruitmentViewModel) {
    val inboxEmails by viewModel.inboxEmails.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val scrollLogs by viewModel.syncStatusLog.collectAsStateWithLifecycle()

    var showManualDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sync overview Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val unsyncedCount = inboxEmails.count { !it.isSynced }
                            Text(
                                text = "Admin Mailbox Synchronizer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$unsyncedCount job application emails waiting to sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Sync Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncAllInbox() },
                            enabled = !isSyncing && inboxEmails.any { !it.isSynced },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, "Bulk Sync")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync All Inbox")
                        }

                        OutlinedButton(
                            onClick = { showManualDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, "Add Candidate")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate")
                        }
                    }
                }
            }
        }

        // Active Synchronization Raw Lists
        item {
            Text(
                text = "Live Candidate Mailbox",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (inboxEmails.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No emails available in mailbox",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(inboxEmails, key = { it.id }) { email ->
                RawEmailItem(
                    email = email,
                    isSyncing = isSyncing,
                    onSyncClick = { viewModel.syncIndividualEmail(email.id) }
                )
            }
        }

        // Mini status log panel inside Inbox view
        if (scrollLogs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "AI Live Classification Diagnostics:",
                            color = Color.Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.height(80.dp)) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                scrollLogs.forEach { log ->
                                    Text(
                                        text = log,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        SimulateCandidateDialog(
            onDismiss = { showManualDialog = false },
            onSubmit = { name, email, subject, body ->
                viewModel.processManualCandidate(name, email, subject, body)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun RawEmailItem(
    email: RawEmail,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    val dateString = remember(email.timestamp) {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(email.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (email.isSynced) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (!email.isSynced) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (email.isSynced) Color.LightGray else MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = email.senderName.take(1).uppercase(Locale.ROOT),
                            fontWeight = FontWeight.Bold,
                            color = if (email.isSynced) Color.DarkGray else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = email.senderName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (email.isSynced) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = email.senderEmail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = email.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (email.isSynced) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (email.isSynced) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Synced Check",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(horizontal = 2.dp)
                        )
                        Text(
                            "AI Categorized",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onSyncClick,
                        enabled = !isSyncing,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Analyze email logo",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Sync",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------- TAB 2: DOMAIN CATEGORIZED VIEW ----------------
@Composable
fun CategoriesTab(viewModel: RecruitmentViewModel) {
    val applications by viewModel.allApplications.collectAsStateWithLifecycle()
    val inboxEmails by viewModel.inboxEmails.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    
    // Domain taxonomy list
    val domains = listOf(
        "Software Development",
        "Full Stack Development",
        "Artificial Intelligence & ML",
        "Data Science",
        "Cybersecurity",
        "UI/UX Design",
        "Cloud Computing",
        "DevOps",
        "Testing & QA",
        "Other Domains",
        "Spam"
    )

    var selectedDomain by remember { mutableStateOf<String?>(null) }
    var selectedCandidate by remember { mutableStateOf<JobApplication?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedDomain == null) {
            // One-Click AI Sync All prominent banner (USP)
            val unsyncedCount = inboxEmails.count { !it.isSynced }
            if (unsyncedCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Unsynced Inbound Emails (${unsyncedCount})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Single-click AI extraction and folder routing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { viewModel.syncAllInbox() },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Sync All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = "Recruitment Folders (Domains)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(domains) { domain ->
                    val domainApps = applications.filter { it.domain == domain }
                    val appCount = domainApps.size
                    val avgScore = if (appCount > 0) domainApps.map { it.matchScore }.average().toInt() else 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDomain = domain },
                        colors = CardDefaults.cardColors(
                            containerColor = if (domain == "Spam") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (domain == "Spam") CardDefaults.outlinedCardBorder() else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (domain == "Spam") MaterialTheme.colorScheme.errorContainer
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (domain == "Spam") Icons.Default.Warning
                                        else if (domain == "UI/UX Design") Icons.Default.Star
                                        else Icons.Default.List,
                                        contentDescription = "Folder logo",
                                        tint = if (domain == "Spam") MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = domain,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$appCount " + (if (appCount == 1) "Application" else "Applications"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (appCount > 0 && domain != "Spam") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (avgScore >= 85) Color(0xFFE8F5E9)
                                            else if (avgScore >= 60) Color(0xFFFFF3E0)
                                            else Color(0xFFFFEBEE)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Avg: $avgScore%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (avgScore >= 85) Color(0xFF2E7D32)
                                        else if (avgScore >= 60) Color(0xFFE65100)
                                        else Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Viewing sub-folder list of candidates
            val activeDomainApps = applications.filter { it.domain == selectedDomain }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedDomain = null }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back To Folders"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = selectedDomain!!,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${activeDomainApps.size} candidates matched",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (activeDomainApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No synced applications in this folder yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeDomainApps) { candidate ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCandidate = candidate },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = candidate.candidateName.take(1).uppercase(Locale.ROOT),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = candidate.candidateName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = candidate.candidateEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (candidate.domain != "Spam") {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, "Score Star", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${candidate.matchScore}%",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedCandidate != null) {
        CandidateProfileDialog(
            app = selectedCandidate!!,
            viewModel = viewModel,
            onDismiss = { selectedCandidate = null },
            onDelete = {
                viewModel.deleteCandidate(selectedCandidate!!)
                selectedCandidate = null
            }
        )
    }
}

// ---------------- TAB 3: ANALYTICS DASHBOARD ----------------
@Composable
fun InteractiveDonutChart(
    slices: List<Pair<String, Float>>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = slices.map { it.second }.sum()
    if (total == 0f) return
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(96.dp)
        ) {
            var startAngle = -90f
            slices.forEachIndexed { idx, slice ->
                val sweep = (slice.second / total) * 360f
                drawArc(
                    color = colors[idx % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 24f)
                )
                startAngle += sweep
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            slices.forEachIndexed { idx, slice ->
                val percent = (slice.second / total * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[idx % colors.size], CircleShape)
                    )
                    Text(
                        text = "${slice.first}: ${slice.second.toInt()} ($percent%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun HiringFunnelWidget(
    stages: List<Triple<String, Int, Color>>
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val maxVal = stages.firstOrNull()?.second ?: 1
        stages.forEachIndexed { index, (stageName, count, color) ->
            val scaleFraction = 1f - (index * 0.12f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stageName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(130.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(scaleFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.12f))
                    )
                    
                    val fillFraction = if (maxVal > 0) count.toFloat() / maxVal else 0f
                    if (count > 0 && fillFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(scaleFraction * fillFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                                .padding(end = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "$count",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Text(
                            text = "0",
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(viewModel: RecruitmentViewModel) {
    val applications by viewModel.allApplications.collectAsStateWithLifecycle()
    val inboxEmails by viewModel.inboxEmails.collectAsStateWithLifecycle()

    val totalApps = applications.size
    val totalValidApps = applications.count { !it.isSpam }
    val totalSpam = applications.count { it.isSpam }
    val avgScore = if (totalValidApps > 0) applications.filter { !it.isSpam }.map { it.matchScore }.average().toInt() else 0
    
    val countReviewed = applications.count { !it.isSpam && it.trackingStatus != "Received" }
    val countShortlisted = applications.count { !it.isSpam && (it.trackingStatus == "Shortlisted" || it.trackingStatus == "Offer") }
    val countOffer = applications.count { !it.isSpam && it.trackingStatus == "Offer" }

    val totalMinutesSaved = totalApps * 15
    val hoursSavedStr = String.format("%.1f hrs", totalMinutesSaved.toFloat() / 60)

    val domains = listOf(
        "Software Development",
        "Full Stack Development",
        "Artificial Intelligence & ML",
        "Data Science",
        "Cybersecurity",
        "UI/UX Design",
        "Cloud Computing",
        "DevOps",
        "Testing & QA",
        "Other Domains"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "ApexCharts Analytics Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Real-time hiring funnel metrics, AI accuracy, and operations overview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Summary Statistics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Screened Apps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalValidApps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("HR Time Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(hoursSavedStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Inbound Spam", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalSpam", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ⭐ HIRING FUNNEL VISUALIZATION (USP)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "⭐ Hiring Pipeline Funnel",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "End-to-end recruitment conversion stages.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(Icons.Default.Star, "Funnel Star", tint = Color(0xFFFFB300))
                    }

                    HiringFunnelWidget(
                        stages = listOf(
                            Triple("1. Inbound Received", totalValidApps, MaterialTheme.colorScheme.primary),
                            Triple("2. HR Reviewed", countReviewed, MaterialTheme.colorScheme.secondary),
                            Triple("3. Shortlisted", countShortlisted, MaterialTheme.colorScheme.tertiary),
                            Triple("4. Offers Extended", countOffer, Color(0xFF2E7D32))
                        )
                    )
                }
            }
        }

        // ⭐ INTERACTIVE SPAM DETECTION & FILTERING DONUT CHART (USP)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📬 Spam Filter & Inflow Integrity",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "AI spam classification and message distribution metrics.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    InteractiveDonutChart(
                        slices = listOf(
                            "Verified Applications" to totalValidApps.toFloat(),
                            "Filtered Junk & Spam" to totalSpam.toFloat()
                        ),
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }

        // KPI: Average Match Score Rating
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Average AI Fit Rating", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$avgScore%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, "Score Check", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Domain Distribution Progress Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📂 Domain Routing Overview",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val maxVal = domains.map { domain -> applications.count { it.domain == domain } }.maxOrNull() ?: 1
                    val scaleMax = if (maxVal == 0) 1 else maxVal

                    domains.forEach { domain ->
                        val actualCount = applications.count { it.domain == domain }
                        val fraction = actualCount.toFloat() / scaleMax

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(domain, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$actualCount", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(if (fraction > 0f) fraction else 0.01f)
                                        .background(
                                            if (fraction > 0.6f) MaterialTheme.colorScheme.primary
                                            else if (fraction > 0.3f) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.tertiary
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ⭐ OPENAI-POWERED RECRUITMENT INTELLIGENCE SEC (USP)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseOnSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, "AI Intelligence", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "OpenAI-Powered Recruitment Intelligence",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val topDomain = domains.maxByOrNull { d -> applications.count { it.domain == d } } ?: "N/A"
                    val syncRatio = if (inboxEmails.isNotEmpty()) (inboxEmails.count { it.isSynced }.toFloat() / inboxEmails.size * 100).toInt() else 0
                    
                    Text(
                        text = "• Top incoming technical demand matches: '$topDomain'.\n" +
                               "• Automatic mail extraction and system routing is complete at $syncRatio% parity.\n" +
                               "• Repetitive manual HR drafting reduced by: ${totalApps * 2} emails fully drafted and sent.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ---------------- TAB 4: OUTBOX auto replies / AUDIT LOGS / AUTOMATION RULES ----------------
@Composable
fun LogsAndOutboxTab(viewModel: RecruitmentViewModel) {
    val applications by viewModel.allApplications.collectAsStateWithLifecycle()
    val scrollLogs by viewModel.syncStatusLog.collectAsStateWithLifecycle()
    val rules by viewModel.automationRules.collectAsStateWithLifecycle()

    var outboxMode by remember { mutableStateOf(0) } // 0: Outbox, 1: Audit Logs, 2: Automation Rules

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Segmented header menu for outbox/operations (SaaS style)
        Text(
            text = when (outboxMode) {
                0 -> "Professional Auto-Reply Outbox"
                1 -> "AI Diagnostics Audit Logs"
                else -> "Recruitment Workflow Automation"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf("📬 Outbox", "📋 Audit Logs", "⚙️ Rules")
            modes.forEachIndexed { idx, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (outboxMode == idx) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { outboxMode = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (outboxMode == idx) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (outboxMode == 1) {
            // Display diagnostics console
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "SYSTEM CLASSIFICATION LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color.Green, CircleShape)
                        )
                    }
                    Divider(color = Color.Green, modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    if (scrollLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No runtime logs. Complete a sync operation.",
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(scrollLogs) { log ->
                                Text(
                                    text = log,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else if (outboxMode == 2) {
            // ⭐ WORKFLOW AUTOMATION RULES WIDGET (USP)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "⚙️ Server-Side Automation Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Toggle active workspace rules to auto-pilot recruiter workflows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    val ruleDef = listOf(
                        Triple("autoResponse", "Inbound Auto-Acknowledgement", "Automatically respond with thank-you draft confirming receipt once a candidate resume is parsed."),
                        Triple("autoCategory", "AI Skill & Domain Categorization", "Analyze experience details and route candidate profiles to appropriate job role folders."),
                        Triple("autoShortlist", "Smart Shortlist Automation", "Dispatch secondary technical assessment link drafts when a candidate gets Shortlisted."),
                        Triple("spamFilter", "Heuristic Spam Filtration & Shunting", "Auto-filter soliciting requests (empty drafts, promotions, sales) to the Spam folder.")
                    )

                    items(ruleDef) { (ruleId, ruleName, ruleDesc) ->
                        val isActive = rules[ruleId] ?: true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = ruleName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ruleDesc,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = isActive,
                                onCheckedChange = { viewModel.toggleAutomationRule(ruleId) }
                            )
                        }
                    }
                }
            }
        } else {
            // Display Auto-reply outbox
            val logs = applications.filter { it.isResponseSent }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Email, "Outbox Info", tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No auto-replies dispatched from outbox yet.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        val timeString = remember(log.responseSentTimestamp) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.responseSentTimestamp))
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(0xFFE8F5E9), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Success tick",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "To: " + log.candidateName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = log.candidateEmail,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Text(
                                        text = timeString,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = log.autoResponse,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (log.isSpam) MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (log.isSpam) "Spam Filtered" else log.domain,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (log.isSpam) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Text(
                                        "Status: SENT",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DIALOG 1: SIMULATED CANDIDATE SUBMITTER ----------------
@Composable
fun SimulateCandidateDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, subject: String, body: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Simulate Custom Inbound Resume",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Directly trigger pipeline NLP analysis with custom email structures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Candidate Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Sender Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Email Subject Line") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Email Proposal Body & Resume") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank() && subject.isNotBlank() && body.isNotBlank()) {
                                onSubmit(name, email, subject, body)
                            }
                        },
                        enabled = name.isNotBlank() && email.isNotBlank() && subject.isNotBlank() && body.isNotBlank()
                    ) {
                        Text("Submit Draft")
                    }
                }
            }
        }
    }
}

// ---------------- DIALOG 2: CANDIDATE PROFILE REVIEW ----------------
@Composable
fun CandidateProfileDialog(
    app: JobApplication,
    viewModel: RecruitmentViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header profile card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.candidateName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = app.appliedRole,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = app.candidateEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (!app.isSpam) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, "Score Star", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${app.matchScore}% Match",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Extracted contact credentials row (Resume Info Extraction USP)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(app.phone, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBox, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("LinkedIn Profile", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Build, null, modifier = Modifier.size(14.dp), tint = Color.DarkGray)
                            Text("GitHub Repo", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Application Report Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    // ⭐ ONE-CLICK CANDIDATE TRACKING TIMELINE WORKFLOW (USP)
                    item {
                        Text(
                            "⭐ ONE-CLICK CANDIDATE PIPELINE TRACKING", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 10.sp, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val workflowStates = listOf("Received", "Reviewed", "Shortlisted", "Offer", "Rejected")
                            workflowStates.forEach { st ->
                                val scoreColor = when (st) {
                                    "Shortlisted" -> Color(0xFF2E7D32)
                                    "Offer" -> Color(0xFFFFB300)
                                    "Rejected" -> Color(0xFFC62828)
                                    "Reviewed" -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                                val stSelected = app.trackingStatus == st
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (stSelected) scoreColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .clickable { viewModel.updateCandidateStatus(app, st) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = st,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (stSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("AI CLASSIFICATION REPORT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Routed Folder: " + app.domain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(app.classificationReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (!app.isSpam) {
                        item {
                            Text("EXTRACTED SKILLS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                app.keySkills.split(",").forEach { skill ->
                                    if (skill.trim().isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(skill.trim(), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("AI RESUME PROFILE SUMMARY", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(app.resumeSummary, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    item {
                        Text("AUTOMATIC ACKNOWLEDGEMENT SENT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(app.autoResponse, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onDelete,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, "Delete Profile")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.toggleSpamStatus(app)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (app.isSpam) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, if (app.isSpam) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = if (app.isSpam) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = "Spam toggle",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (app.isSpam) "Restore Candidate" else "Flag as Spam", 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(onClick = onDismiss) {
                        Text("Close Profile")
                    }
                }
            }
        }
    }
}
