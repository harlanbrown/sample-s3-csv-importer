/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     gildas
 */
package org.nuxeo.sample.blobprovider;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.AbstractBlobProvider;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobInfo;
//import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.model.Document;

/**
 * Dummy Blob provider for tesing the CSV importer. The provider maintains a map digest/path to binary in the resources
 * folder of the project.
 * 
 * @since 1.0.0
 */
public class DummyBlobProvider extends AbstractBlobProvider {

    protected Map<String, String> mapBlobs;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        // Init the map
        mapBlobs = new java.util.HashMap<>();
        mapBlobs.put("4eba1a4ee8690564ad7844a1301e6637", "monk-nurtured.png");
        mapBlobs.put("1ef8f62e461c7974c34c66cdf48d9d16", "grenades-palate.png");
        mapBlobs.put("170a60043423017b18521a5dcd889c04", "brunhilde-contractors.png");
        mapBlobs.put("7776c811af1f98ac9d2e131e15d3eae2", "moorings-colt.png");
        mapBlobs.put("2c4d1efbb2ca197636aac8515203bb91", "habitualness-flavors.png");
    }

    @Override
    public void close() {
        // Clear the map
        mapBlobs.clear();
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        String digest = blobInfo.digest;
        // Check if the digest matches a stored value
        String fileName = mapBlobs.get(digest);
        if (fileName != null) {
            File file = new File(FileUtils.getResourcePathFromContext("testbinaries/" + fileName));
            return new FileBlob(file, blobInfo.mimeType, blobInfo.encoding, blobInfo.filename, blobInfo.digest);
        }
        return null;
    }

    @Override
    //public String writeBlob(Blob blob, Document doc) throws IOException {
    public String writeBlob(Blob blob) throws IOException {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

}
