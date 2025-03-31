# Maldua's Zimbra HSM

## About

**MALDUA'S Zimbra HSM Extension & Administration Zimlet** brought to you by [BTACTIC, open source & cloud solutions](https://www.btactic.com).

Hierarchical Storage Management technique is a data storage technique that moves data between different stores according to a defined policy.
Usually it is used to move older data to slower-but-cheapear storages.

**WARNING: The development stage is in ALPHA QUALITY and it is not ready for production deployment.**

## Developer documentation

This documentation is aimed at developers, not at admins.

### How to build the extension

- Check: [EXTENSION_BUILD.md](EXTENSION_BUILD.md) on how to build the Extension.

### How to install the extension

- Check: [EXTENSION_INSTALL.md](EXTENSION_INSTALL.md) on how to install the Extension.

### How to build the admin zimlet

- Check: [ADMINZIMLET_BUILD.md](ADMINZIMLET_BUILD.md) on how to build the Administration Console Zimlet.

### How to install the admin zimlet

- Check: [ADMINZIMLET_INSTALL.md](ADMINZIMLET_INSTALL.md) on how to install the Administration Console Zimlet.

### How to install the zetahsm binary

- Just copy `bin/zetahsm` it to: `/opt/zimbra/bin/zetahsm` with `root:root` ownership. Make sure it has `755` permissions.

## Licenses

### License (Extension)

```
Maldua Zimbra HSM Extension
Copyright (C) 2023 BTACTIC, S.C.C.L.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.
```

### License (Administration zimlet)

```
Maldua Zimbra HSM Administration zimlet
Copyright (C) 2023 BTACTIC, S.C.C.L.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.
```
