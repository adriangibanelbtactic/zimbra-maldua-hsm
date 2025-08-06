package com.btactic.hsm.storage;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.volume.Volume;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;
import com.zimbra.soap.admin.type.VolumeInfo;

public class VolumeUtil {

    public static List<Short> getValidLocators(SoapProvisioning prov) throws ServiceException {
        List<Short> validLocators = new ArrayList<>();

        GetAllVolumesRequest request = new GetAllVolumesRequest();
        Element requestElement = JaxbUtil.jaxbToElement(request);
        Element respElem = prov.invoke(requestElement);
        GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);

        for (VolumeInfo volumeInfo : response.getVolumes()) {
            if (volumeInfo.getType() == Volume.TYPE_INDEX) {
                continue;
            }

            if ((Volume.StoreType.getStoreTypeBy(volumeInfo.getStoreType()).equals(Volume.StoreType.INTERNAL)) &&
                (volumeInfo.getStoreManagerClass().equals("com.zimbra.cs.store.file.FileBlobStore"))) {
                validLocators.add(volumeInfo.getId());
            }
        }

        return validLocators;
    }

    public static List<Short> getValidOriginLocators(SoapProvisioning prov, int destinationLocator) throws ServiceException {
        List<Short> validOriginLocators = new ArrayList<>();

        GetAllVolumesRequest request = new GetAllVolumesRequest();
        Element requestElement = JaxbUtil.jaxbToElement(request);
        Element respElem = prov.invoke(requestElement);
        GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);

        for (VolumeInfo volumeInfo : response.getVolumes()) {
            if (volumeInfo.getId() == destinationLocator) {
                continue;
            }

            if (volumeInfo.getType() == Volume.TYPE_INDEX) {
                continue;
            }

            if ((Volume.StoreType.getStoreTypeBy(volumeInfo.getStoreType()).equals(Volume.StoreType.INTERNAL)) &&
                (volumeInfo.getStoreManagerClass().equals("com.zimbra.cs.store.file.FileBlobStore"))) {
                validOriginLocators.add(volumeInfo.getId());
            }
        }

        return validOriginLocators;
    }

    public static boolean isValidLocator(SoapProvisioning prov, Short locatorId) throws ServiceException {
        return getValidLocators(prov).contains(locatorId);
    }

    public static boolean isValidOriginLocator(SoapProvisioning prov, int destinationLocator, Short originLocatorId) throws ServiceException {
        return getValidOriginLocators(prov, destinationLocator).contains(originLocatorId);
    }
}
