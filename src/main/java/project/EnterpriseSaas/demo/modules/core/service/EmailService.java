//
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
//import org.springframework.scheduling.annotation.Async;
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
//    private final BillRepository billRepo;
//    private final ObjectMapper objectMapper;
//
//    @Value("${app.mail.from.address:noreply@dinestay.app}")
//    private String fromAddress;
//
//    @Value("${app.mail.from.name:Dine&Stay OS}")
//    private String defaultFromName;
//
//    // ── KAFKA CONSUMER (Runs only if Kafka is alive) ────────────────────────
//    @KafkaListener(
//            topics = "bill-emails-topic",
//            groupId = "dinestay-email-group",
//            autoStartup = "${app.kafka.enabled:false}" // 🟢 Turns off listener if Kafka is disabled!
//    )
//    @Transactional(readOnly = true)
//    public void consumeBillEmailEvent(String message) {
//        try {
//            log.info("📥 [KAFKA] Picked up new email task: {}", message);
//            JsonNode node = objectMapper.readTree(message);
//            UUID billId = UUID.fromString(node.get("billId").asText());
//            UUID tenantId = UUID.fromString(node.get("tenantId").asText());
//            String email = node.get("email").asText();
//
//            executeEmailSending(billId, tenantId, email);
//        } catch (Exception e) {
//            log.error("❌ [KAFKA] Failed to process email event: {}", e.getMessage(), e);
//        }
//    }
//
//    // ── DIRECT ASYNC FALLBACK (Runs when Kafka is disabled) ──────────────────
//    @Async // 🟢 Forces this to run in a background thread so the API doesn't wait!
//    @Transactional(readOnly = true)
//    public void processEmailAsync(UUID billId, UUID tenantId, String email) {
//        log.info("⚡ [ASYNC THREAD] Processing email task directly (Kafka disabled)");
//        try {
//            executeEmailSending(billId, tenantId, email);
//        } catch (Exception e) {
//            log.error("❌ [ASYNC THREAD] Failed to process email: {}", e.getMessage(), e);
//        }
//    }
//
//    // ── CORE LOGIC (Used by both Kafka and Async) ────────────────────────────
//    private void executeEmailSending(UUID billId, UUID tenantId, String email) {
//        Bill bill = billRepo.findByIdAndTenantId(billId, tenantId)
//                .orElseThrow(() -> new RuntimeException("Bill not found in DB"));
//
//        if (bill.getBranch() != null) bill.getBranch().getName();
//        if (bill.getTenant() != null) bill.getTenant().getName();
//
//        sendReceipt(email, bill);
//        log.info("✅ Successfully processed and sent email for Bill {}", billId);
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
//            log.info("📧 Email sent successfully to {} from '{}'", to, senderName);
//
//        } catch (MessagingException | UnsupportedEncodingException e) {
//            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
//        }
//    }
//
//    public void sendReceipt(String toEmail, Bill bill) {
//        String locationName = (bill.getBranch() != null) ? bill.getBranch().getName() : bill.getTenant().getName();
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
//            """, locationName, bill.getBillNumber(), bill.getCreatedAt().toString().substring(0, 10), bill.getGrandTotal());
//
//        String subject = "Your Receipt from " + locationName;
//        sendHtmlEmail(toEmail, subject, html, locationName + " - Dine&Stay");
//    }
//}
//
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
import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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

    // ── KAFKA CONSUMER ───────────────────────────────────────────────────────
    @KafkaListener(
            topics = "bill-emails-topic",
            groupId = "dinestay-email-group",
            autoStartup = "${app.kafka.enabled:false}"
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

    // ── DIRECT ASYNC FALLBACK ────────────────────────────────────────────────
    @Async
    @Transactional(readOnly = true)
    public void processEmailAsync(UUID billId, UUID tenantId, String email) {
        log.info("⚡ [ASYNC THREAD] Processing email task directly (Kafka disabled)");
        try {
            executeEmailSending(billId, tenantId, email);
        } catch (Exception e) {
            log.error("❌ [ASYNC THREAD] Failed to process email: {}", e.getMessage(), e);
        }
    }

    // ── CORE LOGIC ───────────────────────────────────────────────────────────
    private void executeEmailSending(UUID billId, UUID tenantId, String email) {
        Bill bill = billRepo.findByIdAndTenantId(billId, tenantId)
                .orElseThrow(() -> new RuntimeException("Bill not found in DB"));

        // Force-initialize relationships to avoid LazyInitializationException
        if (bill.getBranch() != null) bill.getBranch().getName();
        if (bill.getTenant() != null) bill.getTenant().getName();
        if (bill.getOrder() != null && bill.getOrder().getItems() != null) {
            bill.getOrder().getItems().size();
        }

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
        String date = bill.getCreatedAt() != null ? bill.getCreatedAt().toString().substring(0, 10) : "";

        // 1. Build the dynamic Items Table
        StringBuilder itemsHtml = new StringBuilder();
        if (bill.getOrder() != null && bill.getOrder().getItems() != null && !bill.getOrder().getItems().isEmpty()) {
            itemsHtml.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 15px; font-size: 14px;'>");
            itemsHtml.append("<tr style='border-bottom: 1px solid #ddd; text-align: left;'><th style='padding: 8px 0;'>Item</th><th>Qty</th><th style='text-align: right;'>Amt</th></tr>");

            for (OrderItem item : bill.getOrder().getItems()) {
                if (Boolean.TRUE.equals(item.getIsVoided())) continue; // Skip voided items
                itemsHtml.append("<tr>");
                itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(item.getName()).append("</td>");
                itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(item.getQuantity()).append("</td>");
                itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>₹").append(String.format("%.2f", item.getLineTotal())).append("</td>");
                itemsHtml.append("</tr>");
            }
            itemsHtml.append("</table>");
        } else if (bill.getSource() == project.EnterpriseSaas.demo.common.enums.BillSource.hotel) {
            // Fallback for Hotel Folio checkouts without specific restaurant items
            itemsHtml.append("<p style='text-align: center; color: #666; font-size: 14px; margin-bottom: 20px;'>Accommodation & Folio Charges</p>");
        }

        // 2. Format optional discount row
        String discountHtml = "";
        if (bill.getDiscountAmount() != null && bill.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountHtml = String.format("<tr><td style='padding: 4px 0;'>Discount</td><td style='text-align: right; color: #16a34a;'>-₹%.2f</td></tr>", bill.getDiscountAmount());
        }

        // 3. Inject it into the final beautifully styled HTML envelope
        String html = String.format("""
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 450px; margin: auto; padding: 30px; border: 1px solid #e5e7eb; border-radius: 12px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                <h2 style="text-align: center; color: #111827; margin-bottom: 5px; font-size: 24px;">%s</h2>
                <p style="text-align: center; color: #6b7280; margin-top: 0; font-size: 14px;">Thank you for your visit!</p>
                
                <hr style="border-top: 1px dashed #d1d5db; margin: 20px 0;" />
                
                <div style="font-size: 13px; color: #4b5563; margin-bottom: 20px; display: flex; justify-content: space-between;">
                    <p style="margin: 0;"><strong>Bill No:</strong> %s</p>
                    <p style="margin: 0; text-align: right;"><strong>Date:</strong> %s</p>
                </div>
                
                %s
                
                <table style='width: 100%%; font-size: 14px; color: #374151; border-collapse: collapse;'>
                    <tr><td style='padding: 4px 0;'>Subtotal</td><td style='text-align: right;'>₹%.2f</td></tr>
                    %s
                    <tr><td style='padding: 4px 0;'>Taxes (GST)</td><td style='text-align: right;'>₹%.2f</td></tr>
                    <tr style='font-size: 18px; font-weight: bold; color: #111827;'>
                        <td style='padding-top: 12px; border-top: 1px solid #e5e7eb;'>Grand Total</td>
                        <td style='padding-top: 12px; border-top: 1px solid #e5e7eb; text-align: right;'>₹%.2f</td>
                    </tr>
                </table>
                
                <p style="text-align: center; font-size: 12px; color: #9ca3af; margin-top: 40px; margin-bottom: 0;">
                    Powered by Dine&Stay OS
                </p>
            </div>
            """,
                locationName,
                bill.getBillNumber(),
                date,
                itemsHtml.toString(),
                bill.getSubtotal(),
                discountHtml,
                bill.getTotalTax(),
                bill.getGrandTotal()
        );

        String subject = "Your Receipt from " + locationName;
        sendHtmlEmail(toEmail, subject, html, locationName);
    }
}