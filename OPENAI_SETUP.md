# OpenAI Integration Setup

## 🎯 Hvað er þetta?

OpenAI GPT-4 Vision integration fyrir SkanniApp sem bætir OCR nákvæmni verulega, sérstaklega fyrir:

- **Löng reikningsnúmer** (30+ stafir)
- **Margfalt VSK** (11% og 24% á sama reikning)
- **Flóknar íslenskar kvittanir**
- **Handskrifað texti**

## 🔑 Hvernig á að setja upp API key

### 1. Fá OpenAI API key

1. Farðu á [platform.openai.com/api-keys](https://platform.openai.com/api-keys)
2. Skráðu þig inn eða búðu til aðgang
3. Smelltu á "Create new secret key"
4. Afritaðu keyinn (hann byrjar á `sk-proj-...`)

### 2. Setja API key inn í appið

1. Opnaðu SkanniApp
2. Farðu í **Stillingar** (⚙️ táknið)
3. Kveiktu á "Nota OpenAI"
4. Límdu API key inn í reitinn
5. Smelltu "Vista API Key"

✅ API keyinn er vistaður encrypted á símanum þínum!

## 💰 Kostnaður

OpenAI GPT-4o with Vision kostar:
- **$0.005** per image (ca. 0.7 kr)
- Ef þú skannar 100 reikninga á mánuði = ca. **70 kr/mánuði**

Þú getur séð kostnað hér: [openai.com/pricing](https://openai.com/pricing)

## 🔄 Hvernig virkar þetta?

### Án OpenAI (sjálfgefið):
```
1. ML Kit OCR → Lestur texta
2. IcelandicInvoiceParser → Regex patterns
3. Niðurstaða: Grunnnákvæmni
```

### Með OpenAI:
```
1. ML Kit OCR → Grunnlestur
2. OpenAI GPT-4 Vision → Advanced lestur
3. IcelandicInvoiceParser → Validation
4. Niðurstaða: Mjög hátt accuracy
```

## 📊 Mismunur

| Feature | Án OpenAI | Með OpenAI |
|---------|-----------|------------|
| Einfaldar kvittanir | ✅ Gott | ✅ Frábært |
| Flóknar kvittanir | ⚠️ Frekar gott | ✅ Mjög gott |
| Löng reikningsnúmer | ❌ Vantar oft | ✅ Fullt support |
| Margfalt VSK | ⚠️ Takmarkað | ✅ Fullt support |
| Handskrift | ❌ Virkar ekki | ✅ Frekar gott |
| Kostnaður | Ókeypis | ~0.7 kr/reikningur |
| Hraði | Mjög hratt | 2-3 sek |

## 🔧 Tæknilegar upplýsingar

### Secure Storage
API keyinn er vistaður með Android EncryptedSharedPreferences:
- AES256_GCM encryption
- MasterKey með AES256 scheme
- Öruggari en plain SharedPreferences

### Files búnar til:
```
app/src/main/java/.../
├── ocr/
│   └── OpenAiOcrService.kt        # GPT-4 Vision integration
├── data/
│   └── SecurePreferences.kt        # Encrypted API key storage
├── ui/settings/
│   └── SettingsScreen.kt           # UI fyrir API key setup
└── model/
    └── InvoiceRecord.kt            # Updated með vatBreakdown
```

### Dependencies bætt við:
```gradle
implementation 'com.aallam.openai:openai-client:3.6.2'
implementation 'io.ktor:ktor-client-okhttp:2.3.7'
```

## 🧪 Testing

Prófa án OpenAI:
```kotlin
// Virkar eins og áður
val invoice = IcelandicInvoiceParser.parseInvoiceText(ocrText)
```

Prófa með OpenAI:
```kotlin
val securePrefs = SecurePreferences(context)
if (securePrefs.hasOpenAiApiKey()) {
    val service = OpenAiOcrService(context, securePrefs.getOpenAiApiKey()!!)
    val result = service.processInvoice(imageUri)
    result.onSuccess { invoice ->
        // invoice hefur vatBreakdown og löng reikningsnúmer
    }
}
```

## 📝 Breytingar á InvoiceRecord

### Nýir fields:
```kotlin
data class VatBreakdown(
    val rate: Double,        // 11.0 eða 24.0
    val amount: Double,      // VSK upphæð
    val baseAmount: Double   // Grunnupphæð án VSK
)

data class InvoiceRecord(
    // ...existing fields...
    val vatBreakdown: List<VatBreakdown> = emptyList(),  // NEW
    val invoiceNumber: String? = null,  // Now supports 30+ chars
    // ...
)
```

### Helper functions:
```kotlin
invoice.vatBreakdownString  
// Output: "11%: 123.45 kr, 24%: 456.78 kr"
```

## 🚀 Next Steps

1. ✅ Build appið með nýju dependencies
2. ✅ Test á símanum
3. 📱 Setja API key inn í Settings
4. 🧪 Prófa að skanna reikning með margfalt VSK
5. 📊 Sjá betri nákvæmni!

## ⚠️ Athugasemdir

- OpenAI er **optional** - appið virkar áfram án þess
- API key er **aldrei** sendur til Firebase eða okkar servera
- Ef API key vantar, notar appið bara ML Kit + IcelandicInvoiceParser
- Ef OpenAI call failar, fallback til local parsing

## 🔐 Privacy

- API keyinn er encrypted locally
- Myndir eru sendar beint til OpenAI (ekki í gegnum okkar servera)
- OpenAI privacy policy: [openai.com/privacy](https://openai.com/privacy)
- Þú getur slökkt á OpenAI hvenær sem er í Settings
