package game.cache;

import java.io.BufferedReader;
import java.io.FileReader;

public class CheckFileData {
    static String filePath = "/Users/aord/Desktop/data.txt";

    public static void main(String[] args) {
            int totalLines = 0;

            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    totalLines++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Tổng số dòng: " + totalLines);
        }

}
