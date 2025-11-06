package io.github.saeargeir.skanniapp.model

import java.text.SimpleDateFormat
import java.util.*

data class InvoiceRecord(
    val id: Long,
    val date: Long,          // Timestamp in milliseconds
    val vendorName: String? = null,
    val amount: Double,
    val vat: Double,         // VAT amount (not percent)
    val imagePath: String? = null,   // Local path in internal storage
    val invoiceNumber: String? = null,
    val categoryId: String? = null,  // Smart categorization
    val ocrText: String? = null,     // Full OCR text for ML analysis
    val isManuallyClassified: Boolean = false,  // User manually changed category
    val classificationConfidence: Double = 0.0,  // ML confidence score
    val cloudImageUrl: String? = null,  // Firebase Storage download URL
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.NOT_SYNCED,  // Sync status
    val firestoreId: String? = null  // Firestore document ID for syncing
) {
    // Helper to get formatted date string
    val dateString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
    
    val monthKey: String
        get() = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(date))
    
    // Compatibility property for old code
    val vendor: String
        get() = vendorName ?: ""
}

enum class CloudSyncStatus {
    NOT_SYNCED,     // Local only, not uploaded to cloud
    SYNCING,        // Upload in progress
    SYNCED,         // Successfully uploaded and synced
    SYNC_FAILED     // Upload failed, will retry
}
