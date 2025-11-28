package com.nexus.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.model.Message;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path GROUPS_FILE = DATA_DIR.resolve("groups.json");
    private final ObjectMapper objectMapper;

    public ChatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private void createDir() throws IOException {
        if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
    }

    // Private helper methods

    private Path getConvoFile(String user1, String user2) {
        String u1 = user1.compareTo(user2) <= 0 ? user1 : user2;
        String u2 = u1.equals(user1) ? user2 : user1;
        return DATA_DIR.resolve(u1 + "_" + u2 + ".json");
    }

    private Path getGroupFile(String groupName) {
        return DATA_DIR.resolve("group_" + groupName + ".json");
    }

    // Main features

    public void saveMessage(Message msg) throws IOException {
        createDir();
        Path path;
        if (msg.isGroupMessage())
            path = getGroupFile(msg.getRecipient()); // Recipient is group name
        else
            path = getConvoFile(msg.getSender(), msg.getRecipient());

        File file = path.toFile();
        List<Message> convo;
        if (file.exists()) convo = objectMapper.readValue(file, new TypeReference<List<Message>>() {});
        else convo = new ArrayList<>();
        convo.add(msg);
        objectMapper.writeValue(file, convo);
    }

    public List<Message> getHistory(String requester, String other, boolean isGroup) throws IOException {
        createDir();
        Path path = isGroup ? getGroupFile(other) : getConvoFile(requester, other);
        File file = path.toFile();

        if (!file.exists()) return new ArrayList<>();

        List<Message> allMessages = objectMapper.readValue(file, new TypeReference<List<Message>>() {});

        // deletedForEveryone changes message content to "This message was deleted"
        // messages are not sent to hidden users
        return allMessages.stream().filter(m -> m.getHiddenFor() == null || !m.getHiddenFor().contains(requester)).map(m -> {
            if (m.isDeletedForEveryone()) m.setMessage("🚫 This message was deleted");
            return m;
        }).collect(Collectors.toList());
    }

    // Deletions

    // Clear chat
    public boolean clearChat(String requester, String target, boolean isGroup) throws IOException {
        Path path = isGroup ? getGroupFile(target) : getConvoFile(requester, target);
        File file = path.toFile();
        if (!file.exists()) return false;

        List<Message> messages = objectMapper.readValue(file, new TypeReference<List<Message>>() {});
        for (Message m : messages) {
            if (m.getHiddenFor() == null) m.setHiddenFor(new ArrayList<>());
            // Add user to hidden list if not already present
            if (!m.getHiddenFor().contains(requester)) m.getHiddenFor().add(requester);
        }
        objectMapper.writeValue(file, messages);
        return true;
    }

    // Delete for everyone
    public boolean deleteMessageForEveryone(String sender, String otherUser, String timestamp, boolean isGroup) throws IOException {
        Path path = isGroup ? getGroupFile(otherUser) : getConvoFile(sender, otherUser);
        File file = path.toFile();
        if (!file.exists()) return false;

        List<Message> messages = objectMapper.readValue(file, new TypeReference<List<Message>>() {});
        boolean found = false;
        for (Message m : messages) {
            if (m.getTimestamp().equals(timestamp) && m.getSender().equals(sender)) {
                m.setDeletedForEveryone(true);
                found = true;
                break;
            }
        }
        if (found) objectMapper.writeValue(file, messages);
        return found;
    }

    // Group chat features

    public boolean createGroup(String groupName, String creator) throws IOException {
        createDir();
        File gFile = GROUPS_FILE.toFile();
        Map<String, List<String>> groups;

        if (gFile.exists())
            groups = objectMapper.readValue(gFile, new TypeReference<Map<String, List<String>>>() {});
        else groups = new HashMap<>();

        if (groups.containsKey(groupName)) return false; // Group already exists

        List<String> members = new ArrayList<>();
        members.add(creator);
        groups.put(groupName, members);
        objectMapper.writeValue(gFile, groups);
        return true;
    }

    public boolean addMemberToGroup(String groupName, String newMember) throws IOException {
        File gFile = GROUPS_FILE.toFile();
        if (!gFile.exists()) return false;

        Map<String, List<String>> groups = objectMapper.readValue(gFile, new TypeReference<Map<String, List<String>>>() {});
        if (!groups.containsKey(groupName)) return false;

        List<String> members = groups.get(groupName);
        if (!members.contains(newMember)) {
            members.add(newMember);
            objectMapper.writeValue(gFile, groups);
            return true;
        }
        return false;
    }

    public boolean isGroupMember(String groupName, String user) throws IOException {
        File gFile = GROUPS_FILE.toFile();
        if (!gFile.exists()) return false;
        Map<String, List<String>> groups = objectMapper.readValue(gFile, new TypeReference<Map<String, List<String>>>() {});
        return groups.containsKey(groupName) && groups.get(groupName).contains(user);
    }
}