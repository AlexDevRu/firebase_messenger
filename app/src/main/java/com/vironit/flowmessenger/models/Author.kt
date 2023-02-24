package com.vironit.flowmessenger.models

import com.stfalcon.chatkit.commons.models.IUser


data class Author(
    private val id: String
): IUser {
    override fun getId(): String {
        return id
    }

    override fun getName(): String {
        return id
    }

    override fun getAvatar(): String {
        return ""
    }
}