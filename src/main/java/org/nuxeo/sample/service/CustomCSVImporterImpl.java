/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Thomas Roger (original impl)
 *     Harlan Brown (customization for S3)
 */

package org.nuxeo.sample.service;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.csv.core.CSVImporter;
import org.nuxeo.ecm.csv.core.CSVImporterImpl;
import org.nuxeo.ecm.csv.core.CSVImporterOptions;

/**
 * @since 5.7
 */
public class CustomCSVImporterImpl extends CSVImporterImpl implements CSVImporter {

    @Override
    public String launchImport(CoreSession session, String parentPath, Blob blob, CSVImporterOptions options) {
        return new CustomCSVImporterWork(session.getRepositoryName(), parentPath, session.getPrincipal().getName(), blob,
                options).launch();
    }

}
