# Two Phone Call Forward - Copilot Instructions

## Project Overview
This is an Android application that enables two phones connected via Bluetooth to forward calls from a SIM-enabled phone to a non-SIM phone. The non-SIM phone can answer calls, access contacts, and initiate calls through the SIM-enabled phone.

## Architecture

### Core Components

1. **BluetoothManager** (`bluetooth/BluetoothManager.kt`)
   - Manages Bluetooth SPP connections
   - Handles message serialization/deserialization
   - Tracks connection status
   - Implements message buffering and error handling

2. **Call Services** (`service/`)
   - `CallMonitoringService`: Monitors call states and routes audio
   - `InCallHandlerService`: Implements InCallService for call interception
   - `CallReceiver`: BroadcastReceiver for PHONE_STATE changes
   - `BluetoothConnectionService`: Manages connection lifecycle

3. **UI Components** (`ui/`)
   - `MainActivity`: Main interface for device connection and contact display
   - `IncomingCallActivity`: Full-screen incoming call interface
   - `CallListener`: Call state change listener

4. **Audio Subsystem** (`audio/AudioRouting.kt`)
   - Manages audio recording from microphone
   - Handles audio playback from Bluetooth stream
   - Routes audio between devices
   - Uses Android's AudioManager for device selection

5. **Contact Management** (`contact/ContactManager.kt`)
   - Reads device contacts
   - Serializes for Bluetooth transmission
   - Matches incoming calls to contacts

## Data Flow

### Incoming Call Flow
```
Phone A (SIM): Receives call
    ↓
CallReceiver intercepts PHONE_STATE_CHANGED
    ↓
CallMonitoringService starts
    ↓
Send via BluetoothManager: "incoming_call|+905551234567"
    ↓
Phone B (No SIM): 
    ↓
IncomingCallActivity appears
    ↓
User taps Accept/Reject
    ↓
AudioRouting streams audio bidirectionally
```

### Call Initiation Flow (Phone B)
```
Phone B (No SIM): User selects contact
    ↓
Send via BluetoothManager: "initiate_call|+905551234567"
    ↓
Phone A (SIM): Receives command
    ↓
Makes actual call
    ↓
IncomingCallActivity shows on Phone B
    ↓
User answers on Phone B
    ↓
Audio streams via AudioRouting
```

## Bluetooth Message Protocol

Messages are sent with format: `TYPE|DATA`

| Type | Data Format | Direction | Purpose |
|------|------------|-----------|---------|
| `incoming_call` | Phone number | A→B | Notify of incoming call |
| `outgoing_call` | Phone number | A→B | Notify of outgoing call |
| `call_connected` | Phone number | Bi | Call established |
| `call_ended` | "ended" | Bi | Call terminated |
| `initiate_call` | Phone number | B→A | Request call initiation |
| `contacts` | CSV of contacts | A→B | Share contact list |
| `audio_data` | Base64 encoded | Bi | Audio stream data |
| `call_state` | state:data | Bi | General state change |

## Key Design Decisions

### 1. Why SPP (Serial Port Profile)?
- Simple, reliable connection
- Works across devices
- Good bandwidth for audio
- Standard serial-like interface

### 2. Audio Architecture
- Uses Android MediaRecorder for capture
- Uses AudioTrack for playback
- Routes through IN_CALL audio mode
- Prevents echo by using different channels

### 3. Permissions Handling
- Runtime permissions for Android 6+
- Manifest declares all required permissions
- RequestPermissionsLauncher handles denial flow

### 4. Service Design
- CallMonitoringService survives call duration
- Started by CallReceiver on call events
- Stopped when call ends
- Sticky service for reliability

## Extension Points

### Adding New Bluetooth Message Types
1. Add case in `CallMonitoringService.handleBluetoothMessage()`
2. Create corresponding handler method
3. Send acknowledgment if needed

### Adding UI Features
1. Create new Activity extending AppCompatActivity
2. Use ViewBinding for UI references
3. Register in AndroidManifest.xml
4. Add intent filter if needed for launches

### Custom Audio Codecs
Modify `AudioRouting.startRecording()` to use different encoder:
```kotlin
setAudioEncoder(MediaRecorder.AudioEncoder.OPUS) // or AAC, etc.
```

### Contact Filtering
In `ContactManager.loadContacts()`, add filter logic:
```kotlin
if (pc.moveToFirst()) {
    val phoneNumber = pc.getString(...)
    if (phoneNumber.matches(desiredPattern)) {
        contacts.add(Contact(...))
    }
}
```

## Testing Considerations

### Unit Testing
Mock BluetoothSocket and BluetoothAdapter

### Integration Testing
- Create virtual devices via AVD Manager
- Use Bluetooth emulator features
- Test with actual Bluetooth hardware

### Manual Testing Steps
1. Install APK on both devices
2. Configure Bluetooth pairing
3. Open app on both devices
4. Test incoming call forward
5. Test contact sharing
6. Test audio quality in call

## Performance Optimizations

### Current Limitations
- Fixed buffer size (1024 bytes) for Bluetooth
- Linear device discovery
- No connection pooling

### Potential Improvements
- Increase buffer for larger audio chunks
- Implement threaded device discovery
- Add connection retry logic with backoff
- Cache contact list
- Compress audio on transmission

## Security Considerations

### Current Implementation
- No encryption (uses default Bluetooth security)
- No authentication beyond Bluetooth pairing

### Production Recommendations
- Implement payload encryption (AES)
- Add device authentication tokens
- Validate message integrity (HMAC)
- Sanitize contact data
- Handle permissions carefully

## Debugging Tips

### Enable Verbose Logging
All modules use TAG for categorization:
```kotlin
Log.d(TAG, "Debug message")
Log.e(TAG, "Error message", exception)
```

Filter in logcat: `TAG:I *:S` (show all from TAG, silence others)

### Bluetooth-Specific Debugging
- Check logcat for "BluetoothManager" tag
- Verify socket state: `bluetoothSocket?.isConnected`
- Monitor I/O stream status
- Check permission grants

### Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Connection drops | Buffer overflow | Reduce message size |
| Audio echo | Audio routing conflict | Verify IN_CALL mode |
| Calls not forwarded | Manifest registration | Verify receiver in manifest |
| Permission denied | Runtime permissions | Request at activity start |

## Dependencies

- androidx.appcompat:appcompat - UI framework
- androidx.lifecycle:lifecycle-* - Lifecycle management
- com.google.android.material:material - Material Design
- kotlinx.coroutines - Async operations

## Build Configuration

- Target SDK: 34
- Min SDK: 26
- Kotlin: 1.9.0
- Gradle: 8.1.0

## Next Steps for Development

1. [ ] Implement call recording
2. [ ] Add video call support
3. [ ] Implement contact search
4. [ ] Add call history
5. [ ] Implement call waiting
6. [ ] Add international dialing support
7. [ ] Implement call forwarding rules
8. [ ] Add TLS/encryption for Bluetooth messages

## Resources

- Android Telecom Framework: https://developer.android.com/reference/android/telecom/package-summary
- Bluetooth Documentation: https://developer.android.com/guide/topics/connectivity/bluetooth
- Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
- Material Design 3: https://material.io/design

