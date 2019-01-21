/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Florent Guillaume
 *     Julien Carsique
 *     Harlan Brown (customization for S3)
 */
package org.nuxeo.sample.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.csv.core.CSVImportLog.Status.ERROR;
import static org.nuxeo.ecm.csv.core.Constants.CSV_NAME_COL;
import static org.nuxeo.ecm.csv.core.Constants.CSV_TYPE_COL;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.AWS_ID_ENV;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.AWS_ID_PROPERTY;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.AWS_SECRET_ENV;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.AWS_SECRET_PROPERTY;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_NAME_PROPERTY;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_PREFIX_PROPERTY;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_REGION_PROPERTY;
//import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.SYSTEM_PROPERTY_PREFIX;
//import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.notification.MailTemplateHelper;
import org.nuxeo.ecm.automation.core.operations.notification.SendMail;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.csv.core.CSVImportLog;
import org.nuxeo.ecm.csv.core.CSVImportLog.Status;
import org.nuxeo.ecm.platform.ec.notification.NotificationEventListener;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationServiceHelper;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.csv.core.CSVImporterWork;
import org.nuxeo.ecm.csv.core.CSVImporterOptions;
import org.nuxeo.ecm.csv.core.CSVImportStatus;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.query.sql.NXQL;

//import org.nuxeo.ecm.core.storage.sql.BasicAWSCredentialsProvider;

//import com.amazonaws.AmazonClientException;
//import com.amazonaws.auth.AWSCredentialsProvider;
//import com.amazonaws.auth.InstanceProfileCredentialsProvider;
//import com.amazonaws.regions.Region;
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.model.ObjectMetadata;

//import com.google.common.base.MoreObjects;

import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;

/**
 * Work task to import form a CSV file. Because the file is read from the local filesystem, this must be executed in a
 * local queue. Since NXP-15252 the CSV reader manages "records", not "lines".
 */
public class CustomCSVImporterWork extends CSVImporterWork {

    private static final Logger log = LogManager.getLogger(CustomCSVImporterWork.class);

    protected CSVImporterOptions options;

//    protected AmazonS3 amazonS3;

    public CustomCSVImporterWork(String repositoryName, String parentPath, String username, Blob csvBlob,
            CSVImporterOptions options) {
        super(repositoryName, parentPath, username, csvBlob, options);
        this.options = options;
    }

    static final Serializable EMPTY_LOGS = new ArrayList<CSVImportLog>();



// are launch and getStatus necessary? they are unchanged


    String launch() {
        WorkManager works = Framework.getService(WorkManager.class);

        TransientStore store = getStore();
        store.putParameter(id, "logs", EMPTY_LOGS);
        store.putParameter(id, "status", new CSVImportStatus(CSVImportStatus.State.SCHEDULED));
        works.schedule(this);
        return id;
    }

    static CSVImportStatus getStatus(String id) {
        TransientStore store = getStore();
        if (!store.exists(id)) {
            return null;
        }
        return (CSVImportStatus) store.getParameter(id, "status");
    }

//
    

    @Override
    @SuppressWarnings("unchecked")
    protected Serializable convertValue(CompositeType compositeType, String fieldName, String headerValue,
            String stringValue, long lineNumber) {

        if (compositeType.hasField(fieldName)) {
            Field field = compositeType.getField(fieldName);
            if (field != null) {
                try {
                    Serializable fieldValue = null;
                    Type fieldType = field.getType();
                    if (fieldType.isComplexType()) {
                        if (fieldType.getName().equals(CONTENT_FILED_TYPE_NAME)) {
                            
                            String[] hashAndFilename = stringValue.split(":");

                            String hash = hashAndFilename[0];

                            String filename = hashAndFilename[1];
                            //
                            Long length = Long.parseLong(hashAndFilename[2]);

                            fieldValue = (Serializable) createBlobFromDigest(hash,filename,length);

                            if (fieldValue == null) { 

                                // log entry should be more specific to this case
                                logError(lineNumber, "The file '%s' does not exist",
                                        LABEL_CSV_IMPORTER_NOT_EXISTING_FILE, stringValue);

                                return null;
                            }
                        } else {
                            // this section is not s3 enabled
                            fieldValue = (Serializable) ComplexTypeJSONDecoder.decode((ComplexType) fieldType,
                                    stringValue);
                            replaceBlobs((Map<String, Object>) fieldValue);
                        }
                    } else {
                        if (fieldType.isListType()) {
                            Type listFieldType = ((ListType) fieldType).getFieldType();
                            if (listFieldType.isSimpleType()) {
                                /*
                                 * Array.
                                 */
                                fieldValue = stringValue.split(options.getListSeparatorRegex());
                            } else {
                                /*
                                 * Complex list.
                                 */
                                fieldValue = (Serializable) ComplexTypeJSONDecoder.decodeList((ListType) fieldType,
                                        stringValue);
                                replaceBlobs((List<Object>) fieldValue);
                            }
                        } else {
                            /*
                             * Primitive type.
                             */
                            Type type = field.getType();
                            if (type instanceof SimpleTypeImpl) {
                                type = type.getSuperType();
                            }
                            if (type.isSimpleType()) {
                                if (type instanceof StringType) {
                                    fieldValue = stringValue;
                                } else if (type instanceof IntegerType) {
                                    fieldValue = Integer.valueOf(stringValue);
                                } else if (type instanceof LongType) {
                                    fieldValue = Long.valueOf(stringValue);
                                } else if (type instanceof DoubleType) {
                                    fieldValue = Double.valueOf(stringValue);
                                } else if (type instanceof BooleanType) {
                                    fieldValue = Boolean.valueOf(stringValue);
                                } else if (type instanceof DateType) {
                                    DateFormat dateFormat = options.getDateFormat();
                                    //DateFormat dateFormat = new SimpleDateFormat(options.getDateFormat());
                                    fieldValue = dateFormat != null ? dateFormat.parse(stringValue) : stringValue;
                                }
                            }
                        }
                    }
                    return fieldValue;
                } catch ( ParseException | NumberFormatException | IOException e) {
                    logError(lineNumber, "Unable to convert field '%s' with value '%s'",
                            LABEL_CSV_IMPORTER_CANNOT_CONVERT_FIELD_VALUE, headerValue, stringValue);
                    log.debug(e, e);
                }
            }
        } else {
            logError(lineNumber, "Field '%s' does not exist on type '%s'", LABEL_CSV_IMPORTER_NOT_EXISTING_FIELD,
                    headerValue, compositeType.getName());
        }
        return null;
    }

    protected Blob createBlobFromDigest(String digest, String filename, Long length) {
        // Get the BlobProvider id
        BlobManager blobManager = Framework.getLocalService(BlobManager.class);
        Map<String, BlobProvider> mapBlob = blobManager.getBlobProviders();
        String blobProviderId = null;
        for (Entry<String, BlobProvider> entry : mapBlob.entrySet()) {
            blobProviderId = entry.getKey();
            break;
        }

        // Create BlobInfo
        BlobInfo info = new BlobInfo();
        info.key = blobProviderId + ":" + digest;
        info.digest = digest;
        info.filename = filename;
        info.length = length;
        try {
            info.mimeType = Framework.getService(MimetypeRegistry.class).getMimetypeFromFilename(filename);
        } catch (MimetypeNotFoundException e) {
            log.warn("Mimetype not found for file " + filename);
        }

        return new SimpleManagedBlob(info);
    }

}
