package co.eci.primefinder;

public class PausedControl {
    private boolean paused = false;

    public synchronized void pauseAllThreads() {
        paused = true;
    }

    public synchronized void resumeAllThreads() {
        paused = false;
        notifyAll();
    }

    public synchronized void waitIfPaused() throws InterruptedException {
        while (paused) {
            wait();
        }
    }
}
