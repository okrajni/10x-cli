package com.example.doneyet.domain;

import java.util.Objects;

public class DomainTask {
    private final String id;
    private final String title;
    private final TaskCategory category;
    private final int frequencyDays;
    private final Season season;

    public DomainTask(String id, String title, TaskCategory category, int frequencyDays, Season season) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.frequencyDays = frequencyDays;
        this.season = season;
    }

    public DomainTask(String id, String title, TaskCategory category, int frequencyDays) {
        this(id, title, category, frequencyDays, null);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public int getFrequencyDays() {
        return frequencyDays;
    }

    public Season getSeason() {
        return season;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainTask that = (DomainTask) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
