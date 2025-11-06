# 🔥 Firebase Integration - SkanniApp ↔ Dashboard

## ✅ Hvað var gert

### 1. Nýjar skrár búnar til:

- **`FirebaseRepository.kt`** - Sér um öll Firebase samskipti
  - Upload invoice með mynd
  - Real-time sync með dashboard
  - Update og delete invoices
  - Fylgir **nákvæmlega** dashboard structure

### 2. Uppfærðar skrár:

- **`InvoiceRecord.kt`** - Nýir fields:
  - `firestoreId` - Firestore document ID
  - `date` breytt í `Long` (timestamp)
  - `vendorName` í stað `vendor`
  - Bætti við helper properties fyrir backward compatibility

- **`InvoiceStore.kt`** - Nú með Firebase sync:
  - `add()` uploadar sjálfkrafa í Firebase
  - `update()` syncar breytingar
  - `deleteById()` eyðir úr Firebase
  - Background sync með coroutines

## 📊 Firestore Structure

```
users/
  {userId}/
    invoices/
      {invoiceId}/
        - amount: 15000.0
        - vendor: "Bónus"
        - category: "Matvörur"
        - date: Timestamp
        - imagePath: "https://firebasestorage.googleapis.com/..."
        - imageUrl: "https://firebasestorage.googleapis.com/..."
        - originalImageUrl: "https://firebasestorage.googleapis.com/..."
        - storagePath: "gs://bucket/users/{uid}/invoices/receipt_xxx.jpg"
        - ocrText: "Full OCR text..."
        - invoiceNumber: "BNS-001"
        - vat: 3600.0
        - vat24: 3600.0
        - vat11: 0.0
        - vatRate: 0.24
        - userId: "{uid}"
        - userName: "Jón Jónsson"
        - userEmail: "jon@example.com"
        - timestamps:
            - createdAt: Timestamp
            - updatedAt: Timestamp
        - status: "pending"
```

## 🚀 Hvernig virkar þetta

### 1. Notandi skannar reikning:

```kotlin
// Í scanner kóðanum
val invoice = InvoiceRecord(
    id = System.currentTimeMillis(),
    date = System.currentTimeMillis(),
    vendorName = "Bónus",
    amount = 15000.0,
    vat = 3600.0,
    ocrText = extractedText,
    // ... rest of fields
)

// Vista með mynd
val imageUri = Uri.fromFile(imageFile)
invoiceStore.add(invoice, imageUri)
```

### 2. InvoiceStore syncar í Firebase:

```
Local Storage (JSON)  →  Firebase Storage (myndir)
        ↓                         ↓
    Firestore (invoice data)  →  Dashboard sér strax!
```

### 3. Dashboard sér gögnin strax:

- Real-time listener í dashboard sækir nýjan reikning
- Mynd birtist frá Firebase Storage
- Admin getur breytt category, bætt við notes, etc.

## 🔄 Real-time Sync

Dashboard ↔ App sync virkar í báðar áttir:

**App → Dashboard:**
- Notandi skannar → Birtist strax í dashboard

**Dashboard → App:**
- Admin breytir category → App uppfærist (ef real-time listener er virkur)

## 📝 Næstu skref

### 1. Athuga Scanner kóða

Finndu þar sem `invoiceStore.add()` er kallað og bættu við `imageUri`:

```kotlin
// ÁÐUR:
invoiceStore.add(invoice)

// NÚNA:
invoiceStore.add(invoice, imageUri)  // imageUri er Uri af myndinni
```

### 2. Bæta við Authentication

Ef app-ið er ekki með Google Sign-In virkan, þá þarf að bæta því við:

```kotlin
// í MainActivity eða LoginActivity
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(getString(R.string.default_web_client_id))
    .requestEmail()
    .build()

val googleSignInClient = GoogleSignIn.getClient(this, gso)
```

### 3. Testing

1. **Sign in með Google account** (sama account og í dashboard)
2. **Skanna reikning**
3. **Opna dashboard** → Sjá reikninginn birtast
4. **Athuga Firebase Console:**
   - Storage: `users/{uid}/invoices/receipt_xxx.jpg`
   - Firestore: `users/{uid}/invoices/{doc-id}`

## 🐛 Troubleshooting

### "User not authenticated"
- Athugaðu að `FirebaseAuth.getInstance().currentUser` sé ekki null
- Þarf að sign in með Google/Email

### "Permission denied"
- Firebase Rules þurfa að leyfa write fyrir authenticated users
- Athugaðu Firebase Console → Firestore → Rules

### Myndir birtast ekki í dashboard
- Athugaðu að `imagePath`, `imageUrl` og `originalImageUrl` séu öll HTTPS URLs
- `storagePath` getur verið gs:// en hinar þurfa að vera https://

### Gögn birtast ekki í dashboard
- Athugaðu að structure sé rétt: `users/{uid}/invoices/{doc-id}`
- EKKI: `invoices/{doc-id}` (vantar users subcollection!)

## 📚 Viðbótar Resources

Dashboard repository:
- [skanni-dashboard](https://github.com/savargeir-byte/skanni-dashboard)
- [MOBILE_APP_INTEGRATION_GUIDE.md](https://github.com/savargeir-byte/skanni-dashboard/blob/main/MOBILE_APP_INTEGRATION_GUIDE.md)
- [APP_INTEGRATION_SPEC.md](https://github.com/savargeir-byte/skanni-dashboard/blob/main/APP_INTEGRATION_SPEC.md)

## ✅ Checklist

- [x] FirebaseRepository búin til
- [x] InvoiceRecord uppfært með firestoreId
- [x] InvoiceStore með automatic Firebase sync
- [ ] Scanner kóði uppfærður til að nota imageUri
- [ ] Google Sign-In virkur
- [ ] Prófað á raunverulegum device
- [ ] Staðfest að gögn birtast í dashboard

---

**Staða:** Firebase integration er tilbúin! Þarf bara að uppfæra scanner kóðann til að senda imageUri.
