package com.fsw.gui;

import com.fsw.database.EventDatabase;
import com.fsw.model.ActivityType;
import com.fsw.model.FileEvent;
import com.fsw.query.QueryFilter;
import com.fsw.report.CsvReportWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Non-modal dialog for searching the event database and exporting results.
 *
 * <p>Lets the user filter stored events by start/end date, extension, activity
 * type, and directory path, displays the matching rows in a sortable table, and
 * exports the current results to a CSV file (with a query-summary header). It can
 * also clear the entire database. The main window remains usable while this dialog
 * is open.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class QueryFrame extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] ACTIVITY_OPTIONS  = {"All", "CREATED", "MODIFIED", "DELETED", "RENAMED"};
    private static final String[] COLUMNS           = {"File Name", "Extension", "Path", "Activity", "Date/Time"};

    private final EventDatabase database;

    private JTextField startDateField, endDateField, extensionField, directoryField;
    private JComboBox<String> activityCombo;
    private DefaultTableModel tableModel;
    private JLabel resultLabel;

    private List<FileEvent> lastResults;      // results from the most recent query
    private QueryFilter     lastFilter;       // filter used for the most recent query

    /**
     * Creates the query dialog.
     *
     * @param parent   the owning window
     * @param database the event database to query and export from
     */
    public QueryFrame(Frame parent, EventDatabase database) {
        super(parent, "Query Database", false); // non-modal: main window stays usable
        this.database = database;
        buildUI();
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    /** Assembles the dialog: filter panel, results table, button bar, and Esc-to-close. */
    private void buildUI() {
        setSize(950, 600);
        setMinimumSize(new Dimension(750, 450));
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout(4, 4));
        add(buildFilterPanel(), BorderLayout.NORTH);
        add(buildResultsPanel(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        // Escape closes the window
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    /**
     * Builds the filter panel (date range, extension, activity, directory).
     *
     * @return the assembled filter panel
     */
    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Filter  (leave fields blank to match all)"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;

        // row 0: dates
        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Start Date (yyyy-MM-dd):"), gc);
        gc.gridx = 1;
        startDateField = new JTextField(12);
        startDateField.setToolTipText("e.g. 2024-01-01  — leave blank for no lower bound");
        panel.add(startDateField, gc);

        gc.gridx = 2;
        panel.add(new JLabel("End Date (yyyy-MM-dd):"), gc);
        gc.gridx = 3;
        endDateField = new JTextField(12);
        endDateField.setToolTipText("e.g. 2024-12-31  — leave blank for no upper bound");
        panel.add(endDateField, gc);

        // row 1: extension + activity
        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Extension:"), gc);
        gc.gridx = 1;
        extensionField = new JTextField(10);
        extensionField.setToolTipText("e.g. txt  or  .java  — leave blank for all extensions");
        panel.add(extensionField, gc);

        gc.gridx = 2;
        panel.add(new JLabel("Activity:"), gc);
        gc.gridx = 3;
        activityCombo = new JComboBox<>(ACTIVITY_OPTIONS);
        activityCombo.setToolTipText("Filter by event type, or choose All");
        panel.add(activityCombo, gc);

        // row 2: directory (spans columns)
        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Directory path:"), gc);
        gc.gridx = 1; gc.gridwidth = 3; gc.fill = GridBagConstraints.HORIZONTAL;
        directoryField = new JTextField(36);
        directoryField.setToolTipText("Filter by path prefix — leave blank for all directories");
        panel.add(directoryField, gc);
        gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE;

        return panel;
    }

    /**
     * Builds the scrollable, sortable results table.
     *
     * @return the assembled results panel
     */
    private JScrollPane buildResultsPanel() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(155);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(295);
        table.getColumnModel().getColumn(3).setPreferredWidth(95);
        table.getColumnModel().getColumn(4).setPreferredWidth(155);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        table.setRowSorter(new javax.swing.table.TableRowSorter<>(tableModel));

        return new JScrollPane(table);
    }

    /**
     * Builds the action button bar (Show All, Query, Export, Clear, Close) and wires
     * Enter-to-query on the filter fields.
     *
     * @return the assembled button bar
     */
    private JPanel buildButtonBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        JButton showAllBtn = new JButton("Show All");
        showAllBtn.setToolTipText("Retrieve every record from the database");
        showAllBtn.addActionListener(e -> runQuery(true));

        JButton queryBtn = new JButton("Query");
        queryBtn.setToolTipText("Run query with the filters above (Enter)");
        queryBtn.addActionListener(e -> runQuery(false));

        JButton exportBtn = new JButton("Export CSV");
        exportBtn.setToolTipText("Export current results to a CSV file");
        exportBtn.addActionListener(e -> exportCsv());

        JButton clearDbBtn = new JButton("Clear Database");
        clearDbBtn.setToolTipText("Permanently delete all database records");
        clearDbBtn.addActionListener(e -> clearDatabase());

        JButton closeBtn = new JButton("Close  (Esc)");
        closeBtn.setToolTipText("Return to the main watcher window");
        closeBtn.addActionListener(e -> dispose());

        resultLabel = new JLabel("No query run yet.");

        panel.add(showAllBtn);
        panel.add(queryBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(exportBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(clearDbBtn);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(closeBtn);
        panel.add(Box.createHorizontalStrut(16));
        panel.add(resultLabel);

        // Enter in any filter field triggers Query
        KeyAdapter enterQuery = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) runQuery(false);
            }
        };
        startDateField.addKeyListener(enterQuery);
        endDateField.addKeyListener(enterQuery);
        extensionField.addKeyListener(enterQuery);
        directoryField.addKeyListener(enterQuery);

        // set Query as the default button
        getRootPane().setDefaultButton(queryBtn);

        return panel;
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    /**
     * Builds a filter from the input fields (or an empty one) and runs the query,
     * populating the results table.
     *
     * @param showAll if {@code true}, ignore the filter fields and retrieve all records
     */
    private void runQuery(boolean showAll) {
        QueryFilter filter = new QueryFilter();

        if (!showAll) {
            // parse start date
            String startStr = startDateField.getText().trim();
            if (!startStr.isEmpty()) {
                try {
                    filter.setStartDate(LocalDate.parse(startStr, DATE_FMT));
                } catch (DateTimeParseException ex) {
                    showError("Invalid start date — use yyyy-MM-dd (e.g. 2024-01-15).");
                    startDateField.requestFocus();
                    return;
                }
            }

            // parse end date
            String endStr = endDateField.getText().trim();
            if (!endStr.isEmpty()) {
                try {
                    filter.setEndDate(LocalDate.parse(endStr, DATE_FMT));
                } catch (DateTimeParseException ex) {
                    showError("Invalid end date — use yyyy-MM-dd (e.g. 2024-12-31).");
                    endDateField.requestFocus();
                    return;
                }
            }

            if (filter.getStartDate() != null && filter.getEndDate() != null
                    && filter.getStartDate().isAfter(filter.getEndDate())) {
                showError("Start date must be on or before end date.");
                return;
            }

            filter.setExtension(extensionField.getText().trim());

            String actStr = (String) activityCombo.getSelectedItem();
            if (!"All".equals(actStr)) {
                try { filter.setActivityType(ActivityType.valueOf(actStr)); }
                catch (IllegalArgumentException ignored) {}
            }

            filter.setDirectory(directoryField.getText().trim());
        }

        try {
            lastResults = database.query(filter);
            lastFilter  = filter;
            populateTable(lastResults);
            resultLabel.setText(lastResults.size() + " result(s) found.");
        } catch (Exception ex) {
            showError("Query failed:\n" + ex.getMessage());
        }
    }

    /**
     * Replaces the table contents with the given events.
     *
     * @param events the rows to display
     */
    private void populateTable(List<FileEvent> events) {
        tableModel.setRowCount(0);
        for (FileEvent e : events) {
            tableModel.addRow(new Object[]{
                e.getFileName(),
                e.getExtension(),
                e.getFullPath(),
                e.getActivityType() != null ? e.getActivityType().name() : "",
                e.getTimestamp() != null ? e.getTimestamp().format(DT_FMT) : ""
            });
        }
    }

    /** Prompts for a save location and exports the current results to CSV with a header. */
    private void exportCsv() {
        if (lastResults == null || lastResults.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No results to export. Run a query first.",
                "Export CSV", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Query Results as CSV");
        fc.setSelectedFile(new File("query_results.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File chosen = fc.getSelectedFile();
        String fullPath = chosen.getAbsolutePath();
        if (!fullPath.toLowerCase().endsWith(".csv")) fullPath += ".csv";
        File output = new File(fullPath);

        try {
            CsvReportWriter writer = new CsvReportWriter(output.getParent());
            writer.writeToFileWithHeader(lastResults, output.getAbsolutePath(), buildQueryDescription());
            JOptionPane.showMessageDialog(this,
                "Exported " + lastResults.size() + " row(s) to:\n" + output.getAbsolutePath(),
                "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Export failed:\n" + ex.getMessage());
        }
    }

    /** Builds a human-readable block describing the query that produced lastResults. */
    private String buildQueryDescription() {
        String startStr = startDateField.getText().trim();
        String endStr   = endDateField.getText().trim();
        String ext      = extensionField.getText().trim();
        String activity = (String) activityCombo.getSelectedItem();
        String dir      = directoryField.getText().trim();

        return "# File System Watcher — Query Export\n" +
               "# Generated : " + LocalDateTime.now().format(DT_FMT) + "\n" +
               "# Start Date: " + (startStr.isEmpty() ? "(any)" : startStr) + "\n" +
               "# End Date  : " + (endStr.isEmpty()   ? "(any)" : endStr)   + "\n" +
               "# Extension : " + (ext.isEmpty()      ? "(any)" : ext)      + "\n" +
               "# Activity  : " + ("All".equals(activity) ? "(any)" : activity) + "\n" +
               "# Directory : " + (dir.isEmpty()      ? "(any)" : dir)      + "\n" +
               "# Records   : " + lastResults.size() + "\n";
    }

    /** Deletes all records from the database after the user confirms. */
    private void clearDatabase() {
        int choice = JOptionPane.showConfirmDialog(this,
            "This will permanently delete ALL records from the database.\n" +
            "This cannot be undone. Continue?",
            "Clear Database", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            database.clearAll();
            tableModel.setRowCount(0);
            lastResults = null;
            resultLabel.setText("Database cleared.");
            JOptionPane.showMessageDialog(this,
                "All database records deleted.",
                "Clear Database", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Failed to clear database:\n" + ex.getMessage());
        }
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
