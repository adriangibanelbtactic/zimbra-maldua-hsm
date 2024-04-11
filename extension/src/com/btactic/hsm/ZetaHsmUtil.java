/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.btactic.hsm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.ZetaHsmRequest;
import com.zimbra.soap.admin.message.ZetaHsmResponse;

public class ZetaHsmUtil {

    private static final String LO_HELP = "help";
    private static final String LO_VERBOSE = "verbose";
    private static final String LO_VOLUMES = "volumes";

    private Options options;
    private boolean verbose = false;
    private ZetaHsmRequest.HsmAction action;

    private ZetaHsmUtil() {
        options = new Options();

        options.addOption(new Option("h", LO_HELP, false, "Display this help message."));
        options.addOption(new Option("v", LO_VERBOSE, false, "Display stack trace on error."));
    }

    private void usage(String errorMsg) {
        int exitStatus = 0;

        if (errorMsg != null) {
            System.err.println(errorMsg);
            exitStatus = 1;
        }
        HelpFormatter format = new HelpFormatter();
        format.printHelp(new PrintWriter(System.err, true), 80,
            "zetahsm [options] start/status/stop", null, options, 2, 2,
            "\nThe \"start/stop\" command is required, to avoid unintentionally running an HSM.  ");
        System.exit(exitStatus);
    }

    private void parseArgs(String[] args)
    throws ParseException {
        GnuParser parser = new GnuParser();
        CommandLine cl = parser.parse(options, args);

        if (CliUtil.hasOption(cl, LO_HELP)) {
            usage(null);
        }
        // Require the "start" command, so that someone doesn't inadvertently
        // kick of a ZetaHSM.
        if (cl.getArgs().length == 0) {
            usage(null);
        } else if  (cl.getArgs()[0].equals("stop")) {
            action = ZetaHsmRequest.HsmAction.stop;
        } else if (cl.getArgs()[0].equals("status")) {
            action = ZetaHsmRequest.HsmAction.status;
        } else if (cl.getArgs()[0].equals("start")) {
            action = ZetaHsmRequest.HsmAction.start;
        } else {
            usage(null);
        }

        verbose = CliUtil.hasOption(cl, LO_VERBOSE);
    }

    private void run() throws Exception {
        CliUtil.toolSetup();
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();

        ZetaHsmRequest request = new ZetaHsmRequest(action);
        // Setting:
        //  removePrefixes to true
        //  useContextMarshaller to false
        // and passing a class inside the com.zimbra.soap.admin.message package
        // (classes that you can make yourself in the Extension)
        // let's you use this JaxbUtil.jaxbToElement method to send Soap queries to the
        // zimbraAdmin endpoint quite nicely.
        Element requestElement = JaxbUtil.jaxbToElement(request, XMLElement.mFactory, true, false);
        Element respElem = prov.invoke(requestElement);
        // Workaround in order to be able to use elementToJaxb with non standard Zimbra classes
        // Make sure your custom Response class is inside the com.zimbra.soap.admin.message package
        ZetaHsmResponse response = JaxbUtil.elementToJaxb(respElem, ZetaHsmResponse.class);
        if (action == ZetaHsmRequest.HsmAction.start) {
            System.out.println("ZetaHSM scheduled. Run \"zetahsm status\" to check the status.");
        } else {
            System.out.println("Status = " + response.getStatus().name());
        }
    }

    public static void main(String[] args) {
        ZetaHsmUtil app = new ZetaHsmUtil();

        try {
            app.parseArgs(args);
        } catch (ParseException e) {
            app.usage(e.getMessage());
        }

        try {
            app.run();
        } catch (Exception e) {
            if (app.verbose) {
                e.printStackTrace(new PrintWriter(System.err, true));
            } else {
                String msg = e.getMessage();
                if (msg == null) {
                    msg = e.toString();
                }
                System.err.println(msg);
            }
            System.exit(1);
        }
    }
}
