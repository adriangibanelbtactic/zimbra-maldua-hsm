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
        options.addOption("a", "abort", false, "Abort the current HSM session.");
        options.addOption("h", "help", false, "Displays this help message.");
        options.addOption("s", "server", true, "Mail server hostname. Default is localhost.");
        options.addOption("t", "start", false, "Start the HSM process.");
        options.addOption("u", "status", false, "Get status on the last HSM session.");
    }

    private boolean verbose = false;
    private ZetaHsmRequest.HsmAction action;
    private String serverHost = "localhost";

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
            "zetahsm <options>",
            null,
            options,
            2,
            2,
            null
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

            int actionCount = 0;
            if (cmd.hasOption("t")) {
                opts.put("action", "start");
                actionCount++;
            }
            if (cmd.hasOption("u")) {
                opts.put("action", "status");
                actionCount++;
            }
            if (cmd.hasOption("a")) {
                opts.put("action", "abort");
                actionCount++;
            }

            if (actionCount > 1) {
                usage("Only one action flag can be specified at a time (-t, -u, or -a).");
            }

            if (cmd.hasOption("s")) {
                opts.put("server", cmd.getOptionValue("s"));
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
        Element requestElement = JaxbUtil.jaxbToElement(request, XMLElement.mFactory, true, false);
        Element respElem = prov.invoke(requestElement);
        ZetaHsmResponse response = JaxbUtil.elementToJaxb(respElem, ZetaHsmResponse.class);

        if (action == ZetaHsmRequest.HsmAction.start) {
            System.out.println("ZetaHSM scheduled. Run \"zetahsm -u\" to check the status.");
        } else {
            System.out.println("Status = " + response.getStatus().name());
        }
    }

    public static void main(String[] args) {
        ZetaHsmUtil app = new ZetaHsmUtil();
        Map<String, String> opts = parseArgs(args);

        if (opts.containsKey("server")) {
            app.serverHost = opts.get("server");
        }

        String actionOpt = opts.get("action");
        if (actionOpt == null) {
            usage("Missing action: must specify one of -t (start), -u (status), or -a (abort).");
        }

        switch (actionOpt) {
            case "start":
                app.action = ZetaHsmRequest.HsmAction.start;
                break;
            case "status":
                app.action = ZetaHsmRequest.HsmAction.status;
                break;
            case "abort":
                app.action = ZetaHsmRequest.HsmAction.stop;
                break;
            default:
                usage("Invalid action: " + actionOpt);
        }

        try {
            app.run();
        } catch (Exception e) {
            System.err.println(e.getMessage() != null ? e.getMessage() : e.toString());
            System.exit(1);
        }
    }
}
