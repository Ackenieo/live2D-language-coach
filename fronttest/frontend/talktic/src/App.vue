<template>
  <div class="app">
    <div class="header">
      <h1>百炼相机对话</h1>
      <div class="status" :class="statusClass">{{ statusText }}</div>
      <div v-if="isCalling" class="timer-line">⏱ {{ formatTime(elapsedSeconds) }}</div>
      <div v-if="sessionId" class="session-line">会话ID: {{ sessionId }}</div>
      <div v-if="reportSummary" class="report-line">{{ reportSummary }}</div>
    </div>

    <div class="controls">
      <button v-if="!isCalling" @click="startCall" class="btn btn-start" :disabled="isConnecting">
        {{ isConnecting ? '启动中...' : '开始通话' }}
      </button>
      <button v-else @click="stopCall" class="btn btn-stop">结束通话</button>
      <button v-if="isCalling && !visionCapturing" @click="startVisionCapture" class="btn btn-vision">开启视觉</button>
      <button v-if="visionCapturing" @click="stopVisionCapture" class="btn btn-vision-stop">关闭视觉</button>
      <label v-if="visionCapturing" class="threshold-label">
        相似阈值: {{ similarityThreshold.toFixed(2) }}
        <input type="range" min="0.50" max="0.99" step="0.01" v-model.number="similarityThreshold" />
      </label>
    </div>

    <div class="audio-visualizer">
      <div class="wave-container">
        <div v-for="i in 20" :key="i" class="wave-bar" :style="{ height: getWaveHeight(i) + 'px' }"></div>
      </div>
    </div>

    <video ref="cameraVideoRef" autoplay playsinline muted class="camera-preview" :class="{ active: visionCapturing }"></video>

    <div class="subtitle-container" ref="subtitleContainerRef">
      <p v-if="messages.length === 0" style="color:#999">字幕将在这里显示...</p>
      <div v-for="msg in messages" :key="msg.id" class="message-bubble" :class="messageClass(msg.type)">
        <template v-if="msg.type === 'vision_result'">
          <div class="vision-label">视觉识别结果</div>
          <span>{{ msg.text }}</span>
        </template>
        <template v-else-if="msg.type === 'session_end'">
          <div class="vision-label">对话总结</div>
          <span>{{ msg.text }}</span>
          <div v-if="msg.reportMeta" class="score-line">{{ msg.reportMeta }}</div>
        </template>
        <template v-else>
          <span>{{ msg.text }}</span>
          <div v-if="msg.grammarCorrection" class="grammar-line">语法矫正: {{ msg.grammarCorrection }}</div>
          <div v-if="msg.pronunciationScore" class="score-line">{{ msg.pronunciationScore }}</div>
        </template>
      </div>
    </div>

    <div class="info">
      <p>提示: 请允许浏览器使用麦克风和摄像头权限</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'

const isWsConnected = ref(false)
const isCalling = ref(false)
const isConnecting = ref(false)
const ws = ref(null)
const audioContext = ref(null)
const mediaStream = ref(null)
const scriptProcessor = ref(null)
const audioOutputContext = ref(null)
const audioQueue = ref([])
const isPlayingAudio = ref(false)
const waveHeights = ref(new Array(20).fill(5))
const messages = ref([])
const subtitleContainerRef = ref(null)
const cameraVideoRef = ref(null)
const cameraStream = ref(null)
const visionCapturing = ref(false)
const visionTimer = ref(null)
const lastPHash = ref(null)
const frameCount = ref(0)
const authToken = ref('')
const sessionId = ref(localStorage.getItem('talktic-session-id') || '')
const shouldReconnect = ref(false)
const reconnectAttempts = ref(0)
const heartbeatTimer = ref(null)
const reportSummary = ref('')
const elapsedSeconds = ref(0)
const elapsedTimer = ref(null)

const PHASH_SIZE = 8
const similarityThreshold = ref(0.90)
const CAPTURE_INTERVAL_MS = 3000
const AUDIO_BUFFER_SIZE = 1024
const SCREEN_WIDTH = 240
const SCREEN_HEIGHT = 180
const SCREEN_QUALITY = 0.2
const HEARTBEAT_INTERVAL_MS = 15000
const MAX_RECONNECT_ATTEMPTS = 3

function newMessageId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function formatTime(totalSeconds) {
  const m = Math.floor(totalSeconds / 60)
  const s = totalSeconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function persistSessionId(value) {
  sessionId.value = value || ''
  if (value) {
    localStorage.setItem('talktic-session-id', value)
  } else {
    localStorage.removeItem('talktic-session-id')
  }
}

async function fetchToken() {
  try {
    const res = await fetch('/api/test/generate-token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'userId=web_user&phone=13800138000'
    })
    const data = await res.json()
    if (data.accessToken) {
      authToken.value = data.accessToken
      console.log('获取Token成功')
      return true
    }
    return false
  } catch (e) {
    console.error('获取Token失败', e)
    return false
  }
}

function buildWsUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const params = new URLSearchParams({ token: authToken.value })
  if (shouldReconnect.value && sessionId.value) {
    params.set('reconnectSessionId', sessionId.value)
  }
  return `${protocol}//${window.location.host}/ws/bailian?${params.toString()}`
}

function connectWebSocket() {
  if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
    return
  }

  const wsUrl = buildWsUrl()
  console.log('连接WebSocket:', wsUrl.replace(authToken.value, '***'))
  ws.value = new WebSocket(wsUrl)
  ws.value.binaryType = 'arraybuffer'

  ws.value.onopen = () => {
    isWsConnected.value = true
    isConnecting.value = false
    reconnectAttempts.value = 0
    startHeartbeat()
    if (shouldReconnect.value && sessionId.value) {
      ws.value.send(JSON.stringify({ type: 'reconnect', sessionId: sessionId.value }))
    }
    console.log('WebSocket已连接')
  }

  ws.value.onmessage = (event) => {
    if (event.data instanceof ArrayBuffer) {
      playAudioData(event.data)
      updateWaveAnimation()
    } else if (typeof event.data === 'string') {
      try {
        const msg = JSON.parse(event.data)
        handleServerMessage(msg)
      } catch (e) {
        console.error('parse ws message failed', e)
      }
    }
  }

  ws.value.onclose = () => {
    isWsConnected.value = false
    isCalling.value = false
    isConnecting.value = false
    ws.value = null
    stopHeartbeat()
    cleanupMedia()
    if (shouldReconnect.value && reconnectAttempts.value < MAX_RECONNECT_ATTEMPTS) {
      reconnectAttempts.value++
      window.setTimeout(connectWebSocket, 1000 * reconnectAttempts.value)
    }
    console.log('WebSocket已断开')
  }

  ws.value.onerror = () => {
    isConnecting.value = false
    console.error('WebSocket连接错误')
  }
}

function handleServerMessage(msg) {
  if (msg.type === 'connected' || msg.type === 'reconnected') {
    if (msg.sessionId) {
      persistSessionId(msg.sessionId)
    }
    shouldReconnect.value = true
    return
  }

  if (msg.type === 'config_updated') {
    return
  }

  if (msg.type === 'reconnect_failed') {
    shouldReconnect.value = false
    persistSessionId('')
    reportSummary.value = '重连失败，已创建新会话'
    return
  }

  if (msg.type === 'pong') {
    return
  }

  addMessage(msg)
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer.value = window.setInterval(() => {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type: 'ping' }))
    }
  }, HEARTBEAT_INTERVAL_MS)
}

function stopHeartbeat() {
  if (heartbeatTimer.value) {
    clearInterval(heartbeatTimer.value)
    heartbeatTimer.value = null
  }
}

function startElapsedTimer() {
  stopElapsedTimer()
  elapsedSeconds.value = 0
  elapsedTimer.value = window.setInterval(() => {
    elapsedSeconds.value++
  }, 1000)
}

function stopElapsedTimer() {
  if (elapsedTimer.value) {
    clearInterval(elapsedTimer.value)
    elapsedTimer.value = null
  }
}

watch(messages, async () => {
  await nextTick()
  if (subtitleContainerRef.value) {
    subtitleContainerRef.value.scrollTop = subtitleContainerRef.value.scrollHeight
  }
}, { deep: true })

const statusText = computed(() => {
  if (isConnecting.value) return '启动中...'
  if (isCalling.value) return '通话中'
  if (isWsConnected.value) return '已连接'
  return '未连接'
})

const statusClass = computed(() => {
  if (isCalling.value || isWsConnected.value) return 'status-connected'
  return 'status-disconnected'
})

function messageClass(type) {
  if (type === 'user_subtitle') return 'message-user'
  if (type === 'vision_result') return 'message-vision'
  if (type === 'session_end') return 'message-vision'
  return 'message-ai'
}

function getWaveHeight(index) {
  return waveHeights.value[index - 1] || 5
}

async function startCall() {
  if (isCalling.value || isConnecting.value) {
    return
  }

  isConnecting.value = true
  reportSummary.value = ''
  try {
    if (!authToken.value) {
      const ok = await fetchToken()
      if (!ok) {
        throw new Error('获取Token失败')
      }
    }

    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      connectWebSocket()
      await new Promise((resolve, reject) => {
        const check = setInterval(() => {
          if (ws.value && ws.value.readyState === WebSocket.OPEN) {
            clearInterval(check)
            resolve()
          }
        }, 100)
        setTimeout(() => { clearInterval(check); reject(new Error('WebSocket连接超时')) }, 5000)
      })
    }

    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket not ready')
    }

    mediaStream.value = await navigator.mediaDevices.getUserMedia({
      audio: { sampleRate: 16000, channelCount: 1, echoCancellation: true, noiseSuppression: true }
    })

    messages.value = []
    elapsedSeconds.value = 0
    startElapsedTimer()
    ws.value.send(JSON.stringify({
      type: 'config',
      lang: 'en',
      role: 'English Coach',
      scene: 'default',
      difficulty: 'medium',
      accent: 'us'
    }))
    ws.value.send(JSON.stringify({ type: 'start' }))
    startAudioCapture()
    isCalling.value = true
    isConnecting.value = false
    shouldReconnect.value = true
  } catch (error) {
    console.error('startCall failed', error)
    isConnecting.value = false
    alert('无法获取麦克风权限或开始通话: ' + error.message)
  }
}

function startAudioCapture() {
  audioContext.value = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 })
  const input = audioContext.value.createMediaStreamSource(mediaStream.value)
  scriptProcessor.value = audioContext.value.createScriptProcessor(AUDIO_BUFFER_SIZE, 1, 1)

  scriptProcessor.value.onaudioprocess = (e) => {
    if (!isCalling.value) {
      return
    }
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      const inputData = e.inputBuffer.getChannelData(0)
      const int16Data = new Int16Array(inputData.length)
      for (let i = 0; i < inputData.length; i++) {
        const s = Math.max(-1, Math.min(1, inputData[i]))
        int16Data[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
      }
      ws.value.send(int16Data.buffer)
    }
  }

  input.connect(scriptProcessor.value)
  scriptProcessor.value.connect(audioContext.value.destination)
}

function playAudioData(data) {
  if (!audioOutputContext.value) {
    audioOutputContext.value = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 24000 })
  }
  if (audioOutputContext.value.state === 'suspended') {
    audioOutputContext.value.resume()
  }

  const int16Array = new Int16Array(data)
  const float32Array = new Float32Array(int16Array.length)
  for (let i = 0; i < int16Array.length; i++) {
    float32Array[i] = int16Array[i] / 32768.0
  }

  audioQueue.value.push(float32Array)
  if (!isPlayingAudio.value) playNextInQueue()
}

function playNextInQueue() {
  if (audioQueue.value.length === 0) {
    isPlayingAudio.value = false
    return
  }

  isPlayingAudio.value = true
  const float32Array = audioQueue.value.shift()
  const buffer = audioOutputContext.value.createBuffer(1, float32Array.length, 24000)
  buffer.getChannelData(0).set(float32Array)
  const source = audioOutputContext.value.createBufferSource()
  source.buffer = buffer
  source.connect(audioOutputContext.value.destination)
  source.onended = () => playNextInQueue()
  source.start()
}

function updateWaveAnimation() {
  waveHeights.value = waveHeights.value.map(() => Math.random() * 40 + 10)
  setTimeout(() => {
    waveHeights.value = waveHeights.value.map(() => 5)
  }, 200)
}

function addMessage(msg) {
  if (msg.type === 'ai_subtitle') {
    const last = messages.value[messages.value.length - 1]
    if (last && last.type === 'ai_subtitle') {
      last.text += msg.text || ''
      messages.value = [...messages.value.slice(0, -1), last]
    } else {
      messages.value = [...messages.value, { id: newMessageId(), type: 'ai_subtitle', text: msg.text || '' }]
    }
    return
  }

  if (msg.type === 'ai_subtitle_complete') {
    const last = messages.value[messages.value.length - 1]
    if (last && (last.type === 'ai_subtitle' || last.type === 'ai_subtitle_complete')) {
      last.type = 'ai_subtitle_complete'
      last.text = msg.text || ''
      messages.value = [...messages.value.slice(0, -1), last]
    } else {
      messages.value = [...messages.value, { id: newMessageId(), type: 'ai_subtitle_complete', text: msg.text || '' }]
    }
    return
  }

  if (msg.type === 'user_subtitle') {
    messages.value = [...messages.value, {
      id: msg.turnId || newMessageId(),
      turnId: msg.turnId || '',
      type: 'user_subtitle',
      text: msg.text || '',
      grammarCorrection: '',
      pronunciationScore: ''
    }]
    return
  }

  if (msg.type === 'grammar_correction') {
    const idx = messages.value.findIndex(item => item.turnId && item.turnId === msg.turnId)
    if (idx >= 0) {
      const next = [...messages.value]
      next[idx] = {
        ...next[idx],
        grammarCorrection: msg.text || ''
      }
      messages.value = next
    }
    return
  }

  if (msg.type === 'pronunciation_score') {
    const idx = messages.value.findIndex(item => item.turnId && item.turnId === msg.turnId)
    if (idx >= 0) {
      const next = [...messages.value]
      next[idx] = {
        ...next[idx],
        pronunciationScore: `总评 ${msg.suggestedGrade ?? '-'} / 准确度 ${msg.accuracyGrade ?? '-'} / 流利度 ${msg.fluencyGrade ?? '-'} / 完整度 ${msg.completionGrade ?? '-'}`
      }
      messages.value = next
    }
    return
  }

  if (msg.type === 'session_end') {
    reportSummary.value = msg.summary || ''
    const reportMeta = `总评 ${msg.overallGrade ?? '-'} / 准确度 ${msg.accuracyGrade ?? '-'} / 流利度 ${msg.fluencyGrade ?? '-'} / 完整度 ${msg.completionGrade ?? '-'}`
    messages.value = [...messages.value, {
      id: newMessageId(),
      type: 'session_end',
      text: msg.summary || '对话已结束',
      reportMeta
    }]
    isCalling.value = false
    shouldReconnect.value = false
    stopHeartbeat()
    stopElapsedTimer()
    return
  }

  messages.value = [...messages.value, { id: newMessageId(), type: msg.type, text: msg.text || '' }]
}

function computePHash(imageData) {
  const data = imageData.data
  const w = imageData.width
  const h = imageData.height
  const grays = []
  const cellW = w / PHASH_SIZE
  const cellH = h / PHASH_SIZE
  for (let gy = 0; gy < PHASH_SIZE; gy++) {
    for (let gx = 0; gx < PHASH_SIZE; gx++) {
      const px = Math.floor(gx * cellW + cellW / 2)
      const py = Math.floor(gy * cellH + cellH / 2)
      const idx = (py * w + px) * 4
      const gray = 0.299 * data[idx] + 0.587 * data[idx + 1] + 0.114 * data[idx + 2]
      grays.push(gray)
    }
  }
  const avg = grays.reduce((a, b) => a + b, 0) / grays.length
  let hash = ''
  for (const g of grays) hash += g >= avg ? '1' : '0'
  return hash
}

function hashSimilarity(hash1, hash2) {
  if (!hash1 || !hash2 || hash1.length !== hash2.length) return 0
  let same = 0
  for (let i = 0; i < hash1.length; i++) {
    if (hash1[i] === hash2[i]) same++
  }
  return same / hash1.length
}

async function startVisionCapture() {
  if (!isCalling.value) {
    return
  }

  try {
    cameraStream.value = await navigator.mediaDevices.getUserMedia({
      video: { width: 640, height: 480, facingMode: 'user' }
    })
    cameraVideoRef.value.srcObject = cameraStream.value
    await new Promise(resolve => cameraVideoRef.value.onloadeddata = resolve)
    visionCapturing.value = true
    frameCount.value = 0
    lastPHash.value = null
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type: 'config', vision: 'on', scene: 'default', difficulty: 'medium', accent: 'us' }))
    }
    captureAndSendFrame()
    visionTimer.value = setInterval(captureAndSendFrame, CAPTURE_INTERVAL_MS)
  } catch (e) {
    console.error('getUserMedia(video) failed', e)
    alert('无法获取摄像头权限')
  }
}

function stopVisionCapture() {
  if (visionTimer.value) {
    clearInterval(visionTimer.value)
    visionTimer.value = null
  }
  if (cameraStream.value) {
    cameraStream.value.getTracks().forEach(t => t.stop())
    cameraStream.value = null
  }
  if (cameraVideoRef.value) {
    cameraVideoRef.value.srcObject = null
  }
  visionCapturing.value = false
  lastPHash.value = null
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    ws.value.send(JSON.stringify({ type: 'config', vision: 'off', scene: 'default', difficulty: 'medium', accent: 'us' }))
  }
}

function captureAndSendFrame() {
  if (!isCalling.value) {
    return
  }

  const video = cameraVideoRef.value
  if (!video || video.readyState < 2 || !ws.value || ws.value.readyState !== WebSocket.OPEN) {
    return
  }

  const hashCanvas = document.createElement('canvas')
  const hashCtx = hashCanvas.getContext('2d')
  hashCanvas.width = PHASH_SIZE * 4
  hashCanvas.height = PHASH_SIZE * 4
  hashCtx.drawImage(video, 0, 0, hashCanvas.width, hashCanvas.height)
  const hashImageData = hashCtx.getImageData(0, 0, hashCanvas.width, hashCanvas.height)

  const currentHash = computePHash(hashImageData)
  frameCount.value++

  if (lastPHash.value) {
    const similarity = hashSimilarity(lastPHash.value, currentHash)
    if (similarity >= similarityThreshold.value) {
      return
    }
  }

  lastPHash.value = currentHash
  const sendCanvas = document.createElement('canvas')
  const sendCtx = sendCanvas.getContext('2d')
  sendCanvas.width = SCREEN_WIDTH
  sendCanvas.height = SCREEN_HEIGHT
  sendCtx.drawImage(video, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)
  const base64 = sendCanvas.toDataURL('image/jpeg', SCREEN_QUALITY)

  ws.value.send(JSON.stringify({ type: 'screenshot', image: base64 }))
}

function stopCall() {
  if (!isCalling.value) {
    return
  }

  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    ws.value.send(JSON.stringify({ type: 'finish' }))
  }
  isCalling.value = false
  isConnecting.value = false
  cleanupMedia()
}

function cleanupMedia() {
  if (scriptProcessor.value) {
    scriptProcessor.value.disconnect()
    scriptProcessor.value = null
  }
  if (audioContext.value) {
    audioContext.value.close()
    audioContext.value = null
  }
  if (audioOutputContext.value) {
    audioOutputContext.value.close()
    audioOutputContext.value = null
  }
  if (mediaStream.value) {
    mediaStream.value.getTracks().forEach(t => t.stop())
    mediaStream.value = null
  }
  audioQueue.value = []
  isPlayingAudio.value = false
  stopVisionCapture()
}

function cleanupAll() {
  shouldReconnect.value = false
  stopHeartbeat()
  stopElapsedTimer()
  cleanupMedia()
  if (ws.value) {
    const socket = ws.value
    ws.value = null
    if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
      socket.close()
    }
  }
}

window.addEventListener('beforeunload', () => {})
window.addEventListener('pagehide', () => {})

onMounted(async () => {
  await fetchToken()
})

onUnmounted(() => {
  cleanupAll()
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
.app { max-width: 600px; margin: 0 auto; padding: 20px; }
.header { text-align: center; margin-bottom: 30px; }
.header h1 { color: #333; margin-bottom: 10px; }
.status { display: inline-block; padding: 8px 16px; border-radius: 20px; font-size: 14px; }
.status-connected { background: #e6f7e6; color: #2d8a2d; }
.status-disconnected { background: #fde8e8; color: #c53030; }
.timer-line { margin-top: 8px; color: #4CAF50; font-size: 22px; font-weight: bold; font-variant-numeric: tabular-nums; }
.session-line { margin-top: 8px; color: #666; font-size: 12px; word-break: break-all; }
.report-line { margin-top: 8px; color: #1565C0; font-size: 13px; }
.controls { text-align: center; margin-bottom: 20px; }
.btn { padding: 12px 24px; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; margin: 0 8px; transition: background 0.2s; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-start { background: #4CAF50; color: white; }
.btn-start:hover:not(:disabled) { background: #45a049; }
.btn-stop { background: #f44336; color: white; }
.btn-stop:hover { background: #da190b; }
.btn-vision { background: #2196F3; color: white; }
.btn-vision:hover { background: #1976D2; }
.btn-vision-stop { background: #FF9800; color: white; }
.btn-vision-stop:hover { background: #F57C00; }
.threshold-label { display: block; margin-top: 10px; font-size: 14px; color: #666; }
.threshold-label input { vertical-align: middle; margin-left: 8px; width: 150px; }
.audio-visualizer { text-align: center; margin-bottom: 20px; }
.wave-container { display: inline-flex; align-items: flex-end; gap: 3px; height: 50px; }
.wave-bar { width: 8px; background: linear-gradient(to top, #4CAF50, #81C784); border-radius: 4px; transition: height 0.1s; min-height: 5px; }
.camera-preview { display: none; width: 100%; max-width: 320px; border-radius: 8px; margin: 0 auto 20px; }
.camera-preview.active { display: block; }
.subtitle-container { max-height: 300px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; background: #fafafa; margin-bottom: 20px; }
.message-bubble { padding: 10px 14px; border-radius: 12px; margin-bottom: 8px; max-width: 80%; word-wrap: break-word; font-size: 14px; line-height: 1.5; }
.message-ai { background: #e3f2fd; color: #1565C0; margin-right: auto; }
.message-user { background: #e8f5e9; color: #2E7D32; margin-left: auto; text-align: right; }
.message-vision { background: #fff3e0; color: #E65100; }
.vision-label { font-size: 11px; color: #999; margin-bottom: 4px; }
.grammar-line { font-size: 12px; color: #FF6F00; margin-top: 4px; }
.score-line { font-size: 12px; color: #6A1B9A; margin-top: 4px; }
.info { text-align: center; color: #999; font-size: 13px; }
</style>
