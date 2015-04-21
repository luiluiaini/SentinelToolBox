/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.csa.rstb.dat.actions;

import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.FileFolderUtils;
import org.esa.snap.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;

/**
 * This action launches PolSARPro
 */
public class LaunchPolsarProAction extends ExecCommand {

    private final static String PolsarProPathStr = "external.polsarpro.path";
    private final static String TCLPathStr = "external.TCL.path";

    /**
     * Launches PolSARPro
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        final Preferences pref = SnapApp.getDefault().getPreferences();

        // find tcl wish
        File tclFile = new File(pref.get(TCLPathStr, ""));

        if (!tclFile.exists()) {
            tclFile = findTCLWish();
            if (tclFile.exists())
                pref.put(TCLPathStr, tclFile.getAbsolutePath());
        }

        // find polsar pro
        File polsarProFile = new File(pref.get(PolsarProPathStr, ""));

        if (!polsarProFile.exists()) {
            polsarProFile = findPolsarPro();
        }
        if (!polsarProFile.exists()) {
            // ask for location
            polsarProFile = FileFolderUtils.GetFilePath("PolSARPro Location", "tcl", "tcl", null, "PolSARPro File", false);
        }
        if (polsarProFile.exists()) {
            externalExecute(polsarProFile, tclFile);

            // save location
            pref.put(PolsarProPathStr, polsarProFile.getAbsolutePath());
        }
    }

    private static void externalExecute(final File prog, final File tclWishFile) {
        final File homeFolder = ResourceUtils.findHomeFolder();
        final File program = new File(homeFolder, "bin" + File.separator + "exec.bat");

        String wish = "wish";
        final String args = '\"' + prog.getParent() + "\" " + wish + ' ' + prog.getName();

        System.out.println("Launching PolSARPro " + args);

        final Thread worker = new Thread() {

            @Override
            public void run() {
                try {
                    final Process proc = Runtime.getRuntime().exec(program.getAbsolutePath() + ' ' + args);

                    outputTextBuffers(new BufferedReader(new InputStreamReader(proc.getInputStream())));
                    boolean hasErrors = outputTextBuffers(new BufferedReader(new InputStreamReader(proc.getErrorStream())));

                    if (hasErrors && tclWishFile.exists()) {
                        String wish = '\"' + tclWishFile.getAbsolutePath() + '\"';
                        final String args = '\"' + prog.getParent() + "\" " + wish + ' ' + prog.getName();
                        System.out.println("Launching PolSARPro 2nd attempt " + args);

                        final Process proc2 = Runtime.getRuntime().exec(program.getAbsolutePath() + ' ' + args);

                        outputTextBuffers(new BufferedReader(new InputStreamReader(proc2.getInputStream())));
                        outputTextBuffers(new BufferedReader(new InputStreamReader(proc2.getErrorStream())));
                    }
                } catch (Exception e) {
                    SnapDialogs.showError("Unable to launch PolSARPro:"+e.getMessage());
                }
            }
        };
        worker.start();
    }

    private static File findTCLWish() {
        File progFiles = new File("C:\\Program Files (x86)\\TCL\\bin");
        if (!progFiles.exists())
            progFiles = new File("C:\\Program Files\\TCL\\bin");
        if (!progFiles.exists())
            progFiles = new File("C:\\TCL\\bin");
        if (progFiles.exists()) {
            final File[] files = progFiles.listFiles();
            if(files != null) {
                for (File file : files) {
                    final String name = file.getName().toLowerCase();
                    if (name.equals("wish.exe")) {
                        return file;
                    }
                }
            }
        }
        return new File("");
    }

    private static File findPolsarPro() {
        File progFiles = new File("C:\\Program Files (x86)");
        if (!progFiles.exists())
            progFiles = new File("C:\\Program Files");
        if (progFiles.exists()) {
            final File[] progs = progFiles.listFiles(new PolsarFileFilter());
            if(progs != null) {
                for (File prog : progs) {
                    final File[] fileList = prog.listFiles(new PolsarFileFilter());
                    if(fileList != null) {
                        for (File file : fileList) {
                            if (file.getName().toLowerCase().endsWith("tcl")) {
                                return file;
                            }
                        }
                    }
                }
            }
        }
        return new File("");
    }

    private static class PolsarFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {

            return name.toLowerCase().startsWith("polsar");
        }
    }

    private static boolean outputTextBuffers(BufferedReader in) throws IOException {
        char c;
        boolean hasData = false;
        while ((c = (char) in.read()) != -1 && c != 65535) {
            //errStr += c;
            System.out.print(c);
            hasData = true;
        }
        return hasData;
    }
}