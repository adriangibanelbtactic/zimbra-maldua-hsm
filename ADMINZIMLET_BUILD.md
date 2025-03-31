# Build Maldua Zimbra HSM Admin Zimlet

## Introduction

This is quite straight-forward to do.

## Requisites

- zip
- git

```
apt update
apt install zip git
```

## Prepare build environment

```
cd /tmp
git clone 'https://github.com/btactic/zimbra-maldua-hsm.git'
```

## Build

```
cd /tmp/zimbra-maldua-hsm/adminZimlet/com_btactic_hsm_admin/
zip --quiet -r ../com_btactic_hsm_admin.zip *
```

## Zip

A new zip file should be found at:
```
/tmp/zimbra-maldua-hsm/adminZimlet/com_btactic_hsm_admin.zip
```
.
