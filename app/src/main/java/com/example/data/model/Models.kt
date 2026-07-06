package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String, // phone number or id
    val name: String,
    val phoneNumber: String,
    val avatarColorHex: String = "#075E54", // WhatsApp Teal
    val statusText: String = "Hey there! I am using WhatsApp.",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String, // contact phone number or group UUID
    val isGroup: Boolean,
    val title: String,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val avatarColorHex: String = "#128C7E",
    val groupDescription: String? = null
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String, // "user" or contact phone number
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent", // "sent", "delivered", "read"
    val isDeleted: Boolean = false,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val mediaUri: String? = null,
    val mediaType: String? = null // "image", "audio"
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVideo: Boolean = false,
    val isIncoming: Boolean = true,
    val isMissed: Boolean = false
)

@Entity(tableName = "status_updates")
data class StatusUpdate(
    @PrimaryKey val id: String,
    val contactId: String, // "user" or contact id
    val contactName: String,
    val contactColorHex: String = "#25D366", // WhatsApp Green
    val textContent: String? = null,
    val backgroundColorHex: String? = "#128C7E",
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false
)
