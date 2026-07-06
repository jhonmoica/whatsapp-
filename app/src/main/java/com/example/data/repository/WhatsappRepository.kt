package com.example.data.repository

import com.example.data.local.WhatsappDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class WhatsappRepository(private val whatsappDao: WhatsappDao) {

    val allContacts: Flow<List<Contact>> = whatsappDao.getAllContacts()
    val allChats: Flow<List<Chat>> = whatsappDao.getAllChats()
    val allCallLogs: Flow<List<CallLog>> = whatsappDao.getAllCallLogs()
    val allStatusUpdates: Flow<List<StatusUpdate>> = whatsappDao.getAllStatusUpdates()

    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return whatsappDao.getMessagesForChat(chatId)
    }

    suspend fun insertContact(contact: Contact) {
        whatsappDao.insertContact(contact)
    }

    suspend fun deleteContact(id: String) {
        whatsappDao.deleteContactById(id)
    }

    suspend fun insertChat(chat: Chat) {
        whatsappDao.insertChat(chat)
    }

    suspend fun deleteChat(chatId: String) {
        whatsappDao.deleteChatById(chatId)
    }

    suspend fun insertMessage(message: Message) {
        whatsappDao.insertMessage(message)
        // Update the corresponding chat's last message info
        val chat = whatsappDao.getChatById(message.chatId)
        if (chat != null) {
            val increment = if (message.senderId == "user") 0 else 1
            whatsappDao.updateChatLastMessage(
                chatId = message.chatId,
                lastMessage = if (message.isDeleted) "🚫 Esta mensagem foi apagada" else message.text,
                timestamp = message.timestamp,
                unreadIncrement = increment
            )
        } else {
            // Create chat if it doesn't exist (e.g., when starting chat from contact selection)
            val isGroup = message.chatId.startsWith("group_")
            val contact = if (!isGroup) whatsappDao.getContactById(message.chatId) else null
            val title = contact?.name ?: if (isGroup) "Novo Grupo" else message.senderName
            val avatarColor = contact?.avatarColorHex ?: "#128C7E"
            val newChat = Chat(
                id = message.chatId,
                isGroup = isGroup,
                title = title,
                lastMessage = message.text,
                lastMessageTimestamp = message.timestamp,
                unreadCount = if (message.senderId == "user") 0 else 1,
                avatarColorHex = avatarColor
            )
            whatsappDao.insertChat(newChat)
        }
    }

    suspend fun resetChatUnreadCount(chatId: String) {
        whatsappDao.resetChatUnreadCount(chatId)
        whatsappDao.updateChatMessagesStatus(chatId, "read")
    }

    suspend fun deleteMessage(messageId: String) {
        whatsappDao.markMessageAsDeleted(messageId)
    }

    suspend fun insertCallLog(callLog: CallLog) {
        whatsappDao.insertCallLog(callLog)
    }

    suspend fun clearCallLogs() {
        whatsappDao.clearCallLogs()
    }

    suspend fun insertStatusUpdate(statusUpdate: StatusUpdate) {
        whatsappDao.insertStatusUpdate(statusUpdate)
    }

    suspend fun markStatusAsViewed(statusId: String) {
        whatsappDao.markStatusAsViewed(statusId)
    }

    suspend fun populateInitialDataIfEmpty() {
        val contacts = whatsappDao.getAllContacts().firstOrNull()
        if (contacts.isNullOrEmpty()) {
            // 1. Insert Initial Contacts
            val initialContacts = listOf(
                Contact("ney_jr", "Neymar Jr", "+55 11 91010-1010", "#128C7E", "O pai tá on! ⚽🔥"),
                Contact("mae", "Mãe ❤️", "+55 21 98888-8888", "#E53935", "Deus te abençoe, meu filho! 🙏"),
                Contact("amor", "Amor de Minha Vida 😍", "+55 11 97777-7777", "#EC407A", "Amando você cada dia mais... 💕"),
                Contact("chefe", "Trabalho - Chefe 💼", "+55 11 96666-6666", "#78909C", "Focado e produtivo. Em reunião."),
                Contact("musk", "Elon Musk 🚀", "+1 415-555-0199", "#26C6DA", "Mars is waiting. To the moon! 🌌"),
                Contact("thiago_futebol", "Thiago Racha ⚽", "+55 11 95555-5555", "#4CAF50", "Quarta-feira tem fut!"),
                Contact("amanda_facul", "Amanda Faculdade 🎓", "+55 19 94444-4444", "#AB47BC", "Estudando para as finais.")
            )
            whatsappDao.insertContacts(initialContacts)

            // 2. Insert Initial Chats
            val chatNey = Chat("ney_jr", false, "Neymar Jr", "Bora jogar um CS depois?", System.currentTimeMillis() - 1000 * 60 * 15, 0, "#128C7E")
            val chatMae = Chat("mae", false, "Mãe ❤️", "Não esquece de levar o casaco, está frio!", System.currentTimeMillis() - 1000 * 60 * 35, 1, "#E53935")
            val chatFut = Chat("group_futebol", true, "Grupo do Futebol ⚽️", "Thiago Racha: Quem vai levar as bolas hoje?", System.currentTimeMillis() - 1000 * 60 * 120, 3, "#4CAF50", "Grupo para organizar a pelada semanal de quarta-feira.")
            val chatAmor = Chat("amor", false, "Amor de Minha Vida 😍", "Tô com saudades... me liga quando puder!", System.currentTimeMillis() - 1000 * 60 * 240, 0, "#EC407A")
            val chatChefe = Chat("chefe", false, "Trabalho - Chefe 💼", "Por favor, envie o relatório consolidado até o fim do dia.", System.currentTimeMillis() - 1000 * 60 * 600, 0, "#78909C")

            whatsappDao.insertChat(chatNey)
            whatsappDao.insertChat(chatMae)
            whatsappDao.insertChat(chatFut)
            whatsappDao.insertChat(chatAmor)
            whatsappDao.insertChat(chatChefe)

            // 3. Insert Initial Messages
            val currentTime = System.currentTimeMillis()
            
            // Neymar Jr Messages
            whatsappDao.insertMessage(Message("ney_1", "ney_jr", "user", "Você", "Fala Ney, blz? Vai jogar hoje?", currentTime - 1000 * 60 * 30, "read"))
            whatsappDao.insertMessage(Message("ney_2", "ney_jr", "ney_jr", "Neymar Jr", "Fala irmão! Vou sim, o treino acaba já já.", currentTime - 1000 * 60 * 25, "read"))
            whatsappDao.insertMessage(Message("ney_3", "ney_jr", "user", "Você", "Show, me avisa quando logar que eu entro.", currentTime - 1000 * 60 * 20, "read"))
            whatsappDao.insertMessage(Message("ney_4", "ney_jr", "ney_jr", "Neymar Jr", "Bora jogar um CS depois?", currentTime - 1000 * 60 * 15, "read"))

            // Mãe Messages
            whatsappDao.insertMessage(Message("mae_1", "mae", "mae", "Mãe ❤️", "Oi meu filho, tudo bem? Almoçou direitinho?", currentTime - 1000 * 60 * 60, "read"))
            whatsappDao.insertMessage(Message("mae_2", "mae", "user", "Você", "Oi mãe! Tudo ótimo, acabei de almoçar agora.", currentTime - 1000 * 60 * 50, "read"))
            whatsappDao.insertMessage(Message("mae_3", "mae", "mae", "Mãe ❤️", "Não esquece de levar o casaco, está frio!", currentTime - 1000 * 60 * 35, "delivered"))

            // Futebol Messages
            whatsappDao.insertMessage(Message("fut_1", "group_futebol", "thiago_futebol", "Thiago Racha ⚽", "Fala galera, racha confirmado para quarta-feira às 20h!", currentTime - 1000 * 60 * 150, "read"))
            whatsappDao.insertMessage(Message("fut_2", "group_futebol", "user", "Você", "Tô dentro! Pode contar comigo.", currentTime - 1000 * 60 * 140, "read"))
            whatsappDao.insertMessage(Message("fut_3", "group_futebol", "thiago_futebol", "Thiago Racha ⚽", "Boa! Já temos 10 confirmados.", currentTime - 1000 * 60 * 135, "read"))
            whatsappDao.insertMessage(Message("fut_4", "group_futebol", "thiago_futebol", "Thiago Racha ⚽", "Quem vai levar as bolas hoje?", currentTime - 1000 * 60 * 120, "read"))

            // Amor Messages
            whatsappDao.insertMessage(Message("amor_1", "amor", "user", "Você", "Bom dia meu amor! Tenha um ótimo dia de trabalho.", currentTime - 1000 * 60 * 480, "read"))
            whatsappDao.insertMessage(Message("amor_2", "amor", "amor", "Amor de Minha Vida 😍", "Bom dia lindo! Obrigada, para você também! ❤️", currentTime - 1000 * 60 * 460, "read"))
            whatsappDao.insertMessage(Message("amor_3", "amor", "amor", "Amor de Minha Vida 😍", "Tô com saudades... me liga quando puder!", currentTime - 1000 * 60 * 240, "read"))

            // Chefe Messages
            whatsappDao.insertMessage(Message("chefe_1", "chefe", "chefe", "Trabalho - Chefe 💼", "Bom dia. Conseguiu analisar a planilha de custos?", currentTime - 1000 * 60 * 700, "read"))
            whatsappDao.insertMessage(Message("chefe_2", "chefe", "user", "Você", "Bom dia, chefe. Sim, já finalizei o levantamento.", currentTime - 1000 * 60 * 650, "read"))
            whatsappDao.insertMessage(Message("chefe_3", "chefe", "chefe", "Trabalho - Chefe 💼", "Por favor, envie o relatório consolidado até o fim do dia.", currentTime - 1000 * 60 * 600, "read"))

            // 4. Insert Initial Call Logs
            whatsappDao.insertCallLog(CallLog("call_1", "Neymar Jr", "ney_jr", currentTime - 1000 * 60 * 180, isVideo = true, isIncoming = false, isMissed = false))
            whatsappDao.insertCallLog(CallLog("call_2", "Mãe ❤️", "mae", currentTime - 1000 * 60 * 105, isVideo = false, isIncoming = true, isMissed = true))
            whatsappDao.insertCallLog(CallLog("call_3", "Amor de Minha Vida 😍", "amor", currentTime - 1000 * 60 * 1440, isVideo = false, isIncoming = true, isMissed = false))
            whatsappDao.insertCallLog(CallLog("call_4", "Elon Musk 🚀", "musk", currentTime - 1000 * 60 * 2880, isVideo = true, isIncoming = true, isMissed = true))

            // 5. Insert Initial Statuses
            whatsappDao.insertStatusUpdate(
                StatusUpdate(
                    id = "status_1",
                    contactId = "ney_jr",
                    contactName = "Neymar Jr",
                    contactColorHex = "#128C7E",
                    textContent = "Treino de hoje concluído 💪 O pai tá preparado!",
                    timestamp = currentTime - 1000 * 60 * 120
                )
            )
            whatsappDao.insertStatusUpdate(
                StatusUpdate(
                    id = "status_2",
                    contactId = "mae",
                    contactName = "Mãe ❤️",
                    contactColorHex = "#E53935",
                    textContent = "Domingo abençoado com a família! Almoço maravilhoso. 🍲☕",
                    timestamp = currentTime - 1000 * 60 * 180
                )
            )
            whatsappDao.insertStatusUpdate(
                StatusUpdate(
                    id = "status_3",
                    contactId = "amor",
                    contactName = "Amor de Minha Vida 😍",
                    contactColorHex = "#EC407A",
                    textContent = "Olha que pôr do sol lindo! 🌅 Sem filtro!",
                    timestamp = currentTime - 1000 * 60 * 240
                )
            )
            whatsappDao.insertStatusUpdate(
                StatusUpdate(
                    id = "status_4",
                    contactId = "musk",
                    contactName = "Elon Musk 🚀",
                    contactColorHex = "#000000",
                    textContent = "Starship flight test is scheduled for tomorrow! 🚀🇺🇸",
                    timestamp = currentTime - 1000 * 60 * 45
                )
            )
        }
    }
}
