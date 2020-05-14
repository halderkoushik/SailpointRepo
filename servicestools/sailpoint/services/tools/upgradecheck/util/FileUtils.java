package sailpoint.services.tools.upgradecheck.util;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Created by surya.nellepalli on 09/10/2017.
 */
public class FileUtils {


    static final Logger log = Logger.getLogger(FileUtils.class);


    public static void WriteToFile(String fileName, String content, boolean append) {


        try {
            if (fileName == null)
                throw new Exception("File Name not specified");

            Writer writer = new BufferedWriter(new FileWriter(fileName, append));

            writer.write(content);
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("error in writing file", ex);
        }
    }

    public static String readFromFile(String fileName) {

        if (log.isDebugEnabled()) {
            log.debug("reading " + fileName);
        }

        StringBuffer sb = new StringBuffer();

        try {
            if (fileName == null)
                throw new Exception("File Name not specified");

            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }


            br.close();
        } catch (Exception ex) {
            log.error("error in writing file", ex);
        }
        if (log.isDebugEnabled()) {
            log.debug("reading completed succesfully " + fileName);
        }
        return sb.toString();
    }

    public static String getClasspathDirectory(int idx) {

        String dir = null;

        Properties props = System.getProperties();
        if (props != null) {

            String cp = props.getProperty("java.class.path");
            if (cp != null) {
                StringTokenizer toks = new StringTokenizer(cp, ";");
                for (int i = 0; toks.hasMoreElements(); i++) {
                    String s = (String) toks.nextElement();
                    if (i == idx) {

                        StringBuffer b = new StringBuffer();
                        for (int j = 0; j < s.length(); j++) {
                            char c = s.charAt(j);
                            if (c == '\\') c = '/';

                            if (c != '/' || j < s.length() - 1)
                                b.append(c);
                        }
                        dir = b.toString();

                        break;
                    }
                }
            }
        }

        return dir;
    }

    public static String getResourcePath(String name) {

        String path = null;

        // name must have forward slashes, backslashes will be escaped
        // which never works
        if (name.indexOf("\\") >= 0)
            name = name.replace("\\", "/");

        // Use a ClassLoader to find a URL

        java.net.URL res = null;

        try {

            ClassLoader l = Util.class.getClassLoader();
            if (l != null)
                res = l.getResource(name);

            if (res == null) {
                // use the system class loader
                res = ClassLoader.getSystemResource(name);
            }
        } catch (Exception e) {
            // ingore
        }

        if (res != null) {

            // strip off the file: or systemresource:
            path = res.getFile();

            // debugPrint("path before decode = " + path);
            String fileEncoding = System.getProperty("file.encoding");
            // debugPrint("Using encoding " + fileEncoding + " to decode URL.");
            try {
                path = URLDecoder.decode(path, fileEncoding);
            } catch (UnsupportedEncodingException ex) {
                System.err.println("Encoding " + fileEncoding +
                        " unsupported.  Using UTF-8");
                try {
                    path = URLDecoder.decode(path, "UTF-8");
                } catch (UnsupportedEncodingException ex2) {
                    System.err.println("UTF-8 encoding not supported. " +
                            "No decoding will be performed.");
                }
            }
            // debugPrint("path after decode = " + path);


            if (path.startsWith("/FILE")) {

                // looks like jdk 1.1
                StringBuffer b = new StringBuffer();

                int psn = 5;
                boolean prevSlash = false;

                if (Character.isDigit(path.charAt(psn))) {

                    // looks like a classpath reference
                    int sep = path.indexOf('/', 6);
                    if (sep == -1) {
                        // odd, probably corrupt
                    } else {
                        String nstr = path.substring(5, sep);
                        int n = Util.atoi(nstr);
                        String root = getClasspathDirectory(n);
                        b.append(root);
                        b.append("/");
                        psn = sep + 1;
                        prevSlash = true;
                    }
                } else {
                    // Its probably microsoft, start emitting
                    // the drive letter, but handle slash conversion
                    // and the occasional embedded +
                }

                for (int i = psn; i < path.length(); i++) {

                    char c = path.charAt(i);
                    if (c == '/') {
                        if (prevSlash) {
                            // this must be the "\/+/" pattern,
                            // ignore
                        } else {
                            b.append("/");
                            prevSlash = true;
                        }
                    } else if (c == '+') {
                        // we found the "/+" pattern, ignore
                    } else if (c == '\\') {
                        // convert to forward slash
                        b.append("/");
                        prevSlash = true;
                    } else {
                        b.append(c);
                        prevSlash = false;
                    }
                }

                path = b.toString();
            } else {
                // looks like jdk 1.2
                // If the path contains a colon, and there is an initial
                // slash remove it.  Slashes appear to come out the
                // right direction.

                if (path.charAt(0) == '/' && path.indexOf(":") != -1)
                    path = path.substring(1);
            }
        }

        return path;
    }
}
