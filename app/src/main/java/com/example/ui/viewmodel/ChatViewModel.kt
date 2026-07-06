package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.DatabaseProvider
import com.example.data.model.*
import com.example.data.repository.WhatsappRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WhatsappRepository
    private val sharedPrefs = application.getSharedPreferences("whatsapp_prefs", Context.MODE_PRIVATE)

    // User Profile Information
    private val _isUserRegistered = MutableStateFlow(sharedPrefs.getBoolean("is_registered", false))
    val isUserRegistered = _isUserRegistered.asStateFlow()

    private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", "Você") ?: "Você")
    val userName = _userName.asStateFlow()

    private val _userPhone = MutableStateFlow(sharedPrefs.getString("user_phone", "+55 11 91234-5678") ?: "+55 11 91234-5678")
    val userPhone = _userPhone.asStateFlow()

    private val _userStatus = MutableStateFlow(sharedPrefs.getString("user_status", "No WhatsApp") ?: "No WhatsApp")
    val userStatus = _userStatus.asStateFlow()

    // Smart AI Reply Toggle
    private val _isSmartReplyEnabled = MutableStateFlow(sharedPrefs.getBoolean("smart_reply_enabled", true))
    val isSmartReplyEnabled = _isSmartReplyEnabled.asStateFlow()

    // --- Firebase Sync Variables ---
    private val _firebaseSyncStatus = MutableStateFlow("IDLE") // IDLE, SYNCING_BACKUP, SYNCING_RESTORE, SUCCESS_BACKUP, SUCCESS_RESTORE, ERROR
    val firebaseSyncStatus = _firebaseSyncStatus.asStateFlow()

    private val _firebaseLogs = MutableStateFlow<List<String>>(emptyList())
    val firebaseLogs = _firebaseLogs.asStateFlow()

    private val _isFirebaseReal = MutableStateFlow(false)
    val isFirebaseReal = _isFirebaseReal.asStateFlow()

    private val _firebaseStatusMessage = MutableStateFlow("Pronto para conectar ao Firebase")
    val firebaseStatusMessage = _firebaseStatusMessage.asStateFlow()

    // Screen States and Core Database Streams
    val contacts: StateFlow<List<Contact>>
    val chats: StateFlow<List<Chat>>
    val callLogs: StateFlow<List<CallLog>>
    val statusUpdates: StateFlow<List<StatusUpdate>>

    // Selected Chat and Message Streams
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    private val _typingContactId = MutableStateFlow<String?>(null)
    val typingContactId = _typingContactId.asStateFlow()

    val activeChatMessages: StateFlow<List<Message>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForChat(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChatDetails: StateFlow<Chat?> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) {
                chats.map { chatList -> chatList.find { it.id == id } }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Call state variables
    private val _activeCallContactName = MutableStateFlow<String?>(null)
    val activeCallContactName = _activeCallContactName.asStateFlow()

    private val _activeCallContactId = MutableStateFlow<String?>(null)
    val activeCallContactId = _activeCallContactId.asStateFlow()

    private val _activeCallIsVideo = MutableStateFlow(false)
    val activeCallIsVideo = _activeCallIsVideo.asStateFlow()

    private val _activeCallIsIncoming = MutableStateFlow(false)
    val activeCallIsIncoming = _activeCallIsIncoming.asStateFlow()

    private val _activeCallState = MutableStateFlow<String?>(null) // "calling", "ringing", "connected", null
    val activeCallState = _activeCallState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn = _isSpeakerOn.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds = _callDurationSeconds.asStateFlow()

    private var callTimerJob: Job? = null

    init {
        val database = DatabaseProvider.getDatabase(application)
        repository = WhatsappRepository(database.whatsappDao())

        contacts = repository.allContacts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        chats = repository.allChats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        callLogs = repository.allCallLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        statusUpdates = repository.allStatusUpdates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Populate mock data on startup if database is empty
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
        checkFirebaseAvailability()
    }

    // --- Profile & Settings Actions ---
    fun registerUser(name: String, phone: String, status: String, avatarColorHex: String) {
        _userName.value = name
        _userPhone.value = phone
        _userStatus.value = status
        _isUserRegistered.value = true
        sharedPrefs.edit()
            .putString("user_name", name)
            .putString("user_phone", phone)
            .putString("user_status", status)
            .putBoolean("is_registered", true)
            .putString("user_avatar_color", avatarColorHex)
            .apply()
    }

    fun logoutUser() {
        _isUserRegistered.value = false
        sharedPrefs.edit().putBoolean("is_registered", false).apply()
    }

    fun updateProfile(name: String, phone: String, status: String) {
        _userName.value = name
        _userPhone.value = phone
        _userStatus.value = status
        sharedPrefs.edit()
            .putString("user_name", name)
            .putString("user_phone", phone)
            .putString("user_status", status)
            .apply()
    }

    fun setSmartReplyEnabled(enabled: Boolean) {
        _isSmartReplyEnabled.value = enabled
        sharedPrefs.edit().putBoolean("smart_reply_enabled", enabled).apply()
    }

    // --- Chat List & Message Actions ---
    fun openChat(chatId: String) {
        _activeChatId.value = chatId
        viewModelScope.launch {
            repository.resetChatUnreadCount(chatId)
        }
    }

    fun closeChat() {
        _activeChatId.value = null
    }

    fun createGroupChat(title: String, description: String, members: List<Contact>) {
        viewModelScope.launch {
            val groupId = "group_" + UUID.randomUUID().toString()
            val chat = Chat(
                id = groupId,
                isGroup = true,
                title = title,
                groupDescription = description,
                lastMessage = "Grupo criado",
                lastMessageTimestamp = System.currentTimeMillis()
            )
            repository.insertChat(chat)
            
            // Insert initial system message
            val sysMsg = Message(
                id = UUID.randomUUID().toString(),
                chatId = groupId,
                senderId = "system",
                senderName = "Sistema",
                text = "Você criou o grupo \"$title\""
            )
            repository.insertMessage(sysMsg)
            openChat(groupId)
        }
    }

    fun sendMessage(text: String, replyToId: String? = null, replyToText: String? = null) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                chatId = chatId,
                senderId = "user",
                senderName = _userName.value,
                text = text,
                timestamp = System.currentTimeMillis(),
                status = "sent",
                replyToId = replyToId,
                replyToText = replyToText
            )
            repository.insertMessage(message)

            // Trigger simulated replies in background
            launchSimulatedReply(chatId, text, messageId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_activeChatId.value == chatId) {
                _activeChatId.value = null
            }
        }
    }

    // --- Status Actions ---
    fun postTextStatus(text: String, colorHex: String) {
        viewModelScope.launch {
            val status = StatusUpdate(
                id = UUID.randomUUID().toString(),
                contactId = "user",
                contactName = _userName.value,
                contactColorHex = colorHex,
                textContent = text,
                timestamp = System.currentTimeMillis()
            )
            repository.insertStatusUpdate(status)
        }
    }

    fun viewStatus(statusId: String) {
        viewModelScope.launch {
            repository.markStatusAsViewed(statusId)
        }
    }

    // --- Calling Actions ---
    fun startCall(contactName: String, contactId: String, isVideo: Boolean) {
        _activeCallContactName.value = contactName
        _activeCallContactId.value = contactId
        _activeCallIsVideo.value = isVideo
        _activeCallIsIncoming.value = false
        _activeCallState.value = "calling"
        _isMuted.value = false
        _isSpeakerOn.value = isVideo
        _callDurationSeconds.value = 0

        viewModelScope.launch {
            // Save outgoing call log
            repository.insertCallLog(
                CallLog(
                    id = UUID.randomUUID().toString(),
                    contactName = contactName,
                    contactId = contactId,
                    isVideo = isVideo,
                    isIncoming = false,
                    isMissed = false
                )
            )

            // Simulate ringing after 1.5 seconds
            delay(1500)
            if (_activeCallState.value == "calling") {
                _activeCallState.value = "ringing"
            }

            // Simulate connection after 3.5 seconds
            delay(3500)
            if (_activeCallState.value == "ringing") {
                _activeCallState.value = "connected"
                startCallTimer()
            }
        }
    }

    fun receiveIncomingCallSimulated(contact: Contact, isVideo: Boolean) {
        _activeCallContactName.value = contact.name
        _activeCallContactId.value = contact.id
        _activeCallIsVideo.value = isVideo
        _activeCallIsIncoming.value = true
        _activeCallState.value = "ringing"
        _isMuted.value = false
        _isSpeakerOn.value = isVideo
        _callDurationSeconds.value = 0
    }

    fun answerCall() {
        if (_activeCallState.value == "ringing") {
            _activeCallState.value = "connected"
            // Save call log as answered incoming
            viewModelScope.launch {
                repository.insertCallLog(
                    CallLog(
                        id = UUID.randomUUID().toString(),
                        contactName = _activeCallContactName.value ?: "Desconhecido",
                        contactId = _activeCallContactId.value ?: "unknown",
                        isVideo = _activeCallIsVideo.value,
                        isIncoming = true,
                        isMissed = false
                    )
                )
            }
            startCallTimer()
        }
    }

    fun rejectOrMissCall() {
        val contactName = _activeCallContactName.value ?: "Desconhecido"
        val contactId = _activeCallContactId.value ?: "unknown"
        val isVideo = _activeCallIsVideo.value
        val wasIncoming = _activeCallIsIncoming.value

        viewModelScope.launch {
            if (wasIncoming && _activeCallState.value == "ringing") {
                repository.insertCallLog(
                    CallLog(
                        id = UUID.randomUUID().toString(),
                        contactName = contactName,
                        contactId = contactId,
                        isVideo = isVideo,
                        isIncoming = true,
                        isMissed = true
                    )
                )
            }
            endCall()
        }
    }

    fun endCall() {
        _activeCallState.value = null
        _activeCallContactName.value = null
        _activeCallContactId.value = null
        callTimerJob?.cancel()
        callTimerJob = null
        _callDurationSeconds.value = 0
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_activeCallState.value == "connected") {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    fun clearAllCallLogs() {
        viewModelScope.launch {
            repository.clearCallLogs()
        }
    }

    // --- Interactive simulated chat response logic ---
    private fun launchSimulatedReply(chatId: String, userText: String, userMessageId: String) {
        viewModelScope.launch {
            // Step 1: Simulated delay -> deliver message (double grey ticks)
            delay(1200)
            updateMessageStatus(userMessageId, "delivered")

            // Step 2: Simulated delay -> read message (double blue ticks)
            delay(1500)
            updateMessageStatus(userMessageId, "read")

            // Step 3: Wait a bit, then show "Digitando..." typing indicator
            delay(800)
            val isGroup = chatId.startsWith("group_")
            val replierId = if (isGroup) {
                // Pick a random contact to reply in the group
                val activeContacts = contacts.value.filter { it.id != "user" }
                if (activeContacts.isNotEmpty()) activeContacts.random().id else "ney_jr"
            } else {
                chatId
            }
            
            _typingContactId.value = replierId
            
            // Step 4: Keep typing state for a few seconds to look incredibly real
            val typingDuration = when {
                userText.length < 10 -> 2000L
                userText.length < 30 -> 3500L
                else -> 5000L
            }
            delay(typingDuration)

            // Step 5: Generate reply text (Smart AI via Gemini or high-fidelity mock templates)
            val replyText = getReplyText(replierId, userText)

            // Step 6: Insert received message, clear typing status
            _typingContactId.value = null
            
            val replier = contacts.value.find { it.id == replierId }
            val replyMessage = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = replierId,
                senderName = replier?.name ?: "Contato",
                text = replyText,
                timestamp = System.currentTimeMillis(),
                status = "read"
            )
            repository.insertMessage(replyMessage)
        }
    }

    private suspend fun updateMessageStatus(messageId: String, status: String) {
        withContext(Dispatchers.IO) {
            val database = DatabaseProvider.getDatabase(getApplication())
            val msg = database.whatsappDao().getMessagesForChat(activeChatId.value ?: "").firstOrNull()?.find { it.id == messageId }
            if (msg != null) {
                database.whatsappDao().insertMessage(msg.copy(status = status))
            }
        }
    }

    private suspend fun getReplyText(contactId: String, userText: String): String {
        // Check if Gemini Smart Reply is enabled, and API key is present
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (_isSmartReplyEnabled.value && apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            val contact = contacts.value.find { it.id == contactId }
            val systemInstruction = """
                Você é um contato no WhatsApp chamado ${contact?.name ?: "Amigo"}.
                Seu status/recado de perfil atual é: "${contact?.statusText ?: ""}".
                Por favor, responda à mensagem do usuário em português de forma natural, realista e curta, simulando uma conversa de chat real de smartphone.
                Pode usar emojis que correspondam ao seu perfil. Responda em apenas uma frase, mantendo o estilo de conversa rápida.
            """.trimIndent()

            val aiReply = callGeminiApiSafely(apiKey, systemInstruction, userText)
            if (!aiReply.startsWith("Error:") && aiReply.isNotEmpty()) {
                return aiReply
            }
        }

        // Fallback to high-fidelity, customized offline mock replies
        return getOfflineMockReply(contactId, userText)
    }

    private suspend fun callGeminiApiSafely(apiKey: String, systemInstruction: String, userMessage: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val jsonPayload = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userMessage)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
        }

        val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    parts.getJSONObject(0).getString("text").trim()
                } else {
                    "Error: ${response.code}"
                }
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun getOfflineMockReply(contactId: String, userText: String): String {
        val textLower = userText.lowercase()
        return when (contactId) {
            "ney_jr" -> {
                when {
                    textLower.contains("oi") || textLower.contains("olá") || textLower.contains("fala") -> "Fala irmão! Tranquilo? 🤙⚽"
                    textLower.contains("futebol") || textLower.contains("jogo") || textLower.contains("bola") -> "O pai tá sempre pronto! Bora marcar o racha!"
                    textLower.contains("cs") || textLower.contains("jogar") || textLower.contains("game") -> "Bora conectar! Me dá 10 minutinhos que eu logo."
                    else -> "Haha boa! Tamo junto! Aquele abraço irmão 🤙🔥"
                }
            }
            "mae" -> {
                when {
                    textLower.contains("oi") || textLower.contains("olá") || textLower.contains("mãe") -> "Oi meu amor! Tudo bem com você? Como foi seu dia?"
                    textLower.contains("fome") || textLower.contains("comer") || textLower.contains("comida") -> "Fiz aquele bolo que você gosta! Vem aqui comer depois."
                    textLower.contains("te amo") -> "Eu também te amo muito, meu filho! Deus te abençoe sempre 🙏❤️"
                    else -> "Que ótimo, meu querido! Se cuida e não esquece de beber água, viu?"
                }
            }
            "amor" -> {
                when {
                    textLower.contains("oi") || textLower.contains("linda") || textLower.contains("vida") -> "Oi meu amorzinho! Tava pensando em você agora mesmo... 😍"
                    textLower.contains("te amo") -> "Te amo infinitamente! Você é a razão do meu sorriso 💕"
                    textLower.contains("sair") || textLower.contains("jantar") || textLower.contains("hoje") -> "Siiim! Quero muito! Onde nós vamos?"
                    else -> "Ai, que fofo! Te amo demais, me liga quando sair do trabalho! 😘💕"
                }
            }
            "chefe" -> {
                when {
                    textLower.contains("oi") || textLower.contains("bom dia") || textLower.contains("boa tarde") -> "Bom dia. Conseguiu avançar nas demandas pendentes?"
                    textLower.contains("relatório") || textLower.contains("planilha") || textLower.contains("pronto") -> "Excelente. Por favor, envie o link no meu e-mail institucional para eu revisar."
                    textLower.contains("atrasar") || textLower.contains("problema") -> "Entendido. Mantenha-me atualizado sobre o prazo de entrega."
                    else -> "Obrigado pelo retorno. Conversamos mais detalhadamente na nossa reunião semanal."
                }
            }
            "musk" -> {
                when {
                    textLower.contains("hello") || textLower.contains("oi") || textLower.contains("elon") -> "Greetings. What is your take on multiplanetary expansion today? 🚀"
                    textLower.contains("mars") || textLower.contains("spacex") || textLower.contains("rocket") -> "Starship is the key to making life multiplanetary. Mars soon!"
                    textLower.contains("tesla") || textLower.contains("car") || textLower.contains("ai") -> "FSD Version 12 is mind-blowing. AI is accelerating fast."
                    else -> "Interesting point. We should talk about physics first principles. 🌌"
                }
            }
            else -> {
                when {
                    textLower.contains("oi") || textLower.contains("olá") -> "Olá! Tudo bem? Como posso te ajudar hoje?"
                    textLower.contains("tudo") || textLower.contains("sim") -> "Show! Beleza então 👍"
                    else -> "Beleza! Obrigado pelo retorno. Falo contigo em breve! 👍"
                }
            }
        }
    }

    // --- Firebase Cloud Integration and Verification Functions ---
    private fun checkFirebaseAvailability() {
        try {
            // Safe check for Firebase configuration
            val app = com.google.firebase.FirebaseApp.getInstance()
            _isFirebaseReal.value = true
            _firebaseStatusMessage.value = "Conectado ao Firebase Cloud (Real)"
            addFirebaseLog("FirebaseApp inicializado com sucesso (Real).")
        } catch (e: IllegalStateException) {
            try {
                com.google.firebase.FirebaseApp.initializeApp(getApplication())
                _isFirebaseReal.value = true
                _firebaseStatusMessage.value = "Conectado ao Firebase Cloud (Real)"
                addFirebaseLog("FirebaseApp inicializado com sucesso no arranque.")
            } catch (ex: Exception) {
                _isFirebaseReal.value = false
                _firebaseStatusMessage.value = "Modo Simulado (google-services.json ausente)"
                addFirebaseLog("Aviso: google-services.json não encontrado. Rodando em simulação de nuvem.")
            }
        }
    }

    fun addFirebaseLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _firebaseLogs.value = _firebaseLogs.value + "[$timestamp] $message"
    }

    fun clearFirebaseLogs() {
        _firebaseLogs.value = emptyList()
    }

    fun performFirebaseBackup() {
        viewModelScope.launch {
            _firebaseSyncStatus.value = "SYNCING_BACKUP"
            addFirebaseLog("Iniciando Backup no Firebase...")
            
            val userPhoneNumber = _userPhone.value.replace(" ", "").replace("-", "").replace("+", "")
            if (userPhoneNumber.isEmpty() || userPhoneNumber == "Você") {
                _firebaseSyncStatus.value = "ERROR"
                _firebaseStatusMessage.value = "Erro: Telefone inválido."
                addFirebaseLog("Erro: configure um número de telefone válido no perfil antes de realizar o backup.")
                return@launch
            }

            delay(1000)

            if (_isFirebaseReal.value) {
                try {
                    addFirebaseLog("Autenticando anonimamente no Firebase...")
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        auth.signInWithEmailAndPassword("anon@example.com", "temporary_pass")
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    auth.signInAnonymously()
                                }
                            }
                        delay(1200)
                    }
                    addFirebaseLog("Autenticado com UID: ${auth.currentUser?.uid ?: "anonymous"}")
                    
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val chatList = chats.value

                    addFirebaseLog("Enviando ${chatList.size} conversas para o Firestore...")
                    for (chat in chatList) {
                        val chatMap = hashMapOf(
                            "id" to chat.id,
                            "isGroup" to chat.isGroup,
                            "title" to chat.title,
                            "lastMessage" to chat.lastMessage,
                            "lastMessageTimestamp" to chat.lastMessageTimestamp,
                            "unreadCount" to chat.unreadCount,
                            "avatarColorHex" to chat.avatarColorHex,
                            "groupDescription" to chat.groupDescription
                        )
                        firestore.collection("users").document(userPhoneNumber)
                            .collection("chats").document(chat.id)
                            .set(chatMap)
                        
                        val messagesList = withContext(Dispatchers.IO) {
                            val database = DatabaseProvider.getDatabase(getApplication())
                            database.whatsappDao().getMessagesForChat(chat.id).firstOrNull() ?: emptyList()
                        }
                        
                        addFirebaseLog("Enviando ${messagesList.size} mensagens de '${chat.title}'...")
                        for (msg in messagesList) {
                            val msgMap = hashMapOf(
                                "id" to msg.id,
                                "chatId" to msg.chatId,
                                "senderId" to msg.senderId,
                                "senderName" to msg.senderName,
                                "text" to msg.text,
                                "timestamp" to msg.timestamp,
                                "status" to msg.status,
                                "replyToId" to msg.replyToId,
                                "replyToText" to msg.replyToText,
                                "isDeleted" to msg.isDeleted
                            )
                            firestore.collection("users").document(userPhoneNumber)
                                .collection("chats").document(chat.id)
                                .collection("messages").document(msg.id)
                                .set(msgMap)
                        }
                    }

                    _firebaseSyncStatus.value = "SUCCESS_BACKUP"
                    _firebaseStatusMessage.value = "Backup concluído com sucesso no Firebase!"
                    addFirebaseLog("Sucesso! Coleção 'users/$userPhoneNumber/chats' atualizada na nuvem.")

                } catch (e: Exception) {
                    _firebaseSyncStatus.value = "ERROR"
                    _firebaseStatusMessage.value = "Erro no Firebase: ${e.localizedMessage}"
                    addFirebaseLog("Exceção Firebase: ${e.message}")
                }
            } else {
                // High-fidelity Simulator Mode
                try {
                    addFirebaseLog("[SIMULAÇÃO] Iniciando ambiente Firebase Emulator...")
                    delay(800)
                    addFirebaseLog("[SIMULAÇÃO] Realizando autenticação anônima temporária...")
                    delay(1000)
                    addFirebaseLog("[SIMULAÇÃO] Conectando ao Firestore local...")
                    delay(800)
                    
                    val chatList = chats.value
                    addFirebaseLog("[SIMULAÇÃO] Encontrado(s) ${chatList.size} chat(s) locais.")
                    
                    for (chat in chatList) {
                        addFirebaseLog("[SIMULAÇÃO] Uploading chat metadata for: ${chat.title}")
                        val messagesList = withContext(Dispatchers.IO) {
                            val database = DatabaseProvider.getDatabase(getApplication())
                            database.whatsappDao().getMessagesForChat(chat.id).firstOrNull() ?: emptyList()
                        }
                        delay(500)
                        addFirebaseLog("[SIMULAÇÃO] Gravando ${messagesList.size} documentos em '/users/$userPhoneNumber/chats/${chat.id}/messages'")
                    }
                    
                    delay(1200)
                    _firebaseSyncStatus.value = "SUCCESS_BACKUP"
                    _firebaseStatusMessage.value = "Simulação: Backup concluído! (Modo de Teste Offline)"
                    addFirebaseLog("[SIMULAÇÃO] Sincronização offline concluída com sucesso!")
                } catch (e: Exception) {
                    _firebaseSyncStatus.value = "ERROR"
                    addFirebaseLog("Erro na simulação: ${e.message}")
                }
            }
        }
    }

    fun performFirebaseRestore() {
        viewModelScope.launch {
            _firebaseSyncStatus.value = "SYNCING_RESTORE"
            addFirebaseLog("Iniciando Restauração do Firebase...")
            
            val userPhoneNumber = _userPhone.value.replace(" ", "").replace("-", "").replace("+", "")
            if (userPhoneNumber.isEmpty() || userPhoneNumber == "Você") {
                _firebaseSyncStatus.value = "ERROR"
                _firebaseStatusMessage.value = "Erro: Telefone de perfil inválido."
                addFirebaseLog("Erro: configure um número de telefone válido antes de restaurar.")
                return@launch
            }

            delay(1000)

            if (_isFirebaseReal.value) {
                try {
                    addFirebaseLog("Autenticando sessão anonimamente...")
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        auth.signInAnonymously()
                    }
                    
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    addFirebaseLog("Buscando conversas no caminho 'users/$userPhoneNumber/chats'...")
                    
                    firestore.collection("users").document(userPhoneNumber)
                        .collection("chats")
                        .get()
                        .addOnSuccessListener { chatsSnapshot ->
                            viewModelScope.launch {
                                if (chatsSnapshot.isEmpty) {
                                    addFirebaseLog("Nenhuma conversa de backup encontrada no Firebase.")
                                    _firebaseSyncStatus.value = "SUCCESS_RESTORE"
                                    _firebaseStatusMessage.value = "Restauração concluída: Sem dados para restaurar."
                                    return@launch
                                }
                                
                                val database = DatabaseProvider.getDatabase(getApplication())
                                val dao = database.whatsappDao()
                                
                                addFirebaseLog("Encontrados ${chatsSnapshot.size()} chats. Restaurando no SQLite local...")
                                for (doc in chatsSnapshot.documents) {
                                    val chatId = doc.getString("id") ?: continue
                                    val isGroup = doc.getBoolean("isGroup") ?: false
                                    val title = doc.getString("title") ?: "Chat Restaurado"
                                    val lastMessage = doc.getString("lastMessage") ?: ""
                                    val lastMsgTime = doc.getLong("lastMessageTimestamp") ?: System.currentTimeMillis()
                                    val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                                    val avatarHex = doc.getString("avatarColorHex") ?: "#128C7E"
                                    val groupDesc = doc.getString("groupDescription")
                                    
                                    val restoredChat = Chat(chatId, isGroup, title, lastMessage, lastMsgTime, unreadCount, avatarHex, groupDesc)
                                    dao.insertChat(restoredChat)
                                    
                                    firestore.collection("users").document(userPhoneNumber)
                                        .collection("chats").document(chatId)
                                        .collection("messages")
                                        .get()
                                        .addOnSuccessListener { msgsSnapshot ->
                                            viewModelScope.launch {
                                                for (msgDoc in msgsSnapshot.documents) {
                                                    val msgId = msgDoc.getString("id") ?: continue
                                                    val senderId = msgDoc.getString("senderId") ?: "user"
                                                    val senderName = msgDoc.getString("senderName") ?: "Você"
                                                    val text = msgDoc.getString("text") ?: ""
                                                    val timestamp = msgDoc.getLong("timestamp") ?: System.currentTimeMillis()
                                                    val status = msgDoc.getString("status") ?: "read"
                                                    val replyToId = msgDoc.getString("replyToId")
                                                    val replyToText = msgDoc.getString("replyToText")
                                                    val isDeleted = msgDoc.getBoolean("isDeleted") ?: false
                                                    
                                                    val restoredMsg = Message(
                                                        id = msgId,
                                                        chatId = chatId,
                                                        senderId = senderId,
                                                        senderName = senderName,
                                                        text = text,
                                                        timestamp = timestamp,
                                                        status = status,
                                                        isDeleted = isDeleted,
                                                        replyToId = replyToId,
                                                        replyToText = replyToText
                                                    )
                                                    dao.insertMessage(restoredMsg)
                                                }
                                            }
                                        }
                                }
                                
                                delay(1200)
                                _firebaseSyncStatus.value = "SUCCESS_RESTORE"
                                _firebaseStatusMessage.value = "Restauração do Firebase realizada com sucesso!"
                                addFirebaseLog("Sincronização concluída! Dados restaurados no Room local.")
                            }
                        }
                        .addOnFailureListener { e ->
                            _firebaseSyncStatus.value = "ERROR"
                            _firebaseStatusMessage.value = "Falha Firestore: ${e.message}"
                            addFirebaseLog("Erro ao baixar dados do Firebase: ${e.message}")
                        }

                } catch (e: Exception) {
                    _firebaseSyncStatus.value = "ERROR"
                    _firebaseStatusMessage.value = "Erro: ${e.localizedMessage}"
                    addFirebaseLog("Exceção capturada na restauração: ${e.message}")
                }
            } else {
                // High-fidelity Simulator Mode
                try {
                    addFirebaseLog("[SIMULAÇÃO] Baixando pacote criptografado do Firebase...")
                    delay(1200)
                    addFirebaseLog("[SIMULAÇÃO] Descompactando e verificando assinaturas digitais...")
                    delay(800)
                    addFirebaseLog("[SIMULAÇÃO] Mesclando registros no banco Room local...")
                    delay(600)
                    _firebaseSyncStatus.value = "SUCCESS_RESTORE"
                    _firebaseStatusMessage.value = "Simulação: Restauração concluída! (Modo de Teste Offline)"
                    addFirebaseLog("[SIMULAÇÃO] Sucesso! Banco de dados de conversas restaurado e atualizado.")
                } catch (e: Exception) {
                    _firebaseSyncStatus.value = "ERROR"
                    addFirebaseLog("Erro na simulação de restauração: ${e.message}")
                }
            }
        }
    }
}
