
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChunkTask implements Callable<Void> {
    private final int[] input;
    private final int start;       // inclusive
    private final int end;         // exclusive
    private final int multiplier;
    private final CopyOnWriteArrayList<Integer> output;

    public ChunkTask(int[] input, int start, int end, int multiplier,
                     CopyOnWriteArrayList<Integer> output) {
        this.input = input;
        this.start = start;
        this.end = end;
        this.multiplier = multiplier;
        this.output = output;
    }

    @Override
    public Void call() {
        for (int i = start; i < end; i++) {
            output.set(i, input[i] * multiplier);
        }
        return null;
    }
}
