package com.example.healthx.data.repository

import android.util.Log
import com.example.healthx.data.local.SessionManager
import com.example.healthx.data.local.dao.ReminderDao
import com.example.healthx.data.local.entities.ReminderEntity
import com.example.healthx.data.local.entities.SyncState
import com.example.healthx.data.network.RemindersApi
import com.example.healthx.data.network.SyncRequest
import com.example.healthx.data.models.reminders.core.AlarmConfig
import com.example.healthx.data.models.reminders.core.RepeatRule
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ReminderRepository @Inject constructor(
    private val api: RemindersApi,
    private val dao: ReminderDao,
    private val sessionManager: SessionManager
) {
    private val gson = Gson()

    /**
     * The Master Sync Engine. Call this when the app opens or network reconnects.
     */
    suspend fun performAutoSync() {
        val isSyncEnabled = sessionManager.isSyncEnabledFlow.first()
        if (!isSyncEnabled) return

        try {
            // 1. Get local data ready for upload
            val lastSyncTime = sessionManager.lastSyncTimeFlow.first()
            val pendingEntities = dao.getPendingUploads()

            val pendingJsonObjects = pendingEntities.map { entityToJson(it) }

            // 2. Execute network request
            val request = SyncRequest(lastClientSyncTime = lastSyncTime, clientPendingUploads = pendingJsonObjects)
            val response = api.syncReminders(request)

            if (response.isSuccessful && response.body() != null) {
                val syncData = response.body()!!

                // 3. Mark our local uploads as successfully synced
                val uploadedIds = pendingEntities.map { it.id }
                if (uploadedIds.isNotEmpty()) {
                    dao.markAsSynced(uploadedIds)
                }

                // 4. Save incoming server updates to Room
                val serverUpdates = syncData.updatedReminders ?: emptyList()
                val entitiesToSave = serverUpdates.map { jsonToEntity(it) }
                if (entitiesToSave.isNotEmpty()) {
                    dao.upsertReminders(entitiesToSave)
                }

                // 5. Update global sync time tracker
                sessionManager.updateLastSyncTime(syncData.serverCurrentTime)
                Log.d("SyncEngine", "Sync complete. Downloaded ${entitiesToSave.size} updates.")
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Sync failed. Will retry later. Error: ${e.localizedMessage}")
        }
    }

    /**
     * Converts a flat JSON object from Node.js/Mongoose into our Room Entity.
     */
    private fun jsonToEntity(json: JsonObject): ReminderEntity {
        // Extract Core Fields
        val id = json.get("_id")?.asString ?: json.get("id").asString
        val userId = json.get("userId").asString
        val category = json.get("category").asString
        val title = json.get("title").asString
        val description = json.get("description")?.asString
        val triggerTime = json.get("triggerDateTime").asLong
        val isActive = json.get("isActive").asBoolean
        val createdAt = json.get("createdAt").asLong
        val updatedAt = json.get("updatedAt").asLong

        // Deserialize complex core objects
        val alarmConfig = gson.fromJson(json.get("alarmConfig"), AlarmConfig::class.java)
        val repeatRule = gson.fromJson(json.get("repeatRule"), RepeatRule::class.java)

        // Remove core fields so only the category-specific payload remains
        val payloadObject = json.deepCopy()
        listOf("_id", "id", "userId", "category", "title", "description", "triggerDateTime",
            "isActive", "alarmConfig", "repeatRule", "createdAt", "updatedAt", "__v").forEach {
            payloadObject.remove(it)
        }

        return ReminderEntity(
            id = id, userId = userId, category = category, title = title,
            description = description, triggerDateTime = triggerTime,
            alarmConfig = alarmConfig, repeatRule = repeatRule,
            isActive = isActive, createdAt = createdAt, updatedAt = updatedAt,
            categoryPayload = payloadObject.toString(), // Save the rest as JSON string
            syncState = SyncState.SYNCED // Coming from server, so it's synced
        )
    }

    /**
     * Converts our Room Entity into a flat JSON object for Node.js/Mongoose.
     */
    private fun entityToJson(entity: ReminderEntity): JsonObject {
        // Parse the polymorphic payload back into a JSON object
        val json = gson.fromJson(entity.categoryPayload, JsonObject::class.java)

        // Attach all core fields
        json.addProperty("id", entity.id)
        json.addProperty("userId", entity.userId)
        json.addProperty("category", entity.category)
        json.addProperty("title", entity.title)
        json.addProperty("description", entity.description)
        json.addProperty("triggerDateTime", entity.triggerDateTime)
        json.addProperty("isActive", entity.isActive)
        json.addProperty("createdAt", entity.createdAt)
        json.addProperty("updatedAt", entity.updatedAt)

        json.add("alarmConfig", gson.toJsonTree(entity.alarmConfig))
        json.add("repeatRule", gson.toJsonTree(entity.repeatRule))

        return json
    }
}