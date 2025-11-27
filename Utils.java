
import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    public static int[] generateRandomArray(int size, int min, int max) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        return arr;
    }

    public static void printArray(String title, int[] arr) {
        System.out.println(title);
        if (arr == null || arr.length == 0) {
            System.out.println("[порожньо]");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i + 1 < arr.length) sb.append(", ");
        }
        sb.append("]");
        System.out.println(sb);
    }
}
