package game.cache;

import java.io.BufferedReader;
import java.io.FileReader;

public class CheckFileData {
    static String filePath = "/Users/aord/Desktop/data.txt";

    public static void main(String[] args) {
            int totalLines = 0;
            int countX1 = 0;
            int countX10 = 0;

            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    totalLines++;

                    if (line.contains("receive summon_pet_x10")) {
                        countX10++;
                    }else if (line.contains("receive summon_pet_x1")) {
                        countX1++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Tổng số dòng: " + totalLines);
            System.out.println("X1 = " + countX1);
            System.out.println("X10 = " + countX10);
            System.out.println("Total Point = " + (countX1 + countX10 * 10));
        }

}
