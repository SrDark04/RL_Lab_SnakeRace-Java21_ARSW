package co.eci.snake.core.engine;

import co.eci.snake.core.GameState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class GameClock implements AutoCloseable {
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final long periodMillis;
  private final Runnable tick;
  private final java.util.concurrent.atomic.AtomicReference<GameState> state = new AtomicReference<>(GameState.STOPPED);
  private final ReentrantLock pauseLock = new ReentrantLock();
  private final Condition pauseChanged = pauseLock.newCondition();
  private boolean pauseRequested;
  private int registeredRunners;
  private int pausedRunners;

  public GameClock(long periodMillis, Runnable tick) {
    if (periodMillis <= 0)
      throw new IllegalArgumentException("periodMillis must be > 0");
    this.periodMillis = periodMillis;
    this.tick = java.util.Objects.requireNonNull(tick, "tick");
  }

  public void start() {
    if (state.compareAndSet(GameState.STOPPED, GameState.RUNNING)) {
      scheduler.scheduleAtFixedRate(() -> {
        if (state.get() == GameState.RUNNING)
          tick.run();
      }, 0, periodMillis, TimeUnit.MILLISECONDS);
    }
  }

  public void registerRunners(int count) {
    pauseLock.lock();
    try {
      registeredRunners = count;
    } finally {
      pauseLock.unlock();
    }
  }

  public void awaitIfPaused() throws InterruptedException {
    pauseLock.lockInterruptibly();
    try {
      while (pauseRequested) {
        pausedRunners++;
        pauseChanged.signalAll();
        try {
          pauseChanged.await();
        } finally {
          pausedRunners--;
        }
      }
    } finally {
      pauseLock.unlock();
    }
  }

  public void awaitPaused() throws InterruptedException {
    pauseLock.lockInterruptibly();
    try {
      while (pauseRequested && pausedRunners < registeredRunners) {
        pauseChanged.await();
      }
    } finally {
      pauseLock.unlock();
    }
  }

  public void runnerStopped() {
    pauseLock.lock();
    try {
      registeredRunners--;
      pauseChanged.signalAll();
    } finally {
      pauseLock.unlock();
    }
  }

  public void pause() {
    pauseLock.lock();
    try {
      pauseRequested = true;
      state.set(GameState.PAUSED);
      pauseChanged.signalAll();
    } finally {
      pauseLock.unlock();
    }
  }

  public void resume() {
    pauseLock.lock();
    try {
      pauseRequested = false;
      state.set(GameState.RUNNING);
      pauseChanged.signalAll();
    } finally {
      pauseLock.unlock();
    }
  }

  public void stop() {
    state.set(GameState.STOPPED);
  }

  @Override
  public void close() {
    scheduler.shutdownNow();
  }
}
