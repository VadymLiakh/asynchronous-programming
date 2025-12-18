import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(5);

    private static final String FROM = "Kyiv";
    private static final String TO = "Warsaw";
    private static final String DATE = "2025-12-15";

    public static void main(String[] args) {
        long totalStart = System.nanoTime();

        System.out.println("=== TASK 2 ===");
        System.out.println("Планування маршруту: паралельно перевірити поїзд/автобус/літак і вибрати найкращий.\n");
        System.out.println("FROM: " + FROM + " -> TO: " + TO + " | DATE: " + DATE + "\n");

        // 3 варіанти транспорту паралельно (price + hours)
        CompletableFuture<Option> trainF = checkTransport("TRAIN", 120, 220, 3.0, 5.5);
        CompletableFuture<Option> busF   = checkTransport("BUS",   80,  180, 4.0, 7.0);
        CompletableFuture<Option> planeF = checkTransport("PLANE", 200, 520, 1.0, 2.0);

        // anyOf(): показати "перший готовий" результат (демонстрація)
        CompletableFuture<Void> firstReady = CompletableFuture.anyOf(trainF, busF, planeF)
                .thenAccept(opt -> System.out.println("\n[anyOf] Перший готовий варіант: " + opt));

        // allOf(): дочекатися всіх, потім вибрати найкращий
        CompletableFuture<Void> allReady = CompletableFuture.allOf(trainF, busF, planeF);

        // Вибір найкращого (після allOf)
        CompletableFuture<Option> bestOption = allReady.thenApply(v -> {
            Option train = trainF.join();
            Option bus = busF.join();
            Option plane = planeF.join();

            System.out.println("\n[allOf] Усі варіанти готові:");
            System.out.println(" - " + train);
            System.out.println(" - " + bus);
            System.out.println(" - " + plane);

            // Критерій "найкращий": мінімізуємо (price + hours*30)
            return Collections.min(Arrays.asList(train, bus, plane));
        });

        // thenCompose()
        CompletableFuture<String> details = bestOption.thenCompose(opt -> fetchRouteDetails(opt));

        CompletableFuture<Void> finalFlow = CompletableFuture.allOf(firstReady, details)
                .thenRun(() -> {
                    long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - totalStart);
                    System.out.println("\n=== TASK 2 FINISHED ===");
                    System.out.println("Загальний час: " + totalMs + " мс");
                });

        finalFlow.join();
        EXEC.shutdown();
    }

    // Імітація перевірки транспорту (асинхронно)
    private static CompletableFuture<Option> checkTransport(
            String type,
            int minPrice, int maxPrice,
            double minHours, double maxHours
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();

            int delay = ThreadLocalRandom.current().nextInt(200, 900);
            sleep(delay);

            int price = ThreadLocalRandom.current().nextInt(minPrice, maxPrice + 1);
            double hours = round2(ThreadLocalRandom.current().nextDouble(minHours, maxHours));

            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            Option opt = new Option(type, price, hours, ms);

            System.out.println("[check] " + type + " готово за " + ms + " мс -> " + opt);
            return opt;
        }, EXEC);
    }

    // thenCompose() демонстрація: після bestOption асинхронно отримати "деталі"
    private static CompletableFuture<String> fetchRouteDetails(Option opt) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();

            int delay = ThreadLocalRandom.current().nextInt(200, 700);
            sleep(delay);

            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            String details = "\n[DETAILS] Найкращий маршрут:\n" +
                    "Transport: " + opt.type + "\n" +
                    "From: " + FROM + "\n" +
                    "To: " + TO + "\n" +
                    "Date: " + DATE + "\n" +
                    "Price: " + opt.price + " PLN\n" +
                    "Time: " + opt.hours + " h\n" +
                    "Details fetch time: " + ms + " ms\n";

            System.out.println(details);
            return details;
        }, EXEC);
    }

    // Модель опції
    private static class Option implements Comparable<Option> {
        final String type;
        final int price;       // PLN
        final double hours;    // travel time
        final long calcMs;     // time to compute/check

        Option(String type, int price, double hours, long calcMs) {
            this.type = type;
            this.price = price;
            this.hours = hours;
            this.calcMs = calcMs;
        }

        // Оцінка: ціна + (час * 30)
        double score() {
            return price + hours * 30.0;
        }

        @Override
        public int compareTo(Option o) {
            return Double.compare(this.score(), o.score());
        }

        @Override
        public String toString() {
            return type + " {price=" + price + " PLN, hours=" + hours + ", score=" + round2(score()) +
                    ", checkTime=" + calcMs + "ms}";
        }
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static double round2(double x) {
        return Math.round(x * 100.0) / 100.0;
    }
}
