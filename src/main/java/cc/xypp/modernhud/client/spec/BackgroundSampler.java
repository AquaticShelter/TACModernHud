package cc.xypp.modernhud.client.spec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads a slow sensor on a background thread so the render thread only ever reads a number. The thread starts on the
 * first request and goes quiet a few seconds after the readout stops asking, so a box that is switched off costs
 * nothing.
 */
abstract class BackgroundSampler<T> {

    protected static final Logger LOGGER = LogManager.getLogger("ModernHud/Specs");
    private static final long KEEP_ALIVE_MS = 5_000L;

    private final String name;
    private volatile T value;
    private volatile long valueMs;
    private volatile long requestMs;
    private Thread thread;

    BackgroundSampler(String name) {
        this.name = name;
    }

    /** Reads the sensor once. Returns null when nothing answers. Runs on the sampler thread. */
    protected abstract T sample(long nowMs) throws InterruptedException;

    /** Time between two reads, in milliseconds. */
    protected abstract long intervalMs();

    /** Called once when the readout stopped asking (stop child processes here). */
    protected void onIdle() {
    }

    /** The newest value, or null when there is none yet or it is stale. Call from the render thread, every frame. */
    public final T request() {
        long now = System.currentTimeMillis();
        requestMs = now;
        ensureThread();
        T v = value;
        if (v == null || now - valueMs > Math.max(10_000L, intervalMs() * 5L)) {
            return null;
        }
        return v;
    }

    private synchronized void ensureThread() {
        if (thread != null && thread.isAlive()) {
            return;
        }
        thread = new Thread(this::loop, name);
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        boolean idle = false;
        while (true) {
            try {
                long now = System.currentTimeMillis();
                if (now - requestMs > KEEP_ALIVE_MS) {
                    if (!idle) {
                        idle = true;
                        onIdle();
                    }
                    Thread.sleep(500L);
                    continue;
                }
                idle = false;
                T v = sample(now);
                value = v;
                if (v != null) {
                    valueMs = System.currentTimeMillis();
                }
                Thread.sleep(Math.max(250L, intervalMs()));
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException e) {
                LOGGER.debug("{} failed: {}", name, e.toString());
                try {
                    Thread.sleep(5_000L);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }
}
