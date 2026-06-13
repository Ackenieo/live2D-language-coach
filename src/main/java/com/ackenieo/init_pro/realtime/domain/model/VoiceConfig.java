package com.ackenieo.init_pro.realtime.domain.model;

import com.ackenieo.init_pro.shared.domain.BaseValueObject;

import java.util.Objects;
import java.util.Set;

/**
 * 语音配置值对象
 * 封装百炼 Realtime API 的语音参数
 */
public class VoiceConfig extends BaseValueObject {
    private final String voiceName;
    private final Set<String> modalities;

    /** 支持的语音列表 */
    public static final Set<String> SUPPORTED_VOICES = Set.of(
            "Chelsie", "Serena", "Ethan", "Cherry"
    );

    /** 默认配置 */
    public static final VoiceConfig DEFAULT = new VoiceConfig("Chelsie", Set.of("text", "audio"));

    public VoiceConfig(String voiceName, Set<String> modalities) {
        this.voiceName = voiceName;
        this.modalities = Set.copyOf(modalities);
    }

    public String getVoiceName() {
        return voiceName;
    }

    public Set<String> getModalities() {
        return modalities;
    }

    public boolean hasAudio() {
        return modalities.contains("audio");
    }

    public boolean hasText() {
        return modalities.contains("text");
    }

    public VoiceConfig withVoice(String voiceName) {
        return new VoiceConfig(voiceName, this.modalities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VoiceConfig that)) return false;
        return Objects.equals(voiceName, that.voiceName) && Objects.equals(modalities, that.modalities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voiceName, modalities);
    }

    @Override
    public String toString() {
        return "VoiceConfig{voice='" + voiceName + "', modalities=" + modalities + "}";
    }
}
