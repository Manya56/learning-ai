package com.learningai.backend.util;

import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

        // Max lengths
        private static final int MAX_QUESTION_LENGTH = 1000;
        private static final int MAX_CONCEPT_LENGTH = 200;
        private static final int MAX_MESSAGE_LENGTH = 2000;

        // ── Sanitize user question for AI calls ───────────────────────────────

        public String sanitizeQuestion(String input) {
                if (input == null)
                        return "";

                String cleaned = input.trim()
                                .replaceAll("(?i)ignore (previous|all|above) instructions?.*", "")
                                .replaceAll("(?i)system prompt.*", "")
                                .replaceAll("(?i)you are now.*", "")
                                .replaceAll("(?i)forget (everything|all|your instructions).*", "")
                                .replaceAll("(?i)act as (a |an )?.*", "")
                                .replaceAll("(?i)jailbreak.*", "")
                                .replaceAll("[<>{}\\[\\]|\\\\]{3,}", "")
                                .replaceAll("\\s{3,}", " ")
                                .trim();

                return cleaned.substring(0, Math.min(cleaned.length(), MAX_QUESTION_LENGTH));
        }

        public String sanitizeConcept(String input) {
                if (input == null)
                        return "";
                return input.trim()
                                .replaceAll("[^a-zA-Z0-9\\s\\-_./+#()]", "")
                                .substring(0, Math.min(input.length(), MAX_CONCEPT_LENGTH))
                                .trim();
        }

        public String sanitizeMessage(String input) {
                if (input == null)
                        return "";
                return input.trim()
                                .replaceAll("(?i)ignore (previous|all|above) instructions?.*",
                                                "")
                                .replaceAll("(?i)system:", "")
                                .replaceAll("(?i)\\[system\\].*", "")
                                .substring(0, Math.min(input.length(), MAX_MESSAGE_LENGTH))
                                .trim();
        }

        public boolean containsInjectionAttempt(String input) {
                if (input == null)
                        return false;
                String lower = input.toLowerCase();
                return lower.contains("ignore previous instructions") ||
                                lower.contains("system prompt") ||
                                lower.contains("you are now") ||
                                lower.contains("forget everything") ||
                                lower.contains("act as a different") ||
                                lower.contains("jailbreak") ||
                                lower.contains("dan mode") ||
                                lower.contains("developer mode");
        }
}