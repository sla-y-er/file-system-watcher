package com.fsw.query;

import com.fsw.database.Queryable;
import com.fsw.email.EmailSender;
import com.fsw.model.FileEvent;
import com.fsw.report.ReportWriter;

import java.io.File;
import java.util.List;

public class QueryEngine {

    private final Queryable db;
    private final ReportWriter writer;
    private final EmailSender emailSender;

    public QueryEngine(
            Queryable db,
            ReportWriter writer,
            EmailSender emailSender
    ) {

        this.db = db;
        this.writer = writer;
        this.emailSender = emailSender;
    }

    public List<FileEvent> runQuery(
            QueryFilter q
    ) {

        if (!validateFilter(q)) {

            throw new IllegalArgumentException(
                    "Invalid query filter."
            );
        }

        List<FileEvent> rows =
                db.query(q);

        System.out.println(
                "Found "
                        + rows.size()
                        + " matching events."
        );

        return rows;
    }

    public File promptExport(
            List<FileEvent> rows,
            String outputPath
    ) {

        if (rows == null || rows.isEmpty()) {

            throw new IllegalArgumentException(
                    "No rows available for export."
            );
        }

        File output =
                new File(outputPath);

        writer.write(rows, output);

        System.out.println(
                "Report exported to: "
                        + output.getAbsolutePath()
        );

        return output;
    }

    public void promptEmail(
            String recipient,
            File file
    ) {

        if (file == null || !file.exists()) {

            throw new IllegalArgumentException(
                    "File does not exist."
            );
        }

        emailSender.send(
                recipient,
                file
        );

        System.out.println(
                "Email sent successfully."
        );
    }

    private boolean validateFilter(
            QueryFilter q
    ) {

        return q != null;
    }
}