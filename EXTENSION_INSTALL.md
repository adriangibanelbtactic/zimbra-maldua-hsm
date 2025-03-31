# Install Maldua Zimbra HSM Extension

## Warning

**These are developer instructions.**

For Admin installation instructions please check [README.md](README.md) instead.

## Requisites

You have succesfully built zimbra-maldua-hsm using [EXTENSION_BUILD.md](EXTENSION_BUILD.md) instructions.

## Installation

Get `/opt/zimbra/conf/scripts/zimbra-maldua-hsm/extension/zetahsm.jar` from your build machine and copy it to your production machine on `/tmp/zetahsm.jar` .

This needs to be run as the root user:

```
mkdir /opt/zimbra/lib/ext/zetahsm
chmod 775 /opt/zimbra/lib/ext/zetahsm
cp /tmp/zetahsm.jar /opt/zimbra/lib/ext/zetahsm/zetahsm.jar
chmod 444 /opt/zimbra/lib/ext/zetahsm/zetahsm.jar
```

## Zimbra Mailbox restart

For the new Extension to be used the Zimbra Mailbox has to be restarted.

```
sudo su - zimbra -c 'zmmailboxdctl restart'
```
