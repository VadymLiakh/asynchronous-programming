import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4);

    public static void main(String[] args) {
        long totalStart = System.nanoTime();

        System.out.println("=== TASK 1 ===");
        System.out.println("Отримати дані з кількох джерел одночасно і обробити разом після завершення всіх.\n");

        // 3 "джерела" даних
        CompletableFuture<String> sourceA = fetchFromSource("Source A (API)", 200, 600);
        CompletableFuture<String> sourceB = fetchFromSource("Source B (DB)", 150, 500);
        CompletableFuture<String> sourceC = fetchFromSource("Source C (FILE)", 100, 700);

        // thenCombine(): об'єднуємо A + B
        CompletableFuture<String> combinedAB = sourceA.thenCombine(sourceB,
                (a, b) -> "Combined(A+B) => [" + a + "] + [" + b + "]");

        // thenCombine(): додаємо ще C
        CompletableFuture<String> combinedABC = combinedAB.thenCombine(sourceC,
                (ab, c) -> ab + " + [" + c + "]");

        // allOf(): чекаємо, поки ВСІ завершаться, і тоді "спільна обробка"
        CompletableFuture<Void> allDone = CompletableFuture.allOf(sourceA, sourceB, sourceC)
                .thenRun(() -> {
                    System.out.println("\n[allOf] Усі джерела завершилися. Можна робити загальну обробку.");
                });

        // агрегований результат
        CompletableFuture<Void> finalFlow = allDone
                .thenCompose(v -> combinedABC)
                .thenAccept(aggregate -> {
                    System.out.println("\n[RESULT] Агреговані дані:");
                    System.out.println(aggregate);
                })
                .thenRun(() -> {
                    long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStart);
                    System.out.println("\n=== TASK 1 FINISHED ===");
                    System.out.println("Загальний час: " + totalMs + " мс");
                });

        finalFlow.join();
        EXEC.shutdown();
    }

    // Імітація отримання даних з джерела
    private static CompletableFuture<String> fetchFromSource(String name, int minDelayMs, int maxDelayMs) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            int delay = ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1);

            sleep(delay);

            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            String data = name + " data (delay=" + delay + "ms, time=" + ms + "ms)";
            System.out.println("[fetch] " + name + " завершено за " + ms + " мс");
            return data;
        }, EXEC);
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
