package io.github.saeargeir.skanniapp.data

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.saeargeir.skanniapp.model.InvoiceRecord
import io.github.saeargeir.skanniapp.model.CloudSyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * InvoiceStore with Firebase sync
 * Stores data locally AND syncs to Firebase
 */
class InvoiceStore(private val context: Context) {
    private val file: File by lazy { File(context.filesDir, "invoices.json") }
    private val firebaseRepo = FirebaseRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun loadAll(): List<InvoiceRecord> {
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText()
            val arr = JSONArray(text)
            (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAll(list: List<InvoiceRecord>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        file.writeText(arr.toString())
    }

    fun add(record: InvoiceRecord, imageUri: Uri? = null) {
        // Save locally first
        val current = loadAll().toMutableList()
        current.add(record)
        saveAll(current)
        
        // Sync to Firebase in background
        if (firebaseRepo.isAuthenticated()) {
            scope.launch {
                try {
                    Log.d("InvoiceStore", "Syncing invoice to Firebase...")
                    val result = firebaseRepo.uploadInvoice(record, imageUri)
                    
                    result.onSuccess { firestoreId ->
                        Log.d("InvoiceStore", "✅ Synced to Firebase: $firestoreId")
                        // Update local record with Firestore ID
                        val updated = record.copy(
                            firestoreId = firestoreId,
                            cloudSyncStatus = CloudSyncStatus.SYNCED
                        )
                        update(updated)
                    }
                    
                    result.onFailure { error ->
                        Log.e("InvoiceStore", "❌ Failed to sync to Firebase", error)
                        // Mark as failed
                        val updated = record.copy(cloudSyncStatus = CloudSyncStatus.SYNC_FAILED)
                        update(updated)
                    }
                } catch (e: Exception) {
                    Log.e("InvoiceStore", "❌ Error during Firebase sync", e)
                }
            }
        }
    }

    fun deleteById(id: Long) {
        val current = loadAll().toMutableList()
        val record = current.find { it.id == id }
        val newList = current.filterNot { it.id == id }
        saveAll(newList)
        
        // Delete from Firebase if it was synced
        if (record?.firestoreId != null && firebaseRepo.isAuthenticated()) {
            scope.launch {
                firebaseRepo.deleteInvoice(record.firestoreId)
            }
        }
    }

    fun update(record: InvoiceRecord) {
        val current = loadAll().toMutableList()
        val idx = current.indexOfFirst { it.id == record.id }
        if (idx >= 0) {
            current[idx] = record
            saveAll(current)
            
            // Update Firebase if synced
            if (record.firestoreId != null && firebaseRepo.isAuthenticated()) {
                scope.launch {
                    firebaseRepo.updateInvoice(record)
                }
            }
        }
    }
    
    fun clearAll() {
        try {
            if (file.exists()) {
                file.delete()
            }
            // Also clear any cached images
            val imagesDir = File(context.filesDir, "images")
            if (imagesDir.exists()) {
                imagesDir.listFiles()?.forEach { imageFile ->
                    try {
                        imageFile.delete()
                    } catch (e: Exception) {
                        // Log but don't throw - continue clearing other files
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
        }
    }

    // Simple result wrapper for sync
    data class SyncResult(val uploaded: Int, val downloaded: List<InvoiceRecord>)

    /**
     * Sync local invoices with Firebase: upload unsynced, download remote and merge.
     * This is a suspend function and should be called from a coroutine.
     */
    suspend fun syncWithCloud(localNotes: List<InvoiceRecord>): Result<SyncResult> {
        return try {
            if (!firebaseRepo.isAuthenticated()) return Result.failure(IllegalStateException("Not authenticated"))

            // 1) Upload unsynced local invoices
            var uploaded = 0
            val current = loadAll().toMutableList()

            val unsynced = current.filter { it.cloudSyncStatus != io.github.saeargeir.skanniapp.model.CloudSyncStatus.SYNCED }

            unsynced.forEach { rec ->
                try {
                    val res = firebaseRepo.uploadInvoice(rec, null)
                    res.onSuccess { id ->
                        uploaded += 1
                        val updated = rec.copy(firestoreId = id, cloudSyncStatus = io.github.saeargeir.skanniapp.model.CloudSyncStatus.SYNCED)
                        update(updated)
                    }
                } catch (e: Exception) {
                    // ignore individual failures
                }
            }

            // 2) Download remote invoices and merge into local
            val fetched = firebaseRepo.fetchAllInvoicesOnce()
            val downloaded = fetched.getOrDefault(emptyList())

            // Merge: add any remote invoice that we don't have (by firestoreId)
            val localByFirestore = loadAll().mapNotNull { it.firestoreId to it }.toMap()
            var newAdded = 0
            downloaded.forEach { remote ->
                if (remote.firestoreId != null && !localByFirestore.containsKey(remote.firestoreId)) {
                    // create a local id and mark as synced
                    val localRecord = remote.copy(id = System.currentTimeMillis(), cloudSyncStatus = io.github.saeargeir.skanniapp.model.CloudSyncStatus.SYNCED)
                    val cur = loadAll().toMutableList()
                    cur.add(localRecord)
                    saveAll(cur)
                    newAdded += 1
                }
            }

            Result.success(SyncResult(uploaded = uploaded, downloaded = downloaded))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a timestamped backup of all invoices to filesDir/backups/{timestamp}.json
     */
    suspend fun createBackup(): Result<File> {
        return try {
            val all = loadAll()
            val backupsDir = File(context.filesDir, "backups")
            if (!backupsDir.exists()) backupsDir.mkdirs()

            val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val f = File(backupsDir, "backup_${fmt.format(Date())}.json")
            val arr = JSONArray()
            all.forEach { arr.put(toJson(it)) }
            f.writeText(arr.toString())
            Result.success(f)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toJson(r: InvoiceRecord): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("date", r.date)
        put("vendorName", r.vendorName ?: "")
        put("amount", r.amount)
        put("vat", r.vat)
        put("imagePath", r.imagePath ?: "")
        if (r.invoiceNumber != null) put("invoiceNumber", r.invoiceNumber)
        if (r.categoryId != null) put("categoryId", r.categoryId)
        if (r.ocrText != null) put("ocrText", r.ocrText)
        put("isManuallyClassified", r.isManuallyClassified)
        put("classificationConfidence", r.classificationConfidence)
        if (r.cloudImageUrl != null) put("cloudImageUrl", r.cloudImageUrl)
        put("cloudSyncStatus", r.cloudSyncStatus.name)
        if (r.firestoreId != null) put("firestoreId", r.firestoreId)
    }

    private fun fromJson(o: JSONObject): InvoiceRecord = InvoiceRecord(
        id = o.getLong("id"),
        date = o.getLong("date"),
        vendorName = o.optString("vendorName", "").takeIf { it.isNotEmpty() },
        amount = o.getDouble("amount"),
        vat = o.getDouble("vat"),
        imagePath = o.optString("imagePath", "").takeIf { it.isNotEmpty() },
        invoiceNumber = o.optString("invoiceNumber", "").takeIf { it.isNotEmpty() },
        categoryId = o.optString("categoryId", "").takeIf { it.isNotEmpty() },
        ocrText = o.optString("ocrText", "").takeIf { it.isNotEmpty() },
        isManuallyClassified = o.optBoolean("isManuallyClassified", false),
        classificationConfidence = o.optDouble("classificationConfidence", 0.0),
        cloudImageUrl = o.optString("cloudImageUrl", "").takeIf { it.isNotEmpty() },
        cloudSyncStatus = try {
            CloudSyncStatus.valueOf(o.optString("cloudSyncStatus", "NOT_SYNCED"))
        } catch (e: Exception) {
            CloudSyncStatus.NOT_SYNCED
        },
        firestoreId = o.optString("firestoreId", "").takeIf { it.isNotEmpty() }
    )
}