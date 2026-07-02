package com.example.data.feedback

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val response: String,
    val rating: Int, // 1 = Thumbs Up, -1 = Thumbs Down
    val correctionComment: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
