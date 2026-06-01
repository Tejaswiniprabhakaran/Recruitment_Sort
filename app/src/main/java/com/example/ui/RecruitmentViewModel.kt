package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecruitmentViewModel(private val repository: JobApplicationRepository) : ViewModel() {

    data class RegisteredAccount(
        val email: String,
        val password: String,
        val securityToken: String? = null
    )

    private val _registeredAccounts = MutableStateFlow<Map<String, RegisteredAccount>>(
        mapOf(
            "recruit@techcorp.com" to RegisteredAccount("recruit@techcorp.com", "password123", "123456"),
            "careers@innovate.ai" to RegisteredAccount("careers@innovate.ai", "ai_innovate", "789012"),
            "hiring@futuretech.com" to RegisteredAccount("hiring@futuretech.com", "future_tech", "456789")
        )
    )
    val registeredAccounts: StateFlow<Map<String, RegisteredAccount>> = _registeredAccounts.asStateFlow()

    private val _generatedTokenNotification = MutableStateFlow<String?>(null)
    val generatedTokenNotification: StateFlow<String?> = _generatedTokenNotification.asStateFlow()

    // Login system simulation states
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _adminEmail = MutableStateFlow("")
    val adminEmail: StateFlow<String> = _adminEmail.asStateFlow()

    val adminPassword = MutableStateFlow("")

    val availableAdminProfiles = listOf(
        "recruit@techcorp.com" to "TechCorp (Enterprise)",
        "careers@innovate.ai" to "Innovate AI (AI Lab)",
        "hiring@futuretech.com" to "FutureTech (Scaleup)"
    )

    // List of mock recruitment emails in the inbox waiting for synchronization
    private val _inboxEmails = MutableStateFlow<List<RawEmail>>(emptyList())
    val inboxEmails: StateFlow<List<RawEmail>> = _inboxEmails.asStateFlow()

    // Syncing status
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgressText = MutableStateFlow("")
    val syncProgressText: StateFlow<String> = _syncProgressText.asStateFlow()

    private val _syncStatusLog = MutableStateFlow<List<String>>(emptyList())
    val syncStatusLog: StateFlow<List<String>> = _syncStatusLog.asStateFlow()

    // Database backed list of parsed candidates
    val allApplications: StateFlow<List<JobApplication>> = repository.allApplications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Prepare initial inbox
        _inboxEmails.value = repository.sampleInboxEmails
    }

    // Register a custom administrator account with email and password
    fun registerAccount(email: String, password: String): Boolean {
        val cleanEmail = email.lowercase().trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || password.isEmpty()) {
            return false
        }
        val current = _registeredAccounts.value.toMutableMap()
        current[cleanEmail] = RegisteredAccount(cleanEmail, password)
        _registeredAccounts.value = current
        addLog("Successfully saved credentials for admin mailbox: $cleanEmail")
        return true
    }

    // Generate and "Send" a security access token to their mailbox
    fun sendSecurityToken(email: String): String? {
        val cleanEmail = email.lowercase().trim()
        val account = _registeredAccounts.value[cleanEmail] ?: return null
        val token = (100000..999999).random().toString()
        
        val current = _registeredAccounts.value.toMutableMap()
        current[cleanEmail] = account.copy(securityToken = token)
        _registeredAccounts.value = current

        _generatedTokenNotification.value = "Security Access Token for '$cleanEmail' has been sent. Your token is: $token"
        addLog("Security verification token dispatched successfully to $cleanEmail: $token")
        return token
    }

    fun clearTokenNotification() {
        _generatedTokenNotification.value = null
    }

    // Verify password & security token then login
    fun login(email: String, token: String): Boolean {
        val cleanEmail = email.lowercase().trim()
        val account = _registeredAccounts.value[cleanEmail]
        if (account != null && account.securityToken == token) {
            _adminEmail.value = cleanEmail
            _isLoggedIn.value = true
            addLog("Admin session initialized successfully for $cleanEmail.")
            return true
        }
        return false
    }

    fun logout() {
        _isLoggedIn.value = false
        _adminEmail.value = ""
        addLog("Admin logged out.")
    }

    // Sync an individual email with Gemini AI
    fun syncIndividualEmail(emailId: String) {
        viewModelScope.launch {
            val email = _inboxEmails.value.find { it.id == emailId } ?: return@launch
            if (email.isSynced) return@launch

            _isSyncing.value = true
            _syncProgressText.value = "Analyzing '${email.subject}'..."
            addLog("Starting classification for ${email.senderName} (${email.senderEmail})...")

            try {
                repository.analyzeAndSyncEmail(email)
                // Mark in local list
                _inboxEmails.value = _inboxEmails.value.map {
                    if (it.id == emailId) it.copy(isSynced = true) else it
                }
                addLog("Successfully classified & auto-responded to ${email.senderName}!")
            } catch (e: Exception) {
                addLog("Failed syncing email: ${e.localizedMessage}")
            } finally {
                _isSyncing.value = false
                _syncProgressText.value = ""
            }
        }
    }

    // Sync entire inbox sequentially
    fun syncAllInbox() {
        viewModelScope.launch {
            val unsynced = _inboxEmails.value.filter { !it.isSynced }
            if (unsynced.isEmpty()) {
                addLog("Inbox is already fully synchronized!")
                return@launch
            }

            _isSyncing.value = true
            addLog("Bulk synchronization triggered for ${unsynced.size} emails...")

            unsynced.forEachIndexed { index, email ->
                _syncProgressText.value = "Syncing ${index + 1}/${unsynced.size}: ${email.senderName}..."
                addLog("Processing email ${index + 1} of ${unsynced.size}: ${email.subject}")
                
                try {
                    repository.analyzeAndSyncEmail(email)
                    _inboxEmails.value = _inboxEmails.value.map {
                        if (it.id == email.id) it.copy(isSynced = true) else it
                    }
                } catch (e: Exception) {
                    addLog("Error processing email: ${e.localizedMessage}")
                }
            }

            addLog("Bulk inbox synchronization finished. All categories updated.")
            _isSyncing.value = false
            _syncProgressText.value = ""
        }
    }

    // Add manual custom candidate for live Gemini parsing testing
    fun processManualCandidate(name: String, email: String, subject: String, body: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgressText.value = "Parsing manual application: $name..."
            addLog("Parsing custom candidate input for $name...")

            try {
                repository.insertManualApplication(name, email, subject, body)
                addLog("Custom candidate $name processed, classified, and auto-notified successfully.")
            } catch (e: Exception) {
                addLog("Failed parsing custom candidate: ${e.localizedMessage}")
            } finally {
                _isSyncing.value = false
                _syncProgressText.value = ""
            }
        }
    }

    // Clean or reset DB
    fun resetSystemState() {
        viewModelScope.launch {
            repository.clearAll()
            // Mark all raw emails as unsynced
            _inboxEmails.value = repository.sampleInboxEmails.map { it.copy(isSynced = false) }
            _syncStatusLog.value = emptyList()
            addLog("System database has been reset. Standard mailbox reloaded.")
        }
    }

    // Helper to log activities
    private fun addLog(message: String) {
        val timeLabel = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _syncStatusLog.value = listOf("[$timeLabel] $message") + _syncStatusLog.value
    }

    // Automation Rules Settings for Workflow USP
    private val _automationRules = MutableStateFlow<Map<String, Boolean>>(
        mapOf(
            "autoResponse" to true,
            "autoCategory" to true,
            "autoShortlist" to true,
            "spamFilter" to true
        )
    )
    val automationRules: StateFlow<Map<String, Boolean>> = _automationRules.asStateFlow()

    fun toggleAutomationRule(ruleId: String) {
        val current = _automationRules.value.toMutableMap()
        current[ruleId] = !(current[ruleId] ?: true)
        _automationRules.value = current
        addLog("Automation Rule '${ruleId}' toggled: " + if(current[ruleId] == true) "ENABLED" else "DISABLED")
    }

    // Toggle Spam / Candidate Restore (Spam Filtering USP)
    fun toggleSpamStatus(app: JobApplication) {
        viewModelScope.launch {
            val nextSpam = !app.isSpam
            val nextDomain = if (nextSpam) "Spam" else {
                if (app.domain == "Spam") "Software Development" else app.domain
            }
            val updated = app.copy(
                isSpam = nextSpam,
                domain = nextDomain,
                classificationReason = if (nextSpam) "Manually flagged as unsolicited spam." else "Manually restored as a valid candidate."
            )
            repository.update(updated)
            addLog("Toggled Spam status of ${app.candidateName}. Now classified as: ${if(nextSpam) "Spam Filtered" else "Candidate ($nextDomain)"}")
        }
    }

    // Dynamic State Tracking Update with Automated Responses (Candidate Tracking & Automation USPs)
    fun updateCandidateStatus(app: JobApplication, newStatus: String) {
        viewModelScope.launch {
            val responseText = when (newStatus) {
                "Shortlisted" -> """
                    Dear ${app.candidateName},
                    
                    Excellent news! We are excited to inform you that your profile for the ${app.appliedRole} position has been Shortlisted!
                    
                    Our AI screening verified your match score at ${app.matchScore}% with highly relevant expertise in: ${app.keySkills}.
                    
                    A recruiter from our team will email you within 48 hours with technical panel interview slots.
                    
                    Best regards,
                    Enterprise Recruiting Office
                """.trimIndent()
                "Offer" -> """
                    Dear ${app.candidateName},
                    
                    We are absolutely thrilled to extend you a formal Offer of Employment for the ${app.appliedRole} position!
                    
                    Your high-fidelity resume qualifications and practical evaluations was rated outstanding by our technical panel.
                    
                    You will receive standard offer materials and benefits sheets via email shortly. We look forward to welcome you to our engineering squad.
                    
                    With warm regards,
                    Executive HR Directorate
                """.trimIndent()
                "Rejected" -> """
                    Dear ${app.candidateName},
                    
                    Thank you for your patience during our technical reviews for the position of ${app.appliedRole}.
                    
                    After careful assessment of all candidates, we regret to inform you that we have decided to proceed with other applicants whose experience matches our team's current constraints more closely.
                    
                    We are highly appreciative of your application and time. We will retain your resume in our global database for future openings that match.
                    
                    Sincerely,
                    Hiring & Staffing Team
                """.trimIndent()
                else -> """
                    Dear ${app.candidateName},
                    
                    Your application for the position of ${app.appliedRole} has been successfully Reviewed by our hiring lead.
                    
                    Your profile remains active in our candidates tracking panel. We will notify you should we match your profile for upcoming interview slots.
                    
                    Best,
                    HR Review Department
                """.trimIndent()
            }

            val updated = app.copy(
                trackingStatus = newStatus,
                autoResponse = responseText,
                isResponseSent = true,
                responseSentTimestamp = System.currentTimeMillis()
            )

            repository.update(updated)
            addLog("Status of Candidate ${app.candidateName} updated to: '$newStatus'. Dispatched automated notification email!")
        }
    }

    // Delete candidate
    fun deleteCandidate(app: JobApplication) {
        viewModelScope.launch {
            repository.delete(app)
            addLog("Removed candidate ${app.candidateName} from records.")
        }
    }
}

// Factory to simplify ViewModel instantiation inside Composable / Activity
class RecruitmentViewModelFactory(private val repository: JobApplicationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecruitmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecruitmentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
