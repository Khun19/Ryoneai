package com.example.data.feedback

import kotlinx.coroutines.flow.Flow

class FeedbackRepository(private val feedbackDao: FeedbackDao) {
    val allFeedback: Flow<List<FeedbackEntity>> = feedbackDao.getAllFeedback()

    suspend fun insertFeedback(feedback: FeedbackEntity) {
        feedbackDao.insertFeedback(feedback)
    }

    suspend fun deleteFeedbackById(id: Int) {
        feedbackDao.deleteFeedbackById(id)
    }

    suspend fun clearAllFeedback() {
        feedbackDao.clearAllFeedback()
    }
}
