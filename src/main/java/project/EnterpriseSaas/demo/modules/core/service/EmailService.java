//package project.EnterpriseSaas.demo.modules.core.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.mail.MessagingException;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
//import project.EnterpriseSaas.demo.modules.billing.repository.BillRepository;
//
//import java.io.UnsupportedEncodingException;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//    private final BillRepository billRepo; // Needed to fetch the bill from DB
//    private final ObjectMapper objectMapper; // Needed to parse Kafka JSON messages
//
//    @Value("${app.mail.from.address:noreply@dinestay.app}")
//    private String fromAddress;
//
//    @Value("${app.mail.from.name:Dine&Stay OS}")
//    private String defaultFromName;
//
//    // ── KAFKA CONSUMER ───────────────────────────────────────────────────────
//    // This runs in the background. Whenever a message drops into 'bill-emails-topic', this fires!
//    @KafkaListener(topics = "bill-emails-topic", groupId = "dinestay-email-group")
//    @Transactional(readOnly = true)
//    public void consumeBillEmailEvent(String message) {
//        try {
//            log.info("📥 [KAFKA] Picked up new email task: {}", message);
//
//            // 1. Parse the JSON message from Kafka
//            JsonNode node = objectMapper.readTree(message);
//            UUID billId = UUID.fromString(node.get("billId").asText());
//            UUID tenantId = UUID.fromString(node.get("tenantId").asText());
//            String email = node.get("email").asText();
//
//            // 2. Fetch the Bill securely from DB
//            Bill bill = billRepo.findByIdAndTenantId(billId, tenantId)
//                    .orElseThrow(() -> new RuntimeException("Bill not found in DB"));
//
//            // 3. Force-initialize lazy relationships safely
//            if (bill.getBranch() != null) bill.getBranch().getName();
//            if (bill.getTenant() != null) bill.getTenant().getName();
//
//            // 4. Send the actual email
//            sendReceipt(email, bill);
//
//            log.info("✅ [KAFKA] Successfully processed and sent email for Bill {}", billId);
//        } catch (Exception e) {
//            log.error("❌ [KAFKA] Failed to process email event: {}", e.getMessage(), e);
//        }
//    }
//
//    public void sendHtmlEmail(String to, String subject, String htmlBody, String dynamicFromName) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            String senderName = (dynamicFromName != null && !dynamicFromName.isBlank()) ? dynamicFromName : defaultFromName;
//
//            helper.setFrom(new InternetAddress(fromAddress, senderName));
//            helper.setTo(to);
//            helper.setSubject(subject);
//            helper.setText(htmlBody, true);
//
//            mailSender.send(message);
//            log.info("Email sent successfully to {} from '{}'", to, senderName);
//
//        } catch (MessagingException | UnsupportedEncodingException e) {
//            log.error("Failed to send email to {}: {}", to, e.getMessage());
//        }
//    }
//
//    public void sendReceipt(String toEmail, Bill bill) {
//        String locationName = (bill.getBranch() != null) ? bill.getBranch().getName() : bill.getTenant().getName();
//
//        String html = String.format("""
//            <div style="font-family: Arial, sans-serif; max-width: 400px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
//                <h2 style="text-align: center; color: #333;">%s</h2>
//                <p style="text-align: center; color: #666;">Thank you for your visit!</p>
//                <hr style="border-top: 1px dashed #ccc;" />
//                <p><strong>Bill No:</strong> %s</p>
//                <p><strong>Date:</strong> %s</p>
//                <hr style="border-top: 1px dashed #ccc;" />
//                <h3 style="text-align: right;">Total Paid: ₹%.2f</h3>
//                <p style="text-align: center; font-size: 12px; color: #999; margin-top: 30px;">
//                    Powered by Dine&Stay OS
//                </p>
//            </div>
//            """,
//                locationName,
//                bill.getBillNumber(),
//                bill.getCreatedAt().toString().substring(0, 10),
//                bill.getGrandTotal()
//        );
//
//        String subject = "Your Receipt from " + locationName;
//        String senderName = locationName + " - Dine&Stay";
//
//        sendHtmlEmail(toEmail, subject, html, senderName);
//    }
//}
package project.EnterpriseSaas.demo.modules.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
import project.EnterpriseSaas.demo.modules.billing.repository.BillRepository;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final BillRepository billRepo;
    private final ObjectMapper objectMapper;

    @Value("${app.mail.from.address:noreply@dinestay.app}")
    private String fromAddress;

    @Value("${app.mail.from.name:Dine&Stay OS}")
    private String defaultFromName;

    // ── KAFKA CONSUMER (Runs only if Kafka is alive) ────────────────────────
    @KafkaListener(
            topics = "bill-emails-topic",
            groupId = "dinestay-email-group",
            autoStartup = "${app.kafka.enabled:false}" // 🟢 Turns off listener if Kafka is disabled!
    )
    @Transactional(readOnly = true)
    public void consumeBillEmailEvent(String message) {
        try {
            log.info("📥 [KAFKA] Picked up new email task: {}", message);
            JsonNode node = objectMapper.readTree(message);
            UUID billId = UUID.fromString(node.get("billId").asText());
            UUID tenantId = UUID.fromString(node.get("tenantId").asText());
            String email = node.get("email").asText();

            executeEmailSending(billId, tenantId, email);
        } catch (Exception e) {
            log.error("❌ [KAFKA] Failed to process email event: {}", e.getMessage(), e);
        }
    }

    // ── DIRECT ASYNC FALLBACK (Runs when Kafka is disabled) ──────────────────
    @Async // 🟢 Forces this to run in a background thread so the API doesn't wait!
    @Transactional(readOnly = true)
    public void processEmailAsync(UUID billId, UUID tenantId, String email) {
        log.info("⚡ [ASYNC THREAD] Processing email task directly (Kafka disabled)");
        try {
            executeEmailSending(billId, tenantId, email);
        } catch (Exception e) {
            log.error("❌ [ASYNC THREAD] Failed to process email: {}", e.getMessage(), e);
        }
    }

    // ── CORE LOGIC (Used by both Kafka and Async) ────────────────────────────
    private void executeEmailSending(UUID billId, UUID tenantId, String email) {
        Bill bill = billRepo.findByIdAndTenantId(billId, tenantId)
                .orElseThrow(() -> new RuntimeException("Bill not found in DB"));

        if (bill.getBranch() != null) bill.getBranch().getName();
        if (bill.getTenant() != null) bill.getTenant().getName();

        sendReceipt(email, bill);
        log.info("✅ Successfully processed and sent email for Bill {}", billId);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody, String dynamicFromName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String senderName = (dynamicFromName != null && !dynamicFromName.isBlank()) ? dynamicFromName : defaultFromName;

            helper.setFrom(new InternetAddress(fromAddress, senderName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("📧 Email sent successfully to {} from '{}'", to, senderName);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendReceipt(String toEmail, Bill bill) {
        String locationName = (bill.getBranch() != null) ? bill.getBranch().getName() : bill.getTenant().getName();
        String html = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 400px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="text-align: center; color: #333;">%s</h2>
                <p style="text-align: center; color: #666;">Thank you for your visit!</p>
                <hr style="border-top: 1px dashed #ccc;" />
                <p><strong>Bill No:</strong> %s</p>
                <p><strong>Date:</strong> %s</p>
                <hr style="border-top: 1px dashed #ccc;" />
                <h3 style="text-align: right;">Total Paid: ₹%.2f</h3>
                <p style="text-align: center; font-size: 12px; color: #999; margin-top: 30px;">
                    Powered by Dine&Stay OS
                </p>
            </div>
            """, locationName, bill.getBillNumber(), bill.getCreatedAt().toString().substring(0, 10), bill.getGrandTotal());

        String subject = "Your Receipt from " + locationName;
        sendHtmlEmail(toEmail, subject, html, locationName + " - Dine&Stay");
    }
}

