import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class main {

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4);

    public static void main(String[] args) {
        long totalStart = System.nanoTime();

        System.out.println("=== TASK 2 ===");
        System.out.println("Послідовність (20) -> min(a1+a2, a2+a3, ...)\n");

        // runAsync(): стартове повідомлення
        AtomicLong tStartMsg = new AtomicLong();
        CompletableFuture<Void> startMsg = CompletableFuture.runAsync(() -> {
            tStartMsg.set(System.nanoTime());
            System.out.println("[Task2] Старт асинхронного виконання (runAsync)");
            System.out.println("[Task2] Час: " + ms(tStartMsg.get()) + " мс");
        }, EXEC);

        // supplyAsync(): згенерувати послідовність з 20 натуральних
        AtomicLong tGen = new AtomicLong();
        CompletableFuture<int[]> seqFuture = startMsg.thenComposeAsync(v ->
                        CompletableFuture.supplyAsync(() -> {
                            tGen.set(System.nanoTime());

                            int[] a = new int[20];
                            ThreadLocalRandom rnd = ThreadLocalRandom.current();
                            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(1, 101);

                            System.out.println("[Task2] Генерація послідовності завершена за " + ms(tGen.get()) + " мс");
                            return a;
                        }, EXEC)
                , EXEC);

        // thenApplyAsync(): знайти min(a[i]+a[i+1])
        AtomicLong tCalc = new AtomicLong();
        CompletableFuture<Integer> minFuture = seqFuture.thenApplyAsync(a -> {
            tCalc.set(System.nanoTime());

            int min = Integer.MAX_VALUE;
            for (int i = 0; i < a.length - 1; i++) {
                int s = a[i] + a[i + 1];
                if (s < min) min = s;
            }

            System.out.println("[Task2] Обчислення min(...) завершено за " + ms(tCalc.get()) + " мс");
            return min;
        }, EXEC);

        // thenAcceptAsync(): асинхронний вивід
        AtomicLong tPrint = new AtomicLong();
        CompletableFuture<Void> printFuture = minFuture.thenAcceptAsync(min -> {
            tPrint.set(System.nanoTime());

            int[] a = seqFuture.join();
            System.out.println("\n[Task2] Послідовність: " + Arrays.toString(a));
            System.out.println("[Task2] min(a1+a2, a2+a3, ...) = " + min);

            System.out.println("[Task2] Вивід завершено за " + ms(tPrint.get()) + " мс");
        }, EXEC);

        // thenRunAsync(): фінальна дія + загальний час
        printFuture.thenRunAsync(() -> {
            long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStart);
            System.out.println("\n[Task2] Завдання 2 завершено.");
            System.out.println("[Task2] Загальний час: " + totalMs + " мс");
        }, EXEC).join();

        EXEC.shutdown();
    }

    private static long ms(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
