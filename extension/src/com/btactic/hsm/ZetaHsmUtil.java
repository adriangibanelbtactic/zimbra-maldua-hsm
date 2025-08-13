/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.ZetaHsmRequest;
import com.zimbra.soap.admin.message.ZetaHsmResponse;

public class ZetaHsmUtil {

    private static final Options options = new Options();

    static {
        options.addOption("h", "help", false, "Display this help message.");
        options.addOption("v", "verbose", false, "Display stack trace on error.");
    }

    private boolean verbose = false;
    private ZetaHsmRequest.HsmAction action;

    private static void usage(String errorMsg) {
        int exitStatus = 0;
        if (errorMsg != null) {
            System.err.println(errorMsg);
            exitStatus = 1;
        }
        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(System.err, true);
        formatter.printHelp(
            pw,
            80,
            "zetahsm <options> start|status|stop",
            null,
            options,
            2,
            2,
            "\nThe \"start/stop\" command is required, to avoid unintentionally running an HSM."
        );
        System.exit(exitStatus);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        try {
            CommandLineParser parser = new GnuParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                usage(null);
            }
            if (cmd.hasOption("v")) {
                opts.put("verbose", "true");
            }

            if (cmd.getArgs().length > 0) {
                opts.put("command", cmd.getArgs()[0]);
            } else {
                usage("Missing command: start, status, or stop");
            }
        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            usage(null);
        }
        return opts;
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
        Map<String, String> opts = parseArgs(args);

        app.verbose = Boolean.parseBoolean(opts.get("verbose"));
        String cmd = opts.get("command");

        if ("stop".equals(cmd)) {
            app.action = ZetaHsmRequest.HsmAction.stop;
        } else if ("status".equals(cmd)) {
            app.action = ZetaHsmRequest.HsmAction.status;
        } else if ("start".equals(cmd)) {
            app.action = ZetaHsmRequest.HsmAction.start;
        } else {
            usage("Invalid command: " + cmd);
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
