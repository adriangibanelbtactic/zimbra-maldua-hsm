/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * ***** END LICENSE BLOCK *****
 */

if(ZaSettings && ZaSettings.EnabledZimlet["com_btactic_hsm_admin"]){

    function com_btactic_hsm_ext () {

    }

    if (window.console && console.log) {
        console.log("Start loading com_btactic_hsm_admin.js");
    }

    // Using getResource from a ZmZimletBase object does not seem to work in admin
    com_btactic_hsm_admin.zimletImagesPath = "/service/zimlet/com_btactic_hsm_admin/images"


    com_btactic_hsm_admin.malduaHeader =
      '<a target="_blank" href="https://github.com/maldua-suite/maldua-suite">' +
      '<img align="right" alt="Maldua Suite for Zimbra Collaboration Server" src="' +
      com_btactic_hsm_admin.zimletImagesPath + "/" + "maldua_logo.png" +
      '">' +
      '</a>'

    com_btactic_hsm_admin.zetaPromoWithImage =
      '<img src="' +
      com_btactic_hsm_admin.zimletImagesPath + "/" + "btactic_logo.png" +
      '">' +
      " " +
      com_btactic_hsm_admin.zetaPromo +
      com_btactic_hsm_admin.malduaHeader;

    com_btactic_hsm_admin.zetaPromoCss = "font-size:16pt; font-weight: bold;";

    com_btactic_hsm_ext.hsmRunning = false;
    com_btactic_hsm_ext.hsmAborting = false;

    com_btactic_hsm_ext.enableStartHsmSessionButton = function() {
        return !com_btactic_hsm_ext.hsmRunning;
    };

    com_btactic_hsm_ext.enableAbortHsmSessionButton = function() {
        return (com_btactic_hsm_ext.hsmRunning && (!(com_btactic_hsm_ext.hsmAborting)));
    };

    com_btactic_hsm_ext.updateHsmControlButtons = function(group) {
        // Start button: enabled only when not running
        var enableStart = !com_btactic_hsm_ext.hsmRunning;
        com_btactic_hsm_ext.setButtonEnabledInGroup(group, "startHsmButton", enableStart);

        // Abort button: enabled only when running AND not aborting
        var enableAbort = (com_btactic_hsm_ext.hsmRunning && !com_btactic_hsm_ext.hsmAborting);
        com_btactic_hsm_ext.setButtonEnabledInGroup(group, "abortHsmButton", enableAbort);
    };

    com_btactic_hsm_ext.refreshRunning = false;

    com_btactic_hsm_ext.enableStartHsmRefreshButton = function() {
        // Start should be enabled only if not running
        return !com_btactic_hsm_ext.refreshRunning;
    };

    com_btactic_hsm_ext.enableStopHsmRefreshButton = function() {
        // Stop should be enabled only if running
        return com_btactic_hsm_ext.refreshRunning;
    };

    // Start watchdog-based refresh loop
    com_btactic_hsm_ext.startRefreshLoop = function(group) {
        // Reset state
        com_btactic_hsm_ext.refreshRunning = true;
        com_btactic_hsm_ext._lastRefreshTime = Date.now();

        // Stop previous loop if any
        if (com_btactic_hsm_ext._refreshTimer) {
            clearInterval(com_btactic_hsm_ext._refreshTimer);
        }

        // Create new loop
        com_btactic_hsm_ext._refreshTimer = setInterval(function() {
            var now = Date.now();

            // Timeout watchdog: stop if no successful update within 5s
            if (now - com_btactic_hsm_ext._lastRefreshTime > 5000) {
                clearInterval(com_btactic_hsm_ext._refreshTimer);
                com_btactic_hsm_ext._refreshTimer = null;
                com_btactic_hsm_ext.refreshRunning = false;

                com_btactic_hsm_ext.setAlertContentInGroup(
                    group,
                    "statusInfo",
                    "Cannot connect to server. HSM refresh stopped."
                );
                return;
            }

            // Try to refresh
            com_btactic_hsm_ext.refreshStatus(group);
        }, 1000);
    };

    // Show additional HSM attributes for GlobalConfig
    if (ZaGlobalConfig && ZaGlobalConfig.myXModel && ZaGlobalConfig.myXModel.items) {
        ZaGlobalConfig.myXModel.items.push({id: "zimbraHsmPolicy", ref:"attrs/" + "zimbraHsmPolicy", type:_LIST_, listItem:{ type:_STRING_, maxLength: 10240}});
    }

    if(ZaTabView.XFormModifiers["GlobalConfigXFormView"]) {
        com_btactic_hsm_ext.GlobalConfigXFormModifier= function (xFormObject,entry) {
            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            var tabBar = xFormObject.items[1] ;
            var hsmTabIx = ++this.TAB_INDEX;
            tabBar.choices.push({value:hsmTabIx, label:com_btactic_hsm_admin.zetaHSMTab});

            var hsmGlobalConfigTab={
                type : _ZATABCASE_,
                caseKey : hsmTabIx,
                paddingStyle : "padding-left:15px;",
                width : "98%",
                cellpadding : 2,
                colSizes : [ "auto" ],
                numCols : 1,
                id : "global_zeta_hsm",
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_hsm_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_hsm_admin.zetaPromoCss},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZA_TOP_GROUPER_,
                        label:com_btactic_hsm_admin.zetaHSMTab,
                        items:[
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationSyntax, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationTypes, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationQueries, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationExamples, colSpan : "*"},
                            {
                            ref : "zimbraHsmPolicy",
                            type : _REPEAT_,
                            label : com_btactic_hsm_admin.HSMPolicy,
                            labelLocation : _LEFT_,
                            align : _LEFT_,
                            repeatInstance : "",
                            showAddButton : true,
                            showRemoveButton : true,
                            showAddOnNextRow : true,
                            addButtonLabel : com_btactic_hsm_admin.Add_zimbraHsmPolicy,
                            removeButtonLabel : com_btactic_hsm_admin.Remove_zimbraHsmPolicy,
                            removeButtonCSSStyle : "margin-left: 50px",
                            visibilityChecks : [ ZaItem.hasReadPermission ],
                            items: [
                                {
                                    ref: ".",
                                    type: _TEXTFIELD_,
                                    label: null,
                                    labelLocation: _NONE_,
                                    toolTipContent : com_btactic_hsm_admin.tt_zimbraHsmPolicy,
                                    width: "60em",
                                    visibilityChecks: [ ZaItem.hasReadPermission ]
                                },
                                {
                                    type: _DWT_BUTTON_,
                                    label: com_btactic_hsm_admin.EditButtonLabel,
                                    width: "10em",
                                    onActivate: function () {
                                      let form = this.getForm();
                                      let parentItem = this.getParentItem(); // gets the XFormItem
                                      let currentHSMValue = parentItem.getInstanceValue();
                                      com_btactic_hsm_ext.launchEditWizard(currentHSMValue, parentItem, this);
                                    }
                                }
                            ]
                            }

                        ]
                    }
                ]
            };

            xFormObject.items[i].items.push(hsmGlobalConfigTab);
        }
        ZaTabView.XFormModifiers["GlobalConfigXFormView"].push(com_btactic_hsm_ext.GlobalConfigXFormModifier);
    }

    // Deal with zimbraHsmPolicy having multiple values
    if (ZaGlobalConfig && ZaGlobalConfig.myXModel) {
        ZaGlobalConfig.loadHsmMethod =
        function () {
            if(AjxUtil.isString(this.attrs["zimbraHsmPolicy"])) {
                this.attrs["zimbraHsmPolicy"] = [this.attrs["zimbraHsmPolicy"]];
            }
        }
        ZaItem.loadMethods["ZaGlobalConfig"].push(ZaGlobalConfig.loadHsmMethod);
    }

    // Show additional HSM attributes for Server
    if (ZaServer && ZaServer.myXModel && ZaServer.myXModel.items) {
        ZaServer.myXModel.items.push({id: "zimbraHsmPolicy", ref:"attrs/" + "zimbraHsmPolicy", type:_LIST_, listItem:{ type:_STRING_, maxLength: 10240}});
    }

    if(ZaTabView.XFormModifiers["ZaServerXFormView"]) {
        com_btactic_hsm_ext.ServerXFormModifier= function (xFormObject,entry) {
            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            var tabBar = xFormObject.items[1] ;
            var hsmTabIx = ++this.TAB_INDEX;
            tabBar.choices.push({value:hsmTabIx, label:com_btactic_hsm_admin.zetaHSMTab});

            var hsmServerTab={
                type : _ZATABCASE_,
                caseKey : hsmTabIx,
                paddingStyle : "padding-left:15px;",
                width : "98%",
                cellpadding : 2,
                colSizes : [ "auto" ],
                id : "server_zeta_hsm",
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_hsm_admin.zetaPromoWithImage, colSpan:"*", cssStyle:com_btactic_hsm_admin.zetaPromoCss},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZA_TOP_GROUPER_,
                        colSpan:"*",
                        label:com_btactic_hsm_admin.zetaHSMTab,
                        items:[
                            {
                                type: _GROUP_,
                                width: "100%",
                                colSpan:"*",
                                colSizes: ["20%","20%","20%","20%","20%"],
                                items: [
                                    {
                                        type: _SPACER_,
                                        height: 10
                                    },
                                    // HSM Status Block
                                    {
                                        colSpan: 10,
                                        colSizes: ["10%","10%","10%","10%","10%","10%","10%","10%","10%","10%"],
                                        type: _ZAALLSCREEN_GROUPER_,
                                        width: "100%",
                                        label: "HSM Status",
                                        items: [
                                            {
                                                colSpan: 1,
                                                cssClass: "HsmStatusButton",
                                                type: _DWT_BUTTON_,
                                                label: "Monitor ON",
                                                hsmRole: "startRefreshButton",
                                                onActivate: function() {
                                                    var group = this.getParentItem();

                                                    com_btactic_hsm_ext.setAlertContentInGroup(group, "statusInfo", "Fetching HSM status...");
                                                    com_btactic_hsm_ext.refreshStatus(group);
                                                    com_btactic_hsm_ext.startRefreshLoop(group);

                                                    com_btactic_hsm_ext.refreshRunning = true;

                                                    this.getForm().refresh();
                                                },
                                                enableDisableChecks: [com_btactic_hsm_ext.enableStartHsmRefreshButton]
                                            },
                                            {
                                                colSpan: 1,
                                                cssClass: "HsmStatusButton",
                                                type: _DWT_BUTTON_,
                                                label: "Monitor OFF",
                                                hsmRole: "stopRefreshButton",
                                                onActivate: function() {
                                                    var group = this.getParentItem();

                                                    clearInterval(com_btactic_hsm_ext._refreshTimer);
                                                    com_btactic_hsm_ext._refreshTimer = null;
                                                    com_btactic_hsm_ext.refreshRunning = false;

                                                    this.getForm().refresh();
                                                },
                                                enableDisableChecks: [com_btactic_hsm_ext.enableStopHsmRefreshButton]
                                            },
                                            {
                                                type: _SPACER_,
                                                height: 10
                                            },
                                            {
                                                colSpan: 10,
                                                type: _DWT_ALERT_,
                                                hsmRole: "statusInfo",
                                                id: "HsmStatusInfo",
                                                containerCssStyle: "padding-bottom:0px",
                                                style: DwtAlert.INFO,
                                                iconVisible: true,
                                                content: "Click on: '" + "Monitor ON" + "' to see the HSM Status live."
                                            }
                                        ]
                                    },
                                    {
                                        type: _SPACER_,
                                        height: 10
                                    },
                                    // HSM Schedule
                                    {
                                        colSpan: 1,
                                        type: _ZALEFT_GROUPER_,
                                        width: "100%",
                                        label: "HSM Schedule",
                                        items: [
                                            {
                                                type: _SPACER_,
                                                height: 10
                                            }
                                        ]
                                    },
                                    // HSM Controls
                                    {
                                        colSpan: 1,
                                        type: _ZALEFT_GROUPER_,
                                        width: "100%",
                                        label: "HSM Controls",
                                        items: [
                                            {
                                                type: _DWT_BUTTON_,
                                                label: "Start HSM Session",
                                                hsmRole: "startHsmButton",
                                                onActivate: function() {
                                                    com_btactic_hsm_ext.startHsmSession();
                                                    console.log("DEBUG-form - BEGIN");
                                                    console.log(this.getForm());
                                                    console.log("DEBUG-form - END");
                                                    this.getForm().refresh();
                                                },
                                                enableDisableChecks: [com_btactic_hsm_ext.enableStartHsmSessionButton]
                                            },
                                            {
                                                type: _DWT_BUTTON_,
                                                label: "Abort HSM Session",
                                                hsmRole: "abortHsmButton",
                                                onActivate: function() {
                                                    com_btactic_hsm_ext.abortHsmSession();
                                                    this.getForm().refresh();
                                                },
                                                enableDisableChecks: [com_btactic_hsm_ext.enableAbortHsmSessionButton]
                                            }
                                        ]
                                    }
                                ]
                            },
                            {
                                type: _SPACER_,
                                colSpan: "*"
                            },
                            {
                            ref : "zimbraHsmPolicy",
                            type : _REPEAT_,
                            label : com_btactic_hsm_admin.HSMPolicy,
                            labelLocation : _LEFT_,
                            align : _LEFT_,
                            repeatInstance : "",
                            showAddButton : true,
                            showRemoveButton : true,
                            showAddOnNextRow : true,
                            addButtonLabel : com_btactic_hsm_admin.Add_zimbraHsmPolicy,
                            removeButtonLabel : com_btactic_hsm_admin.Remove_zimbraHsmPolicy,
                            removeButtonCSSStyle : "margin-left: 50px",
                            visibilityChecks : [ ZaItem.hasReadPermission ],
                            items: [
                                {
                                    ref: ".",
                                    type: _TEXTFIELD_,
                                    label: null,
                                    labelLocation: _NONE_,
                                    toolTipContent : com_btactic_hsm_admin.tt_zimbraHsmPolicy,
                                    width: "60em",
                                    visibilityChecks: [ ZaItem.hasReadPermission ]
                                },
                                {
                                    type: _DWT_BUTTON_,
                                    label: com_btactic_hsm_admin.EditButtonLabel,
                                    width: "10em",
                                    onActivate: function () {
                                      let form = this.getForm();
                                      let parentItem = this.getParentItem(); // gets the XFormItem
                                      let currentHSMValue = parentItem.getInstanceValue();
                                      com_btactic_hsm_ext.launchEditWizard(currentHSMValue, parentItem, this);
                                    }
                                }
                            ]
                            },
                            // Embedded help
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationSyntax, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationTypes, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationQueries, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationExamples, colSpan : "*"}
                        ]
                    }
                ]
            };

            xFormObject.items[i].items.push(hsmServerTab);
        }
        ZaTabView.XFormModifiers["ZaServerXFormView"].push(com_btactic_hsm_ext.ServerXFormModifier);
    }

    // Deal with zimbraHsmPolicy having multiple values
    if (ZaServer && ZaServer.myXModel) {
        ZaServer.loadHsmMethod =
        function () {
            if(AjxUtil.isString(this.attrs["zimbraHsmPolicy"])) {
                this.attrs["zimbraHsmPolicy"] = [this.attrs["zimbraHsmPolicy"]];
            }
        }
        ZaItem.loadMethods["ZaServer"].push(ZaServer.loadHsmMethod);
    }

    com_btactic_hsm_ext._refreshTimer = null;

    // Find a direct child XFormItem in a group by an attribute we set in the schema
    com_btactic_hsm_ext.findChildByAttr = function (group, key, value) {
      if (!group || !group.items) return null;
      for (var i = 0; i < group.items.length; i++) {
        var it = group.items[i];
        if (it && it.__attributes && it.__attributes[key] === value) return it;
      }
      return null;
    };

    // Render string safely into the DwtAlert in that group
    com_btactic_hsm_ext.setAlertContentInGroup = function (group, role, html) {
      var item = com_btactic_hsm_ext.findChildByAttr(group, "hsmRole", role);
      if (item) {
        var ctrl = item.widget;
        if (ctrl) ctrl.setContent(html || "");
      }
    };

    // Update the label of a button in a given XForm group by its hsmRole
    com_btactic_hsm_ext.setHsmButtonLabel = function(group, role, label) {
        var item = com_btactic_hsm_ext.findChildByAttr(group, "hsmRole", role);
        if (item && item.widget && typeof item.widget.setText === "function") {
            item.widget.setText(label);
        }
    };

    // Enable/disable a button in a group by hsmRole
    com_btactic_hsm_ext.setButtonEnabledInGroup = function(group, role, enabled) {
        var item = com_btactic_hsm_ext.findChildByAttr(group, "hsmRole", role);
        if (item && item.widget && typeof item.widget.setEnabled === "function") {
            item.widget.setEnabled(enabled);
        }
    };

    com_btactic_hsm_ext.startHsmSession = function() {
        var controller = ZaApp.getInstance().getCurrentController();
        try {
            var soapDoc = AjxSoapDoc.create("HsmRequest", ZaZimbraAdmin.URN, null);
            var params = { soapDoc: soapDoc };
            var reqMgrParams = { controller: controller, busyMsg: "Starting HSM Session..." };
            var resp = ZaRequestMgr.invoke(params, reqMgrParams).Body.HsmResponse;

            // Mark running, reset aborting
            com_btactic_hsm_ext.hsmRunning = true;
            com_btactic_hsm_ext.hsmAborting = false;

        } catch (e) {
            controller._handleException(e);
        }
    };

    com_btactic_hsm_ext.abortHsmSession = function() {
        if (com_btactic_hsm_ext.hsmAborting) {
            return; // already aborting
        }

        var controller = ZaApp.getInstance().getCurrentController();
        try {
            com_btactic_hsm_ext.hsmAborting = true;
            var soapDoc = AjxSoapDoc.create("AbortHsmRequest", ZaZimbraAdmin.URN, null);
            var params = { soapDoc: soapDoc };
            var reqMgrParams = { controller: controller, busyMsg: "Aborting HSM Session..." };
            var resp = ZaRequestMgr.invoke(params, reqMgrParams).Body.AbortHsmResponse;

            if (resp && (resp.aborted === true || resp.aborted === "1" || resp.aborted === 1)) {
                com_btactic_hsm_ext.hsmAborting = true;
                com_btactic_hsm_ext.hsmRunning = false;
            }

        } catch (e) {
            com_btactic_hsm_ext.hsmAborting = false;
            controller._handleException(e);
        }
    };

    com_btactic_hsm_ext.refreshStatus = function (group) {
      var controller = ZaApp.getInstance().getCurrentController();
      try {
        var soapDoc = AjxSoapDoc.create("GetHsmStatusRequest", ZaZimbraAdmin.URN, null);
        var params = { soapDoc: soapDoc };
        var reqMgrParams = { controller: controller, busyMsg: "Fetching HSM Status..." };
        var resp = ZaRequestMgr.invoke(params, reqMgrParams).Body.GetHsmStatusResponse;

        var content = "No HSM session was run after restart.";
        if (resp) {
          var running = (resp.running === true) || (resp.running === "1") || (resp.running === 1);
          if (running) {
            var numBlobs = resp.numBlobsMoved || 0;
            var numBytes = resp.numBytesMoved || 0;
            var numMbx   = resp.numMailboxes   || 0;
            var totalMbx = resp.totalMailboxes || 0;
            content = numBlobs + " BLOBS moved. " +
                      numBytes + " BYTES moved. " +
                      numMbx + " of " + totalMbx + " mailboxes moved.";
          } else if (resp.startDate > 0 && resp.endDate > 0) {
            var start = new Date(parseInt(resp.startDate, 10));
            var end   = new Date(parseInt(resp.endDate, 10));
            content = "Latest SM was run from " + start + " to " + end + ".";
          }
          // else: leave empty if missing/invalid dates
        }
        com_btactic_hsm_ext.setAlertContentInGroup(group, "statusInfo", content);
        com_btactic_hsm_ext._lastRefreshTime = Date.now();
      } catch (e) {
        controller._handleException(e);
      }
    };

    // Helper to launch the HSM Policy Edit Wizard
    com_btactic_hsm_ext.CustomZaXFormDialog = function (params) {
        shell = params.parent;
        title = params.title;
        if (!shell) {
            throw new Error("Shell must be provided to CustomZaXFormDialog");
        }

        if (!(shell instanceof DwtShell)) {
          throw new Error("shell must be a DwtShell (CustomZaXFormDialog)");
        }

        this._standardButtons = params.standardButtons;

        // Call the parent constructor (ZaXDialog)
        ZaXDialog.call(this, shell, null, title, params.w, params.h, params.iKeyName, params.contextId);

        // Store the shell reference
        this.shell = shell;
    };

    // Inherit from ZaXDialog
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype = new ZaXDialog();
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype.constructor = com_btactic_hsm_ext.CustomZaXFormDialog;

    // Override setHSMEditObject to set the object to be edited
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype.setHSMEditObject = function (obj) {
        this._HSMEditObject = obj;  // Store the object
    };

    com_btactic_hsm_ext.CustomZaXFormDialog.prototype.setContent = function (content) {
        this._xformDef = {
            type: _GROUP_,
            numCols: 1,
            items: [
                { type: _CHECKBOX_, ref: "message", label: com_btactic_hsm_admin.EmailsType },
                { type: _CHECKBOX_, ref: "document", label: com_btactic_hsm_admin.DocumentsType },
                { type: _CHECKBOX_, ref: "task", label: com_btactic_hsm_admin.TasksType },
                { type: _CHECKBOX_, ref: "appointment", label: com_btactic_hsm_admin.AppointmentsType },
                { type: _CHECKBOX_, ref: "contact", label: com_btactic_hsm_admin.ContactsType },
                { type: _TEXTFIELD_, ref: "query", label: com_btactic_hsm_admin.Query, width: "50em" }
            ]
        };
    };

    // Method to create the form manually
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype._createForm = function () {

        // Define the XModel metadata (structure)
        const xModel = [
            { id: "message", type: _CHECKBOX_ },
            { id: "document", type: _CHECKBOX_ },
            { id: "task", type: _CHECKBOX_ },
            { id: "appointment", type: _CHECKBOX_ },
            { id: "contact", type: _CHECKBOX_ },
            { id: "query", type: _STRING_ }
        ];

        // Actually create the form using ZaXDialog's supported method
        this.initForm(xModel, this._xformDef, this._HSMEditObject);
    };

    // OK button callback function
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype._okCallback = function () {
        let oldHSMValue = this._HsmPolicyEditContainer.getInstanceValue();
        var selectedTypes = [];
        // Collect selected types based on the object values
        for (let key of ["message", "document", "task", "appointment", "contact"]) {
            if (this._HSMEditObject[key]) selectedTypes.push(key);
        }

        // Check if any types are selected and if the query is not empty
        if (selectedTypes.length === 0 || !this._HSMEditObject.query.trim()) {
            alert(com_btactic_hsm_admin.MinimumHSMValues);
            return;
        }

        // Format the newHSMValue and set the value to the form item
        let newHSMValue = selectedTypes.join(",") + ":" + this._HSMEditObject.query.trim();

        if (newHSMValue !== oldHSMValue) {
            this._HsmPolicyEditContainer.setInstanceValue(newHSMValue); // Set the newHSMValue to the form item
            // simulate the onchange logic: call elementChangedMethod
            const hsmPolicyEditTextFieldChangedMethod = this._HsmPolicyEditTextField.getElementChangedMethod();
            hsmPolicyEditTextFieldChangedMethod.call(this._HsmPolicyEditTextField, newHSMValue, oldHSMValue, event||window.event);
        }

        this.popdown();  // Close the dialog
    };

    // Cancel button callback function
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype._cancelCallback = function () {
        this.popdown();  // Close the dialog
    };

    // Method to launch the edit wizard dialog
    com_btactic_hsm_ext.launchEditWizard = function (currentHSMValue, HsmPolicyEditContainer, HsmPolicyEditTextField) {
        if (typeof currentHSMValue === 'undefined') {
            currentHSMValue = "";
        }

        let colonIndex = currentHSMValue.indexOf(":");

        let typesPart = colonIndex !== -1 ? currentHSMValue.slice(0, colonIndex) : currentHSMValue;
        let queryPart = colonIndex !== -1 ? currentHSMValue.slice(colonIndex + 1) : "";

        let selectedTypes = typesPart ? typesPart.split(",") : [];
        let query = queryPart;

        // Retrieve the existing DwtShell instance
        let shell = DwtShell.getShell(window);  // window refers to the current browser window

        if (!(shell instanceof DwtShell)) {
          throw new Error("shell must be a DwtShell (launchEditWizard)");
        }

        // Create the dialog instance and pass the shell
        let dlg = new com_btactic_hsm_ext.CustomZaXFormDialog({
            parent: shell,      // Pass the DwtShell as the parent
            className: "DwtDialog",   // You can customize the class name if needed
            title: com_btactic_hsm_admin.EditHSMPolicy,  // Set the title of the dialog
            w: "500px",              // Width (can be adjusted)
            h: "350px",              // Height (can be adjusted)
            iKeyName: "HSM_POLICY_EDIT",   // Internal key name
            contextId: Dwt.getNextId(ZaId.DLG_UNDEF),   // Context ID
            standardButtons: [DwtDialog.OK_BUTTON, DwtDialog.CANCEL_BUTTON]  // Specify standard buttons
        });
        // Set the object to be edited
        dlg.setHSMEditObject({
            message: selectedTypes.includes("message"),
            document: selectedTypes.includes("document"),
            task: selectedTypes.includes("task"),
            appointment: selectedTypes.includes("appointment"),
            contact: selectedTypes.includes("contact"),
            query: query
        });
        // Define the form content structure
        dlg.setContent({
            type: _GROUP_,
            numCols: 1,
            items: [
                { type: _CHECKBOX_, ref: "message", label: com_btactic_hsm_admin.EmailsType },
                { type: _CHECKBOX_, ref: "document", label: com_btactic_hsm_admin.DocumentsType },
                { type: _CHECKBOX_, ref: "task", label: com_btactic_hsm_admin.TasksType },
                { type: _CHECKBOX_, ref: "appointment", label: com_btactic_hsm_admin.AppointmentsType },
                { type: _CHECKBOX_, ref: "contact", label: com_btactic_hsm_admin.ContactsType },
                { type: _TEXTFIELD_, ref: "query", label: com_btactic_hsm_admin.Query, width: "50em" }
            ]
        });
        dlg._createForm();
        // Register the callback functions for OK and Cancel
        dlg.registerCallback(DwtDialog.OK_BUTTON, dlg._okCallback.bind(dlg));
        dlg.registerCallback(DwtDialog.CANCEL_BUTTON, dlg._cancelCallback.bind(dlg));

        // Open the dialog
        dlg._HsmPolicyEditContainer = HsmPolicyEditContainer;
        dlg._HsmPolicyEditTextField = HsmPolicyEditTextField;
        dlg.popup();
    };


}
