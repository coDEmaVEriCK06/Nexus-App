package com.nexus.chat;

/**
 * USER  — a message written by a participant.
 * SYSTEM — a generated notice about a membership change (added / removed / promoted /
 *          demoted / left / group created). Rendered as a centered notice, not a bubble.
 */
public enum MessageType {
    USER,
    SYSTEM
}
