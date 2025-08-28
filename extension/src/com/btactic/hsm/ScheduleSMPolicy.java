/*
 * ***** BEGIN LICENSE BLOCK *****
 * Maldua Zimbra HSM Extension
 * Copyright (C) 2025 BTACTIC, S.C.C.L.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ScheduleSMPolicy {

    private static final String ZETAHSM_CMD = "/opt/zimbra/bin/zetahsm --start";
    private static final String CRON_BEGIN = "# STORAGE MANAGEMENT BEGIN";
    private static final String CRON_END = "# STORAGE MANAGEMENT END";
    private static final String TMP_CRON_FILE = "/tmp/zetaschedulesmpolicy_cron";

    private boolean enabled = false;
    private String error = null;
    private int startTime = -1; // -1 means not scheduled

    // -------------------------------
    // Public API
    // -------------------------------

    public boolean isEnabled() {
        try {
            parseCrontab();
        } catch (IOException e) {
            this.error = e.getMessage();
        }
        return enabled;
    }

    public String getError() {
        // TODO: Fetch error from actual HSM session.
        return error;
    }

    public int getStartTime() {
        try {
            parseCrontab();
        } catch (IOException e) {
            this.error = e.getMessage();
        }
        return startTime;
    }

    public String getStartTimeString() {
        int hour = getStartTime();
        // format as HH:00 with leading zero if needed
        String hourString;

        // Return an empty string if not scheduled
        if (hour == -1) {
            hourString = "";
        } else {
            hourString = String.format("%02d:00", hour);
        }

        return hourString;
    }

    public void setSchedule(int smSchedulePolicyStartTime) throws IOException {
        if (smSchedulePolicyStartTime < 0 || smSchedulePolicyStartTime > 23) {
            throw new IOException("Invalid hour: " + smSchedulePolicyStartTime);
        }
        this.startTime = smSchedulePolicyStartTime;
        this.enabled = true;

        List<String> cron = getCrontab();
        List<String> newCron = new ArrayList<>();
        boolean inside = false;
        for (String line : cron) {
            if (line.equals(CRON_BEGIN)) {
                newCron.add(CRON_BEGIN);
                newCron.add(String.format("0 %02d * * * %s", startTime, ZETAHSM_CMD));
                inside = true;
                continue;
            }
            if (line.equals(CRON_END)) {
                newCron.add(CRON_END);
                inside = false;
                continue;
            }
            if (!inside) {
                newCron.add(line);
            }
        }
        if (!cron.contains(CRON_BEGIN) || !cron.contains(CRON_END)) {
            // block missing, rebuild it
            newCron.add(CRON_BEGIN);
            newCron.add(String.format("0 %02d * * * %s", startTime, ZETAHSM_CMD));
            newCron.add(CRON_END);
        }
        writeCrontab(newCron);
    }

    public void setSchedule(String smSchedulePolicyStartTime) throws IOException {
        if (smSchedulePolicyStartTime == null || !smSchedulePolicyStartTime.matches("^\\d{2}:\\d{2}$")) {
            throw new IOException("Invalid time format. Expected HH:00, got: " + smSchedulePolicyStartTime);
        }

        String[] parts = smSchedulePolicyStartTime.split(":");
        int hour;
        try {
            hour = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid hour: " + parts[0]);
        }

        String minutes = parts[1];
        if (!"00".equals(minutes)) {
            throw new IOException("Minutes must be 00. Got: " + minutes);
        }

        // Reuse the existing int-based method
        setSchedule(hour);
    }

    public void enable() throws IOException {
        if (startTime < 0) {
            throw new IOException("No start time set. Call setSchedule() first.");
        }
        setSchedule(startTime);
    }

    public void disable() throws IOException {
        List<String> cron = getCrontab();
        List<String> newCron = new ArrayList<>();
        boolean inside = false;
        for (String line : cron) {
            if (line.equals(CRON_BEGIN)) {
                newCron.add(CRON_BEGIN);
                inside = true;
                continue;
            }
            if (line.equals(CRON_END)) {
                newCron.add(CRON_END);
                inside = false;
                continue;
            }
            if (!inside) {
                newCron.add(line);
            }
        }
        if (!cron.contains(CRON_BEGIN) || !cron.contains(CRON_END)) {
            // If block missing, just append empty block
            newCron.add(CRON_BEGIN);
            newCron.add(CRON_END);
        }
        writeCrontab(newCron);
        this.enabled = false;
        this.startTime = -1;
    }

    // -------------------------------
    // Helpers
    // -------------------------------

    private List<String> getCrontab() throws IOException {
        List<String> lines = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("crontab", "-l");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void writeCrontab(List<String> lines) throws IOException {
        File tmpFile = new File(TMP_CRON_FILE);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
        ProcessBuilder pb = new ProcessBuilder("crontab", TMP_CRON_FILE);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while installing new crontab", e);
        } finally {
            tmpFile.delete();
        }
    }

    private void parseCrontab() throws IOException {
        List<String> cron = getCrontab();
        boolean inside = false;
        enabled = false;
        startTime = -1;
        for (String line : cron) {
            if (line.equals(CRON_BEGIN)) {
                inside = true;
                continue;
            }
            if (line.equals(CRON_END)) {
                inside = false;
                continue;
            }
            if (inside && line.matches("^0\\s+(\\d{2})\\s+\\*\\s+\\*\\s+\\*\\s+" + java.util.regex.Pattern.quote(ZETAHSM_CMD) + "$")) {
                String hour = line.split("\\s+")[1];
                startTime = Integer.parseInt(hour);
                enabled = true;
            }
        }
    }
}
