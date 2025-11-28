package com.nexus.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String sender;
    private String recipient; // can be a username or a groupname
    private String message;
    private String timestamp;
    private boolean isGroupMessage; // boolean flag to distinguish group chats
    private boolean deletedForEveryone = false;
    private List<String> hiddenFor = new ArrayList<>(); // Users who deleted this chat
}