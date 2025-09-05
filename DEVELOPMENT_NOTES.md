# Maldua's Zimbra HSM - Development notes

## Introduction

These notes are aimed at developers that want to either create new Zimbra admin zimlets or extensions.

## Your custom SoapRequest and SoapResponse

If you cannot reuse default SoapRequest or SoapResponse classes that come bundled by default by Zimbra you can use your own classes.

It's a bit tricky how to do that so here there is the code.

- Create your own [Request under com.zimbra.soap.admin.message package](https://github.com/adriangibanelbtactic/zimbra-maldua-hsm/blob/v0.0.3/extension/src/com/zimbra/soap/admin/message/ZetaHsmRequest.java)
- Create your own [Response under com.zimbra.soap.admin.message package](https://github.com/adriangibanelbtactic/zimbra-maldua-hsm/blob/v0.0.3/extension/src/com/zimbra/soap/admin/message/ZetaHsmResponse.java)
- Return jaxbToElement properly in your [service/soap main class](https://github.com/adriangibanelbtactic/zimbra-maldua-hsm/blob/v0.0.3/extension/src/com/btactic/hsm/soap/ZetaHsm.java#L86-L93).

Let's copy and paste this last part here:
```java
        // Setting:
        //  removePrefixes to true
        //  useContextMarshaller to false
        // and passing a class inside the com.zimbra.soap.admin.message package
        // (classes that you can make yourself in the Extension)
        // let's you use this JaxbUtil.jaxbToElement method to reply Soap queries from the
        // zimbraAdmin endpoint quite nicely.
        return JaxbUtil.jaxbToElement(resp, XMLElement.mFactory, true, false);
```
.

Optionally you are encouraged to be using your [own Constant class](https://github.com/adriangibanelbtactic/zimbra-maldua-hsm/blob/v0.0.3/extension/src/com/btactic/hsm/soap/ZetaHsmAdminConstants.java) so that everything it's nicer.

I think you could move the Request and Response to your own package if you imported whatever it's needed there. If I am using com.zimbra.soap.admin.message maybe it's a clue that it cannot be done elsewhere.

Finally here there is the [complete snapshot of the extension](https://github.com/adriangibanelbtactic/zimbra-maldua-hsm/tree/v0.0.3/extension/) that was using this trick.

