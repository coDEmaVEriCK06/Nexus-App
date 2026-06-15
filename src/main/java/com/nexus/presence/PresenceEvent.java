package com.nexus.presence;

/** Pushed over the socket when a contact comes online or goes offline. */
public record PresenceEvent(String username, boolean online) {}
