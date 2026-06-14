# Speech Link Plan - Backend

> Goal: define the backend side of the realtime speech link from Bailian realtime audio to the frontend WebSocket client, including audio forwarding, subtitle forwarding, metadata, and latency rules.

## 1. Positioning

`speech-link` is the shared realtime speech capability for the Live2D language coach.

The backend owns:

- connecting to Bailian realtime WebSocket
- sending user audio to Bailian
- forwarding AI audio to the frontend as WebSocket `BinaryMessage`
- forwarding AI/user subtitles and lifecycle messages as JSON text messages
- preserving lightweight response metadata for frontend audio/subtitle alignment

The backend does not own:

- generating Live2D mouth shapes
- asking the main model to output lip-sync JSON
- converting AI audio to JSON/base64 for the browser

The frontend owns audio playback, lip-frame generation, WebAudio clock alignment, and Live2D parameter writes.

## 2. Current Backend Flow

Current implemented path:

1. Frontend connects to `ws://localhost:8520/ws/bailian?token=<accessToken>`.
2. `ChatWebSocketHandler` creates or restores a session.
3. `BailianRealtimeClient` connects to Bailian realtime API.
4. Frontend microphone audio is sent as binary PCM Int16, 16 kHz, mono.
5. Backend forwards user audio to Bailian with `input_audio_buffer.append`.
6. Bailian returns AI audio as `response.audio.delta`.
7. Backend decodes the audio delta and publishes `AiAudioDeltaEvent`.
8. `ChatWebSocketHandler` forwards the decoded audio bytes to the frontend as `BinaryMessage`.
9. Bailian subtitle events are forwarded as JSON messages:
   - `ai_subtitle`
   - `ai_subtitle_complete`
   - `user_subtitle`

This flow should stay streaming-first. Audio must not wait for final subtitles or formatted model output.

## 3. Protocol Contract

### 3.1 Frontend to Backend

JSON text messages:

```json
{ "type": "config", "scene": "hotel", "difficulty": "medium", "accent": "us", "vision": "on" }
{ "type": "start" }
{ "type": "finish" }
{ "type": "text", "text": "I'd like to check in." }
{ "type": "screenshot", "image": "data:image/jpeg;base64,..." }
{ "type": "ping" }
```

Binary messages:

- user microphone audio
- format: PCM Int16
- sample rate: 16000 Hz
- channels: 1

### 3.2 Backend to Frontend

Binary messages:

- AI audio chunks
- format: PCM Int16
- sample rate: 24000 Hz
- channels: 1
- transport remains WebSocket binary, not JSON/base64

JSON text messages:

```json
{ "type": "connected", "sessionId": "uuid", "reconnect": false }
{ "type": "reconnected", "sessionId": "uuid", "reconnect": true }
{ "type": "config_updated", "sessionId": "uuid", "scene": "hotel", "difficulty": "medium", "accent": "us", "vision": true }
{ "type": "ai_subtitle", "text": "Certainly" }
{ "type": "ai_subtitle_complete", "text": "Certainly. Do you have a reservation?" }
{ "type": "user_subtitle", "turnId": "turn-1", "text": "I'd like to check in." }
{ "type": "pronunciation_score", "turnId": "turn-1", "suggestedGrade": "B" }
{ "type": "session_end", "sessionId": "uuid" }
```

## 4. Lightweight Metadata Enhancement

When Bailian provides response metadata such as `response_id` or `item_id`, the backend should preserve it internally and attach it to related JSON messages where possible.

Recommended message shape:

```json
{
  "type": "ai_subtitle",
  "responseId": "response-xxx",
  "itemId": "item-xxx",
  "text": "Certainly"
}
```

Audio should remain binary. Do not wrap each audio chunk in JSON just to attach metadata.

Optional lifecycle messages can be added later if needed:

```json
{ "type": "ai_audio_start", "responseId": "response-xxx", "sampleRate": 24000 }
{ "type": "ai_audio_done", "responseId": "response-xxx" }
```

These lifecycle messages are metadata only. They must not delay binary audio forwarding.

## 5. Lip-Sync Responsibility Boundary

Bailian realtime audio currently provides audio deltas and transcript deltas, not A/I/U/E/O viseme frames. Therefore:

- backend should not require the main model to format lip-sync JSON
- backend should not block audio waiting for final transcript
- backend should not do forced alignment in the realtime path
- frontend should generate approximate lip frames from the actual audio playback stream and subtitle text

This keeps latency low and keeps mouth movement tied to the audio the user actually hears.

## 6. Latency Rules

Backend forwarding rules:

- Forward `response.audio.delta` as soon as it arrives and decodes.
- Do not aggregate audio chunks for lip-sync purposes.
- Do not wait for `response.audio_transcript.done`.
- Do not run heavy alignment or phoneme extraction before forwarding audio.
- Keep audio as binary WebSocket frames.
- Treat subtitles as helpful metadata, not as a dependency for audio playback.

Frontend alignment depends on WebAudio playback time, so network jitter is acceptable as long as chunks arrive before playback underruns.

## 7. Future Upgrade Path

If a future TTS or realtime model provides true phoneme/viseme marks, the backend can pass them through as optional JSON events:

```json
{
  "type": "lip_sync",
  "responseId": "response-xxx",
  "frames": [
    { "tMs": 0, "viseme": "A", "mouthOpen": 0.4 },
    { "tMs": 32, "viseme": "O", "mouthOpen": 0.7 }
  ]
}
```

The frontend should treat service-provided viseme marks as an override for future, unplayed frames only. Already-played frames must not be rewritten.

## 8. Backend Implementation Notes

Likely backend touch points for future implementation:

- `BailianRealtimeClient`
  - read `response_id` and `item_id` from Bailian events
  - include metadata in subtitle domain events
  - optionally publish `ai_audio_start` / `ai_audio_done`

- `AiAudioDeltaEvent`
  - keep `byte[] audioData`
  - optionally add `responseId` and `itemId`

- `AiSubtitleDeltaEvent` / `AiSubtitleCompleteEvent`
  - optionally add `responseId` and `itemId`

- `ChatWebSocketHandler`
  - forward metadata in JSON messages
  - keep audio forwarding as `BinaryMessage`

## 9. Acceptance Criteria

- AI audio still reaches the frontend as low-latency binary chunks.
- Subtitle JSON can carry response metadata when available.
- The main model is never asked to produce lip-sync JSON.
- Audio forwarding does not wait for subtitles, scoring, grammar correction, or session finalization.
- Existing `/ws/bailian` clients continue to work if they ignore new metadata fields.

