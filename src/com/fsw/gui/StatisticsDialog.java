package com.fsw.gui;

import com.fsw.database.DatabaseStats;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Read-only dialog presenting a summary of everything stored in the database:
 * total records, the time span they cover, and breakdowns by activity type
 * and by file extension. The summary can be exported to a CSV file.
 *
 * @author Sudip Chaudhary
 * @author Ali Wafaee
 * @version 1.0
 */
public class StatisticsDialog extends JDialog {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DatabaseStats stats;

    /**
     * Creates the statistics dialog for the given snapshot.
     *
     * @param parent the owning window
     * @param stats  the statistics snapshot to display
     */
    public StatisticsDialog(Frame parent, DatabaseStats stats) {
        super(parent, "Database Statistics", true);
        this.stats = stats;
        buildUI(stats);
        setSize(440, 480);
        setMinimumSize(new Dimension(380, 380));
        setLocationRelativeTo(parent);
    }

    /**
     * Builds the dialog content: an HTML summary pane plus Export/Close buttons.
     *
     * @param stats the statistics to render
     */
    private void buildUI(DatabaseStats stats) {
        setLayout(new BorderLayout(8, 8));

        JEditorPane pane = new JEditorPane("text/html", buildHtml(stats));
        pane.setEditable(false);
        pane.setBackground(UIManager.getColor("Panel.background"));
        pane.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        add(new JScrollPane(pane), BorderLayout.CENTER);

        JButton exportBtn = new JButton("Export CSV");
        exportBtn.setToolTipText("Save these statistics to a CSV file");
        exportBtn.setEnabled(stats.getTotal() > 0);
        exportBtn.addActionListener(e -> exportCsv());

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(exportBtn);
        south.add(close);
        add(south, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(close);
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Renders the statistics as an HTML fragment for display.
     *
     * @param s the statistics to render
     * @return the HTML document text
     */
    private String buildHtml(DatabaseStats s) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif'>");
        sb.append("<h2 style='margin:0 0 6px 0'>Database Statistics</h2>");

        if (s.getTotal() == 0) {
            sb.append("<p>The database is empty — no events have been saved yet.</p>");
            return sb.append("</body></html>").toString();
        }

        sb.append("<table cellspacing='0' cellpadding='2'>");
        sb.append(row("Total records", String.valueOf(s.getTotal())));
        sb.append(row("Earliest event", s.getEarliest() == null ? "—" : s.getEarliest()));
        sb.append(row("Latest event", s.getLatest() == null ? "—" : s.getLatest()));
        sb.append("</table>");

        sb.append(section("By Activity Type", s.getByActivity(), s.getTotal()));
        sb.append(section("By Extension", s.getByExtension(), s.getTotal()));

        return sb.append("</body></html>").toString();
    }

    /**
     * Formats one label/value pair as an HTML table row.
     *
     * @param label the row label
     * @param value the row value
     * @return the HTML {@code <tr>} markup
     */
    private String row(String label, String value) {
        return "<tr><td><b>" + label + ":</b></td><td>&nbsp;" + value + "</td></tr>";
    }

    /**
     * Formats a titled breakdown table (key, count, and percentage of total) as HTML.
     *
     * @param title the section heading
     * @param data  the counts keyed by category
     * @param total the overall total used to compute percentages
     * @return the HTML markup for the section
     */
    private String section(String title, Map<String, Integer> data, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3 style='margin:12px 0 4px 0'>").append(title).append("</h3>");
        if (data.isEmpty()) {
            return sb.append("<p>(none)</p>").toString();
        }
        sb.append("<table cellspacing='0' cellpadding='2'>");
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            int pct = total > 0 ? Math.round(e.getValue() * 100f / total) : 0;
            sb.append("<tr><td>").append(e.getKey()).append("</td>")
              .append("<td align='right'>&nbsp;&nbsp;").append(e.getValue()).append("</td>")
              .append("<td>&nbsp;(").append(pct).append("%)</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    // -----------------------------------------------------------------------
    // CSV export
    // -----------------------------------------------------------------------

    /** Prompts for a save location and writes the statistics summary to a CSV file. */
    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Statistics as CSV");
        fc.setSelectedFile(new File("statistics.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String fullPath = fc.getSelectedFile().getAbsolutePath();
        if (!fullPath.toLowerCase().endsWith(".csv")) fullPath += ".csv";
        File output = new File(fullPath);

        try (FileWriter w = new FileWriter(output)) {
            w.write(buildCsv());
            JOptionPane.showMessageDialog(this,
                "Statistics exported to:\n" + output.getAbsolutePath(),
                "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Export failed:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Builds the CSV text: a summary block followed by the two breakdown tables. */
    private String buildCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("# File System Watcher — Statistics Export\n");
        sb.append("# Generated,").append(LocalDateTime.now().format(DT_FMT)).append("\n");
        sb.append("\n");

        sb.append("Metric,Value\n");
        sb.append("Total records,").append(stats.getTotal()).append("\n");
        sb.append("Earliest event,").append(csv(stats.getEarliest())).append("\n");
        sb.append("Latest event,").append(csv(stats.getLatest())).append("\n");
        sb.append("\n");

        appendBreakdown(sb, "By Activity Type", stats.getByActivity());
        sb.append("\n");
        appendBreakdown(sb, "By Extension", stats.getByExtension());
        return sb.toString();
    }

    /**
     * Appends a titled breakdown (category, count, percent) to the CSV builder.
     *
     * @param sb    the builder to append to
     * @param title the section title / first-column header
     * @param data  the counts keyed by category
     */
    private void appendBreakdown(StringBuilder sb, String title, Map<String, Integer> data) {
        sb.append(csv(title)).append(",Count,Percent\n");
        int total = stats.getTotal();
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            int pct = total > 0 ? Math.round(e.getValue() * 100f / total) : 0;
            sb.append(csv(e.getKey())).append(",")
              .append(e.getValue()).append(",")
              .append(pct).append("%\n");
        }
    }

    /** Quotes a value for CSV if needed (commas, quotes, newlines). */
    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
