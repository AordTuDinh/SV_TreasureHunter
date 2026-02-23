package game.monitor;

import com.google.common.base.Charsets;
import ozudo.base.helper.GZip;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileData {

    public static final String PATH = "/home/DB/data/";

    private static String getRealPath(int userId, String strFunction, String filename, String PATH) {
        String tmp = StringHelper.formatLengthNumber(userId, 9);
        String path = String.format("%s/%s/%s/%s/", PATH, tmp.substring(0, 3), tmp.substring(3, 6), tmp.substring(6, 9));
        File file = new File(path);
        if (!file.exists()) file.mkdirs();
        return path + strFunction + "_" + filename;
    }

    public static String readFile(int userId, String strFunction, String filename) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(getRealPath(userId, strFunction, filename, PATH)));
            if (data == null || data.length < 2) return "";
            if (GZip.isCompressed(data)) {
                return GZip.decompress(data);
            }
            return new String(data, Charsets.UTF_8);
        } catch (NoSuchFileException ex) {
            return "";
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return null;
    }

    public static boolean writeFile(int userId, String strFunction, String filename, String data) {
        try {
//            List<String> aStr = new ArrayList<>(Arrays.asList("labyrinth", "trial", "pot"));
//            for (String value : aStr) {
//                Files.write(P aths.get(getRealPath(userId, strFunction, filename, PATH)), GZip.compress(data));
//                return true;
//            }
            Files.write(Paths.get(getRealPath(userId, strFunction, filename, PATH)), data.getBytes());
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return false;
    }

    public static String readFileWithPath(int userId, String strFunction, String filename, String newPath) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(getRealPath(userId, strFunction, filename, newPath)));
            if (data == null || data.length < 2) return "";
            if (GZip.isCompressed(data)) {
                return GZip.decompress(data);
            }
            return new String(data, Charsets.UTF_8);
        } catch (NoSuchFileException ex) {
            return "";
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return null;
    }

    public static boolean writeFileWithPath(int userId, String strFunction, String filename, String data, String newPath) {
        try {
            List<String> aStr = new ArrayList<>(Arrays.asList("labyrinth", "trial", "pot"));
            for (String value : aStr) {
                Files.write(Paths.get(getRealPath(userId, strFunction, filename, newPath)), GZip.compress(data));
                return true;
            }
            Files.write(Paths.get(getRealPath(userId, strFunction, filename, newPath)), data.getBytes());
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return false;
    }
}
