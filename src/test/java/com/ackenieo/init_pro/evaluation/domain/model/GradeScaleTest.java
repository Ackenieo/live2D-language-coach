package com.ackenieo.init_pro.evaluation.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GradeScaleTest {

    @Test
    void mapsTenPointScoresToFriendlyGrades() {
        assertThat(GradeScale.fromScore(8.4)).isEqualTo("A");
        assertThat(GradeScale.fromScore(6.0)).isEqualTo("A");
        assertThat(GradeScale.fromScore(4.5)).isEqualTo("B");
        assertThat(GradeScale.fromScore(1.4)).isEqualTo("E");
    }

    @Test
    void mapsHundredPointScoresToFriendlyGrades() {
        assertThat(GradeScale.fromScore(85D)).isEqualTo("S");
        assertThat(GradeScale.fromScore(60D)).isEqualTo("A");
        assertThat(GradeScale.fromScore(45D)).isEqualTo("B");
        assertThat(GradeScale.fromScore(30D)).isEqualTo("C");
        assertThat(GradeScale.fromScore(15D)).isEqualTo("D");
        assertThat(GradeScale.fromScore(14.9)).isEqualTo("E");
    }
}
