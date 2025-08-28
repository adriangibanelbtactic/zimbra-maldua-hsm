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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
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
import com.zimbra.soap.admin.message.HsmRequest;
// import com.zimbra.soap.admin.message.HsmResponse;
import com.zimbra.soap.admin.message.AbortHsmRequest;
// import com.zimbra.soap.admin.message.AbortHsmResponse;
import com.zimbra.soap.admin.message.GetHsmStatusRequest;
import com.zimbra.soap.admin.message.GetHsmStatusResponse;

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
    private String serverHost = "localhost";

    private static void usage() {
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
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> opts = new HashMap<>();
        try {
            CommandLineParser parser = new GnuParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                usage();
                System.exit(0);
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
                System.err.println("Only one action flag can be specified at a time (-t, -u, or -a).");
                usage();
                System.exit(1);
            }

            if (cmd.hasOption("s")) {
                opts.put("server", cmd.getOptionValue("s"));
            }

        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            usage();
            System.exit(2);
        }
        return opts;
    }

    private void startHSM() throws Exception {
        CliUtil.toolSetup();
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();

        HsmRequest req = new HsmRequest();

        Element reqElement = JaxbUtil.jaxbToElement(req);
        Element respElement = prov.invoke(reqElement);
        // HsmResponse resp = JaxbUtil.elementToJaxb(respElement);

        System.out.println("ZetaHSM scheduled. Run \"zetahsm --status\" to check the status.");
    }

    private void abortHSM() throws Exception {
        CliUtil.toolSetup();
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();

        AbortHsmRequest req = new AbortHsmRequest();

        Element reqElement = JaxbUtil.jaxbToElement(req);
        Element respElement = prov.invoke(reqElement);
        // AbortHsmResponse resp = JaxbUtil.elementToJaxb(respElement);

        System.out.println("ZetaHSM abort was sent. Run \"zetahsm --status\" to check the status.");
    }

    private void printHSMStatus() throws Exception {
        CliUtil.toolSetup();
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.soapZimbraAdminAuthenticate();

        GetHsmStatusRequest req = new GetHsmStatusRequest();
        Element reqElement = JaxbUtil.jaxbToElement(req);
        Element respElement = prov.invoke(reqElement);
        GetHsmStatusResponse resp = JaxbUtil.elementToJaxb(respElement);

        // Start and end times
        Long startMillis = resp.getStartDate();
        Long endMillis = resp.getEndDate();

        // Format dates for display
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());
        String startTime = startMillis != null ? dtf.format(Instant.ofEpochMilli(startMillis)) : "N/A";
        String endTime = endMillis != null ? dtf.format(Instant.ofEpochMilli(endMillis)) : "N/A";

        if (resp.getRunning()) {
            System.out.println("Last SM Session Stats");
        }

        // Print times
        System.out.println("Start time: " + startTime);
        if (!resp.getRunning()) {
            System.out.println("End time: " + endTime);
        }

        // Print query if available
        if (resp.getQuery() != null) {
            System.out.println("Query: " + resp.getQuery());
        }

        // Print running status
        if (resp.getRunning()) {
            System.out.println("Currently running.");
        } else {
            System.out.println("Not currently running.");
        }

        // Print blobs moved
        if (resp.getNumBlobsMoved() != null && resp.getDestVolumeId() != null) {
            System.out.println("Moved " + resp.getNumBlobsMoved() + " blob" +
                    (resp.getNumBlobsMoved() == 1 ? "" : "s") +
                    " to volume " + resp.getDestVolumeId() + ".");
        }

        // Print mailboxes processed
        if (resp.getNumMailboxes() != null && resp.getTotalMailboxes() != null) {
            System.out.println("Mailboxes processed: " + resp.getNumMailboxes() +
                    " out of " + resp.getTotalMailboxes() + ".");
        }

        // Print error if any
        if ( (resp.getError() != null) && (!("".equals(resp.getError()))) ) {
            System.out.println("Error: " + resp.getError());
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
            System.err.println("Missing action: must specify one of -t (start), -u (status), or -a (abort).");
            usage();
            System.exit(3);
        }

        try {
            if ("start".equals(actionOpt)) {
                app.startHSM();
            } else if ("abort".equals(actionOpt)) {
                app.abortHSM();
            } else { // status
                app.printHSMStatus();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage() != null ? e.getMessage() : e.toString());
            System.exit(5);
        }
    }
}
