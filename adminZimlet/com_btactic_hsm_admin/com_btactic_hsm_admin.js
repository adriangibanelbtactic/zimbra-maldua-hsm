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

    com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER="com_btactic_hsm_ext" + "_id"

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
    com_btactic_hsm_ext.hsmAborted   = false;

    com_btactic_hsm_ext.enableStartHsmSessionButton = function() {
        return ((!com_btactic_hsm_ext.hsmRunning) && (!com_btactic_hsm_ext.hsmAborting));
    };

    com_btactic_hsm_ext.enableAbortHsmSessionButton = function() {
        return (com_btactic_hsm_ext.hsmRunning && (!(com_btactic_hsm_ext.hsmAborting)));
    };

    com_btactic_hsm_ext.refreshRunning = false;
    com_btactic_hsm_ext._loopId = com_btactic_hsm_ext._loopId || 0;
    com_btactic_hsm_ext._abortMonitoring = false;

    com_btactic_hsm_ext.enableStartHsmRefreshButton = function() {
        // Start should be enabled only if not running
        return !com_btactic_hsm_ext.refreshRunning;
    };

    com_btactic_hsm_ext.enableStopHsmRefreshButton = function() {
        // Stop should be enabled only if running
        return com_btactic_hsm_ext.refreshRunning;
    };

    // Start watchdog-based refresh loop
    com_btactic_hsm_ext.startRefreshLoop = function() {
        // Reset state
        com_btactic_hsm_ext.refreshRunning = true;
        com_btactic_hsm_ext._lastRefreshTime = Date.now();
        com_btactic_hsm_ext._shouldDeactivateMonitor = false;

        // Stop previous loop if any
        if (com_btactic_hsm_ext._refreshTimer) {
            clearInterval(com_btactic_hsm_ext._refreshTimer);
            com_btactic_hsm_ext._refreshTimer = null;
        }

        var myLoopId = ++com_btactic_hsm_ext._loopId;

        // Create new loop
        com_btactic_hsm_ext._refreshTimer = setInterval(function() {

            // Stale loop guard
            if (myLoopId !== com_btactic_hsm_ext._loopId) {
              clearInterval(com_btactic_hsm_ext._refreshTimer);
              return;
            }

            // startRefreshLoop: stopping due to deactivate flag.
            if (com_btactic_hsm_ext._shouldDeactivateMonitor) {
                clearInterval(com_btactic_hsm_ext._refreshTimer);
                com_btactic_hsm_ext._refreshTimer = null;
                com_btactic_hsm_ext.refreshRunning = false;
                com_btactic_hsm_ext.updateMonitoringStatus();
                // Force UI refresh
                ZaApp.getInstance().getCurrentController()._view._localXForm.refresh();
                return;
            }

            var now = Date.now();

            // Timeout watchdog: stop if no successful update within 5s
            if (now - com_btactic_hsm_ext._lastRefreshTime > 5000) {
                clearInterval(com_btactic_hsm_ext._refreshTimer);
                com_btactic_hsm_ext._refreshTimer = null;
                com_btactic_hsm_ext.refreshRunning = false;

                com_btactic_hsm_ext.updateStatusInfo("Cannot connect to server. HSM refresh stopped.");
                return;
            }

            // Try to refresh
            com_btactic_hsm_ext.refreshStatus();
        }, 1000);
    };

    com_btactic_hsm_ext.getMonitoringStatusText = function() {
        return com_btactic_hsm_ext.refreshRunning ? "Monitoring: ON" : "Monitoring: OFF";
    };

    com_btactic_hsm_ext.updateMonitoringStatus = function() {
        var widget = com_btactic_hsm_ext.getWidgetById("monitoringStatus");
        if (widget) {
            widget.setContent(com_btactic_hsm_ext.getMonitoringStatusText());
        }
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
                    {
                        type: _SPACER_,
                        height: 10
                    },
                    {type:_ZA_TOP_GROUPER_,
                        colSpan:"*",
                        colSizes: ["100%"],
                        label:com_btactic_hsm_admin.zetaHSMTab,
                        items:[
                            // HSM Status block
                            {
                                type: _SPACER_,
                                height: 10
                            },
                            {
                                type: _ZAALLSCREEN_GROUPER_,
                                width: "100%",
                                colSpan: 1,
                                colSizes: ["10%","10%","10%","10%","10%","10%","10%","10%","10%","10%"], // from original ZAALLSCREEN_GROUPER
                                label: "HSM Status",
                                items: [
                                    {
                                        type: _GROUP_,
                                        colSpan: 4,
                                        colSizes: ["25%", "25%", "25%", "25%"], // distribute space evenly
                                        width: "100%",
                                        items: [
                                            {
                                                colSpan: 1,
                                                type: _DWT_ALERT_,
                                                [com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER]: "monitoringStatus",
                                                style: DwtAlert.WARNING,
                                                iconVisible: false,
                                                content: "Monitoring: OFF"
                                            },
                                            {
                                                type: _ZALEFT_GROUPER_,
                                                colSpan: 2,  // spans across two columns
                                                colSizes: ["30%", "30%"], // split space evenly
                                                width: "100%",
                                                items: [
                                                    {
                                                        colSpan: 1,
                                                        type: _DWT_BUTTON_,
                                                        cssClass: "HsmStatusButton",
                                                        label: "Monitor ON",
                                                        [com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER]: "startRefreshButton",
                                                        onActivate: function() {
                                                            com_btactic_hsm_ext.activateMonitor();
                                                            this.getForm().refresh();
                                                        },
                                                        enableDisableChecks: [com_btactic_hsm_ext.enableStartHsmRefreshButton]
                                                    },
                                                    {
                                                        colSpan: 1,
                                                        type: _DWT_BUTTON_,
                                                        cssClass: "HsmStatusButton",
                                                        label: "Monitor OFF",
                                                        [com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER]: "stopRefreshButton",
                                                        onActivate: function() {
                                                            com_btactic_hsm_ext.deactivateMonitor();
                                                            // handled internally
                                                        },
                                                        enableDisableChecks: [com_btactic_hsm_ext.enableStopHsmRefreshButton]
                                                    },
                                                ]
                                            }
                                        ]
                                    },
                                    {
                                        type: _SPACER_,
                                        height: 10
                                    },
                                    {
                                        colSpan: 10,
                                        type: _DWT_ALERT_,
                                        [com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER]: "statusInfo",
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
                                height: 10,
                                colSpan:"*"
                            },
                            // HSM Schedule + HSM Controls block
                            {
                                type: _GROUP_,
                                width: "100%",
                                colSpan: 1,
                                colSizes: ["20%","20%","20%","20%","20%"],
                                items: [
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
                                    {
                                        colSpan: 1,
                                        type: _ZALEFT_GROUPER_,
                                        width: "100%",
                                        label: "HSM Controls",
                                        items: [
                                            {
                                                type: _DWT_BUTTON_,
                                                label: "Start HSM Session",
                                                [com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER]: "startHsmButton",
                                                onActivate: function() {
                                                    com_btactic_hsm_ext.activateMonitor();
                                                    com_btactic_hsm_ext.startHsmSession();
                                                    this.getForm().refresh();
                                                },
                                                enableDisableChecks: [com_btactic_hsm_ext.enableStartHsmSessionButton]
                                            },
                                            {
                                                type: _DWT_BUTTON_,
                                                label: "Abort HSM Session",
                                                [com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER]: "abortHsmButton",
                                                onActivate: function() {
                                                    com_btactic_hsm_ext.abortHsmSession();
                                                    com_btactic_hsm_ext.activateMonitor();
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
                                height: 10,
                                colSpan:"*"
                            },
                            {
                                type: _ZAALLSCREEN_GROUPER_,
                                colSpan: "*",
                                width: "100%",
                                label: com_btactic_hsm_admin.HSMPolicy,
                                items: [
                                    {
                                        type: _SPACER_,
                                        height: 10,
                                        colSpan:"*"
                                    },
                                    {
                                        colSpan: "*",
                                        ref: "zimbraHsmPolicy",
                                        type: _REPEAT_,
                                        label: null,                      // already using group label above
                                        labelLocation: _NONE_,
                                        align: _LEFT_,
                                        repeatInstance: "",
                                        showAddButton: true,
                                        showRemoveButton: true,
                                        showAddOnNextRow: true,
                                        addButtonLabel: com_btactic_hsm_admin.Add_zimbraHsmPolicy,
                                        removeButtonLabel: com_btactic_hsm_admin.Remove_zimbraHsmPolicy,
                                        removeButtonCSSStyle: "margin-left: 50px",
                                        visibilityChecks: [ ZaItem.hasReadPermission ],
                                        items: [
                                            {
                                                ref: ".",
                                                type: _TEXTFIELD_,
                                                label: null,
                                                labelLocation: _NONE_,
                                                toolTipContent: com_btactic_hsm_admin.tt_zimbraHsmPolicy,
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
                            },
                            // Embedded help
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationSyntax, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationTypes, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationQueries, colSpan : "*"},
                            {type: _DWT_ALERT_, containerCssStyle: "padding-bottom:0px", style: DwtAlert.INFO, iconVisible: true, content : com_btactic_hsm_admin.HSMExplanationExamples, colSpan : "*"},
                            // HSM Special block
                            {
                                type: _SPACER_,
                                height: 10
                            },
                            {
                                type: _ZAALLSCREEN_GROUPER_,
                                width: "100%",
                                colSpan: 1,
                                colSizes: ["10%","10%","10%","10%","10%","10%","10%","10%","10%","10%"], // from original ZAALLSCREEN_GROUPER
                                label: "Special",
                                items: [
                                    {
                                        colSpan: 1,
                                        type: _DWT_BUTTON_,
                                        cssClass: "HsmStatusButton",
                                        label: "RefreshPage !",
                                        [com_btactic_hsm_ext.ADMIN_ZIMLET_IDENTIFIER]: "refreshPageButton",
                                        onActivate: function() {
                                            this.getForm().refresh();
                                        }
                                    }
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

    /**
    * Recursively searches a container (group/item) for a child with a given attribute.
    * Returns the widget if found.
    * @param {object} container  The group or form item to search
    * @param {string} attrName   The attribute name to match (e.g., 'hsmRole')
    * @param {string} attrValue  The attribute value to match
    * @return {object|null}      The widget of the first matching item or null if not found
    */
    com_btactic_hsm_ext.findItemByAttr = function(container, attrName, attrValue) {
        if (!container || !container.items) return null;

        for (var i = 0; i < container.items.length; i++) {
            var item = container.items[i];

            // Prefer __attributes if available
            var value = item.__attributes && item.__attributes[attrName] !== undefined
                        ? item.__attributes[attrName]
                        : item[attrName];

            if (value === attrValue) {
                if (item.widget) {
                    return item.widget;
                } else {
                    return null;
                }
            }

            // Recurse into nested items
            var foundWidget = com_btactic_hsm_ext.findItemByAttr(item, attrName, attrValue);
            if (foundWidget) return foundWidget;
        }

        return null;
    };

    /**
    * Convenience wrapper for finding a widget by ADMIN_ZIMLET_IDENTIFIER in the saved server form.
    * @param {string} attrValue  The ADMIN_ZIMLET_IDENTIFIER value to search for
    * @return {object|null}      The widget or null if not found
    */
    com_btactic_hsm_ext.getWidgetById = function(attrValue) {

        var serverForm = ZaApp.getInstance().getCurrentController()._view._localXForm

        var widget = this.findItemByAttr(serverForm, this.ADMIN_ZIMLET_IDENTIFIER, attrValue);

        return widget;
    };

    // Update the content of a HSM status widget directly
    com_btactic_hsm_ext.updateStatusInfo = function(message) {
        var statusWidget = com_btactic_hsm_ext.getWidgetById("statusInfo");
        if (!statusWidget || typeof statusWidget.setContent !== "function") {
            return;
        }

        // Directly set the value on the XForm widget
        statusWidget.setContent(message);
    };

    /**
    * Activates the monitor for the HSM status.
    */
    com_btactic_hsm_ext.activateMonitor = function() {
        // activateMonitor: Monitor already running, skipping.
        if (com_btactic_hsm_ext.refreshRunning) {
            return;
        }

        // Avoid double loops
        com_btactic_hsm_ext.refreshRunning = true;
        com_btactic_hsm_ext._lastRefreshTime = Date.now();

        // Show initial status
        com_btactic_hsm_ext.updateStatusInfo("Fetching HSM status...");

        // Refresh HSM status once immediately
        com_btactic_hsm_ext.refreshStatus();

        // Start the refresh loop
        com_btactic_hsm_ext.startRefreshLoop();

        com_btactic_hsm_ext.updateMonitoringStatus();

    };

    /**
    * Schedules deactivation the monitor for the HSM status.
    */
    com_btactic_hsm_ext.deactivateMonitor = function() {
        com_btactic_hsm_ext._shouldDeactivateMonitor = true;
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
                com_btactic_hsm_ext.hsmAborting = false;
                // Let refreshStatus detect the Running changes only
                // So that its own comparison logic works
                // com_btactic_hsm_ext.hsmRunning = false;
                // com_btactic_hsm_ext.hsmAborted = true;
            }

            // mark that we expect a stop soon
            com_btactic_hsm_ext._abortMonitoring = true;

        } catch (e) {
            com_btactic_hsm_ext.hsmAborting = false;
            controller._handleException(e);
        }
    };

    com_btactic_hsm_ext.formatBytes = function(bytes) {
        if (bytes === 0) return "0 B (0 bytes)";
        const k = 1024;
        const sizes = ["B", "KB", "MB", "GB", "TB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        const humanValue = (bytes / Math.pow(k, i)).toFixed(3); // 3 decimal places
        return humanValue + " " + sizes[i] + " (" + bytes.toLocaleString() + " bytes)";
    };

    com_btactic_hsm_ext.refreshStatus = function() {
        var controller = ZaApp.getInstance().getCurrentController();

        try {
            var soapDoc = AjxSoapDoc.create("GetHsmStatusRequest", ZaZimbraAdmin.URN, null);
            var params = { soapDoc: soapDoc };
            var reqMgrParams = { controller: controller, busyMsg: "Fetching HSM Status..." };
            var resp = ZaRequestMgr.invoke(params, reqMgrParams).Body.GetHsmStatusResponse;

            var message = null;
            var running = false, aborting = false, wasAborted = false;

            if (resp) {
                running    = (resp.running === true) || (resp.running === "1") || (resp.running === 1);
                aborting   = (resp.aborting === true) || (resp.aborting === "1") || (resp.aborting === 1);
                wasAborted = (resp.wasAborted === true) || (resp.wasAborted === "1") || (resp.wasAborted === 1);

                let parts = [];

                // --- Part 2: Stats first to detect if we have meaningful data ---
                let numBlobs = (resp.numBlobsMoved !== undefined) ? parseInt(resp.numBlobsMoved, 10) : undefined;
                let numBytes = (resp.numBytesMoved !== undefined) ? parseInt(resp.numBytesMoved, 10) : undefined;
                let numMbx   = (resp.numMailboxes !== undefined) ? parseInt(resp.numMailboxes, 10) : undefined;
                let totalMbx = (resp.totalMailboxes !== undefined) ? parseInt(resp.totalMailboxes, 10) : undefined;

                let stats = [];
                if (numBlobs !== undefined && numBlobs !== -1) {
                    stats.push("- " + numBlobs + " blobs moved");
                }
                if (numBytes !== undefined && numBytes !== -1) {
                    stats.push("- " + com_btactic_hsm_ext.formatBytes(numBytes) + " moved");
                }
                if (numMbx !== undefined && totalMbx !== undefined &&
                    numMbx !== -1 && totalMbx !== -1) {
                    stats.push("- " + numMbx + " of " + totalMbx + " mailboxes moved");
                }

                // --- Part 1: Status ---
                if (aborting) {
                    parts.push("Status: Aborting");
                } else if (wasAborted) {
                    parts.push("Status: Aborted");
                } else if (running) {
                    parts.push("Status: Running");
                } else if (stats.length > 0) {
                    // session finished and we have stats → idle
                    parts.push("Status: Idle");
                }

                if (resp.error && resp.error.trim() !== "") {
                    parts.push("Error: " + resp.error);
                }

                // Add stats if any
                if (stats.length > 0) {
                    parts.push(stats.join("<br>"));
                }

                // --- Part 3: Start/End times (only if stats exist) ---
                if (stats.length > 0) {
                    let times = [];
                    if (resp.startDate && parseInt(resp.startDate, 10) > 0) {
                        let start = new Date(parseInt(resp.startDate, 10));
                        times.push("- Start: " + start);
                    } else {
                        times.push("- Start: N/A");
                    }
                    if (resp.endDate && parseInt(resp.endDate, 10) > 0) {
                        let end = new Date(parseInt(resp.endDate, 10));
                        times.push("- End: " + end);
                    } else {
                        times.push("- End: N/A");
                    }
                    parts.push(times.join("<br>"));
                }

                // Assemble message if anything meaningful exists
                if (parts.length > 0) {
                    message = parts.join("<br><br>");
                }
            }

            // default message if nothing meaningful
            if (!message) {
                message = "No HSM session was run after restart.";
            }

            // --- Monitor state handling ---
            var wasRunning = com_btactic_hsm_ext.hsmRunning;
            var stopDetected = false;

            if (wasRunning && !running) stopDetected = true;
            if (com_btactic_hsm_ext._abortMonitoring && !running) {
                stopDetected = true;
                com_btactic_hsm_ext._abortMonitoring = false;
            }

            if (stopDetected) com_btactic_hsm_ext.deactivateMonitor();

            // --- Update globals ---
            var changed = (com_btactic_hsm_ext.hsmRunning !== running) ||
                          (com_btactic_hsm_ext.hsmAborting !== aborting) ||
                          (com_btactic_hsm_ext.hsmAborted !== wasAborted);

            com_btactic_hsm_ext.hsmRunning  = running;
            com_btactic_hsm_ext.hsmAborting = aborting;
            com_btactic_hsm_ext.hsmAborted  = wasAborted;

            // --- Update status widget ---
            com_btactic_hsm_ext.updateStatusInfo(message);
            com_btactic_hsm_ext._lastRefreshTime = Date.now();

            if (changed || stopDetected) {
                var statusWidget = com_btactic_hsm_ext.getWidgetById("statusInfo");
                if (statusWidget && typeof statusWidget.getForm === "function") {
                    statusWidget.getForm().refresh();
                }
            }

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
