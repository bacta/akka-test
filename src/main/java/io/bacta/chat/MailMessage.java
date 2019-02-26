package io.bacta.chat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MailMessage {
    private final String from;
    private final String subject;
    private final String message;
    private final String outOfBand;

    public MailMessage(String from, String subject, String message) {
        this(from, subject, message, "");
    }
}
