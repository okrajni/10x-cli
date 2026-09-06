package com.example.doneyet.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class MockEmailService implements EmailService {
    private final ConcurrentHashMap<String, String> sentEmails = new ConcurrentHashMap<>();

    @Override
    public void sendInvitationEmail(String toEmail, String invitationLink, String householdName) {
        String message = String.format(
                "[MOCK EMAIL] To: %s\nSubject: You're invited to join %s\n\nInvitation Link: %s",
                toEmail, householdName, invitationLink
        );

        sentEmails.put(toEmail + "_" + System.currentTimeMillis(), message);
        System.out.println(message);
    }

    public ConcurrentHashMap<String, String> getSentEmails() {
        return sentEmails;
    }

    public void clear() {
        sentEmails.clear();
    }
}
