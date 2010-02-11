/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.installer.javafx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 *
 * @author Adam
 */
public class FilesListTask extends Task {

    private File src, tar;
    private static char[] HEX = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public void setFile(File zip) {
        this.src = zip;
    }

    public void setToFile(File zip) {
        this.tar = zip;
    }

    @Override
    public void execute() throws BuildException {
        try {
            ZipFile f = new ZipFile(src);
            ZipOutputStream out = new ZipOutputStream(tar);
            ByteArrayOutputStream lArray = new ByteArrayOutputStream();
            PrintStream list = new PrintStream(lArray, false, "UTF-8");
            list.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<files-list>\n");
            out.setEncoding(f.getEncoding());
            Enumeration en = f.getEntries();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte buf[] = new byte[16384];
            while (en.hasMoreElements()) {
                ZipEntry ze = (ZipEntry)en.nextElement();
                out.putNextEntry(ze);
                if (ze.isDirectory()) {
                    list.print("    <entry type=\"directory\" empty=\"false\" modified=\"" + ze.getTime() + "\" permissions=\"" + Integer.toOctalString(ze.getUnixMode() & 0x1ff) + "\">" + ze.getName() + "</entry>\n");
                    getProject().log(ze.getName() + ' ' + Integer.toOctalString(ze.getUnixMode() & 0x1ff) + ' ' + ze.getTime(), Project.MSG_VERBOSE);
                } else {
                    md5.reset();
                    InputStream in = f.getInputStream(ze);
                    int i;
                    while ((i = in.read(buf)) >= 0) {
                        out.write(buf, 0, i);
                        md5.update(buf, 0, i);
                    }
                    in.close();
                    byte dig[] = md5.digest();
                    list.print("    <entry type=\"file\" size=\"" + ze.getSize() + "\" md5=\"");
                    for (i=0; i<dig.length; i++) list.print("" + HEX[((int)dig[i] & 0xf0) >> 4] + HEX[dig[i] & 0xf]);
                    list.print("\" jar=\"" + ze.getName().endsWith(".jar") + "\" packed=\"false\" signed=\"false\" modified=\"" + ze.getTime() + "\" permissions=\"" + Integer.toOctalString(ze.getUnixMode() & 0x1ff) + "\">" + ze.getName() + "</entry>\n");
                    getProject().log(ze.getName() + ' ' + Integer.toOctalString(ze.getUnixMode() & 0x1ff) + ' ' + ze.getTime() + ' ' + + ze.getSize(), Project.MSG_VERBOSE);
                }
                out.closeEntry();
            }
            list.println("</files-list>\n");
            list.close();
            out.putNextEntry(new ZipEntry("META-INF/files.list"));
            out.write(lArray.toByteArray());
            out.closeEntry();
            out.close();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

}