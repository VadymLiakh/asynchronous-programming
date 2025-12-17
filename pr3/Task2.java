import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Task2FileCounter {

    static class FileCountTask extends RecursiveTask<Long> {
        private final File directory;
        private final String extension;

        public FileCountTask(File directory, String extension) {
            this.directory = directory;
            this.extension = extension.toLowerCase();
        }

        @Override
        protected Long compute() {
            long count = 0;
            File[] files = directory.listFiles();
            if (files == null) {
                return 0L;
            }

            List<FileCountTask> subTasks = new ArrayList<>();

            for (File f : files) {
                if (f.isDirectory()) {
                    FileCountTask subTask = new FileCountTask(f, extension);
                    subTask.fork();
                    subTasks.add(subTask);
                } else {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(extension)) {
                        count++;
                    }
                }
            }

            for (FileCountTask t : subTasks) {
                count += t.join();
            }

            return count;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введіть шлях до директорії: ");
        String dirPath = scanner.nextLine().trim();

        System.out.print("Введіть потрібне розширення файлів (наприклад, pdf або .pdf): ");
        String extInput = scanner.nextLine().trim().toLowerCase();
        if (!extInput.startsWith(".")) {
            extInput = "." + extInput;
        }

        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Помилка: вказаний шлях не є існуючою директорією.");
            return;
        }

        ForkJoinPool pool = new ForkJoinPool(); // Fork/Join Framework, work stealing

        long startTime = System.nanoTime();
        FileCountTask rootTask = new FileCountTask(dir, extInput);
        long count = pool.invoke(rootTask);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("Кількість файлів з розширенням " + extInput + ": " + count);
        System.out.println("Час виконання: " + durationMs + " мс");
    }
}
