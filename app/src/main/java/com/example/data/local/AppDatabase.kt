package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WhatsappDao {
    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: String): Contact?

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: String)

    // Chats
    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: String): Chat?

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp, unreadCount = unreadCount + :unreadIncrement WHERE id = :chatId")
    suspend fun updateChatLastMessage(chatId: String, lastMessage: String, timestamp: Long, unreadIncrement: Int)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun resetChatUnreadCount(chatId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: String)

    // Messages
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("UPDATE messages SET isDeleted = 1, text = '🚫 Esta mensagem foi apagada' WHERE id = :messageId")
    suspend fun markMessageAsDeleted(messageId: String)

    @Query("UPDATE messages SET status = :status WHERE chatId = :chatId AND senderId != 'user'")
    suspend fun updateChatMessagesStatus(chatId: String, status: String)

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND text LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun searchMessagesInChat(chatId: String, query: String): List<Message>

    // Call Logs
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()

    // Status Updates
    @Query("SELECT * FROM status_updates ORDER BY timestamp DESC")
    fun getAllStatusUpdates(): Flow<List<StatusUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusUpdate(statusUpdate: StatusUpdate)

    @Query("UPDATE status_updates SET isViewed = 1 WHERE id = :statusId")
    suspend fun markStatusAsViewed(statusId: String)

    @Query("DELETE FROM status_updates WHERE timestamp < :expiryTime")
    suspend fun deleteExpiredStatusUpdates(expiryTime: Long)
}

@Database(
    entities = [Contact::class, Chat::class, Message::class, CallLog::class, StatusUpdate::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun whatsappDao(): WhatsappDao
}
