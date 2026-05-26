package com.veil.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.veil.domain.model.MessageFirebase
import com.veil.domain.model.UserPublicProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

class FirebaseDataSource {

    companion object {
        private const val TAG = "VeilFirebase"
    }

    private val db = FirebaseDatabase.getInstance().reference

    // ── User public profile ────────────────────────────────────────────

    suspend fun uploadUserPublicProfile(profile: UserPublicProfile) {
        Log.d(TAG, "Uploading profile for ${profile.userId.take(8)}")
        db.child("users").child(profile.userId).setValue(profile).await()
    }

    suspend fun getUserPublicProfile(userId: String): UserPublicProfile? {
        return try {
            val snapshot = db.child("users").child(userId).get().await()
            snapshot.getValue(UserPublicProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile: ${e.message}")
            null
        }
    }

    // ── conversationId ─────────────────────────────────────────────────

    fun conversationId(userIdA: String, userIdB: String): String {
        val sorted = listOf(userIdA, userIdB).sorted()
        val input  = "${sorted[0]}:${sorted[1]}"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash   = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.take(32)
    }

    // ── Messages ───────────────────────────────────────────────────────

    suspend fun sendMessage(message: MessageFirebase) {
        val convId = conversationId(message.senderId, message.receiverId)
        Log.d(TAG, "Sending to convId=${convId.take(8)}")
        db.child("messages").child(convId).child(message.messageId).setValue(message).await()
    }

    fun listenToMessages(
        currentUserId: String,
        otherUserId  : String
    ): Flow<List<MessageFirebase>> = callbackFlow {
        val convId = conversationId(currentUserId, otherUserId)
        Log.d(TAG, "Listening on convId=${convId.take(8)}")
        val ref = db.child("messages").child(convId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    runCatching { it.getValue(MessageFirebase::class.java) }.getOrNull()
                }.sortedBy { it.timestamp }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Message listener cancelled: ${error.message} (code=${error.code})")
                // Don't close the flow on permission error — just log it
                // The app will retry when rules are fixed
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    Log.e(TAG, "PERMISSION DENIED on messages. Check Firebase rules.")
                } else {
                    close(error.toException())
                }
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // ── Inbox notifications ────────────────────────────────────────────
    //
    // inbox/{receiverId}/{senderId} = { senderId, timestamp, convId }
    //
    // This node lets the receiver discover new messages from unknowns
    // without having to know all possible conversationIds.

    fun listenForIncomingNotifications(myUserId: String): Flow<List<IncomingNotification>> {
        // Guard: don't even try if userId is blank
        if (myUserId.isBlank()) {
            Log.e(TAG, "listenForIncomingNotifications: myUserId is blank, skipping")
            return emptyFlow()
        }

        return callbackFlow {
            Log.d(TAG, "Starting inbox listener for ${myUserId.take(8)}")
            val ref = db.child("inbox").child(myUserId)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = snapshot.children.mapNotNull {
                        runCatching {
                            IncomingNotification(
                                senderId  = it.child("senderId").getValue(String::class.java) ?: "",
                                timestamp = it.child("timestamp").getValue(Long::class.java) ?: 0L,
                                convId    = it.child("convId").getValue(String::class.java) ?: ""
                            )
                        }.getOrNull()
                    }.filter { it.senderId.isNotBlank() }
                    Log.d(TAG, "Inbox update: ${notifications.size} notifications")
                    trySend(notifications)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Inbox listener cancelled: ${error.message} (code=${error.code})")

                    when (error.code) {
                        DatabaseError.PERMISSION_DENIED -> {
                            // Non-fatal: Firebase rules don't include inbox node yet.
                            // App continues working — just no message request detection.
                            // Fix: paste the rules from firebase-rules-fixed.json into
                            // Firebase Console → Realtime Database → Rules
                            Log.e(TAG, """
                                ══════════════════════════════════════════════
                                INBOX PERMISSION DENIED — action required:
                                1. Open Firebase Console
                                2. Go to Realtime Database → Rules
                                3. Replace rules with contents of:
                                   docs/firebase-rules-fixed.json
                                4. Click Publish
                                ══════════════════════════════════════════════
                            """.trimIndent())
                            // Send empty list so app doesn't crash — requests just won't work
                            trySend(emptyList())
                        }
                        else -> {
                            // For other errors, close the flow
                            close(error.toException())
                        }
                    }
                }
            }

            ref.addValueEventListener(listener)
            awaitClose {
                Log.d(TAG, "Removing inbox listener for ${myUserId.take(8)}")
                ref.removeEventListener(listener)
            }
        }
    }

    suspend fun writeInboxNotification(senderId: String, receiverId: String) {
        try {
            val convId = conversationId(senderId, receiverId)
            val data   = mapOf(
                "senderId"  to senderId,
                "timestamp" to System.currentTimeMillis(),
                "convId"    to convId
            )
            db.child("inbox").child(receiverId).child(senderId).setValue(data).await()
            Log.d(TAG, "Inbox notification written: ${senderId.take(8)} → ${receiverId.take(8)}")
        } catch (e: Exception) {
            // Non-fatal — message was sent, notification is best-effort
            Log.e(TAG, "Failed to write inbox notification: ${e.message}")
        }
    }

    suspend fun removeInboxNotification(receiverId: String, senderId: String) {
        try {
            db.child("inbox").child(receiverId).child(senderId).removeValue().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove inbox notification: ${e.message}")
        }
    }

    suspend fun deleteMessage(senderId: String, receiverId: String, messageId: String) {
        val convId = conversationId(senderId, receiverId)
        db.child("messages").child(convId).child(messageId).removeValue().await()
    }

    suspend fun deleteConversation(userIdA: String, userIdB: String) {
        val convId = conversationId(userIdA, userIdB)
        db.child("messages").child(convId).removeValue().await()
    }
}

data class IncomingNotification(
    val senderId : String,
    val timestamp: Long,
    val convId   : String
)
