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

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.soap.admin.message.MoveBlobsRequest;
import com.zimbra.soap.admin.message.MoveBlobsResponse;
import com.zimbra.soap.JaxbUtil;

import com.btactic.hsm.storage.VolumeUtil;

public class ZetaHsmMoveBlobsUtil {

    private static final Options options = new Options();

    static {
        options.addOption("t", "types", true, "Comma-separated list of item types or 'all'");
        options.addOption("sid", "sourcevolumeid", true, "Source volume ID");
        options.addOption("did", "destinationvolumeid", true, "Destination volume ID");
        options.addOption("q", "query", true, "Query parameters (default: 'is:anywhere')");
        options.addOption("mb", "maxbytes", true, "Limit for the total number of bytes to move");
        options.addOption("h", "help", false, "Show usage summary.");
    }

    private ZetaHsmMoveBlobsUtil() {
        // Prevent instantiation
    }

    private static void usage() {
        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(System.err, true);
        formatter.printHelp(
            pw,
            80,
            "zetamoveblobs <options>",
            null,
            options,
            2,
            2,
            "\nThis command moves blobs between volumes based on a search query. All options are required unless defaults are specified."
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

            if (cmd.hasOption("t")) {
                opts.put("types", cmd.getOptionValue("t"));
            }
            if (cmd.hasOption("sid")) {
                opts.put("sourcevolumeid", cmd.getOptionValue("sid"));
            }
            if (cmd.hasOption("did")) {
                opts.put("destinationvolumeid", cmd.getOptionValue("did"));
            }
            if (cmd.hasOption("q")) {
                opts.put("query", cmd.getOptionValue("q"));
            }
            if (cmd.hasOption("mb")) {
                opts.put("maxbytes", cmd.getOptionValue("mb"));
            }
        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            usage();
            System.exit(1);
        }
        return opts;
    }

    public static void main(String[] args) {
        CliUtil.toolSetup();
        String defaultMoveBlobsQuery = "is:anywhere";

        Map<String, String> opts = parseArgs(args);

        String types = opts.get("types");
        String sourceVol = opts.get("sourcevolumeid");
        String destVol = opts.get("destinationvolumeid");
        String query = opts.get("query");
        String maxBytes = opts.get("maxbytes");

        if (types == null) {
            System.err.println("Missing required argument: --types");
            usage();
            System.exit(2);
        }

        if (sourceVol == null) {
            System.err.println("Missing required argument: --sourcevolumeid");
            usage();
            System.exit(3);
        }

        if (destVol == null) {
            System.err.println("Missing required argument: --destinationvolumeid");
            usage();
            System.exit(4);
        }

        Short destVolId = null;
        try {
            destVolId = Short.parseShort(destVol);
        } catch (NumberFormatException e) {
            System.err.println("Invalid destination volume ID. Must be a number.");
            System.exit(5);
        }

        Short sourceVolId = null;
        try {
            sourceVolId = Short.parseShort(sourceVol);
        } catch (NumberFormatException e) {
            System.err.println("Invalid source volume ID. Must be a number.");
            System.exit(6);
        }

        String sourceVolIds = sourceVol;

        // Set default values if needed
        String hsmQuery = query != null ? query : defaultMoveBlobsQuery;
        long maxByteLimit = maxBytes != null ? Long.parseLong(maxBytes) : 0L;
        if ("all".equals(types)) {
            types="message,document,task,appointment,contact"; // TODO: Do not hardcode this and get it from somewhere else.
        }

        try {
            SoapProvisioning prov = SoapProvisioning.getAdminInstance();
            prov.soapZimbraAdminAuthenticate();

            // ADVANCED CHECKS
            // types: No need to check if they are valid types. If they don't exist the search will not give results for them
            // maxbytes: No need to check. If maxbytes is not a number execution fails long before.
            // query: No need to check. Either no results (for being empty) or an error for its syntax not being correct

            // destinationvolumeid
            if (!(VolumeUtil.isValidLocator(prov, destVolId))) {
                System.err.println("destinationvolumeid: '" + destVol + "' is not a valid destination Volume ID");
                System.exit(7);
            }

            // sourcevolumeid
            if (!(VolumeUtil.isValidOriginLocator(prov, destVolId, sourceVolId))) {
                System.err.println("sourcevolumeid: '" + sourceVol + "' is not a valid source Volume ID");
                System.exit(8);
            }

            // Construct MoveBlobsRequest
            MoveBlobsRequest req = new MoveBlobsRequest();
            req.setDestVolumeId(destVolId);
            req.setSourceVolumeIds(sourceVolIds);
            req.setTypes(types);
            req.setMaxBytes(maxByteLimit);
            req.setQuery(hsmQuery);

            Element reqElement = JaxbUtil.jaxbToElement(req);
            Element respElement = prov.invoke(reqElement);
            MoveBlobsResponse resp = JaxbUtil.elementToJaxb(respElement);

            // Optional: log or display results
            System.out.printf(
                "Moved %d blobs (%d bytes) from %d mailboxes.\n",
                resp.getNumBlobsMoved(), resp.getNumBytesMoved(), resp.getNumMailboxesMoved()
            );

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(9);
        }

        System.exit(0);
    }
}
