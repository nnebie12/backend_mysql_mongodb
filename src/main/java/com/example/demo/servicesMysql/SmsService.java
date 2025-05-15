package com.example.demo.servicesMysql;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.phone_number}")
    private String fromNumber;

    public void sendSms(String to, String body) {
        Message.creator(
            new PhoneNumber(to),
            new PhoneNumber(fromNumber),
            body
        ).create();
    }
}