package com.example.mynotes;

public class Note {
    private final int id;
    private final int myId;
    private final String title;
    private final String description;
    private final String data;
    private final int priority;

    public Note(int id, int myId, String title, String description, String data, int priority) {
        this.id = id;
        this.myId = myId;
        this.title = title;
        this.description = description;
        this.data = data;
        this.priority = priority;
    }

    public int getId() {
        return id;
    }

    public int getMyId() {
        return myId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getData() {
        return data;
    }

    public int getPriority() {
        return priority;
    }
}
