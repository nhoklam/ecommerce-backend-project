package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.OrderDTO;
import com.nhom13.ecommerce.entity.RefundRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // Đảm bảo @Value được import
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOrderConfirmation(String toEmail, OrderDTO order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Order Confirmation - #" + order.getId());
            message.setText(buildOrderEmailContent(order));
            
            mailSender.send(message);
            log.info("Order confirmation email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
        }
    }
    
    public void sendOrderStatusUpdate(String toEmail, OrderDTO order) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Order Status Update - #" + order.getId());
            message.setText(buildStatusUpdateEmailContent(order));
            
            mailSender.send(message);
            log.info("Order status update email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send status update email to: {}", toEmail, e);
        }
    }
    
    private String buildOrderEmailContent(OrderDTO order) {
        StringBuilder content = new StringBuilder();
        content.append("Dear Customer,\n\n");
        content.append("Your order has been confirmed!\n\n");
        content.append("Order Details:\n");
        content.append("Order ID: #").append(order.getId()).append("\n");
        content.append("Total Amount: $").append(order.getTotalAmount()).append("\n");
        content.append("Status: ").append(order.getStatus()).append("\n");
        // Cập nhật để sử dụng snapshot
        content.append("Shipping Address: ").append(order.getShippingAddressSnapshot()).append("\n\n");
        content.append("Thank you for your business!\n\n");
        content.append("Best regards,\nE-commerce Team");
        
        return content.toString();
    }
    
    private String buildStatusUpdateEmailContent(OrderDTO order) {
        StringBuilder content = new StringBuilder();
        content.append("Dear Customer,\n\n");
        content.append("Your order status has been updated.\n\n");
        content.append("Order ID: #").append(order.getId()).append("\n");
        content.append("New Status: ").append(order.getStatus()).append("\n\n");
        content.append("Thank you for your patience!\n\n");
        content.append("Best regards,\nE-commerce Team");
        
        return content.toString();
    }

    // [MỚI] Thêm phương thức sendPasswordResetEmail
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Yêu cầu Đặt lại Mật khẩu E-commerce");
            
            // CHÚ Ý: Trong production, hãy trỏ URL này đến frontend của bạn
            String resetUrl = "http://localhost:3000/reset-password?token=" + token;

            message.setText("Chào bạn,\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n"
                + "Vui lòng nhấp vào đường link sau để đặt lại mật khẩu:\n"
                + resetUrl + "\n\n"
                + "Nếu bạn không yêu cầu việc này, vui lòng bỏ qua email này. Link sẽ hết hạn sau 1 giờ.\n\n"
                + "Trân trọng,\nĐội ngũ E-commerce");
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }
    public void sendRefundConfirmation(String toEmail, RefundRequest refund) {
    try {
        SimpleMailMessage message = new SimpleMailMessage(); // [27, 28]
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Refund Request Received - #" + refund.getId());
        message.setText(buildRefundConfirmationContent(refund));

        mailSender.send(message);
        log.info("Refund confirmation email sent to: {}", toEmail);
    } catch (Exception e) {
        log.error("Failed to send refund confirmation email to: {}", toEmail, e);
    }
}

public void sendRefundStatusUpdate(String toEmail, RefundRequest refund) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Refund Status Update - #" + refund.getId());
        message.setText(buildRefundStatusUpdateContent(refund));

        mailSender.send(message);
        log.info("Refund status update email sent to: {}", toEmail);
    } catch (Exception e) {
        log.error("Failed to send refund status update email to: {}", toEmail, e);
    }
}

private String buildRefundConfirmationContent(RefundRequest refund) {
    // [27]
    return String.format(
        "Dear Customer,\n\n" +
        "We have received your refund request (ID: #%d) for Order ID: #%d.\n" +
        "Reason: %s\n" +
        "Total Amount: $%.2f\n" +
        "Status: %s\n\n" +
        "Our team will review your request and update you soon.\n\n" +
        "Best regards,\nE-commerce Team",
        refund.getId(),
        refund.getOrder().getId(),
        refund.getReason(),
        refund.getTotalRefundAmount(),
        refund.getStatus()
    );
}

private String buildRefundStatusUpdateContent(RefundRequest refund) {
    return String.format(
        "Dear Customer,\n\n" +
        "Your refund request (ID: #%d) for Order ID: #%d has been updated.\n\n" +
        "New Status: %s\n" +
        "Admin Notes: %s\n\n" +
        "Thank you,\nE-commerce Team",
        refund.getId(),
        refund.getOrder().getId(),
        refund.getStatus(),
        refund.getAdminNotes()!= null? refund.getAdminNotes() : "N/A"
    );
}
}