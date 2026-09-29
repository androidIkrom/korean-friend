package uz.hangulfriend.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepository(private val db: AppDatabase) {
    private val dao = db.progress()

    fun observeAll(): Flow<Map<String, LessonProgressEntity>> =
        dao.observeAll().map { list -> list.associateBy { it.lessonId } }

    suspend fun setStage(lessonId: String, stage: Int) = update(lessonId) {
        val status = if (it.status == LessonStatus.NOT_STARTED || it.status == LessonStatus.PASSED) {
            LessonStatus.IN_PROGRESS
        } else {
            it.status
        }
        it.copy(stage = stage, status = status)
    }

    suspend fun recordTest(lessonId: String, scorePercent: Int) = update(lessonId) {
        val status = when {
            it.status == LessonStatus.VERIFIED || it.status == LessonStatus.COMPLETED -> it.status
            scorePercent >= PASS_PERCENT -> LessonStatus.COMPLETED
            else -> LessonStatus.IN_PROGRESS
        }
        it.copy(status = status, bestTestScore = maxOf(it.bestTestScore ?: 0, scorePercent))
    }

    suspend fun markPassed(lessonIds: List<String>) = db.withTransaction {
        for (id in lessonIds) {
            val current = dao.get(id)
            if (current == null || current.status == LessonStatus.NOT_STARTED) {
                dao.upsert(LessonProgressEntity(id, LessonStatus.PASSED, stage = 0, bestTestScore = null))
            }
        }
    }

    private suspend fun update(lessonId: String, change: (LessonProgressEntity) -> LessonProgressEntity) =
        db.withTransaction {
            val current = dao.get(lessonId) ?: LessonProgressEntity(lessonId, LessonStatus.NOT_STARTED, 0, null)
            dao.upsert(change(current))
        }

    companion object {
        const val PASS_PERCENT = 80
    }
}
