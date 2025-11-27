
import java.util.*;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Введіть розмір масиву (40..60): ");
        int n = safeIntInput(sc, 50);
        if (n < 40 || n > 60) {
            System.out.println("Неправильний розмір. Використовую 50.");
            n = 50;
        }

        System.out.print("Введіть множник (ціле число): ");
        int multiplier = safeIntInput(sc, 2);

        System.out.print("Введіть кількість потоків (>0): ");
        int parts = safeIntInput(sc, 4);
        if (parts <= 0) parts = 4;
        parts = Math.min(parts, n);

        // Випадковий масив у діапазоні [-100; 100]
        int[] input = Utils.generateRandomArray(n, -100, 100);

        // Результат у потокобезпечній структурі фіксованого розміру
        CopyOnWriteArrayList<Integer> result =
                new CopyOnWriteArrayList<>(Collections.nCopies(n, 0));

        ExecutorService pool = Executors.newFixedThreadPool(parts);
        List<Future<Void>> futures = new ArrayList<>();

        int chunkSize = (n + parts - 1) / parts; // стеля ділення
        long startTime = System.nanoTime();

        // Стартуємо завдання по частинах
        for (int p = 0; p < parts; p++) {
            int from = p * chunkSize;
            int to = Math.min(from + chunkSize, n);
            if (from >= to) break;
            futures.add(pool.submit(new ChunkTask(input, from, to, multiplier, result)));
        }

        // Очікування завершення + перевірки isDone()/isCancelled()
        for (int i = 0; i < futures.size(); i++) {
            Future<Void> f = futures.get(i);
            System.out.printf("Завдання #%d: isDone=%s, isCancelled=%s%n",
                    i, f.isDone(), f.isCancelled());
            try {
                f.get(3, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                boolean cancelled = f.cancel(true);
                System.out.printf("Завдання #%d: перевищено час очікування, скасовано=%s%n",
                        i, cancelled);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("Завдання #%d: перервано%n", i);
            } catch (ExecutionException e) {
                System.out.printf("Завдання #%d: помилка виконання: %s%n",
                        i, e.getCause());
            } finally {
                System.out.printf("Завдання #%d: фінально -> isDone=%s, isCancelled=%s%n",
                        i, f.isDone(), f.isCancelled());
            }
        }

        pool.shutdown();
        long endTime = System.nanoTime();

        // Вивід
        Utils.printArray("\nВхідний масив:", input);
        Utils.printArray("\nРезультат (помножено на " + multiplier + "):",
                result.stream().mapToInt(Integer::intValue).toArray());

        System.out.println("\nЧас виконання: " +
                TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " мс");
    }

    private static int safeIntInput(Scanner sc, int def) {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Некоректне число. Використовую " + def + ".");
            return def;
        }
    }
}
