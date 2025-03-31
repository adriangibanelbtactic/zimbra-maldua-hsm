# Install Maldua Zimbra HSM Admin Zimlet

## Warning

**These are developer instructions.**

For Admin installation instructions please check [README.md](README.md) instead.

## Requisites

You have succesfully built zimbra-maldua-hsm admin zimlet using [ADMINZIMLET_BUILD.md](ADMINZIMLET_BUILD.md) instructions.

## Installation

**Note**: This procedure has to be done on all of your mailboxes.

Get `/tmp/zimbra-maldua-hsm/adminZimlet/com_btactic_hsm_admin.zip` from your build machine and copy it to your production machine on `/tmp/com_btactic_hsm_admin.zip` .

This needs to be run as the root user:

```
chown zimbra:zimbra /tmp/com_btactic_hsm_admin.zip
```

And then:
```
sudo su - zimbra
zmzimletctl deploy /tmp/com_btactic_hsm_admin.zip
```
.

## Zimbra Mailbox restart

For the new admin Zimlet to be used the Zimbra Mailbox needs to be restarted.

```
sudo su - zimbra -c 'zmmailboxdctl restart'
```

## Network Edition notes

This is not supposed to work in a Zimbra NE installation.
If you insist on using this admin zimlet in a Zimbra NE installation please undeploy the original admin zimlet so that they do not collide.
You should also run: `zmzimletctl undeploy com_zimbra_hsm` so that the admin zimlet is actually uninstalled.
