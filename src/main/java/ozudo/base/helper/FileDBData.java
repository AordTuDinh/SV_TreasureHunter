package ozudo.base.helper;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import ozudo.base.log.Logs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileDBData {
    public static final String PATH = "/var/www/pack.aordgame.com/cpack/";

    public static String readFile(String filename) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(PATH + filename));
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

    public static boolean writeFile(String filename, String data) {
        try {
            Files.write(Paths.get(PATH + filename), data.getBytes());
            System.out.println("Save " + filename + " successful");
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return false;
    }

    public static boolean writeFile2(String filename, String data) {
        try {
            Filer.saveFile(PATH + filename, data);
            System.out.println("Save " + filename + " successful");
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        }
        return false;
    }

}
