package ozudo.base.helper;

import game.config.lang.Lang;
import com.google.common.base.Charsets;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import ozudo.base.log.Logs;
import ozudo.base.log.slib_Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class Filer {
    final static String DEFAULT_FILE = "./deffile.log";

    public static void main(String[] args) throws IOException {
    }

    public static List<Path> listFileRecursively(String location) throws Exception {
        return Files.walk(Paths.get(location))
                .filter(Files::isRegularFile).collect(Collectors.toList());
    }

    public static boolean delete(String filename) {
        try {
            Files.delete(Paths.get(filename));
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    @SuppressWarnings("Since15")
    public static boolean saveBinFile(String filename, byte[] data) {
        try {
            Path path = Paths.get(filename);
            Files.createDirectories(path.getParent());
            Files.write(path, data);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("Since15")
    public static byte[] readBinFile(String filename) {
        try {
            return Files.readAllBytes(Paths.get(filename));
        } catch (Exception ex) {
        }
        return null;
    }

    public static void append(String text) {
        append(DEFAULT_FILE, text);
    }

    public static void append(String filePath, String text) {
        PrintWriter out = null;
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            out.println(text);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void saveFile(String filename, String text) {
        PrintWriter out = null;
        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            out = new PrintWriter(new BufferedWriter(new FileWriter(file, false)));
            out.println(text);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static List<String> realAllFile(String filename) {
        try {
            return Files.readAllLines(Paths.get(filename), Charsets.UTF_8);
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return null;
    }

    public static String readFile(String filename) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filename), Charsets.UTF_8);
            return lines.stream().collect(Collectors.joining());
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return null;
    }

    /**
     * Login to get information from facebook
     *
     * @param fbId
     * @param token
     * @return
     */
    public static String facebookLogin(String fbId, String token) {
        String url = String.format("https://graph.facebook.com/me?access_token=%s", token);
        try {
            long l = System.currentTimeMillis();
            String result = getHttpContent(url);
//            getLogger().info(url + " -> " + (System.currentTimeMillis() - l));
            JSONObject obj = JSONObject.fromObject(result);
            if (obj.containsKey("id")) {
                if (obj.getString("id").equals(fbId)) {
                    return result;
                } else {
                    return  Lang.err_wrong_login;
                }
            } else {
                return  Lang.err_not_login_facebook;
            }
        } catch (Exception ex) {
//            getLogger().error(url + " -> " + Util.exToString(ex));
        }
        return  Lang.err_facebook_login_fail;
    }

//    public static byte[] sendByteOverHttp(String address, byte[] data) {
//        URLConnection connection = null;
//        try {
//            URL url = new URL(address);
//            connection = url.openConnection();
//            connection.setDoOutput(true);
//            connection.setRequestProperty("Content-Type", "application/json");
//            connection.setConnectTimeout(5000);
//            connection.setReadTimeout(5000);
//            OutputStream out;
//            try {
//                out = connection.getOutputStream();
//                out.write(data);
//                out.close();
//            } catch (Exception e) {
//                Logs.error("sendByteOverHttp1 = " + GUtil.exToString(e));
//                Logs.error(e);
//            }
//            return org.apache.commons.io.IOUtils.toByteArray(connection.getInputStream());
//        } catch (Exception e) {
//            Logs.error("sendByteOverHttp2 = " + GUtil.exToString(e));
//        } finally {
//        }
//        return null;
//    }

    public static String getHttpContent(String address) {
        try {
            URL page = new URL(address);
            StringBuffer text = new StringBuffer();
            InputStreamReader in = null;
            if (address.startsWith("https")) {
                HttpsURLConnection conn = (HttpsURLConnection) page.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.setHostnameVerifier((hostname, sslSession) -> true);
                conn.connect();
                in = new InputStreamReader((InputStream) conn.getContent());
            } else {
                HttpURLConnection conn = (HttpURLConnection) page.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.connect();
                in = new InputStreamReader((InputStream) conn.getContent());
            }
            BufferedReader buff = new BufferedReader(in);
            String line = buff.readLine();
            while (line != null) {
                text.append(line);
                line = buff.readLine();
            }
            return text.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String getDataFromUrl(String url) {
        try {
            URL oracle = new URL(url);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(oracle.openStream()));

            String result = "";
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                result += inputLine;
            }
            in.close();
            return result;
        } catch (Exception ex) {
            getLogger().error(url + " -> " + GUtil.exToString(ex));
        }
        return "";
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
        return getClasses(packageName, true);
    }

    public static Class[] getClasses(String packageName, boolean includeSubPackage)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName, includeSubPackage));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    public static List<Class> findClasses(File directory, String packageName, boolean includeSubPackage) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory() && includeSubPackage) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName(), includeSubPackage));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    public static Logger getLogger() {
        return slib_Logger.root();
    }
}
