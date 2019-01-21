/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Contributors:
 *     anechaev
 *     Harlan Brown (customization for S3)
 */
package org.nuxeo.sample;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.csv.core.CSVImportResult;
import org.nuxeo.ecm.csv.core.CSVImportStatus;
import org.nuxeo.ecm.csv.core.operation.CSVImportLogOperation;
import org.nuxeo.ecm.csv.core.operation.CSVImportResultOperation;
import org.nuxeo.ecm.csv.core.operation.CSVImportStatusOperation;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.sample.blobprovider.DummyBlobProvider;
import org.nuxeo.sample.operation.CustomCSVImportOperation;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 8.10
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, TransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.csv.core") 
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.sample.sample-s3-csv-importer-core") 

@LocalDeploy({
"org.nuxeo.sample.sample-s3-csv-importer-core:OSGI-INF/test-types-contrib.xml",
"org.nuxeo.sample.sample-s3-csv-importer-core:OSGI-INF/dummy-blob-provider.xml"})

public class TestImportOperation {

    private static final int TIMEOUT_SECONDS = 20;

    private static final String TEST_CSV = "s3assets.csv";

    @Inject
    private CoreSession session;

    @Inject
    AutomationService service;

    OperationChain chain;

    DocumentModel testWorkspace;

    @Before
    public void setUp() {
        testWorkspace = session.createDocumentModel("/default-domain/workspaces", "ws1", "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testImportOperation() throws OperationException, InterruptedException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("path", testWorkspace.getPathAsString());

        chain = new OperationChain("test-chain");
        chain.add(CustomCSVImportOperation.ID).from(params);

        OperationContext ctx = new OperationContext(session);
        File csv = FileUtils.getResourceFileFromContext(TEST_CSV);
        Blob blob = new FileBlob(csv);
        ctx.setInput(blob);

        String importId = (String) service.run(ctx, chain);

        assertNotNull(importId);

        boolean completed = false;
        long start = System.currentTimeMillis();
        long end = start + TIMEOUT_SECONDS * 1000;
        do {
            if (System.currentTimeMillis() > end) {
                fail(String.format("CSV could not complete after %d seconds", TIMEOUT_SECONDS));
            }
            chain = new OperationChain("test-chain");
            chain.add(CSVImportStatusOperation.ID);

            ctx = new OperationContext(session);
            ctx.setInput(importId);

            CSVImportStatus status = (CSVImportStatus) service.run(ctx, chain);

            assertNotNull(status);
            completed = status.isComplete();
            if (!completed) {
                Thread.sleep(100);
            }
        } while (!completed);

        chain = new OperationChain("test-chain");
        chain.add(CSVImportResultOperation.ID);

        ctx = new OperationContext(session);
        ctx.setInput(importId);

        CSVImportResult result = (CSVImportResult) service.run(ctx, chain);

        assertNotNull(result);
        assertEquals(0, result.getErrorLineCount());
        assertEquals(0, result.getSkippedLineCount());
        assertEquals(5, result.getSuccessLineCount());
        assertEquals(5, result.getTotalLineCount());
    }
}
