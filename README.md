# sample-s3-csv-importer

This project demonstrates a custom CSV importer which is able to import files in an S3 container.

### The project consists of the following:
*Main classes:*
* CustomCSVImportOperation - Nuxeo operation which initiates the import
* CustomCSVImporterImpl - Service class which queues the Work class
* CustomCSVImporterWork - Work class which reads the CSV file and creates the Documents

*Main resources:*
* custom-csv-importer-service-contrib.xml - registers the custom service in place of the default CSV importer service
* customcsvimportoperation-operation-contrib.xml - registers the custom import operation
* deployment-fragment.xml - copies the custom version of nuxeo-document-import-csv.html which references the custom import operation (Note: a client may choose to override nuxeo-document-import-csv.html in their Nuxeo Studio project)

*Test classes:*
* TestImportOperation - launch a test import via operation
* DummyBlobProvider - Mock Nuxeo blob provider for test

*Test resources:*
* dummy-blob-provider.xml - registers the dummy blob provider
* test-types-contrib.xml - registers doctype info used by TestImportOperation
* s3assets.csv - the CSV file used for the test
* testbinaries - directory containing copies of the binaries referenced in s3assets.csv. These are referenced by the dummy blob provider

### CSV file format
The file:content field contains the md5 hash of a binary, the filename of the binary, and the file length, delimited by the colon (:) character. All other fields (name,type,dc:title,dc:description) follow the example shown in the documentation for the default CSV importer. Documentation link: [CSV File Definition](https://doc.nuxeo.com/nxdoc/nuxeo-csv/#csv-file-definition)

Example ([link](https://github.com/harlanbrown/sample-s3-csv-importer/blob/master/src/test/resources/s3assets.csv)) :
```
name,type,dc:title,dc:description,file:content
monk-nurtured.png,File,monk-nurtured.png,monk-nurtured.png image/png 4eba1a4ee8690564ad7844a1301e6637,4eba1a4ee8690564ad7844a1301e6637:monk-nurtured.png:27239
grenades-palate.png,File,grenades-palate.png,grenades-palate.png image/png 1ef8f62e461c7974c34c66cdf48d9d16,1ef8f62e461c7974c34c66cdf48d9d16:grenades-palate.png:33755
```

### Usage
Binaries must be evaluated for md5 digest prior to storage in S3. Importer expects that binary filename is equal to its md5 sum. Properly named files should be uploaded directly to the S3 bucket referenced by the nuxeo.s3storage.bucket configuration property. Imports can be started in the Nuxeo Web UI by using the CSV tab on the default creation form. Documentation link: [With Web UI](https://doc.nuxeo.com/nxdoc/nuxeo-csv/#with-web-ui)
