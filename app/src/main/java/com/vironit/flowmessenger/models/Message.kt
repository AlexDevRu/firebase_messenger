package com.vironit.flowmessenger.models

import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import java.util.*

data class Message(
    private val id: String,
    private val email: String,
    private val text: String,
    private val createdAt: Date
): IMessage {

    private val author = Author(email)

    override fun getId(): String {
        return id
    }

    override fun getText(): String {
        return text
    }

    override fun getUser(): IUser {
        return author
    }

    override fun getCreatedAt(): Date {
        return createdAt
    }

    operator fun compareTo(message: Message) : Int {
        return when {
            createdAt < message.createdAt -> -1
            createdAt == message.createdAt -> 0
            else -> 1
        }
    }
}
