package com.nhom13.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    /**
     * Provides a mock mail sender for development/default profiles
     * to prevent the application from failing to start if mail properties are not set.
     * This bean will NOT be used when the 'prod' profile is active.
     */
    @Bean
    @Profile("!prod") // Active for any profile *except* 'prod'
    public JavaMailSender devJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // Use a mock host. For real dev, you might use MailHog or GreenMail
        mailSender.setHost("localhost");
        mailSender.setPort(1025); // Common port for mock mail servers like MailHog
        mailSender.setUsername("mockuser");
        mailSender.setPassword("mockpass");
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        
        // Log "true" to see mock mail activity, or "false" to hide it.
        props.put("mail.debug", "false"); 
        
        return mailSender;
    }
}