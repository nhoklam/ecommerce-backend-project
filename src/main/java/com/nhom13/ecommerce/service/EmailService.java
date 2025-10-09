package com.nhom13.ecommerce.service;

import com.nhom13.ecommerce.dto.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
        content.append("Shipping Address: ").append(order.getShippingAddress()).append("\n\n");
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
}