package org.nuxeo.sample.operation;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.csv.core.CSVImporter;
import org.nuxeo.ecm.csv.core.CSVImporterOptions;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;
import org.nuxeo.runtime.api.Framework;

/**
 *
 */
@Operation(id=CustomCSVImportOperation.ID, category=Constants.CAT_DOCUMENT, label="Custom.CSVImport", description="Launch a custom CSV import job")
public class CustomCSVImportOperation {

    public static final String ID = "Custom.CSVImport";

    @Context
    protected CoreSession mSession;

    @Param(name = "path", required = false)
    protected String mPath;

    @Param(name = "sendReport", required = false)
    protected boolean mSendReport;

    @Param(name = "documentMode", required = false)
    protected boolean mDocumentMode;

    @OperationMethod
    public String importCSV(Blob blob) {
        ImportMode importMode = mDocumentMode ? ImportMode.IMPORT : ImportMode.CREATE;
        CSVImporterOptions options = new CSVImporterOptions.Builder().sendEmail(mSendReport)
                                                                     .importMode(importMode)
                                                                     .build();
        CSVImporter csvImporter = Framework.getService(CSVImporter.class); 
        return csvImporter.launchImport(mSession, mPath, blob, options);
    }
}
