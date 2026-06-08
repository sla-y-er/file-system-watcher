package com.fsw.gui;

import com.fsw.database.EventDatabase;
import com.fsw.model.FileEvent;
import com.fsw.watcher.FileWatcher;

import com.fsw.database.DatabaseStats;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application window.
 *
 * <p>Provides the user interface for selecting a folder, starting and stopping
 * monitoring, and viewing events live in a sortable, quick-filterable table. It
 * owns a {@link FileWatcher} (created when monitoring starts), persists events to
 * the {@link EventDatabase} on demand, and opens the {@link QueryFrame} and
 * {@link StatisticsDialog}. Events arrive on the watcher's background thread and
 * are marshalled onto the Swing event-dispatch thread before the table updates.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class WatcherFrame extends JFrame {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] COLUMNS = {
        "File Name", "Extension", "Path", "Activity", "Date/Time"
    };

    private static final String[] PRESET_EXTENSIONS = {
        "All Files", ".txt", ".java", ".py", ".log", ".csv",
        ".xml", ".json", ".html", ".pdf", ".docx", "Custom..."
    };

    // --- state ---
    private final EventDatabase database;
    private FileWatcher watcher;
    private final List<FileEvent> pendingEvents = new ArrayList<>();   // unsaved
    private final List<FileEvent> allEvents      = new ArrayList<>();   // all displayed

    // --- UI components ---
    private DefaultTableModel tableModel;
    private JTable eventTable;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private JTextField quickFilterField;
    private JTextField pathField;
    private JComboBox<String> extensionCombo;
    private JTextField customExtField;
    private JCheckBox recursiveCheck;
    private JLabel statusLabel;
    private JLabel countLabel;

    // buttons kept as fields so updateButtonStates() can reach them
    private JButton startBtn, stopBtn, writeDbBtn, queryBtn, clearBtn;
    private JMenuItem startItem, stopItem, writeItem;

    // only one query window at a time
    private QueryFrame queryFrame;

    /**
     * Creates the main window wired to the given database.
     *
     * @param database the open event database used for saving and querying
     */
    public WatcherFrame(EventDatabase database) {
        super("File System Watcher");
        this.database = database;
        buildUI();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    /** Builds the window: menu bar, north controls, event table, and status bar. */
    private void buildUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 600);
        setMinimumSize(new Dimension(750, 460));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onExit(); }
        });

        setJMenuBar(buildMenuBar());
        add(buildNorthPanel(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);

        updateButtonStates(false);
    }

    /**
     * Builds the menu bar (File, Watcher, Database, Help) with accelerators.
     *
     * @return the assembled menu bar
     */
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        // --- File ---
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem clearViewItem = new JMenuItem("Clear Event View");
        clearViewItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        clearViewItem.addActionListener(e -> clearEventView());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> onExit());

        fileMenu.add(clearViewItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // --- Watcher ---
        JMenu watchMenu = new JMenu("Watcher");
        watchMenu.setMnemonic(KeyEvent.VK_W);

        startItem = new JMenuItem("Start Monitoring");
        startItem.setMnemonic(KeyEvent.VK_S);
        startItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        startItem.addActionListener(e -> startWatcher());

        stopItem = new JMenuItem("Stop Monitoring");
        stopItem.setMnemonic(KeyEvent.VK_T);
        stopItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        stopItem.addActionListener(e -> stopWatcher());

        writeItem = new JMenuItem("Write Events to Database");
        writeItem.setMnemonic(KeyEvent.VK_W);
        writeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        writeItem.addActionListener(e -> writeToDatabase());

        watchMenu.add(startItem);
        watchMenu.add(stopItem);
        watchMenu.addSeparator();
        watchMenu.add(writeItem);

        // --- Database ---
        JMenu dbMenu = new JMenu("Database");
        dbMenu.setMnemonic(KeyEvent.VK_D);

        JMenuItem queryItem = new JMenuItem("Query Database");
        queryItem.setMnemonic(KeyEvent.VK_Q);
        queryItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
        queryItem.addActionListener(e -> openQueryWindow());

        JMenuItem statsItem = new JMenuItem("Statistics");
        statsItem.setMnemonic(KeyEvent.VK_S);
        statsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        statsItem.addActionListener(e -> showStatistics());

        JMenuItem clearDbItem = new JMenuItem("Clear Database");
        clearDbItem.setMnemonic(KeyEvent.VK_C);
        clearDbItem.addActionListener(e -> clearDatabase());

        dbMenu.add(queryItem);
        dbMenu.add(statsItem);
        dbMenu.addSeparator();
        dbMenu.add(clearDbItem);

        // --- Help ---
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setMnemonic(KeyEvent.VK_A);
        aboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        aboutItem.addActionListener(e -> showAbout());

        helpMenu.add(aboutItem);

        bar.add(fileMenu);
        bar.add(watchMenu);
        bar.add(dbMenu);
        bar.add(helpMenu);
        return bar;
    }

    /** Returns the combined toolbar + filter panel for BorderLayout.NORTH. */
    private JPanel buildNorthPanel() {
        JPanel north = new JPanel(new BorderLayout());

        // --- Toolbar ---
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        startBtn   = toolButton("Start",    "Start monitoring the selected folder (F5)");
        stopBtn    = toolButton("Stop",     "Stop monitoring (F6)");
        writeDbBtn = toolButton("Write DB", "Save current events to database (Ctrl+S)");
        queryBtn   = toolButton("Query DB", "Open query window (Ctrl+D)");
        JButton statsBtn = toolButton("Stats", "Show database statistics (Ctrl+T)");
        clearBtn   = toolButton("Clear",    "Clear the event display (Ctrl+L)");

        startBtn.addActionListener(e   -> startWatcher());
        stopBtn.addActionListener(e    -> stopWatcher());
        writeDbBtn.addActionListener(e -> writeToDatabase());
        queryBtn.addActionListener(e   -> openQueryWindow());
        statsBtn.addActionListener(e   -> showStatistics());
        clearBtn.addActionListener(e   -> clearEventView());

        toolbar.add(startBtn);
        toolbar.add(stopBtn);
        toolbar.addSeparator();
        toolbar.add(writeDbBtn);
        toolbar.add(queryBtn);
        toolbar.add(statsBtn);
        toolbar.addSeparator();
        toolbar.add(clearBtn);

        // --- Filter panel ---
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        filter.add(new JLabel("Watch Folder:"));

        pathField = new JTextField(26);
        pathField.setToolTipText("Folder to monitor");
        filter.add(pathField);

        JButton browseBtn = new JButton("Browse...");
        browseBtn.setToolTipText("Choose folder");
        browseBtn.addActionListener(e -> browseFolder());
        filter.add(browseBtn);

        filter.add(Box.createHorizontalStrut(12));
        filter.add(new JLabel("Extension:"));

        extensionCombo = new JComboBox<>(PRESET_EXTENSIONS);
        extensionCombo.setToolTipText("Select extension filter (All Files = watch everything)");
        extensionCombo.addActionListener(e -> onExtensionComboChanged());
        filter.add(extensionCombo);

        customExtField = new JTextField(7);
        customExtField.setToolTipText("Enter extension without dot, e.g. txt");
        customExtField.setEnabled(false);
        filter.add(customExtField);

        recursiveCheck = new JCheckBox("Include subfolders");
        recursiveCheck.setToolTipText("Also monitor every sub-directory of the watched folder");
        filter.add(Box.createHorizontalStrut(8));
        filter.add(recursiveCheck);

        north.add(toolbar, BorderLayout.NORTH);
        north.add(filter,  BorderLayout.SOUTH);
        return north;
    }

    /**
     * Builds the event table (sortable columns) with the quick-filter row above it.
     *
     * @return the assembled table panel
     */
    private JPanel buildTablePanel() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        eventTable = new JTable(tableModel);
        eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        eventTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        eventTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        eventTable.getColumnModel().getColumn(2).setPreferredWidth(290);
        eventTable.getColumnModel().getColumn(3).setPreferredWidth(95);
        eventTable.getColumnModel().getColumn(4).setPreferredWidth(155);
        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventTable.setGridColor(Color.LIGHT_GRAY);
        eventTable.setFillsViewportHeight(true);
        eventTable.setRowHeight(22);

        // click column headers to sort
        tableSorter = new TableRowSorter<>(tableModel);
        eventTable.setRowSorter(tableSorter);

        // live quick-filter across all columns
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filterRow.add(new JLabel("Quick filter:"));
        quickFilterField = new JTextField(24);
        quickFilterField.setToolTipText("Show only rows containing this text (any column)");
        quickFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyQuickFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyQuickFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyQuickFilter(); }
        });
        filterRow.add(quickFilterField);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(filterRow, BorderLayout.NORTH);
        panel.add(new JScrollPane(eventTable), BorderLayout.CENTER);
        return panel;
    }

    /** Applies the quick-filter text as a case-insensitive "contains" filter over all columns. */
    private void applyQuickFilter() {
        String text = quickFilterField.getText().trim();
        if (text.isEmpty()) {
            tableSorter.setRowFilter(null);
        } else {
            // (?i) = case-insensitive; quote the text so it is matched literally
            tableSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        }
    }

    /**
     * Builds the bottom status bar (status message on the left, event count on the right).
     *
     * @return the assembled status bar
     */
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createLoweredBevelBorder());

        statusLabel = new JLabel("  Ready — choose a folder and press Start.");
        countLabel  = new JLabel("0 events  ");

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(countLabel,  BorderLayout.EAST);
        return bar;
    }

    /**
     * Creates a toolbar button with the given label and tooltip.
     *
     * @param text the button label
     * @param tip  the tooltip text
     * @return the configured button
     */
    private JButton toolButton(String text, String tip) {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        b.setFocusPainted(false);
        return b;
    }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /** Enables the custom-extension field only when "Custom..." is selected and not watching. */
    private void onExtensionComboChanged() {
        boolean isCustom = "Custom...".equals(extensionCombo.getSelectedItem());
        customExtField.setEnabled(isCustom && !stopBtn.isEnabled()); // only if not watching
        if (isCustom) customExtField.requestFocus();
    }

    /** Opens a directory chooser and, on approval, fills the path field. */
    private void browseFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select folder to monitor");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    /** Validates the inputs and starts monitoring the selected folder. */
    private void startWatcher() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            showError("Please enter or browse for a folder path to monitor.");
            return;
        }
        if (!Files.exists(Paths.get(path))) {
            showError("Path does not exist:\n" + path);
            return;
        }
        if (!Files.isDirectory(Paths.get(path))) {
            showError("Path is not a directory:\n" + path);
            return;
        }

        String ext = resolveExtension();
        if (ext == null) return; // validation failed inside resolveExtension()

        boolean recursive = recursiveCheck.isSelected();

        try {
            watcher = new FileWatcher(Paths.get(path), ext, recursive, this::onFileEvent);
            watcher.start();
            updateButtonStates(true);
            setStatus("Monitoring: " + path +
                      (ext.isBlank() ? " (all files)" : " [." + ext + "]") +
                      (recursive ? " + subfolders" : ""));
        } catch (IOException ex) {
            showError("Failed to start watcher:\n" + ex.getMessage());
        }
    }

    /** Returns the effective extension string (may be blank for "all"), or null on validation error. */
    private String resolveExtension() {
        String sel = (String) extensionCombo.getSelectedItem();
        if ("All Files".equals(sel)) return "";
        if ("Custom...".equals(sel)) {
            String custom = customExtField.getText().replace(".", "").trim();
            if (custom.isEmpty()) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Custom extension is blank. Watch all files?",
                    "Extension", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) return "";
                return null; // user cancelled
            }
            return custom;
        }
        return sel.replace(".", "").trim();
    }

    /** Stops the watcher (if running) and updates the controls and status. */
    private void stopWatcher() {
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
        updateButtonStates(false);
        setStatus("Monitoring stopped. " + allEvents.size() + " event(s) displayed.");
    }

    /** Called from the FileWatcher background thread — must dispatch to EDT. */
    private void onFileEvent(FileEvent event) {
        SwingUtilities.invokeLater(() -> {
            pendingEvents.add(event);
            allEvents.add(event);
            appendTableRow(event);
            countLabel.setText(allEvents.size() + " events  ");
        });
    }

    /**
     * Appends one event as a new table row and scrolls it into view.
     *
     * @param event the event to display
     */
    private void appendTableRow(FileEvent event) {
        tableModel.addRow(new Object[]{
            event.getFileName(),
            event.getExtension(),
            event.getFullPath(),
            event.getActivityType() != null ? event.getActivityType().name() : "",
            event.getTimestamp() != null ? event.getTimestamp().format(DT_FMT) : ""
        });
        // scroll to the newest row (convert model index -> view index for the sorter)
        int last = tableModel.getRowCount() - 1;
        int viewRow = eventTable.convertRowIndexToView(last);
        if (viewRow >= 0) {
            eventTable.scrollRectToVisible(eventTable.getCellRect(viewRow, 0, true));
        }
    }

    /** Persists the unsaved (pending) events to the database, avoiding duplicates. */
    private void writeToDatabase() {
        if (pendingEvents.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No new events to save — everything is already written.",
                "Write to Database", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            List<FileEvent> toSave = new ArrayList<>(pendingEvents);
            database.insertAll(toSave);
            pendingEvents.clear();
            setStatus("Saved " + toSave.size() + " event(s) to database.");
            JOptionPane.showMessageDialog(this,
                toSave.size() + " event(s) saved to database.",
                "Write to Database", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Failed to write to database:\n" + ex.getMessage());
        }
    }

    /** Clears the displayed events (with confirmation if there are unsaved events). */
    private void clearEventView() {
        if (!allEvents.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Clear the event display?" +
                (pendingEvents.isEmpty() ? "" : "\n(" + pendingEvents.size() + " unsaved event(s) will be lost.)"),
                "Clear Events", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        tableModel.setRowCount(0);
        allEvents.clear();
        pendingEvents.clear();
        countLabel.setText("0 events  ");
        setStatus("Event view cleared.");
    }

    /** Opens the query window, bringing the existing one to front if already open. */
    private void openQueryWindow() {
        if (queryFrame != null && queryFrame.isVisible()) {
            queryFrame.toFront();
            return;
        }
        queryFrame = new QueryFrame(this, database);
        queryFrame.setVisible(true);
    }

    /** Computes statistics from the database and shows them in a dialog. */
    private void showStatistics() {
        try {
            DatabaseStats stats = database.getStatistics();
            new StatisticsDialog(this, stats).setVisible(true);
        } catch (Exception ex) {
            showError("Could not compute statistics:\n" + ex.getMessage());
        }
    }

    /** Deletes all records from the database after the user confirms. */
    private void clearDatabase() {
        int choice = JOptionPane.showConfirmDialog(this,
            "This will permanently delete ALL records from the database.\nAre you sure?",
            "Clear Database", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            database.clearAll();
            setStatus("Database cleared.");
            JOptionPane.showMessageDialog(this,
                "Database cleared successfully.", "Clear Database", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Failed to clear database:\n" + ex.getMessage());
        }
    }

    /** Shows the About/Help dialog with usage instructions and shortcuts. */
    private void showAbout() {
        String msg =
            "<html>" +
            "<h2 style='margin-bottom:4px'>File System Watcher</h2>" +
            "<b>Version:</b> 1.0<br>" +
            "<b>Developer:</b> TCSS 360 Team<br>" +
            "<hr style='margin:6px 0'>" +
            "<b>How to use:</b><br>" +
            "<ol style='margin:4px 0 0 16px;padding:0'>" +
            "<li>Browse or type a folder path to monitor.</li>" +
            "<li>Select an extension filter (or keep <i>All Files</i>).</li>" +
            "<li>Press <b>Start</b> (F5) — events appear live in the table.</li>" +
            "<li>Press <b>Stop</b> (F6) when finished.</li>" +
            "<li>Press <b>Write DB</b> (Ctrl+S) to persist events to SQLite.</li>" +
            "<li>Press <b>Query DB</b> (Ctrl+D) to search records and export CSV.</li>" +
            "<li>Press <b>Stats</b> (Ctrl+T) for a breakdown of saved events.</li>" +
            "<li>On exit you will be prompted to save any unsaved events.</li>" +
            "</ol>" +
            "<b>Tips:</b> tick <i>Include subfolders</i> to watch nested folders, " +
            "click a column header to sort, and use <i>Quick filter</i> to search the live table.<br>" +
            "<br><b>Keyboard shortcuts:</b> F5 Start &nbsp; F6 Stop &nbsp; " +
            "Ctrl+S Save &nbsp; Ctrl+D Query &nbsp; Ctrl+T Stats &nbsp; Ctrl+Q Exit &nbsp; F1 About" +
            "</html>";
        JOptionPane.showMessageDialog(this, msg,
            "About File System Watcher", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Handles application exit: prompts to save unsaved events, stops the watcher,
     * closes the database, and terminates.
     */
    private void onExit() {
        if (!pendingEvents.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "You have " + pendingEvents.size() + " unsaved event(s).\n" +
                "Save to database before exiting?",
                "Unsaved Events",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    database.insertAll(pendingEvents);
                } catch (Exception ex) {
                    showError("Could not save events:\n" + ex.getMessage() +
                              "\n\nExit anyway?");
                    // fall through — still exit
                }
            }
        }

        if (watcher != null) watcher.stop();
        database.disconnect();
        dispose();
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Enables or disables controls based on whether monitoring is active.
     *
     * @param watching {@code true} while monitoring is running
     */
    private void updateButtonStates(boolean watching) {
        startBtn.setEnabled(!watching);
        stopBtn.setEnabled(watching);
        startItem.setEnabled(!watching);
        stopItem.setEnabled(watching);
        pathField.setEditable(!watching);
        extensionCombo.setEnabled(!watching);
        recursiveCheck.setEnabled(!watching);
        boolean isCustom = "Custom...".equals(extensionCombo.getSelectedItem());
        customExtField.setEnabled(!watching && isCustom);
    }

    /**
     * Updates the status bar message.
     *
     * @param msg the message to show
     */
    private void setStatus(String msg) {
        statusLabel.setText("  " + msg);
    }

    /**
     * Shows a modal error dialog.
     *
     * @param msg the error message to display
     */
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
