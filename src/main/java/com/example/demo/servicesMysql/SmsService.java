package com.example.demo.servicesMysql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);
    
    @Value("${twilio.account_sid}")
    private String accountSid;
    
    @Value("${twilio.auth_token}")
    private String authToken;
    
    @Value("${twilio.phone_number}")
    private String fromNumber;
    
    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }
    
    public void sendSms(String to, String body) {
        try {
            Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(fromNumber),
                body
            ).create();
            
            logger.info("SMS envoyé avec succès. SID: {}", message.getSid());
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi SMS : {}", e.getMessage(), e);
            throw e;
        }
    }
}