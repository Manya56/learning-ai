package com.learningai.backend.service;

import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.User;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.RevisionCardRepository;
import com.learningai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * EmailNotificationService — replaces FCM with free email notifications.
 *
 * Uses Spring Boot JavaMailSender (works with Gmail, Resend, Mailgun, etc.)
 * All sends are @Async so they never block the request thread.
 *
 * Email types:
 *  1. Daily revision reminder  — "You have N concepts due"
 *  2. Streak reminder          — "Don't break your streak"
 *  3. Motivational             — AI-personalized message
 *  4. Topic completed          — celebration email
 *  5. Welcome                  — sent after onboarding
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender            mailSender;
    private final UserRepository            userRepository;
    private final LearningProfileRepository profileRepository;
    private final RevisionCardRepository    revisionCardRepository;
    private final AiService                 aiService;

    @Value("${app.mail.from:noreply@learnai.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:LearnAI}")
    private String fromName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ─── Daily revision reminder ──────────────────────────────────────────

    @Async
    public void sendRevisionReminder(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        long dueCount = revisionCardRepository.countDueCards(userId, LocalDate.now());
        if (dueCount == 0) return;

        LearningProfile profile = profileRepository.findByUserId(userId).orElse(null);
        String goal = profile != null ? profile.getGoal() : "your goal";

        String subject = dueCount == 1
                ? "📚 1 concept waiting for review"
                : "📚 " + dueCount + " concepts waiting for review";

        String html = buildRevisionHtml(user.getFullName(), dueCount, goal);
        sendEmail(user.getEmail(), subject, html);
    }

    // ─── Streak reminder ──────────────────────────────────────────────────

    @Async
    public void sendStreakReminder(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        LearningProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null || profile.getCurrentDayStreak() < 2) return;

        int streak  = profile.getCurrentDayStreak();
        String subject = "🔥 Don't break your " + streak + "-day streak!";
        String html = buildStreakHtml(user.getFullName(), streak, profile.getGoal());
        sendEmail(user.getEmail(), subject, html);
    }

    // ─── AI motivational message ──────────────────────────────────────────

    @Async
    public void sendMotivationalMessage(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        LearningProfile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return;

        List<String> weakConcepts = profile.getWeakConcepts() != null
                ? profile.getWeakConcepts().entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByValue())
                        .limit(2)
                        .map(java.util.Map.Entry::getKey)
                        .collect(Collectors.toList())
                : List.of();

        String motivation = aiService.generateMotivationalMessage(
                profile.getGoal(),
                profile.getOverallAccuracy(),
                profile.getCurrentDayStreak(),
                weakConcepts);

        String subject = "💡 Your daily learning boost";
        String html    = buildMotivationalHtml(user.getFullName(), motivation, profile.getGoal());
        sendEmail(user.getEmail(), subject, html);
    }

    // ─── Topic completed ──────────────────────────────────────────────────

    @Async
    public void sendTopicCompletedEmail(UUID userId, String topicName, String nextTopic) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String subject = "🎉 You completed: " + topicName;
        String html    = buildTopicCompleteHtml(user.getFullName(), topicName, nextTopic);
        sendEmail(user.getEmail(), subject, html);
    }

    // ─── Welcome email after onboarding ──────────────────────────────────

    @Async
    public void sendWelcomeEmail(UUID userId, String goal, List<String> roadmapTopics) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String subject = "🚀 Your learning roadmap is ready!";
        String html    = buildWelcomeHtml(user.getFullName(), goal, roadmapTopics);
        sendEmail(user.getEmail(), subject, html);
    }

    // ─── Core send method ─────────────────────────────────────────────────

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ─── HTML builders ────────────────────────────────────────────────────

    private String buildRevisionHtml(String name, long dueCount, String goal) {
        return baseTemplate(name,
                "📚 Time to Review",
                "You have <strong>" + dueCount + " concept" + (dueCount > 1 ? "s" : "") +
                "</strong> due for review today for your <strong>" + goal + "</strong> goal.",
                "Spaced repetition works best when you review on schedule. " +
                "Your brain is ready to strengthen these memories right now.",
                "Start Revision",
                frontendUrl + "/revision");
    }

    private String buildStreakHtml(String name, int streak, String goal) {
        return baseTemplate(name,
                "🔥 Your Streak is at Risk!",
                "You're on a <strong>" + streak + "-day streak</strong> for <strong>" +
                goal + "</strong> — don't let it slip today.",
                "You only need 10 minutes of study to keep the streak alive. " +
                "Consistency beats intensity every time.",
                "Study Now",
                frontendUrl + "/dashboard");
    }

    private String buildMotivationalHtml(String name, String motivation, String goal) {
        return baseTemplate(name,
                "💡 Your Daily Boost",
                motivation,
                "Your goal: <strong>" + goal + "</strong>. " +
                "Every concept you learn today compounds into mastery tomorrow.",
                "Continue Learning",
                frontendUrl + "/dashboard");
    }

    private String buildTopicCompleteHtml(String name, String topicName, String nextTopic) {
        String nextSection = nextTopic != null
                ? "<p style='color:#888;margin-top:16px;'>Up next: <strong style='color:#6366f1'>"
                  + nextTopic + "</strong></p>"
                : "<p style='color:#888;margin-top:16px;'>You're making incredible progress!</p>";

        return baseTemplate(name,
                "🎉 Topic Complete!",
                "You finished <strong>" + topicName + "</strong>. " +
                "That's a big milestone!",
                "Every topic you complete is a permanent addition to your knowledge." + nextSection,
                "See Your Roadmap",
                frontendUrl + "/roadmap");
    }

    private String buildWelcomeHtml(String name, String goal, List<String> topics) {
        StringBuilder topicList = new StringBuilder();
        for (int i = 0; i < topics.size(); i++) {
            topicList.append("<li style='padding:4px 0;color:#ccc;'>")
                     .append(i + 1).append(". ").append(topics.get(i))
                     .append("</li>");
        }

        return baseTemplate(name,
                "🚀 Your Roadmap is Ready!",
                "Your personalized learning path for <strong>" + goal +
                "</strong> is ready. Here's what you'll master:",
                "<ul style='text-align:left;padding-left:20px;margin:16px 0;'>"
                + topicList + "</ul>",
                "Start Learning",
                frontendUrl + "/dashboard");
    }

    /**
     * Base dark-themed HTML email template.
     * Matches the app's dark design (#0f0f0f background, #6366f1 accent).
     */
    private String baseTemplate(String name, String heading,
                                 String mainText, String subText,
                                 String ctaText, String ctaUrl) {
        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>LearnAI</title>
</head>
<body style="margin:0;padding:0;background:#0f0f0f;font-family:'Segoe UI',Arial,sans-serif;">
  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0f0f0f;padding:40px 20px;">
    <tr>
      <td align="center">
        <table width="560" cellpadding="0" cellspacing="0"
               style="background:#1a1a1a;border:1px solid #2e2e2e;border-radius:16px;overflow:hidden;max-width:560px;width:100%%;">

          <!-- Header -->
          <tr>
            <td style="background:#6366f1;padding:24px 32px;text-align:center;">
              <span style="color:#fff;font-size:22px;font-weight:700;letter-spacing:-0.5px;">
                LearnAI
              </span>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="padding:32px;">
              <p style="color:#888;margin:0 0 8px;font-size:14px;">
                Hi %s,
              </p>
              <h1 style="color:#e8e8e8;margin:0 0 20px;font-size:24px;font-weight:600;line-height:1.3;">
                %s
              </h1>
              <p style="color:#ccc;margin:0 0 16px;font-size:16px;line-height:1.6;">
                %s
              </p>
              <div style="color:#aaa;font-size:14px;line-height:1.6;margin-bottom:28px;">
                %s
              </div>

              <!-- CTA Button -->
              <table cellpadding="0" cellspacing="0">
                <tr>
                  <td style="border-radius:10px;background:#6366f1;">
                    <a href="%s"
                       style="display:inline-block;padding:14px 28px;color:#fff;
                              font-weight:600;font-size:15px;text-decoration:none;
                              border-radius:10px;letter-spacing:0.2px;">
                      %s →
                    </a>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="padding:20px 32px;border-top:1px solid #2e2e2e;text-align:center;">
              <p style="color:#555;font-size:12px;margin:0;">
                You're receiving this because you have an active LearnAI account.<br>
                <a href="%s/profile" style="color:#6366f1;text-decoration:none;">
                  Manage email preferences
                </a>
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""".formatted(name, heading, mainText, subText, ctaUrl, ctaText, frontendUrl);
    }
}