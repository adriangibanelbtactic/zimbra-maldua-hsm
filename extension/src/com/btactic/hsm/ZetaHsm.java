/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
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

package com.btactic.hsm;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.volume.Volume;

import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;

import com.zimbra.soap.admin.type.VolumeInfo;

import com.zimbra.soap.JaxbUtil;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ZetaHsm {

    private boolean aborted = false;
    private boolean aborting = false;
    private boolean running = false;
    private String error = new String("");
    Long startDate = 0L;
    Long endDate = null;

    private BlobMover blobMover = null;

    private final static ZetaHsm SINGLETON = new ZetaHsm();

    private ZetaHsm() {
    }

    public static ZetaHsm getInstance() {
        return SINGLETON;
    }

    public synchronized void abort() {
        if (running) {
            ZetaHsmLog.info("Setting aborting flag.");
            aborting = true;
            BlobMoveStats blobMoveStats = getLatestBlobMoveStats();
            if (blobMoveStats != null) {
                blobMoveStats.setAborting(true);
            }
        }
    }

    public synchronized boolean wasAborted() {
        return aborted;
    }

    public synchronized boolean isAborting() {
        return aborting;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized String getError() {
        return error;
    }

    public synchronized Long getStartDate() {
        return startDate;
    }

    public synchronized Long getEndDate() {
        return endDate;
    }

    private synchronized BlobMoveStats getLatestBlobMoveStats() {
        if (blobMover != null) {
           return blobMover.getStats();
        } else {
            return null;
        }
    }

    public synchronized int getNumBlobsMoved() {
        BlobMoveStats blobMoveStats = getLatestBlobMoveStats();
        if (blobMoveStats != null) {
            int numBlobsMoved = blobMoveStats.getNumBlobsMoved();
            return numBlobsMoved;
        } else {
            return -1;
        }
    }

    public synchronized long getNumBytesMoved() {
        BlobMoveStats blobMoveStats = getLatestBlobMoveStats();
        if (blobMoveStats != null) {
            long numBytesMoved = blobMoveStats.getNumBytesMoved();
            return numBytesMoved;
        } else {
            return -1L;
        }
    }

    public synchronized int getNumMailboxesMoved() {
        BlobMoveStats blobMoveStats = getLatestBlobMoveStats();
        if (blobMoveStats != null) {
            int numMailboxesMoved = blobMoveStats.getNumMailboxesMoved();
            return numMailboxesMoved;
        } else {
            return -1;
        }
    }

    public synchronized int getTotalMailboxes() {
        BlobMoveStats blobMoveStats = getLatestBlobMoveStats();
        if (blobMoveStats != null) {
            int totalMailboxes = blobMoveStats.getTotalMailboxes();
            return totalMailboxes;
        } else {
            return -1;
        }
    }

    public synchronized short getDestinationVolumeId() {
        BlobMoveStats blobMoveStats = getLatestBlobMoveStats();
        if (blobMoveStats != null) {
            short destinationVolumeId = blobMoveStats.getDestinationVolumeId();
            return destinationVolumeId;
        } else {
            return -1;
        }
    }

    public synchronized String getQuery() {
        BlobMoveStats blobMoveStats = getLatestBlobMoveStats();
        if (blobMoveStats != null) {
            String query = blobMoveStats.getQuery();
            return query;
        } else {
            return null;
        }
    }

    public void doHsm() throws ServiceException, IOException {
        synchronized (this) {
            if (running) {
                throw MailServiceException.TRY_AGAIN("ZetaHsm is already in progress. Only one request can be run at a time.");
            }
            aborting = false;
            aborted = false;
            running = true;
            error = new String("");
            startDate = System.currentTimeMillis();
            endDate = null;

        }
        Thread thread = new ZetaHsmThread();
        thread.setName("ZetaHsm");
        thread.start();
    }

    private void endHsm() {
        endDate = System.currentTimeMillis();
        if (aborting) {
            aborted = true;
            aborting = false;
        }
        running = false;
    }

    private class ZetaHsmThread extends Thread {

        private boolean isValidHsmPolicySyntaxList(String[] zimbraHsmPolicyList) {
            boolean validHsmPolicySyntaxList = true;
            for (String nZimbraHsmPolicy: zimbraHsmPolicyList) {
                Pattern hsmPolicyPattern = Pattern.compile("^(message|document|task|appointment|contact)(,(message|document|task|appointment|contact))*:(?<hsmSearch>.+)$");
                Matcher hsmPolicyMatcher = hsmPolicyPattern.matcher(nZimbraHsmPolicy);
                boolean validHsmPolicySyntax = hsmPolicyMatcher.matches();
                if (!(validHsmPolicySyntax)) {
                    validHsmPolicySyntaxList = false;
                    ZetaHsmLog.error("zimbraHsmPolicy: '" + nZimbraHsmPolicy + "' syntax is not valid!");
                }
                // TODO: Check also if the search is valid or not at this point
                // TODO: Seems quite difficult to implement because you usually need an actual mailbox for testing it
            }
            return validHsmPolicySyntaxList;
        }

        private String getHsmTypesStringFromHsmPolicy (String zimbraHsmPolicy){
            String hsmTypesString = null;
            Pattern hsmPolicySplitPattern = Pattern.compile("^(?<hsmTypes>.*?):(?<hsmSearchQuery>.*)$");
            Matcher hsmPolicySplitMatcher = hsmPolicySplitPattern.matcher(zimbraHsmPolicy);
            if (hsmPolicySplitMatcher.find()) {
                hsmTypesString = hsmPolicySplitMatcher.group("hsmTypes");
            }
            return hsmTypesString;
        }

        private String getHsmSearchQueryStringFromHsmPolicy (String zimbraHsmPolicy){
            String hsmSearchQueryString = null;
            Pattern hsmPolicySplitPattern = Pattern.compile("^(?<hsmTypes>.*?):(?<hsmSearchQuery>.*)$");
            Matcher hsmPolicySplitMatcher = hsmPolicySplitPattern.matcher(zimbraHsmPolicy);
            if (hsmPolicySplitMatcher.find()) {
                hsmSearchQueryString = hsmPolicySplitMatcher.group("hsmSearchQuery");
            }
            return hsmSearchQueryString;
        }

        private short getDestinationLocator(SoapProvisioning prov) throws ServiceException {
            short destinationLocator = -1;

            GetAllVolumesRequest request = new GetAllVolumesRequest();
            Element requestElement = JaxbUtil.jaxbToElement(request);
            Element respElem = prov.invoke(requestElement);
            GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);
            for (VolumeInfo volumeInfo : response.getVolumes()) {
                if (
                       (volumeInfo.isCurrent()) &&
                       (volumeInfo.getType() == Volume.TYPE_MESSAGE_SECONDARY) &&
                       (Volume.StoreType.getStoreTypeBy(volumeInfo.getStoreType()).equals(Volume.StoreType.INTERNAL)) &&
                       (volumeInfo.getStoreManagerClass().equals("com.zimbra.cs.store.file.FileBlobStore"))
                   ) {
                       destinationLocator = volumeInfo.getId();
                }
            }

            return destinationLocator;
        }

        public ZetaHsmThread() {
        }

        public void run() {
            ZetaHsmLog.debug("ZetaHsm RUN function - Start");
            try {
                String[] zimbraHsmPolicyList = Provisioning.getInstance().getLocalServer().getMultiAttr("zimbraHsmPolicy");

                if (zimbraHsmPolicyList.length == 0) {
                    error = "'zimbraHsmPolicy' attribute is empty. Nothing to do. Aborting.";
                    ZetaHsmLog.info(error);
                    return;
                }

                if (!(isValidHsmPolicySyntaxList(zimbraHsmPolicyList))) {
                    error = "One or more of the 'zimbraHsmPolicy' values does not have a valid syntax. Aborting.";
                    ZetaHsmLog.error(error);
                    return;
                }

                SoapProvisioning prov = SoapProvisioning.getAdminInstance();
                prov.soapZimbraAdminAuthenticate();

                short destinationLocator = getDestinationLocator(prov);
                if (destinationLocator == -1) {
                    error = "We did not find an expected (Secondary, internal, current and FileBlobStore class) destination volume. Aborting.";
                    ZetaHsmLog.error(error);
                    return;
                }

                ZetaHsmLog.debug("destinationLocator: " + destinationLocator);

                int zimbraHsmPolicyCounter = 0;
                for (String nZimbraHsmPolicy: zimbraHsmPolicyList) {
                    zimbraHsmPolicyCounter = zimbraHsmPolicyCounter + 1 ;

                    String hsmTypesString = getHsmTypesStringFromHsmPolicy(nZimbraHsmPolicy);
                    String hsmSearchQueryString = getHsmSearchQueryStringFromHsmPolicy(nZimbraHsmPolicy);
                    // No need to check if the values are null because of prior isValidHsmPolicySyntaxList check

                    ZetaHsmLog.debug("hsmTypesString - (" + zimbraHsmPolicyCounter + "/" + zimbraHsmPolicyList.length + ")" + " : '" + hsmTypesString + "'");
                    ZetaHsmLog.debug("hsmSearchQueryString - (" + zimbraHsmPolicyCounter + "/" + zimbraHsmPolicyList.length + ")" + " : '" + hsmSearchQueryString + "'");

                    if (aborting) {
                        break;
                    }
                    blobMover = new BlobMover();
                    blobMover.moveItems(prov, hsmTypesString, hsmSearchQueryString, destinationLocator);
                }
                ZetaHsmLog.debug("ZetaHsm RUN function - End");
            }
            catch (ServiceException e) {
                error = "Unable to get 'zimbraHsmPolicy' attribute. Aborting.";
                ZetaHsmLog.info(error, e);
                return;
            } finally {
                endHsm();
            }
        }
    }
}
