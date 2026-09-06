package com.example.doneyet.service;

public interface EmailService {
    void sendInvitationEmail(String toEmail, String invitationLink, String householdName);
}
