/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.btactic.hsm.soap;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

import com.zimbra.cs.account.soap.SoapProvisioning;

import com.zimbra.cs.volume.Volume;

import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;
import com.zimbra.soap.admin.message.MoveBlobsRequest;
import com.zimbra.soap.admin.message.MoveBlobsResponse;

import com.zimbra.soap.admin.type.VolumeInfo;

import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.service.admin.AdminDocumentHandler;



public class MoveBlobs extends AdminDocumentHandler {

    private List<Short> getValidLocators(SoapProvisioning prov) throws ServiceException {
        List<Short> validLocators = new ArrayList<Short>();

        GetAllVolumesRequest request = new GetAllVolumesRequest();
        Element requestElement = JaxbUtil.jaxbToElement(request);
        Element respElem = prov.invoke(requestElement);
        GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);

        for (VolumeInfo volumeInfo : response.getVolumes()) {

            if (volumeInfo.getType() == Volume.TYPE_INDEX) {
                break;
            }

            if ((Volume.StoreType.getStoreTypeBy(volumeInfo.getStoreType()).equals(Volume.StoreType.INTERNAL)) && (volumeInfo.getStoreManagerClass().equals("com.zimbra.cs.store.file.FileBlobStore"))) {
                validLocators.add(volumeInfo.getId());
            }
        }

        return validLocators;
    }

    // TODO: Add util library so this code can be reused here and also in BlobMover.java file
    private List<Short> getValidOriginLocators(SoapProvisioning prov, int destinationLocator) throws ServiceException {
        List<Short> validOriginLocators = new ArrayList<Short>();

        GetAllVolumesRequest request = new GetAllVolumesRequest();
        Element requestElement = JaxbUtil.jaxbToElement(request);
        Element respElem = prov.invoke(requestElement);
        GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);

        for (VolumeInfo volumeInfo : response.getVolumes()) {

            if (volumeInfo.getId() == destinationLocator) {
                break;
            }

            if (volumeInfo.getType() == Volume.TYPE_INDEX) {
                break;
            }

            if ((Volume.StoreType.getStoreTypeBy(volumeInfo.getStoreType()).equals(Volume.StoreType.INTERNAL)) && (volumeInfo.getStoreManagerClass().equals("com.zimbra.cs.store.file.FileBlobStore"))) {
                validOriginLocators.add(volumeInfo.getId());
            }
        }

        return validOriginLocators;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        String defaultMoveBlobsQuery = "is:anywhere";

        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();

        MoveBlobsRequest req = JaxbUtil.elementToJaxb(request);
        MoveBlobsResponse resp = new MoveBlobsResponse();

        String types = req.getTypes();
        String sourceVolumeIdsString = req.getSourceVolumeIds();
        Short destVolumeId = req.getDestVolumeId();
        Long maxBytes = req.getMaxBytes();
        String query = req.getQuery();

        // Basic checks
        if (types == null) {
            throw ServiceException.INVALID_REQUEST("must specify types", null);
        }
        if (sourceVolumeIdsString == null) {
            throw ServiceException.INVALID_REQUEST("must specify sourceVolumeIds", null);
        }
        if (destVolumeId == null) {
            throw ServiceException.INVALID_REQUEST("must specify destVolumeId", null);
        }

        if (query == null) {
            query = defaultMoveBlobsQuery;
        }

        // ADVANCED CHECKS
        // types: No need to check if they are valid types. If they don't exist the search will not give results for them
        // maxbytes: No need to check. If maxbytes is not a number execution fails long before.
        // query: No need to check. Either no results (for being empty) or an error for its syntax not being correct

        // destVolumeId
        List<Short> validLocators = getValidLocators(prov);
        if (!(validLocators.contains(destVolumeId))) {
            throw ServiceException.INVALID_REQUEST("destVolumeId: '" + destVolumeId + "' is not a valid destination Volume ID", null);
        }

        // sourceVolumeIds
        String[] sourceVolumeIdsStringArray = sourceVolumeIdsString.split(",");
        ArrayList<String> sourceVolumeIds = new ArrayList<>(Arrays.asList(sourceVolumeIdsStringArray));
        List<Short> validOriginLocators = getValidOriginLocators(prov, destVolumeId);
        for (String sourceVolumeId : sourceVolumeIds) {
            if (!(validOriginLocators.contains(sourceVolumeId))) {
                throw ServiceException.INVALID_REQUEST("sourceVolumeId: '" + sourceVolumeId + "' is not a valid source Volume ID", null);
            }
        }

        // TODO: Manage loops based on maxBytes being null (no maximum value) or not (with maximum value)
        // TODO: Move some blobs

        boolean moveNextBlob = true;
        Long currentMaximumBytes = Long.valueOf(0);
        Long blobBytes;
        blobBytes = Long.valueOf(0);

        while (moveNextBlob) {
            if (!(maxBytes == null)) {
                // TODO: Get blob size and save it in blobBytes (if any)
                Long expectedMaximumBytes = currentMaximumBytes + blobBytes;
                if (expectedMaximumBytes > maxBytes) {
                    moveNextBlob = false;
                } else {
                    currentMaximumBytes = currentMaximumBytes + blobBytes;
                }
            }
            // TODO: Move the blob
        }

        Integer numBlobsMovedMockup = Integer.valueOf(1);
        resp.setNumBlobsMoved(numBlobsMovedMockup);
        Long numBytesMovedMockup = Long.valueOf(2);
        resp.setNumBytesMoved(numBytesMovedMockup);
        Integer totalMailboxesMockup = Integer.valueOf(3);
        resp.setTotalMailboxes(totalMailboxesMockup);

        return zsc.jaxbToElement(resp);
    }
}
