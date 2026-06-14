package com.ackenieo.init_pro.evaluation.domain.model;

public final class GradeScale {
    private GradeScale() {
    }

    public static String fromScore(Double score) {
        if (score == null) {
            return "-";
        }
        return fromNormalizedScore(normalizeScore(score));
    }

    public static String fromNormalizedScore(double score) {
        if (score >= 85) return "S";
        if (score >= 60) return "A";
        if (score >= 45) return "B";
        if (score >= 30) return "C";
        if (score >= 15) return "D";
        return "E";
    }

    public static Double toRepresentativeScore(String grade) {
        if (grade == null || grade.isBlank() || "-".equals(grade)) {
            return null;
        }
        return switch (grade) {
            case "S" -> 95D;
            case "A" -> 72D;
            case "B" -> 52D;
            case "C" -> 37D;
            case "D" -> 22D;
            case "E" -> 12D;
            default -> null;
        };
    }

    private static double normalizeScore(double score) {
        if (score >= 0 && score <= 10) {
            return score * 10;
        }
        return score;
    }
}
