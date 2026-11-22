package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.OperationType
import com.example.data.PreferencesRepository
import com.example.data.handleFirestoreError
import com.example.model.CLASS_ID
import com.example.model.ClassNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ClassNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val currentUid = Firebase.auth.currentUser?.uid ?: return Result.success()
        val prefs = PreferencesRepository(applicationContext)
        if (!prefs.isNotificationsEnabled()) return Result.success()

        val lastSeenMs = prefs.getLastSeenNotificationMs()
        val db = FirebaseFirestore.getInstance(
            applicationContext.getString(R.string.firestore_database_id)
        )

        return try {
            val snapshot = db.collection("class_notifications")
                .whereEqualTo("classId", CLASS_ID)
                .get()
                .await()

            val notifications = snapshot.documents.mapNotNull { doc ->
                doc.toObject(
                    ClassNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

            val unseen = notifications
                .filter { item ->
                    val createdMs = item.createdAt?.toDate()?.time ?: 0L
                    createdMs > lastSeenMs && item.userId != currentUid
                }
                .sortedBy { it.createdAt?.toDate()?.time ?: 0L }

            var newestMs = lastSeenMs
            unseen.forEach { item ->
                val createdMs = item.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                if (createdMs > newestMs) newestMs = createdMs
                NotificationHelper.showClassNotification(
                    context = applicationContext,
                    notifId = (item.id.hashCode() and 0x7FFFFFFF),
                    title = item.title,
                    body = item.body,
                    senderName = item.senderName,
                    targetTab = item.targetTab
                )
            }

            if (newestMs > lastSeenMs) {
                prefs.updateLastSeenNotificationMs(newestMs)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "class_notifications")
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "class_xi_friend_notifications_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ClassNotificationWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
