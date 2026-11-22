package com.roberto.voicepocket.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "idea_drafts")
internal data class IdeaDraftEntity(
    val sessionId: String,
    val ideaId: Long,
    val originalText: String,
    val draft: String,
    val voicePreview: String? = null,
    val revision: Long = 0,
    @PrimaryKey val slot: Int = 1
) {
    init {
        require(slot == 1)
    }
}
