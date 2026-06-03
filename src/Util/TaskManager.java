package Util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }
}
