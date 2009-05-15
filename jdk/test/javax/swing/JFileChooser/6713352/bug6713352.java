/*
 * Copyright 2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/* @test
   @bug 6713352
   @summary Deadlock in JFileChooser with synchronized custom FileSystemView
   @author Pavel Porvatov
   @run main bug6713352
*/

import sun.awt.shell.ShellFolder;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class bug6713352 {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                MyFileSystemView systemView = new MyFileSystemView();

                synchronized (systemView) { // Get SystemView lock
                    new JFileChooser(systemView);

                    // Wait a little bit. BasicDirectoryModel will lock Invoker and stop on
                    // the bug6713352.MyFileSystemView.getFiles() method
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        System.out.println("Try to get Invokers lock");

                        ShellFolder.getShellFolder(new File("c:/")).listFiles(true);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                // To avoid RejectedExecutionException in BasicDirectoryModel wait a second
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static class MyFileSystemView extends FileSystemView {

        public File createNewFolder(File containingDir) throws IOException {
            return null;
        }

        public File[] getFiles(File dir, boolean useFileHiding) {
            System.out.println("getFiles start");

            File[] result;

            synchronized (this) {
                result = super.getFiles(dir, useFileHiding);
            }

            System.out.println("getFiles finished");

            return result;
        }

        public synchronized Boolean isTraversable(File f) {
            return super.isTraversable(f);
        }
    }
}
