/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
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
                    {label: null, type: _OUTPUT_, value: com_btactic_hsm_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
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
                              items : [
                                {
                                  type: _GROUP_,
                                  numCols: 2,
                                  colSizes: ["80%", "20%"],
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
                                        let currentValue = parentItem.getInstanceValue();
                                        com_btactic_hsm_ext.launchEditWizard(currentValue, parentItem, this);
                                      }
                                    }
                                  ]
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
                numCols : 1,
                id : "server_zeta_hsm",
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_hsm_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
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
                              items : [
                                {
                                  type: _GROUP_,
                                  numCols: 2,
                                  colSizes: ["80%", "20%"],
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
                                        let currentValue = parentItem.getInstanceValue();
                                        com_btactic_hsm_ext.launchEditWizard(currentValue, parentItem, this);
                                      }
                                    }
                                  ]
                                }
                              ]
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

    // Define additional UI labels
    com_btactic_hsm_admin.EditButtonLabel = "Edit...";

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

        // Call the parent constructor (ZaXDialog)
        ZaXDialog.call(this, shell, null, title, params.w, params.h, params.iKeyName, params.contextId);

        // Store the shell reference
        this.shell = shell;
    };

    // Inherit from ZaXDialog
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype = new ZaXDialog();
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype.constructor = com_btactic_hsm_ext.CustomZaXFormDialog;

    com_btactic_hsm_ext.CustomZaXFormDialog.prototype._initializeShell = function () {
        if (!this.shell) {
            this.shell = this.parent.shell || this.parent;
        }
        if (!this.shell) {
            throw new Error("Shell is required for CustomZaXFormDialog.");
        }
    };

    // Override setObject to set the object to be edited
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype.setObject = function (obj) {
        this._object = obj;  // Store the object
        // this._initializeForm();  // Initialize the form when the object is set
    };

    com_btactic_hsm_ext.CustomZaXFormDialog.prototype.setContent = function (content) {
        this._xformDef = {
            type: _GROUP_,
            numCols: 1,
            items: [
                { type: _CHECKBOX_, ref: "message", label: "E-mails" },
                { type: _CHECKBOX_, ref: "document", label: "Documents" },
                { type: _CHECKBOX_, ref: "task", label: "Tasks" },
                { type: _CHECKBOX_, ref: "appointment", label: "Appointments" },
                { type: _CHECKBOX_, ref: "contact", label: "Contacts" },
                { type: _TEXTFIELD_, ref: "query", label: "Query", width: "50em" }
            ]
        };

        // Ensure the shell is set before creating the form
        this._initializeShell();  // Make sure shell is properly initialized
    };

    // Method to create the form manually
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype._createForm = function () {
        this._initializeShell();

        if (!this._xformDef) {
            throw new Error("Form definition not set. Call setContent() first.");
        }

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
        this.initForm(xModel, this._xformDef, this._object);
    };

    // OK button callback function
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype._okCallback = function () {
        let originalValue = this._HsmPolicyEditContainer.getInstanceValue();
        var selectedTypes = [];
        // Collect selected types based on the object values
        for (let key of ["message", "document", "task", "appointment", "contact"]) {
            if (this._object[key]) selectedTypes.push(key);
        }

        // Check if any types are selected and if the query is not empty
        if (selectedTypes.length === 0 || !this._object.query.trim()) {
            alert("Please select at least one type and enter a query.");
            return;
        }

        // Format the result and set the value to the form item
        let result = selectedTypes.join(",") + ":" + this._object.query.trim();

        if (result !== originalValue) {
            this._HsmPolicyEditContainer.setInstanceValue(result); // Set the result to the form item
            // simulate the onchange logic: call elementChangedMethod
            const hsmPolicyEditTextFieldChangedMethod = this._HsmPolicyEditTextField.getElementChangedMethod();
            hsmPolicyEditTextFieldChangedMethod.call(this._HsmPolicyEditTextField, result, originalValue, event||window.event);
        }

        this.popdown();  // Close the dialog
    };

    // Cancel button callback function
    com_btactic_hsm_ext.CustomZaXFormDialog.prototype._cancelCallback = function () {
        this.popdown();  // Close the dialog
    };

    // Method to launch the edit wizard dialog
    com_btactic_hsm_ext.launchEditWizard = function (currentValue, HsmPolicyEditContainer, HsmPolicyEditTextField) {

        let colonIndex = currentValue.indexOf(":");

        let typesPart = colonIndex !== -1 ? currentValue.slice(0, colonIndex) : currentValue;
        let queryPart = colonIndex !== -1 ? currentValue.slice(colonIndex + 1) : "";

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
            title: "Edit HSM Policy",  // Set the title of the dialog
            w: "500px",              // Width (can be adjusted)
            h: "350px",              // Height (can be adjusted)
            iKeyName: "HSM_POLICY_EDIT",   // Internal key name
            contextId: Dwt.getNextId(ZaId.DLG_UNDEF),   // Context ID
            standardButtons: [DwtDialog.OK_BUTTON, DwtDialog.CANCEL_BUTTON]  // Specify standard buttons
        });
        // Set the object to be edited
        dlg.setObject({
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
                { type: _CHECKBOX_, ref: "message", label: "E-mails" },
                { type: _CHECKBOX_, ref: "document", label: "Documents" },
                { type: _CHECKBOX_, ref: "task", label: "Tasks" },
                { type: _CHECKBOX_, ref: "appointment", label: "Appointments" },
                { type: _CHECKBOX_, ref: "contact", label: "Contacts" },
                { type: _TEXTFIELD_, ref: "query", label: "Query", width: "50em" }
            ]
        });

        dlg._createForm();

        // Register the callback functions for OK and Cancel
        dlg.registerCallback(DwtDialog.OK_BUTTON, dlg._okCallback.bind(dlg));
        // dlg.registerCallback(DwtDialog.CANCEL_BUTTON, dlg._cancelCallback.bind(dlg));

        // Open the dialog
        dlg._HsmPolicyEditContainer = HsmPolicyEditContainer;
        dlg._HsmPolicyEditTextField = HsmPolicyEditTextField;
        dlg.popup();
    };


}
