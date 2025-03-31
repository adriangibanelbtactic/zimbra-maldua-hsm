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
                              items : [ {
                                ref : ".",
                                type : _TEXTFIELD_,
                                label : null,
                                labelLocation : _NONE_,
                                toolTipContent : com_btactic_hsm_admin.tt_zimbraHsmPolicy,
                                visibilityChecks : [ ZaItem.hasReadPermission ],
                                width : "80em"
                              } ]
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
                              items : [ {
                                ref : ".",
                                type : _TEXTFIELD_,
                                label : null,
                                labelLocation : _NONE_,
                                toolTipContent : com_btactic_hsm_admin.tt_zimbraHsmPolicy,
                                visibilityChecks : [ ZaItem.hasReadPermission ],
                                width : "80em"
                              } ]
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

}
