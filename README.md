# İki Telefon Bir Uygulama - Two Phone Call Forward

Bu uygulama, Bluetooth aracılığıyla iki Android telefonu birbirine bağlayarak, SIM kartlı telefondan gelen aramaları SIM kartısız telefonda gösterip cevaplamayı sağlar.

## Proje Açıklaması

### Senaryo
1. **Telefon A (SIM kartlı):** Arama yapabilen ve arama alabilen asıl telefon
2. **Telefon B (SIM kartısız):** Bluetooth üzerinden Telefon A'ya bağlı olan yardımcı telefon

### Özellikler
✅ Bluetooth üzerinden iki telefonu bağlama  
✅ Gelen aramaları Telefon B'de gösterme  
✅ Telefon B'den aramaları cevaplama ve konuşma  
✅ Telefon A'nın rehberini Telefon B'de görüntüleme  
✅ Telefon B'den Telefon A'yı kullanarak arama yapma  
✅ Ses yönlendirmesi (Telefon B'de ses işitilir ve cevabı verilir)

## Proje Yapısı

```
app/
├── src/main/
│   ├── java/com/twophones/callforward/
│   │   ├── bluetooth/
│   │   │   ├── BluetoothManager.kt        # Bluetooth bağlantı yönetimi
│   │   │   └── BluetoothMessage.kt        # Bluetooth mesaj modeli
│   │   ├── service/
│   │   │   ├── CallMonitoringService.kt   # Çağrı izleme servisi
│   │   │   ├── InCallHandlerService.kt    # Çağrı işleme servisi
│   │   │   ├── BluetoothConnectionService.kt  # Bluetooth bağlantı servisi
│   │   │   └── CallReceiver.kt            # Çağrı alıcısı
│   │   ├── ui/
│   │   │   ├── MainActivity.kt            # Ana UI ekranı
│   │   │   ├── IncomingCallActivity.kt    # Gelen çağrı ekranı
│   │   │   └── CallListener.kt            # Çağrı dinleyicisi
│   │   ├── audio/
│   │   │   └── AudioRouting.kt            # Ses yönlendirmesi
│   │   └── contact/
│   │       └── ContactManager.kt          # Rehber yönetimi
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml          # Ana aktivite layoutu
│       │   └── activity_incoming_call.xml # Gelen çağrı layoutu
│       └── values/
│           ├── strings.xml                # String kaynakları
│           ├── colors.xml                 # Renk kaynakları
│           └── themes.xml                 # Tema ayarları
├── build.gradle                           # App build config
└── AndroidManifest.xml                    # Manifest dosyası
```

## Teknik Mimarı

### Modüller

#### 1. **BluetoothManager**
- Bluetooth bağlantı yönetimi
- SPP (Serial Port Profile) kullanarak iletişim
- Mesaj gönderme/alma
- Bağlantı durumu takibi

```kotlin
val btManager = BluetoothManager(context)
btManager.connectToDevice(device)
btManager.sendMessage(BluetoothMessage("call", "555-1234"))
```

#### 2. **CallMonitoringService**
- Telefon çağrılarını izleme
- Çağrı bilgilerini Bluetooth üzerinden iletme
- Ses işletme

#### 3. **InCallHandlerService**
- TelecomManager ile çağrıları yönetme
- Çağrı durumu değişikliklerini takip etme

#### 4. **AudioRouting**
- Yerel ses kaydı ve oynatma
- Ses cihazı seçimi
- In-call modu yönetimi

#### 5. **ContactManager**
- Cihaz rehberini okuma
- Rehberi Bluetooth üzerinden paylaşma
- Telefon numarasından kişi bulma

## Kurulum

### Gereksinimler
- Android SDK 26 veya üzeri
- Android Gradle Plugin 8.1.0
- Kotlin 1.9.0
- Bu izinler istenir:
  - Bluetooth
  - Telefon state okuma
  - Çağrı yapma
  - Rehber okuma
  - Ses kaydı

### Build Etme

```bash
./gradlew clean build
```

### APK Oluşturma

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Bluetooth Protokolü

Uygulama, Bluetooth üzerinden aşağıdaki mesaj türlerini gönderir:

```
Mesaj Formatı: "type|data"

Örnek Mesajlar:
- "incoming_call|+905551234567"
- "outgoing_call|+905559876543"
- "call_connected|+905551234567"
- "call_ended|ended"
- "contacts|id1,Name1,5551111|id2,Name2,5552222"
- "audio_data|base64encodedaudiodata"
- "call_state|added:+905551234567"
```

## Kullanım Akışı

### Çağrı Alma (Telefon A → Telefon B)

1. Telefon A'ya çağrı gelir
2. CallReceiver tetiklenir
3. CallMonitoringService başlatılır
4. Çağrı bilgisi Bluetooth üzerinden Telefon B'ye gönderilir
5. Telefon B'de IncomingCallActivity gösterilir
6. Kullanıcı Telefon B'de çağrıyı cevaplar
7. Ses Telefon B'den işitilir

### Çağrı Yapma (Telefon B → Telefon A → Karşı Taraf)

1. Telefon B'de rehberden kişi seçilir
2. Çağrı bilgisi Bluetooth üzerinden Telefon A'ya gönderilir
3. Telefon A, çağrıyı Telefon B'nin adını kullanarak yapamaz (sistem kısıtlaması)
4. Telefon A'dan çağrı yapılır
5. Ses Telefon B'de işitilir

## İzinler

### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

## Bilinen Sınırlamalar

1. Sistem kısıtlaması nedeniyle Telefon B'den doğrudan çağrı yapılamaz. Seçili numarayı Telefon A'ya göndermek gerekir.
2. Iki telefon aynı Bluetooth profiline bağlı olmalıdır.
3. Arayan taraf, arayanın numarasını Telefon A olarak görür (Telefon B'nin numarası görülmez).

## Geliştirme Rehberi

### Yeni Bir Mesaj Türü Ekleme

1. `BluetoothMessage` türüne yeni tip ekle
2. `CallMonitoringService` veya `InCallHandlerService`'de işle
3. Alıcı cihazda işleyici ekleme

### Ses İşlemesini Özelleştirme

`AudioRouting` sınıfında ses codec ve parameterlerini değiştir:

```kotlin
// Örnek: AMR codec kullanma
setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

// Örnek: Opus codec kullanma
setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
```

## Sorun Giderme

### Bluetooth Bağlantı Kurulamıyor
- İki cihazi da eşleştirildiğinden emin olun
- İzinleri kontrol edin
- MAC adreslerini doğrulayın

### Ses İşitilmiyor
- Ses düzeyini kontrol edin
- Hoparlör/mikrofon seçimini doğrulayın
- AudioRouting konfigürasyonunu kontrol edin

### Çağrı Almıyor
- CallReceiver'ın kayıtlı olduğunu kontrol edin
- READ_PHONE_STATE izni verilmiş mi deneyin
- Manifest'te IntentFilter ayarlarını kontrol edin

## Lisans

MIT License

## Yazar

- Developed for dual-phone call forwarding system

## Destek

Sorularınız veya sorunlarınız için lütfen bir issue açın.
