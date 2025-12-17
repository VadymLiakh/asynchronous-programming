import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class main {

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4);

    public static void main(String[] args) {
        long totalStart = System.nanoTime();

        System.out.println("=== TASK 1 ===");
        System.out.println("Масив (10) -> +5 -> факторіал від (sum(arr2) + sum(arr1))\n");

        AtomicLong tStartMsg = new AtomicLong();
        CompletableFuture<Void> startMsg = CompletableFuture.runAsync(() -> {
            tStartMsg.set(System.nanoTime());
            System.out.println("[Task1] Старт асинхронного ланцюжка (runAsync)");
            System.out.println("[Task1] Час: " + ms(tStartMsg.get()) + " мс");
        }, EXEC);

        // supplyAsync(): згенерувати масив (10)
        AtomicLong tGen = new AtomicLong();
        CompletableFuture<int[]> arr1Future = startMsg.thenComposeAsync(v ->
                        CompletableFuture.supplyAsync(() -> {
                            tGen.set(System.nanoTime());

                            int[] arr = new int[10];
                            ThreadLocalRandom rnd = ThreadLocalRandom.current();
                            for (int i = 0; i < arr.length; i++) arr[i] = rnd.nextInt(1, 11);

                            System.out.println("[Task1] Генерація arr1 завершена за " + ms(tGen.get()) + " мс");
                            return arr;
                        }, EXEC)
                , EXEC);

        // thenApplyAsync(): arr2 = arr1 + 5
        AtomicLong tPlus = new AtomicLong();
        CompletableFuture<int[]> arr2Future = arr1Future.thenApplyAsync(arr1 -> {
            tPlus.set(System.nanoTime());

            int[] arr2 = new int[arr1.length];
            for (int i = 0; i < arr1.length; i++) arr2[i] = arr1[i] + 5;

            System.out.println("[Task1] Побудова arr2 (+5) завершена за " + ms(tPlus.get()) + " мс");
            return arr2;
        }, EXEC);

        // thenApplyAsync(): S = sum(arr1) + sum(arr2)
        AtomicLong tSum = new AtomicLong();
        CompletableFuture<Integer> sFuture = arr2Future.thenApplyAsync(arr2 -> {
            tSum.set(System.nanoTime());

            int[] arr1 = arr1Future.join();
            int S = sum(arr1) + sum(arr2);

            System.out.println("[Task1] Обчислення S завершено за " + ms(tSum.get()) + " мс");
            return S;
        }, EXEC);

        // thenApplyAsync(): factorial(S)
        AtomicLong tFact = new AtomicLong();
        CompletableFuture<BigInteger> factFuture = sFuture.thenApplyAsync(S -> {
            tFact.set(System.nanoTime());

            BigInteger fact = factorial(S);

            System.out.println("[Task1] Факторіал (" + S + "!) завершено за " + ms(tFact.get()) + " мс");
            return fact;
        }, EXEC);

        // thenAcceptAsync(): асинхронний вивід масивів і факторіалу
        AtomicLong tPrint = new AtomicLong();
        CompletableFuture<Void> printFuture = factFuture.thenAcceptAsync(fact -> {
            tPrint.set(System.nanoTime());

            int[] arr1 = arr1Future.join();
            int[] arr2 = arr2Future.join();
            int S = sFuture.join();

            System.out.println("\n[Task1] arr1: " + Arrays.toString(arr1));
            System.out.println("[Task1] arr2: " + Arrays.toString(arr2));
            System.out.println("[Task1] S = sum(arr1)+sum(arr2) = " + S);
            System.out.println("[Task1] S! = " + fact);

            System.out.println("[Task1] Вивід завершено за " + ms(tPrint.get()) + " мс");
        }, EXEC);

        // thenRunAsync(): фінальна дія
        printFuture.thenRunAsync(() -> {
            long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStart);
            System.out.println("\n[Task1] Завдання 1 завершено.");
            System.out.println("[Task1] Загальний час: " + totalMs + " мс");
        }, EXEC).join();

        EXEC.shutdown();
    }

    private static long ms(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static int sum(int[] arr) {
        int s = 0;
        for (int x : arr) s += x;
        return s;
    }

    private static BigInteger factorial(int n) {
        BigInteger res = BigInteger.ONE;
        for (int i = 2; i <= n; i++) res = res.multiply(BigInteger.valueOf(i));
        return res;
    }
}
