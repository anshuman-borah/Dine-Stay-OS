
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
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
//import project.EnterpriseSaas.demo.modules.billing.repository.BillRepository;
//import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;
//
//import java.io.UnsupportedEncodingException;
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
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
//    private final JdbcTemplate jdbcTemplate;
//
//    @Value("${app.mail.from.address:noreply@dinestay.app}")
//    private String fromAddress;
//
//    @Value("${app.mail.from.name:Dine&Stay OS}")
//    private String defaultFromName;
//
//    // ── KAFKA CONSUMER ───────────────────────────────────────────────────────
//    @KafkaListener(
//        topics = "bill-emails-topic",
//        groupId = "dinestay-email-group",
//        autoStartup = "${app.kafka.enabled:false}"
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
//    // ── DIRECT ASYNC FALLBACK ────────────────────────────────────────────────
//    @Async
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
//    // ── CORE LOGIC ───────────────────────────────────────────────────────────
//    private void executeEmailSending(UUID billId, UUID tenantId, String email) {
//        Bill bill = billRepo.findByIdAndTenantId(billId, tenantId)
//                .orElseThrow(() -> new RuntimeException("Bill not found in DB"));
//
//        if (bill.getBranch() != null) bill.getBranch().getName();
//        if (bill.getTenant() != null) bill.getTenant().getName();
//        if (bill.getOrder() != null && bill.getOrder().getItems() != null) {
//            bill.getOrder().getItems().size();
//        }
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
//        String date = bill.getCreatedAt() != null ? bill.getCreatedAt().toString().substring(0, 10) : "";
//
//        StringBuilder itemsHtml = new StringBuilder();
//        itemsHtml.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 15px; font-size: 14px;'>");
//        itemsHtml.append("<tr style='border-bottom: 1px solid #ddd; text-align: left;'><th style='padding: 8px 0;'>Item / Description</th><th>Qty</th><th style='text-align: right;'>Amt</th></tr>");
//
//        boolean hasItems = false;
//
//        // 🟢 RESTAURANT BILL -> Render Order Items
//        if (bill.getOrder() != null && bill.getOrder().getItems() != null && !bill.getOrder().getItems().isEmpty()) {
//            for (OrderItem item : bill.getOrder().getItems()) {
//                if (Boolean.TRUE.equals(item.getIsVoided())) continue;
//
//                // 🟢 FIXED: Strips trailing zeros so 1.000 becomes 1
//                String displayQty = item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "1";
//
//                itemsHtml.append("<tr>");
//                itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(item.getName()).append("</td>");
//                itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(displayQty).append("</td>");
//                itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>₹").append(String.format("%.2f", item.getLineTotal())).append("</td>");
//                itemsHtml.append("</tr>");
//                hasItems = true;
//            }
//        }
//        // 🟢 HOTEL BILL -> Fetch & Render Folio Charges
//        else if (bill.getSource() != null && "hotel".equalsIgnoreCase(bill.getSource().name()) && bill.getReservationId() != null) {
//            try {
//                String sql = "SELECT description, amount FROM hotel_folio_charges WHERE reservation_id = ? AND amount > 0 ORDER BY created_at ASC";
//                List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, bill.getReservationId());
//                for (Map<String, Object> charge : charges) {
//                    itemsHtml.append("<tr>");
//                    itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(charge.get("description")).append("</td>");
//                    itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>1</td>");
//                    itemsHtml.append("<td style='padding: 8px 0; border-bottom: 1px solid #f5f5f5; text-align: right;'>₹").append(String.format("%.2f", new BigDecimal(charge.get("amount").toString()))).append("</td>");
//                    itemsHtml.append("</tr>");
//                    hasItems = true;
//                }
//            } catch (Exception e) {
//                log.error("Failed to fetch folio charges for hotel email receipt", e);
//            }
//        }
//
//        if (!hasItems) {
//            itemsHtml.append("<tr><td colspan='3' style='padding: 8px 0; text-align: center; color: #666;'>Standard Charges Applied</td></tr>");
//        }
//        itemsHtml.append("</table>");
//
//        String discountHtml = "";
//        if (bill.getDiscountAmount() != null && bill.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
//            discountHtml = String.format("<tr><td style='padding: 4px 0;'>Discount</td><td style='text-align: right; color: #16a34a;'>-₹%.2f</td></tr>", bill.getDiscountAmount());
//        }
//
//        String html = String.format("""
//            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 450px; margin: auto; padding: 30px; border: 1px solid #e5e7eb; border-radius: 12px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
//                <h2 style="text-align: center; color: #111827; margin-bottom: 5px; font-size: 24px;">%s</h2>
//                <p style="text-align: center; color: #6b7280; margin-top: 0; font-size: 14px;">Thank you for your visit!</p>
//
//                <hr style="border-top: 1px dashed #d1d5db; margin: 20px 0;" />
//
//                <!-- 🟢 FIXED: Using a bulletproof HTML Table instead of Flexbox so Gmail spaces them perfectly -->
//                <table style="width: 100%%; font-size: 13px; color: #4b5563; margin-bottom: 20px; border-collapse: collapse;">
//                    <tr>
//                        <td style="text-align: left; padding: 0;"><strong>Bill No:</strong> %s</td>
//                        <td style="text-align: right; padding: 0;"><strong>Date:</strong> %s</td>
//                    </tr>
//                </table>
//
//                %s
//
//                <table style='width: 100%%; font-size: 14px; color: #374151; border-collapse: collapse;'>
//                    <tr><td style='padding: 4px 0;'>Subtotal</td><td style='text-align: right;'>₹%.2f</td></tr>
//                    %s
//                    <tr><td style='padding: 4px 0;'>Taxes (GST)</td><td style='text-align: right;'>₹%.2f</td></tr>
//                    <tr style='font-size: 18px; font-weight: bold; color: #111827;'>
//                        <td style='padding-top: 12px; border-top: 1px solid #e5e7eb;'>Grand Total</td>
//                        <td style='padding-top: 12px; border-top: 1px solid #e5e7eb; text-align: right;'>₹%.2f</td>
//                    </tr>
//                </table>
//
//                <p style="text-align: center; font-size: 12px; color: #9ca3af; margin-top: 40px; margin-bottom: 0;">
//                    Powered by Dine&Stay OS
//                </p>
//            </div>
//            """,
//            locationName,
//            bill.getBillNumber(),
//            date,
//            itemsHtml.toString(),
//            bill.getSubtotal(),
//            discountHtml,
//            bill.getTotalTax(),
//            bill.getGrandTotal()
//        );
//
//        String subject = "Your Receipt from " + locationName;
//        sendHtmlEmail(toEmail, subject, html, locationName);
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
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final BillRepository billRepo;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

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

        StringBuilder itemsHtml = new StringBuilder();
        itemsHtml.append("<table width='100%' cellpadding='0' cellspacing='0' border='0' style='margin-bottom: 15px; font-size: 14px;'>");
        itemsHtml.append("<tr style='border-bottom: 1px solid #ddd;'>");
        itemsHtml.append("<th width='60%' align='left' style='padding: 8px 0;'>Item / Description</th>");
        itemsHtml.append("<th width='15%' align='center' style='padding: 8px 0;'>Qty</th>");
        itemsHtml.append("<th width='25%' align='right' style='padding: 8px 0;'>Amt</th>");
        itemsHtml.append("</tr>");

        boolean hasItems = false;

        if (bill.getOrder() != null && bill.getOrder().getItems() != null && !bill.getOrder().getItems().isEmpty()) {
            for (OrderItem item : bill.getOrder().getItems()) {
                if (Boolean.TRUE.equals(item.getIsVoided())) continue;

                String displayQty = item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "1";

                itemsHtml.append("<tr>");
                itemsHtml.append("<td align='left' style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(item.getName()).append("</td>");
                itemsHtml.append("<td align='center' style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(displayQty).append("</td>");
                itemsHtml.append("<td align='right' style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>₹").append(String.format("%.2f", item.getLineTotal())).append("</td>");
                itemsHtml.append("</tr>");
                hasItems = true;
            }
        }
        else if (bill.getSource() != null && "hotel".equalsIgnoreCase(bill.getSource().name()) && bill.getReservationId() != null) {
            try {
                String sql = "SELECT description, amount FROM hotel_folio_charges WHERE reservation_id = ? AND amount > 0 ORDER BY created_at ASC";
                List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, bill.getReservationId());

                for (Map<String, Object> charge : charges) {
                    String desc = charge.get("description").toString();
                    BigDecimal amt = new BigDecimal(charge.get("amount").toString());

                    // 🟢 MAGICAL FIX: Detect POS Orders and fetch their actual food items!
                    if (desc.startsWith("Restaurant POS Order #")) {
                        String orderNumber = desc.replace("Restaurant POS Order #", "").trim();

                        // Print the main summary line in bold
                        itemsHtml.append("<tr>");
                        itemsHtml.append("<td align='left' style='padding: 8px 0 0 0; font-weight: bold;'>").append(desc).append("</td>");
                        itemsHtml.append("<td align='center' style='padding: 8px 0 0 0;'></td>");
                        itemsHtml.append("<td align='right' style='padding: 8px 0 0 0; font-weight: bold;'>₹").append(String.format("%.2f", amt)).append("</td>");
                        itemsHtml.append("</tr>");

                        try {
                            String itemsSql = "SELECT oi.name, oi.quantity, oi.line_total FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.order_number = ? AND (oi.is_voided = false OR oi.is_voided IS NULL)";
                            List<Map<String, Object>> foodItems = jdbcTemplate.queryForList(itemsSql, orderNumber);

                            // Print the food items underneath as sub-items
                            for (int i = 0; i < foodItems.size(); i++) {
                                Map<String, Object> food = foodItems.get(i);
                                String fName = "↳ " + food.get("name").toString();
                                String fQty = new BigDecimal(food.get("quantity").toString()).stripTrailingZeros().toPlainString();
                                BigDecimal fAmt = new BigDecimal(food.get("line_total").toString());

                                // Add a bottom border only to the last sub-item
                                String border = (i == foodItems.size() - 1) ? "border-bottom: 1px solid #f5f5f5;" : "";

                                itemsHtml.append("<tr>");
                                itemsHtml.append("<td align='left' style='padding: 4px 0 4px 15px; font-size: 12px; color: #6b7280; ").append(border).append("'>").append(fName).append("</td>");
                                itemsHtml.append("<td align='center' style='padding: 4px 0; font-size: 12px; color: #6b7280; ").append(border).append("'>").append(fQty).append("</td>");
                                itemsHtml.append("<td align='right' style='padding: 4px 0; font-size: 12px; color: #6b7280; ").append(border).append("'></td>");
                                itemsHtml.append("</tr>");
                            }
                        } catch (Exception ex) {
                            log.warn("Could not fetch restaurant items for order {}", orderNumber);
                        }
                    } else {
                        // Regular Hotel Charges (e.g. Room Charge, Minibar, Laundry)
                        itemsHtml.append("<tr>");
                        itemsHtml.append("<td align='left' style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>").append(desc).append("</td>");
                        itemsHtml.append("<td align='center' style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>1</td>");
                        itemsHtml.append("<td align='right' style='padding: 8px 0; border-bottom: 1px solid #f5f5f5;'>₹").append(String.format("%.2f", amt)).append("</td>");
                        itemsHtml.append("</tr>");
                    }
                    hasItems = true;
                }
            } catch (Exception e) {
                log.error("Failed to fetch folio charges for hotel email receipt", e);
            }
        }

        if (!hasItems) {
            itemsHtml.append("<tr><td colspan='3' align='center' style='padding: 8px 0; color: #666;'>Standard Charges Applied</td></tr>");
        }
        itemsHtml.append("</table>");

        String discountHtml = "";
        if (bill.getDiscountAmount() != null && bill.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountHtml = String.format("<tr><td align='left' style='padding: 4px 0;'>Discount</td><td align='right' style='color: #16a34a; padding: 4px 0;'>-₹%.2f</td></tr>", bill.getDiscountAmount());
        }

        String html = String.format("""
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 450px; margin: auto; padding: 30px; border: 1px solid #e5e7eb; border-radius: 12px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                <h2 style="text-align: center; color: #111827; margin-bottom: 5px; font-size: 24px;">%s</h2>
                <p style="text-align: center; color: #6b7280; margin-top: 0; font-size: 14px;">Thank you for your visit!</p>
                
                <hr style="border-top: 1px dashed #d1d5db; margin: 20px 0;" />
                
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin-bottom: 20px; font-size: 13px; color: #4b5563;">
                    <tr>
                        <td align="left" style="padding: 0;"><strong>Bill No:</strong> %s</td>
                        <td align="right" style="padding: 0;"><strong>Date:</strong> %s</td>
                    </tr>
                </table>
                
                %s
                
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="font-size: 14px; color: #374151;">
                    <tr><td align="left" style="padding: 4px 0;">Subtotal</td><td align="right" style="padding: 4px 0;">₹%.2f</td></tr>
                    %s
                    <tr><td align="left" style="padding: 4px 0;">Taxes (GST)</td><td align="right" style="padding: 4px 0;">₹%.2f</td></tr>
                    <tr style="font-size: 18px; font-weight: bold; color: #111827;">
                        <td align="left" style="padding-top: 12px; border-top: 1px solid #e5e7eb;">Grand Total</td>
                        <td align="right" style="padding-top: 12px; border-top: 1px solid #e5e7eb;">₹%.2f</td>
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