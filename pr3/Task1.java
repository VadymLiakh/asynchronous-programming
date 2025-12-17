import java.util.*;
import java.util.concurrent.*;

public class Task1PairwiseSum {

    // Fork/Join (Work Stealing)
    static class PairwiseSumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 10_000;

        private final int[] array;
        private final int start; // індекс i (0..n-2), включно
        private final int end;   // індекс i, виключно (end <= n-1)

        public PairwiseSumTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;
            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i] + array[i + 1];
                }
                return sum;
            } else {
                int mid = start + length / 2;
                PairwiseSumTask left = new PairwiseSumTask(array, start, mid);
                PairwiseSumTask right = new PairwiseSumTask(array, mid, end);
                left.fork();
                long rightResult = right.compute();
                long leftResult = left.join();
                return leftResult + rightResult;
            }
        }
    }

    // Callable для Work Dealing
    static class PairwiseSumChunk implements Callable<Long> {
        private final int[] array;
        private final int start; // індекс i (0..n-2), включно
        private final int end;   // індекс i, виключно

        public PairwiseSumChunk(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        public Long call() {
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i] + array[i + 1];
            }
            return sum;
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        int n = readInt(scanner, "Введіть кількість елементів масиву (>=2): ", 2, Integer.MAX_VALUE);
        int minVal = readInt(scanner, "Введіть мінімальне значення елементів: ", Integer.MIN_VALUE, Integer.MAX_VALUE);
        int maxVal = readInt(scanner, "Введіть максимальне значення елементів: ", minVal, Integer.MAX_VALUE);
        int threads = readInt(scanner, "Введіть кількість потоків (>0): ", 1, 64);

        int[] array = generateRandomArray(n, minVal, maxVal);
        System.out.println("Згенерований масив:");
        System.out.println(Arrays.toString(array));

        // Послідовне обчислення (для перевірки)
        long startSeq = System.nanoTime();
        long seqResult = sequentialSum(array);
        long seqTimeMs = (System.nanoTime() - startSeq) / 1_000_000;
        System.out.println("\nПослідовний результат: " + seqResult);
        System.out.println("Час послідовного виконання: " + seqTimeMs + " мс");

        // Work Stealing: ForkJoinPool
        ForkJoinPool forkJoinPool = new ForkJoinPool(); // Fork/Join Framework, work stealing

        long startSteal = System.nanoTime();
        PairwiseSumTask rootTask = new PairwiseSumTask(array, 0, array.length - 1); // i: 0..n-2
        long stealingResult = forkJoinPool.invoke(rootTask);
        long stealingTimeMs = (System.nanoTime() - startSteal) / 1_000_000;

        System.out.println("\n[Work Stealing] Результат: " + stealingResult);
        System.out.println("[Work Stealing] Час виконання: " + stealingTimeMs + " мс");

        // Work Dealing: FixedThreadPool + ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(threads); // Thread Pool + ExecutorService

        int rangeLength = array.length - 1; // кількість індексів i (0..n-2)
        int tasksCount = threads * 4;
        int chunkSize = Math.max(1, rangeLength / tasksCount);

        List<Future<Long>> futures = new ArrayList<>();

        long startDeal = System.nanoTime();
        int currentStart = 0;
        while (currentStart < rangeLength) {
            int currentEnd = Math.min(currentStart + chunkSize, rangeLength);
            PairwiseSumChunk chunk = new PairwiseSumChunk(array, currentStart, currentEnd);
            futures.add(executor.submit(chunk));
            currentStart = currentEnd;
        }

        long dealingResult = 0;
        for (Future<Long> f : futures) {
            dealingResult += f.get();
        }
        long dealingTimeMs = (System.nanoTime() - startDeal) / 1_000_000;
        executor.shutdown();

        System.out.println("\n[Work Dealing] Результат: " + dealingResult);
        System.out.println("[Work Dealing] Час виконання: " + dealingTimeMs + " мс");

        // Перевірка коректності
        if (seqResult == stealingResult && seqResult == dealingResult) {
            System.out.println("\nУсі результати співпадають");
        } else {
            System.out.println("\nУВАГА: результати відрізняються");
        }
    }

    // Допоміжні методи

    private static long sequentialSum(int[] array) {
        long sum = 0;
        for (int i = 0; i < array.length - 1; i++) {
            sum += array[i] + array[i + 1];
        }
        return sum;
    }

    private static int[] generateRandomArray(int n, int minVal, int maxVal) {
        Random random = new Random();
        int[] arr = new int[n];
        int bound = maxVal - minVal + 1;
        for (int i = 0; i < n; i++) {
            arr[i] = minVal + random.nextInt(bound);
        }
        return arr;
    }

    private static int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value < min || value > max) {
                    System.out.println("Помилка: значення має бути в діапазоні [" + min + "; " + max + "].");
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                System.out.println("Помилка: введіть ціле число.");
            }
        }
    }
}
