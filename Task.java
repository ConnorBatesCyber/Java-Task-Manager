// Task.java
// Represents a single task with a name, priority, due date, and completion status.
// Uses only concepts from Building Java Programs (Ch. 1-15): classes, fields,
// constructors, accessor/mutator methods, String methods, and basic logic.

public class Task {
    // Priority constants - using ints keeps things simple and easy to sort.
    // Lower number = higher priority (1 = High, 2 = Medium, 3 = Low).
    public static final int HIGH = 1;
    public static final int MEDIUM = 2;
    public static final int LOW = 3;

    // Delimiter used when saving/loading. Picked a character that is very
    // unlikely to appear in a task name.
    public static final String DELIM = "|";

    private String name;
    private int priority;     // 1, 2, or 3
    private String dueDate;   // stored as YYYY-MM-DD text (no Date class needed)
    private boolean completed;

    // Full constructor
    public Task(String name, int priority, String dueDate, boolean completed) {
        this.name = name;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = completed;
    }

    // Convenience constructor for new tasks (not yet completed)
    public Task(String name, int priority, String dueDate) {
        this(name, priority, dueDate, false);
    }

    // --- Accessors ---
    public String getName()      { return name; }
    public int getPriority()     { return priority; }
    public String getDueDate()   { return dueDate; }
    public boolean isCompleted() { return completed; }

    // --- Mutators ---
    public void setName(String name)         { this.name = name; }
    public void setPriority(int priority)    { this.priority = priority; }
    public void setDueDate(String dueDate)   { this.dueDate = dueDate; }
    public void setCompleted(boolean done)   { this.completed = done; }

    // Convert priority number to a readable label
    public String getPriorityLabel() {
        if (priority == HIGH) {
            return "High";
        } else if (priority == MEDIUM) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    // Returns true if this task's due date is before today's date AND
    // the task is not yet completed. Dates are compared as strings, which
    // works because we use YYYY-MM-DD format (sorts correctly as text).
    public boolean isOverdue(String today) {
        if (completed) {
            return false;
        }
        if (dueDate == null || dueDate.equals("")) {
            return false;
        }
        // String comparison works because of YYYY-MM-DD ordering
        return dueDate.compareTo(today) < 0;
    }

    // Convert this task to a single line of text for saving to a file.
    // Format:  name|priority|dueDate|completed
    public String toFileString() {
        return name + DELIM + priority + DELIM + dueDate + DELIM + completed;
    }

    // Build a Task from a single saved line. Returns null if the line is bad.
    // (Static factory method - parallels what the book shows for parsing input.)
    public static Task fromFileString(String line) {
        // Split on the literal pipe character. We use Pattern.quote-style escape
        // by passing "\\|" because split() takes a regex.
        String[] parts = line.split("\\|");
        if (parts.length != 4) {
            return null;
        }
        try {
            String name = parts[0];
            int priority = Integer.parseInt(parts[1]);
            String dueDate = parts[2];
            boolean completed = Boolean.parseBoolean(parts[3]);
            return new Task(name, priority, dueDate, completed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Used when displaying tasks in plain text form
    public String toString() {
        String status;
        if (completed) {
            status = "[X]";
        } else {
            status = "[ ]";
        }
        return status + " " + name + " (Priority: " + getPriorityLabel()
                + ", Due: " + dueDate + ")";
    }
}
