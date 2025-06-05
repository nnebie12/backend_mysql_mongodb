package com.example.demo.servicesMysql;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class SmsService {
    
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
            
            System.out.println("SMS envoyé avec succès. SID: " + message.getSid());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi SMS : " + e.getMessage());
            throw e;
        }
    }
}