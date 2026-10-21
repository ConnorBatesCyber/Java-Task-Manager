// TaskManagerApp.java
// A desktop task manager built with Swing. Uses only concepts from
// Building Java Programs (Ch. 1-15): classes, ArrayList, file I/O with
// Scanner and PrintStream, String methods, sorting, and Swing GUI basics.
//
// Required methods (all present below):
//   addTask, deleteTask, markTaskComplete, displayTasks,
//   saveTasks, loadTasks, sortTasksByPriority, filterTasks,
//   editTask, clearCompletedTasks, validateInput, sortTasksByDueDate,
//   searchTasks, highlightOverdueTasks

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class TaskManagerApp extends JFrame implements ActionListener {

    // ---------- Configuration ----------
    public static final String SAVE_FILE = "tasks.txt";
    public static final Color HEADER_COLOR    = new Color(45, 90, 135);
    public static final Color OVERDUE_COLOR   = new Color(255, 215, 215);
    public static final Color COMPLETED_COLOR = new Color(220, 240, 220);
    public static final Color STRIPE_COLOR    = new Color(245, 248, 252);

    // ---------- Filter modes used by filterTasks() ----------
    public static final int FILTER_ALL        = 0;
    public static final int FILTER_ACTIVE     = 1;
    public static final int FILTER_COMPLETED  = 2;

    // ---------- Data ----------
    private ArrayList<Task> tasks;     // master list
    private int currentFilter;         // which filter is active
    private String currentSearch;      // current search keyword (empty = none)

    // ---------- GUI components ----------
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, completeButton;
    private JButton sortPriorityButton, sortDateButton, clearCompletedButton;
    private JButton saveButton, loadButton;
    private JTextField searchField;
    private JComboBox<String> filterBox;
    private JLabel statusLabel;

    public TaskManagerApp() {
        tasks = new ArrayList<Task>();
        currentFilter = FILTER_ALL;
        currentSearch = "";

        buildGui();
        loadTasks();      // try to restore previous session on startup
        displayTasks();   // refresh the table
    }

    // ====================================================================
    //                          GUI CONSTRUCTION
    // ====================================================================

    private void buildGui() {
        setTitle("Task Manager");
        setSize(820, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // ----- Header banner -----
        JLabel header = new JLabel("  My Task Manager", JLabel.LEFT);
        header.setOpaque(true);
        header.setBackground(HEADER_COLOR);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.setPreferredSize(new Dimension(0, 50));
        // Optional: load an icon if one exists in the working folder.
        // Drop a 32x32 PNG named "icon.png" next to the .class files and
        // it will appear automatically. Safe if missing.
        File iconFile = new File("icon.png");
        if (iconFile.exists()) {
            header.setIcon(new ImageIcon("icon.png"));
        }
        // (header gets added to the frame later, inside northPanel)

        // ----- Center: task table -----
        String[] columns = {"Done", "Task", "Priority", "Due Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            // Make all cells read-only; editing happens through buttons.
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom renderer paints each row based on its state -> highlightOverdueTasks
        table.setDefaultRenderer(Object.class, new TaskRowRenderer());

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(380);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(scroll, BorderLayout.CENTER);

        // ----- Top bar: search + filter -----
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        topBar.add(new JLabel("Search:"));
        searchField = new JTextField(18);
        searchField.addActionListener(this);   // pressing Enter triggers search
        topBar.add(searchField);

        JButton searchButton = new JButton("Find");
        searchButton.setActionCommand("search");
        searchButton.addActionListener(this);
        topBar.add(searchButton);

        JButton clearSearchButton = new JButton("Clear");
        clearSearchButton.setActionCommand("clearSearch");
        clearSearchButton.addActionListener(this);
        topBar.add(clearSearchButton);

        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(new JLabel("Show:"));
        filterBox = new JComboBox<String>(
                new String[] {"All", "Active only", "Completed only"});
        filterBox.setActionCommand("filter");
        filterBox.addActionListener(this);
        topBar.add(filterBox);

        // Stack header above topBar
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(header, BorderLayout.NORTH);
        northPanel.add(topBar, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // ----- Bottom: action buttons + status bar -----
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));

        addButton = makeButton("Add",            "add");
        editButton = makeButton("Edit",          "edit");
        deleteButton = makeButton("Delete",      "delete");
        completeButton = makeButton("Mark Done", "complete");
        sortPriorityButton = makeButton("Sort: Priority", "sortPriority");
        sortDateButton = makeButton("Sort: Due Date",     "sortDate");
        clearCompletedButton = makeButton("Clear Completed", "clearCompleted");
        saveButton = makeButton("Save",          "save");
        loadButton = makeButton("Load",          "load");

        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        buttons.add(completeButton);
        buttons.add(sortPriorityButton);
        buttons.add(sortDateButton);
        buttons.add(clearCompletedButton);
        buttons.add(saveButton);
        buttons.add(loadButton);

        statusLabel = new JLabel(" Ready.");
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttons, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    // Helper to keep button creation tidy
    private JButton makeButton(String label, String command) {
        JButton b = new JButton(label);
        b.setActionCommand(command);
        b.addActionListener(this);
        b.setFocusPainted(false);
        return b;
    }

    // ====================================================================
    //                       EVENT HANDLING (Ch. 14)
    // ====================================================================

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        // The text field also fires events on Enter; treat that as search.
        if (cmd == null) cmd = "";

        if (cmd.equals("add")) {
            addTask();
        } else if (cmd.equals("edit")) {
            editTask();
        } else if (cmd.equals("delete")) {
            deleteTask();
        } else if (cmd.equals("complete")) {
            markTaskComplete();
        } else if (cmd.equals("sortPriority")) {
            sortTasksByPriority();
        } else if (cmd.equals("sortDate")) {
            sortTasksByDueDate();
        } else if (cmd.equals("clearCompleted")) {
            clearCompletedTasks();
        } else if (cmd.equals("save")) {
            saveTasks();
        } else if (cmd.equals("load")) {
            loadTasks();
            displayTasks();
        } else if (cmd.equals("filter")) {
            currentFilter = filterBox.getSelectedIndex();
            filterTasks(currentFilter);
        } else if (cmd.equals("search") || e.getSource() == searchField) {
            searchTasks(searchField.getText());
        } else if (cmd.equals("clearSearch")) {
            searchField.setText("");
            currentSearch = "";
            displayTasks();
        }
    }

    // ====================================================================
    //                       REQUIRED METHODS
    // ====================================================================

    // --------- addTask ---------
    public void addTask() {
        // Build a small form using JOptionPane.showConfirmDialog
        JTextField nameField = new JTextField();
        JComboBox<String> priorityField = new JComboBox<String>(
                new String[] {"High", "Medium", "Low"});
        JTextField dateField = new JTextField(todayString());

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Task name:"));
        form.add(nameField);
        form.add(new JLabel("Priority:"));
        form.add(priorityField);
        form.add(new JLabel("Due date (YYYY-MM-DD):"));
        form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form, "Add Task",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText();
        int priority = priorityField.getSelectedIndex() + 1;  // 1, 2, or 3
        String date = dateField.getText();

        if (!validateInput(name, date)) {
            return;
        }

        tasks.add(new Task(name.trim(), priority, date.trim()));
        setStatus("Added: " + name.trim());
        displayTasks();
    }

    // --------- deleteTask ---------
    public void deleteTask() {
        int taskIndex = getSelectedTaskIndex();
        if (taskIndex < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task first.");
            return;
        }
        Task t = tasks.get(taskIndex);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this task?\n\n" + t.getName(),
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            tasks.remove(taskIndex);
            setStatus("Deleted: " + t.getName());
            displayTasks();
        }
    }

    // --------- markTaskComplete ---------
    public void markTaskComplete() {
        int taskIndex = getSelectedTaskIndex();
        if (taskIndex < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task first.");
            return;
        }
        Task t = tasks.get(taskIndex);
        // Toggle: if it's already done, allow un-marking
        t.setCompleted(!t.isCompleted());
        if (t.isCompleted()) {
            setStatus("Marked complete: " + t.getName());
        } else {
            setStatus("Marked active: " + t.getName());
        }
        displayTasks();
    }

    // --------- displayTasks ---------
    // Refreshes the table to show whatever's in 'tasks', after applying the
    // current filter and search keyword. This is the single rendering path -
    // every change calls displayTasks() to refresh.
    public void displayTasks() {
        tableModel.setRowCount(0);   // clear all rows

        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);

            // Apply filter
            if (currentFilter == FILTER_ACTIVE && t.isCompleted()) {
                continue;
            }
            if (currentFilter == FILTER_COMPLETED && !t.isCompleted()) {
                continue;
            }

            // Apply search keyword (case-insensitive)
            if (currentSearch.length() > 0) {
                String lowerName = t.getName().toLowerCase();
                String lowerKey = currentSearch.toLowerCase();
                if (lowerName.indexOf(lowerKey) < 0) {
                    continue;
                }
            }

            String doneMark;
            if (t.isCompleted()) {
                doneMark = "✓";
            } else {
                doneMark = "";
            }

            Object[] row = {
                doneMark,
                t.getName(),
                t.getPriorityLabel(),
                t.getDueDate()
            };
            tableModel.addRow(row);
        }

        // Update status counts
        int total = tasks.size();
        int done = 0;
        int overdue = 0;
        String today = todayString();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).isCompleted()) done++;
            if (tasks.get(i).isOverdue(today)) overdue++;
        }
        setStatus(" " + total + " task(s)  -  " + done + " done  -  "
                + overdue + " overdue");
    }

    // --------- saveTasks ---------
    // Writes every task as one line to tasks.txt using PrintStream (Ch. 6).
    public void saveTasks() {
        try {
            PrintStream out = new PrintStream(new File(SAVE_FILE));
            for (int i = 0; i < tasks.size(); i++) {
                out.println(tasks.get(i).toFileString());
            }
            out.close();
            setStatus("Saved " + tasks.size() + " task(s) to " + SAVE_FILE);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not save tasks: " + e.getMessage());
        }
    }

    // --------- loadTasks ---------
    // Reads tasks.txt line by line with a Scanner (Ch. 6).
    public void loadTasks() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) {
            // No saved file yet - that's fine on first run
            return;
        }
        try {
            Scanner in = new Scanner(file);
            tasks.clear();
            int loaded = 0;
            while (in.hasNextLine()) {
                String line = in.nextLine();
                if (line.length() == 0) {
                    continue;
                }
                Task t = Task.fromFileString(line);
                if (t != null) {
                    tasks.add(t);
                    loaded++;
                }
            }
            in.close();
            setStatus("Loaded " + loaded + " task(s) from " + SAVE_FILE);
        } catch (FileNotFoundException e) {
            // Already checked exists() above, but Scanner still declares it
            JOptionPane.showMessageDialog(this,
                    "Could not load tasks: " + e.getMessage());
        }
    }

    // --------- sortTasksByPriority ---------
    // Simple selection sort (Ch. 13). Sorts ascending by priority number,
    // so High (1) comes before Medium (2) and Low (3).
    public void sortTasksByPriority() {
        for (int i = 0; i < tasks.size() - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < tasks.size(); j++) {
                if (tasks.get(j).getPriority() < tasks.get(minIndex).getPriority()) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                Task temp = tasks.get(i);
                tasks.set(i, tasks.get(minIndex));
                tasks.set(minIndex, temp);
            }
        }
        setStatus("Sorted by priority.");
        displayTasks();
    }

    // --------- sortTasksByDueDate ---------
    // Selection sort by due date. Because dates are stored as "YYYY-MM-DD",
    // String.compareTo gives the right chronological order.
    public void sortTasksByDueDate() {
        for (int i = 0; i < tasks.size() - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < tasks.size(); j++) {
                String a = tasks.get(j).getDueDate();
                String b = tasks.get(minIndex).getDueDate();
                if (a.compareTo(b) < 0) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                Task temp = tasks.get(i);
                tasks.set(i, tasks.get(minIndex));
                tasks.set(minIndex, temp);
            }
        }
        setStatus("Sorted by due date.");
        displayTasks();
    }

    // --------- filterTasks ---------
    // Sets the active filter mode and refreshes the display.
    public void filterTasks(int mode) {
        if (mode != FILTER_ALL && mode != FILTER_ACTIVE && mode != FILTER_COMPLETED) {
            return;
        }
        currentFilter = mode;
        if (mode == FILTER_ALL)        setStatus("Showing all tasks.");
        if (mode == FILTER_ACTIVE)     setStatus("Showing active tasks.");
        if (mode == FILTER_COMPLETED)  setStatus("Showing completed tasks.");
        displayTasks();
    }

    // --------- editTask ---------
    public void editTask() {
        int taskIndex = getSelectedTaskIndex();
        if (taskIndex < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task first.");
            return;
        }
        Task t = tasks.get(taskIndex);

        JTextField nameField = new JTextField(t.getName());
        JComboBox<String> priorityField = new JComboBox<String>(
                new String[] {"High", "Medium", "Low"});
        priorityField.setSelectedIndex(t.getPriority() - 1);
        JTextField dateField = new JTextField(t.getDueDate());

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Task name:"));
        form.add(nameField);
        form.add(new JLabel("Priority:"));
        form.add(priorityField);
        form.add(new JLabel("Due date (YYYY-MM-DD):"));
        form.add(dateField);

        int result = JOptionPane.showConfirmDialog(this, form, "Edit Task",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String newName = nameField.getText();
        String newDate = dateField.getText();
        if (!validateInput(newName, newDate)) {
            return;
        }

        t.setName(newName.trim());
        t.setPriority(priorityField.getSelectedIndex() + 1);
        t.setDueDate(newDate.trim());
        setStatus("Edited: " + t.getName());
        displayTasks();
    }

    // --------- clearCompletedTasks ---------
    public void clearCompletedTasks() {
        int before = tasks.size();
        // Iterate backwards so removing items doesn't skip indices
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).isCompleted()) {
                tasks.remove(i);
            }
        }
        int removed = before - tasks.size();
        setStatus("Removed " + removed + " completed task(s).");
        displayTasks();
    }

    // --------- validateInput ---------
    // Returns true if the input is acceptable; otherwise shows a message and
    // returns false. Checks: non-empty name, and a YYYY-MM-DD date that
    // contains only digits and dashes in the expected positions.
    public boolean validateInput(String name, String date) {
        if (name == null || name.trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "Task name cannot be empty.");
            return false;
        }
        if (name.indexOf(Task.DELIM) >= 0) {
            JOptionPane.showMessageDialog(this,
                    "Task name cannot contain the '|' character.");
            return false;
        }
        if (date == null || !isValidDateFormat(date.trim())) {
            JOptionPane.showMessageDialog(this,
                    "Due date must be in YYYY-MM-DD format (e.g. 2026-05-01).");
            return false;
        }
        return true;
    }

    // Helper - checks YYYY-MM-DD pattern manually (no regex needed)
    private boolean isValidDateFormat(String date) {
        if (date.length() != 10) return false;
        if (date.charAt(4) != '-' || date.charAt(7) != '-') return false;
        for (int i = 0; i < date.length(); i++) {
            if (i == 4 || i == 7) continue;
            char c = date.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        // Light range checks on month and day
        int month = Integer.parseInt(date.substring(5, 7));
        int day   = Integer.parseInt(date.substring(8, 10));
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;
        return true;
    }

    // --------- searchTasks ---------
    // Case-insensitive keyword match against task names. Updates the active
    // search keyword and re-renders the table.
    public void searchTasks(String keyword) {
        if (keyword == null) keyword = "";
        currentSearch = keyword.trim();
        if (currentSearch.length() == 0) {
            setStatus("Search cleared.");
        } else {
            setStatus("Searching for: \"" + currentSearch + "\"");
        }
        displayTasks();
    }

    // --------- highlightOverdueTasks ---------
    // The actual highlighting happens in TaskRowRenderer below (Swing renders
    // each cell through that class). This method exists as the public entry
    // point: it re-runs the display so the renderer re-evaluates every row
    // against today's date. Calling this is equivalent to a refresh, and it
    // also pops up a summary so the user gets immediate feedback.
    public void highlightOverdueTasks() {
        String today = todayString();
        int count = 0;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).isOverdue(today)) {
                count++;
            }
        }
        displayTasks();
        if (count == 0) {
            setStatus("No overdue tasks.");
        } else {
            setStatus(count + " task(s) overdue (highlighted in red).");
        }
    }

    // ====================================================================
    //                          HELPERS
    // ====================================================================

    // Returns today's date as YYYY-MM-DD using only basic Java
    // (no java.time, no SimpleDateFormat - keeps this beginner-friendly).
    private String todayString() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int y = c.get(java.util.Calendar.YEAR);
        int m = c.get(java.util.Calendar.MONTH) + 1;  // 0-indexed
        int d = c.get(java.util.Calendar.DAY_OF_MONTH);
        return y + "-" + pad2(m) + "-" + pad2(d);
    }

    private String pad2(int n) {
        if (n < 10) return "0" + n;
        return "" + n;
    }

    // Maps a row in the visible table to its index in the master 'tasks'
    // list. Needed because filtering/searching can hide rows.
    private int getSelectedTaskIndex() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return -1;

        // Walk through tasks, counting only the ones that pass the current
        // filter+search, until we hit the right visible row.
        int visible = 0;
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (currentFilter == FILTER_ACTIVE && t.isCompleted()) continue;
            if (currentFilter == FILTER_COMPLETED && !t.isCompleted()) continue;
            if (currentSearch.length() > 0) {
                if (t.getName().toLowerCase().indexOf(
                        currentSearch.toLowerCase()) < 0) continue;
            }
            if (visible == viewRow) {
                return i;
            }
            visible++;
        }
        return -1;
    }

    private void setStatus(String text) {
        statusLabel.setText(" " + text);
    }

    // ====================================================================
    //                  CUSTOM ROW RENDERER (overdue/done coloring)
    // ====================================================================

    // Inner class that paints each cell with a background color depending on
    // the task's state. This is what makes highlightOverdueTasks() visible.
    private class TaskRowRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value,
                    isSelected, hasFocus, row, col);

            // Find which Task this row corresponds to using the same
            // filter/search logic as displayTasks().
            Task task = findTaskForVisibleRow(row);
            if (task != null && !isSelected) {
                String today = todayString();
                if (task.isOverdue(today)) {
                    c.setBackground(OVERDUE_COLOR);
                } else if (task.isCompleted()) {
                    c.setBackground(COMPLETED_COLOR);
                } else if (row % 2 == 0) {
                    c.setBackground(STRIPE_COLOR);
                } else {
                    c.setBackground(Color.WHITE);
                }
            }
            return c;
        }
    }

    // Same mapping logic used by getSelectedTaskIndex, but for any row
    private Task findTaskForVisibleRow(int viewRow) {
        int visible = 0;
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (currentFilter == FILTER_ACTIVE && task.isCompleted()) continue;
            if (currentFilter == FILTER_COMPLETED && !task.isCompleted()) continue;
            if (currentSearch.length() > 0) {
                if (task.getName().toLowerCase().indexOf(
                        currentSearch.toLowerCase()) < 0) continue;
            }
            if (visible == viewRow) {
                return task;
            }
            visible++;
        }
        return null;
    }

    // ====================================================================
    //                              MAIN
    // ====================================================================

    public static void main(String[] args) {
        // Use the system's native look-and-feel so it matches the OS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back silently to the default look-and-feel
        }
        TaskManagerApp app = new TaskManagerApp();
        app.setLocationRelativeTo(null);
        app.setVisible(true);
    }
}
