package com.ackenieo.init_pro.conversation.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateServiceTest {

    private final PromptTemplateService service = new PromptTemplateService();

    @Test
    void loadsDefaultTemplateFromResourceAndRendersVariables() {
        String prompt = service.getSystemPrompt("default", Map.of("difficulty", "Easy"));

        assertThat(prompt).contains("outgoing and friendly classmate");
        assertThat(prompt).contains("Use Easy vocabulary");
        assertThat(prompt).doesNotContain("{difficulty}");
    }

    @Test
    void fillsMissingSceneVariablesWithSafeDefaults() {
        String prompt = service.getSystemPrompt("airport", Map.of("difficulty", "Easy"));

        assertThat(prompt).contains("your destination");
        assertThat(prompt).contains("scheduled");
        assertThat(prompt).doesNotContain("{destination}");
        assertThat(prompt).doesNotContain("{flight_status}");
    }

    @Test
    void fallsBackUnknownTemplateToDefault() {
        String prompt = service.getSystemPrompt("missing-template", Map.of());

        assertThat(prompt).contains("outgoing and friendly classmate");
    }

    @Test
    void rendersGrammarCorrectionPromptFromTemplate() {
        String prompt = service.getGrammarCorrectionPrompt("I has a apple.");

        assertThat(prompt).contains("grammar correction assistant");
        assertThat(prompt).contains("I has a apple.");
        assertThat(prompt).contains("无");
    }

    @Test
    void appendsRealtimeVisionInstruction() {
        String prompt = service.getRealtimeSystemPrompt("default", "medium", "us", true);

        assertThat(prompt).contains("outgoing and friendly classmate");
        assertThat(prompt).contains("Image input may be available");
        assertThat(prompt).contains("do not describe visual information");
    }
}
