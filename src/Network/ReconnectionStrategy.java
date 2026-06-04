package Network;

import Config.SocketConfig;

public class ReconnectionStrategy {

    private int attemptCount;
    private long lastDelayMillis;

    public ReconnectionStrategy() {
        this.attemptCount = 0;
        this.lastDelayMillis = 0;
    }

    public boolean shouldRetry(int attemptNumber) {
        return attemptNumber <= SocketConfig.MAX_RECONNECTION_ATTEMPTS;
    }

    public long getDelayMillis(int attemptNumber) {
        if (attemptNumber <= 1) {
            lastDelayMillis = SocketConfig.getInitialReconnectionDelayMillis();
        } else {
            lastDelayMillis = (long) (lastDelayMillis * SocketConfig.RECONNECTION_BACKOFF_MULTIPLIER);
            if (lastDelayMillis > SocketConfig.getMaxReconnectionDelayMillis()) {
                lastDelayMillis = SocketConfig.getMaxReconnectionDelayMillis();
            }
        }
        return lastDelayMillis;
    }

    public void reset() {
        this.attemptCount = 0;
        this.lastDelayMillis = 0;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }
}
