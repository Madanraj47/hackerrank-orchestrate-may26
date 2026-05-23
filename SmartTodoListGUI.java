package Todoapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SmartTodoListGUI {

    // Removed unused ANSI color constants.

    // Priority colors (now Color objects)
    public static final Map<String, Color> PRIORITY_COLOR = Map.of(
            "High", new Color(0xE53935),    // Red
            "Medium", new Color(0xFBBC05),  // Amber
            "Low", new Color(0x43A047)      // Green
    );
    
    // Default Category Icons
    public static final Map<String, String> CATEGORY_ICONS = Map.of(
            "Work", "💼",
            "Study", "📚",
            "Personal", "🏠",
            "Shopping", "🛒",
            "Finance", "💰", // Added new category
            "Health", "🏃",  // Added new category
            "Other", "📝"
    );

    public static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String FILE_NAME = "tasks_backup.dat";

    // ----- Task class (Enhanced Encapsulation) -----
    public static class Task implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private String description;
        private LocalDate dueDate;
        private String priority;
        private String category;
        private boolean isCompleted;

        public Task(String title, String description, LocalDate dueDate, String priority, String category) {
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.priority = priority;
            this.category = category;
            this.isCompleted = false;
        }

        // Getters - enforce read-only access
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDate getDueDate() { return dueDate; }
        public String getPriority() { return priority; }
        public String getCategory() { return category; }
        public boolean isCompleted() { return isCompleted; }

        public String getCategoryIcon() {
            return CATEGORY_ICONS.getOrDefault(category, "📝");
        }

        public boolean isOverdue() {
            return LocalDate.now().isAfter(dueDate) && !isCompleted;
        }

        public boolean isDueToday() {
            return LocalDate.now().isEqual(dueDate);
        }

        // Behavior defined in the object itself
        public void toggleComplete() {
            this.isCompleted = !this.isCompleted;
        }

        public void edit(String title, String description, LocalDate dueDate, String priority, String category, boolean completed) {
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.priority = priority;
            this.category = category;
            this.isCompleted = completed;
        }

        @Override
        public String toString() {
            return title + " (" + dueDate.format(DF) + ")";
        }
    }

    // ----- Task Manager (The OOP Core) -----
    public static class TaskManager {
        private final List<Task> tasks = new ArrayList<>();

        public TaskManager() {
            loadTasks();
        }

        public void addTask(Task t) {
            tasks.add(t);
            saveTasks();
        }

        // Returns a copy of the list to prevent external modification
        public List<Task> getTasks() {
            return new ArrayList<>(tasks);
        }

        public void removeTask(Task t) {
            tasks.remove(t);
            saveTasks();
        }

        public void saveChanges() {
            saveTasks();
        }

        public List<Task> search(String keyword) {
            String k = keyword.toLowerCase();
            return tasks.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(k) ||
                            t.getDescription().toLowerCase().contains(k) ||
                            t.getCategory().toLowerCase().contains(k) ||
                            t.getPriority().toLowerCase().contains(k))
                    .collect(Collectors.toList());
        }

        /**
         * Smart Sorting Logic: 
         * 1. Prioritize incomplete tasks over completed ones.
         * 2. Sort by Priority (High > Medium > Low).
         * 3. Sort by Due Date (Earliest first).
         */
        public void smartSort() {
            Map<String, Integer> priOrder = Map.of("High", 0, "Medium", 1, "Low", 2);
            tasks.sort(Comparator
                .comparing(Task::isCompleted) // Incomplete first (false < true)
                .thenComparingInt(t -> priOrder.getOrDefault(t.getPriority(), 3)) // Priority order
                .thenComparing(Task::getDueDate)); // Earliest date first
            saveTasks();
        }

        public long completedCount() {
            return tasks.stream().filter(Task::isCompleted).count();
        }

        public long totalCount() {
            return tasks.size();
        }
        
        // Persistence methods remain the same
        @SuppressWarnings("unchecked")
        private void loadTasks() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
                Object obj = ois.readObject();
                if (obj instanceof List) {
                    tasks.addAll((List<Task>) obj);
                }
            } catch (Exception ignored) {}
        }

        private void saveTasks() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                oos.writeObject(tasks);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error saving tasks: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----- GUI components and State -----
    private final JFrame frame;
    private final DefaultListModel<Task> listModel = new DefaultListModel<>();
    private final JList<Task> taskJList = new JList<>(listModel);
    private final TaskManager manager = new TaskManager();
    private final JTextField searchField = new JTextField(20);
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    // State for filtering
    private String currentCategoryFilter = "All";
    private String currentStatusFilter = "All";


    public SmartTodoListGUI() {
        // Set Look and Feel (assuming FlatLaf or System L&F)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        frame = new JFrame("🏆 Smart To-Do List Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 680);
        frame.setLocationRelativeTo(null);

        initUI();
        loadListFromManager();
        updateProgressBar();

        frame.setVisible(true);
    }

    private void initUI() {
        // Top Header Panel
        JPanel top = new JPanel(new BorderLayout(15, 0));
        top.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("<html><span style='font-size:18pt;font-weight:bold;'>✨ Smart Task Board</span></html>");
        top.add(title, BorderLayout.WEST);

        // Progress Bar in Header
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(150, 24));
        progressBar.setForeground(new Color(0x4CAF50)); // Green color
        top.add(progressBar, BorderLayout.CENTER);

        // Search Area
        JPanel searchPanel = new JPanel();
        searchPanel.add(searchField);
        JButton searchBtn = new JButton("Search");
        JButton clearSearch = new JButton("Clear");
        searchPanel.add(searchBtn);
        searchPanel.add(clearSearch);
        top.add(searchPanel, BorderLayout.EAST);

        // Main Content Panel (Filter Panel + Task List)
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.add(createFilterPanel(), BorderLayout.WEST);
        mainContent.add(createTaskListPanel(), BorderLayout.CENTER);
        mainContent.add(createControlPanel(), BorderLayout.EAST);

        // Layout Main Frame
        frame.setLayout(new BorderLayout());
        frame.add(top, BorderLayout.NORTH);
        frame.add(mainContent, BorderLayout.CENTER);
        
        // Footer
        JLabel footer = new JLabel("Persistence File: " + FILE_NAME);
        footer.setBorder(new EmptyBorder(6, 12, 6, 12));
        frame.add(footer, BorderLayout.SOUTH);

        // Listeners
        searchBtn.addActionListener(e -> applySearchAndFilters());
        clearSearch.addActionListener(e -> { searchField.setText(""); applySearchAndFilters(); });
        searchField.addActionListener(e -> applySearchAndFilters()); // Search on Enter
    }

    // --- Filter Panel ---
    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(0xE0E0E0), 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        filterPanel.setPreferredSize(new Dimension(180, 0));
        filterPanel.setBackground(new Color(0xF7F7F7));

        // Category Filter
        filterPanel.add(new JLabel("<html><b>Filter by Category:</b></html>"));
        filterPanel.add(Box.createVerticalStrut(5));
        
        String[] categories = CATEGORY_ICONS.keySet().stream().sorted().toArray(String[]::new);
        String[] catOptions = new String[categories.length + 1];
        catOptions[0] = "All";
        System.arraycopy(categories, 0, catOptions, 1, categories.length);

        JComboBox<String> categoryFilterCb = new JComboBox<>(catOptions);
        categoryFilterCb.setMaximumSize(new Dimension(Integer.MAX_VALUE, categoryFilterCb.getPreferredSize().height));
        categoryFilterCb.addActionListener(e -> {
            currentCategoryFilter = (String) categoryFilterCb.getSelectedItem();
            applySearchAndFilters();
        });
        filterPanel.add(categoryFilterCb);

        filterPanel.add(Box.createVerticalStrut(20));

        // Status Filter
        filterPanel.add(new JLabel("<html><b>Filter by Status:</b></html>"));
        filterPanel.add(Box.createVerticalStrut(5));

        JComboBox<String> statusFilterCb = new JComboBox<>(new String[]{"All", "Pending", "Completed", "Overdue"});
        statusFilterCb.setMaximumSize(new Dimension(Integer.MAX_VALUE, statusFilterCb.getPreferredSize().height));
        statusFilterCb.addActionListener(e -> {
            currentStatusFilter = (String) statusFilterCb.getSelectedItem();
            applySearchAndFilters();
        });
        filterPanel.add(statusFilterCb);
        
        filterPanel.add(Box.createVerticalGlue()); // Push components to the top

        return filterPanel;
    }
    
    // --- Task List Panel ---
    private JScrollPane createTaskListPanel() {
        taskJList.setCellRenderer(new TaskCardRenderer()); 
        taskJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskJList.setFixedCellHeight(100); 
        
        // Double-click to edit/toggle
        taskJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Task sel = taskJList.getSelectedValue();
                    if (sel != null) openTaskDialog(sel);
                } else if (e.getClickCount() == 1) {
                    // Check if click was on the icon area to quickly toggle complete
                    int index = taskJList.locationToIndex(e.getPoint());
                    if (index != -1) {
                         Task taskToToggle = listModel.getElementAt(index);
                         // Simple heuristic: if the click is in the first 10% of the item width, toggle
                         if (e.getX() < taskJList.getWidth() * 0.1) { 
                             toggleTaskCompletion(taskToToggle);
                         }
                    }
                }
            }
        });

        // Key listener for delete
        taskJList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedTask();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(taskJList);
        listScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return listScroll;
    }


    // --- Control Panel ---
    private JPanel createControlPanel() {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(10, 10, 10, 10));
        right.setPreferredSize(new Dimension(200, 0));

        JButton addBtn = new JButton("➕ Add New Task");
        JButton editBtn = new JButton("✏️ Edit Selected");
        JButton removeBtn = new JButton("❌ Remove Selected");
        JButton toggleCompleteBtn = new JButton("✅ Toggle Status");
        JButton smartSortBtn = new JButton("⭐ Smart Sort Tasks");
        JButton statsBtn = new JButton("📊 View Statistics");
        JButton exportBtn = new JButton("📄 Export Tasks");

        // Style spacing
        Arrays.asList(addBtn, editBtn, removeBtn, toggleCompleteBtn, smartSortBtn, statsBtn, exportBtn)
                .forEach(b -> {
                    b.setAlignmentX(Component.CENTER_ALIGNMENT);
                    b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                    right.add(b);
                    right.add(Box.createVerticalStrut(8));
                });
                
        right.add(Box.createVerticalGlue());

        // Button listeners
        addBtn.addActionListener(e -> openTaskDialog(null));
        editBtn.addActionListener(e -> {
            Task sel = taskJList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(frame, "Select a task to edit.", "No selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            openTaskDialog(sel);
        });
        removeBtn.addActionListener(e -> removeSelectedTask());
        toggleCompleteBtn.addActionListener(e -> {
            Task sel = taskJList.getSelectedValue();
            if (sel == null) {
                JOptionPane.showMessageDialog(frame, "Select a task to toggle completion.", "No selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            toggleTaskCompletion(sel);
        });
        smartSortBtn.addActionListener(e -> { manager.smartSort(); loadListFromManager(); });
        statsBtn.addActionListener(e -> showStats());
        exportBtn.addActionListener(e -> exportTasksToText());
        
        return right;
    }
    
    // --- Core Logic Methods ---

    private void removeSelectedTask() {
        Task sel = taskJList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(frame, "Select a task to remove.", "No selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(frame, "Remove selected task?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            manager.removeTask(sel);
            listModel.removeElement(sel);
            updateProgressBar();
        }
    }
    
    private void toggleTaskCompletion(Task task) {
        task.toggleComplete();
        manager.saveChanges();
        taskJList.repaint();
        updateProgressBar();
    }
    
    private void updateProgressBar() {
        long completed = manager.completedCount();
        long total = manager.totalCount();
        int percent = total == 0 ? 0 : (int) ((completed * 100.0) / total);
        
        progressBar.setValue(percent);
        progressBar.setString(percent + "% Completed (" + completed + "/" + total + ")");
    }


    private void loadListFromManager() {
        // Default load, then apply current filters
        applySearchAndFilters();
    }
    
    // Primary method for displaying data
    private void loadList(List<Task> tasks) {
        listModel.clear();
        tasks.forEach(listModel::addElement);
    }
    
    private void applySearchAndFilters() {
        List<Task> filteredTasks = manager.getTasks();
        String keyword = searchField.getText().trim();

        // 1. Search Filter (Title, Description, etc.)
        if (!keyword.isEmpty()) {
            filteredTasks = manager.search(keyword);
        }

        // 2. Category Filter
        if (!currentCategoryFilter.equals("All")) {
            filteredTasks = filteredTasks.stream()
                .filter(t -> t.getCategory().equals(currentCategoryFilter))
                .collect(Collectors.toList());
        }

        // 3. Status Filter
        if (!currentStatusFilter.equals("All")) {
            filteredTasks = filteredTasks.stream().filter(t -> {
                return switch (currentStatusFilter) {
                    case "Pending" -> !t.isCompleted();
                    case "Completed" -> t.isCompleted();
                    case "Overdue" -> t.isOverdue();
                    default -> true;
                };
            }).collect(Collectors.toList());
        }

        // 4. Default Sort (Smart Sort is the only exposed sorting method now)
        manager.smartSort(); 
        
        loadList(filteredTasks);
    }
    
    private void openTaskDialog(Task existing) {
        JDialog d = new JDialog(frame, existing == null ? "Add Task" : "Edit Task", true);
        d.setSize(480, 420);
        d.setLocationRelativeTo(frame);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        // Fields
        JTextField titleField = new JTextField(existing != null ? existing.getTitle() : "");
        JTextArea descArea = new JTextArea(existing != null ? existing.getDescription() : "", 4, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        JTextField dueField = new JTextField(existing != null ? existing.getDueDate().format(DF) : LocalDate.now().format(DF));
        JComboBox<String> priorityCb = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        JComboBox<String> categoryCb = new JComboBox<>(CATEGORY_ICONS.keySet().toArray(new String[0]));
        JCheckBox completedCb = new JCheckBox("Completed", existing != null && existing.isCompleted());
        
        if (existing != null) {
            priorityCb.setSelectedItem(existing.getPriority());
            categoryCb.setSelectedItem(existing.getCategory());
        }

        // Layout labels + fields
        c.gridx = 0; c.gridy = 0; c.weightx = 0; p.add(new JLabel("Title:"), c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1.0; p.add(titleField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; p.add(new JLabel("Description:"), c);
        c.gridx = 1; c.gridy = 1; c.weightx = 1.0; p.add(descScroll, c);

        c.gridx = 0; c.gridy = 2; c.weightx = 0; p.add(new JLabel("Due Date (YYYY-MM-DD):"), c);
        c.gridx = 1; c.gridy = 2; c.weightx = 1.0; p.add(dueField, c);

        c.gridx = 0; c.gridy = 3; c.weightx = 0; p.add(new JLabel("Priority:"), c);
        c.gridx = 1; c.gridy = 3; c.weightx = 1.0; p.add(priorityCb, c);

        c.gridx = 0; c.gridy = 4; c.weightx = 0; p.add(new JLabel("Category:"), c);
        c.gridx = 1; c.gridy = 4; c.weightx = 1.0; p.add(categoryCb, c);

        c.gridx = 1; c.gridy = 5; p.add(completedCb, c);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton(existing == null ? "Add Task" : "Save Changes");
        JButton cancelBtn = new JButton("Cancel");
        btns.add(saveBtn);
        btns.add(cancelBtn);

        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; c.weightx = 0; c.anchor = GridBagConstraints.EAST; p.add(btns, c);

        d.getContentPane().add(p);
        d.getRootPane().setDefaultButton(saveBtn);

        saveBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String desc = descArea.getText().trim();
            String dueS = dueField.getText().trim();
            String pri = (String) priorityCb.getSelectedItem();
            String cat = (String) categoryCb.getSelectedItem();
            boolean completed = completedCb.isSelected();
            
            // --- Enhanced Validation ---
            if (title.isEmpty() || desc.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Title and Description are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate due;
            try {
                due = LocalDate.parse(dueS, DF);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Invalid date format. Use YYYY-MM-DD.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Check if due date is in the past (unless existing and already completed)
            if (due.isBefore(LocalDate.now()) && !completed && existing == null) {
                 JOptionPane.showMessageDialog(d, "Tasks should be due today or in the future.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                 return;
            }
            // --- End Validation ---

            if (existing == null) {
                Task t = new Task(title, desc, due, pri, cat);
                t.isCompleted = completed;
                manager.addTask(t);
            } else {
                existing.edit(title, desc, due, pri, cat, completed);
                manager.saveChanges();
                taskJList.repaint();
            }
            
            d.dispose();
            applySearchAndFilters(); // Reload/filter list and re-sort
            updateProgressBar();
        });

        cancelBtn.addActionListener(e -> d.dispose());

        d.setVisible(true);
    }

    private void showStats() {
        long completed = manager.completedCount();
        long total = manager.totalCount();
        long overdue = manager.getTasks().stream().filter(Task::isOverdue).count();
        long pending = total - completed;

        int percent = total == 0 ? 0 : (int) ((completed * 100.0) / total);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(ta.getFont().deriveFont(Font.PLAIN, 12f));
        ta.append("📊 Summary:\n");
        ta.append("  Total Tasks: " + total + "\n");
        ta.append("  Completed: " + completed + "\n");
        ta.append("  Pending: " + pending + "\n");
        ta.append("  Overdue: " + overdue + "\n\n");
        ta.append("🎯 Completion Rate: " + percent + "%\n");
        p.add(new JScrollPane(ta), BorderLayout.CENTER);

        // Simple progress bar
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent);
        bar.setStringPainted(true);
        bar.setString(percent + "% Progress");
        p.add(bar, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(frame, p, "Task Statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportTasksToText() {
        // ... (Export code remains the same as it is mostly fine)
        StringBuilder sb = new StringBuilder();
        sb.append("SMART TO-DO LIST EXPORT - ").append(LocalDate.now().format(DF)).append("\n\n");
        for (int i = 0; i < listModel.size(); i++) {
            Task t = listModel.get(i);
            sb.append("TASK #").append(i + 1).append("\n");
            sb.append("-------------------------------------------------\n");
            sb.append("Title: ").append(t.getTitle()).append("\n");
            sb.append("Description: ").append(t.getDescription()).append("\n");
            sb.append("Category: ").append(t.getCategory()).append("\n");
            sb.append("Priority: ").append(t.getPriority()).append("\n");
            sb.append("Due: ").append(t.getDueDate().format(DF)).append("\n");
            sb.append("Completed: ").append(t.isCompleted() ? "Yes" : "No").append("\n");
            sb.append("Status: ").append(t.isOverdue() ? "OVERDUE" : (t.isCompleted() ? "Completed" : "Pending")).append("\n\n");
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(640, 400));
        JOptionPane.showMessageDialog(frame, sp, "Exported Tasks (text)", JOptionPane.INFORMATION_MESSAGE);
    }

    // =================================================================
    // The modern Task Card UI component (Refactored for cleaner OOP)
    // =================================================================
    private static class TaskCardPanel extends JPanel {
        // Components defined here...
        private final JLabel iconLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel descLabel = new JLabel();
        private final JLabel dueDateLabel = new JLabel();
        private final JPanel categoryPill = new JPanel();
        private final JLabel categoryLabel = new JLabel();
        private final JPanel priorityPill = new JPanel();
        private final JLabel priorityLabel = new JLabel();
        private final JLabel badgeLabel = new JLabel();
        
        // Custom color for the task completion status marker
        private final Color COMPLETION_MARKER_COLOR = new Color(0x66BB6A); 

        public TaskCardPanel() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createCompoundBorder(
                    new EmptyBorder(4, 8, 4, 8), 
                    new LineBorder(new Color(0xE0E0E0), 1, true) // Rounded border simulation
            ));
            setBackground(Color.WHITE);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 4, 3, 4);

            // 1. Completion Marker/Icon (Far Left)
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridheight = 3; 
            gbc.anchor = GridBagConstraints.NORTHWEST;
            iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 20f));
            add(iconLabel, gbc);

            // 2. Title and Badges (Top Center)
            gbc.gridx = 1; gbc.gridy = 0;
            gbc.gridheight = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JPanel titlePanel = new JPanel(new BorderLayout(8, 0));
            titlePanel.setOpaque(false);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
            titlePanel.add(titleLabel, BorderLayout.WEST);
            titlePanel.add(badgeLabel, BorderLayout.EAST);
            add(titlePanel, gbc);

            // 3. Description (Middle Center)
            gbc.gridx = 1; gbc.gridy = 1;
            descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC, 11f));
            descLabel.setForeground(new Color(0x666666));
            add(descLabel, gbc);

            // 4. Tags/Pills (Bottom - Full Width)
            gbc.gridx = 1; gbc.gridy = 2;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JPanel pillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            pillPanel.setOpaque(false);

            setupPill(categoryPill, categoryLabel);
            setupPill(priorityPill, priorityLabel);

            dueDateLabel.setFont(dueDateLabel.getFont().deriveFont(Font.PLAIN, 11f));
            dueDateLabel.setForeground(new Color(0x777777));

            pillPanel.add(categoryPill);
            pillPanel.add(priorityPill);
            pillPanel.add(dueDateLabel);

            add(pillPanel, gbc);
        }
        
        // Helper to set up the look of the "pill" JPanels
        private void setupPill(JPanel pill, JLabel label) {
            pill.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 2));
            label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
            pill.add(label);
            pill.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            // FlatLaf utility to enforce rounded corners if L&F supports it
            pill.putClientProperty("FlatLaf.style", "arc: 999"); 
        }

        public void configure(Task task, boolean isSelected) {
            // Background and Selection
            Color defaultBg = task.isCompleted() ? new Color(0xF0F0F0) : Color.WHITE;
            if (isSelected) {
                setBackground(new Color(0xDCEFFD)); 
                setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(4, 8, 4, 8),
                        new LineBorder(new Color(0x1E88E5), 2, true) 
                ));
            } else {
                setBackground(defaultBg);
                setBorder(BorderFactory.createCompoundBorder(
                        new EmptyBorder(4, 8, 4, 8),
                        new LineBorder(new Color(0xE0E0E0), 1, true)
                ));
            }

            // Task Data
            titleLabel.setText(task.getTitle());
            descLabel.setText(task.getDescription().isEmpty() ? "No description provided." : task.getDescription());
            categoryLabel.setText(task.getCategory());
            priorityLabel.setText(task.getPriority());
            dueDateLabel.setText("Due: " + task.getDueDate().format(DF));

            // --- Status and Visual Indicators ---
            
            // Icon / Completion Marker
            if (task.isCompleted()) {
                iconLabel.setText("✔");
                iconLabel.setForeground(COMPLETION_MARKER_COLOR);
                titleLabel.setForeground(Color.GRAY.darker());
                titleLabel.putClientProperty("FlatLaf.style", "text.font: 14 bold; text.strike: true"); 
                descLabel.putClientProperty("FlatLaf.style", "text.strike: true");
                
            } else {
                iconLabel.setText(task.getCategoryIcon());
                iconLabel.setForeground(new Color(0x424242));
                titleLabel.setForeground(Color.BLACK);
                titleLabel.putClientProperty("FlatLaf.style", "text.font: 14 bold; text.strike: false");
                descLabel.putClientProperty("FlatLaf.style", "text.strike: false");
            }
            
            // Priority Pill Style
            Color priColor = PRIORITY_COLOR.getOrDefault(task.getPriority(), Color.GRAY);
            priorityPill.setBackground(priColor);
            priorityLabel.setForeground(Color.WHITE);

            // Category Pill Style
            categoryPill.setBackground(new Color(0xF0F0F0));
            categoryLabel.setForeground(Color.BLACK);

            // Badges (Due Today/Overdue)
            String badgeHtml = "";
            if (task.isOverdue()) {
                badgeHtml = "<html><span style='color:#e53935;font-size:10pt;font-weight:bold;'>⚠ OVERDUE</span></html>";
            } else if (task.isDueToday() && !task.isCompleted()) {
                badgeHtml = "<html><span style='color:#1e88e5;font-size:10pt;font-weight:bold;'>⏰ TODAY</span></html>";
            } 
            badgeLabel.setText(badgeHtml);
        }
    }

    private static class TaskCardRenderer extends TaskCardPanel implements ListCellRenderer<Task> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Task> list, Task value, int index, boolean isSelected, boolean cellHasFocus) {
            configure(value, isSelected);
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmartTodoListGUI::new);
    }
}
