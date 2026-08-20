package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public final class Snake {
  private static final AtomicInteger DEATH_SEQUENCE = new AtomicInteger();
  private final Deque<Position> body = new ArrayDeque<>();
  private volatile Direction direction;
  private int maxLength = 5;
  private boolean alive = true;
  private int deathOrder;

  private Snake(Position start, Direction dir) {
    body.addFirst(start);
    this.direction = dir;
  }

  public static Snake of(int x, int y, Direction dir) {
    return new Snake(new Position(x, y), dir);
  }

  public Direction direction() {
    return direction;
  }

  public void turn(Direction dir) {
    if ((direction == Direction.UP && dir == Direction.DOWN) ||
        (direction == Direction.DOWN && dir == Direction.UP) ||
        (direction == Direction.LEFT && dir == Direction.RIGHT) ||
        (direction == Direction.RIGHT && dir == Direction.LEFT)) {
      return;
    }
    this.direction = dir;
  }

  public synchronized Position head() {
    return body.peekFirst();
  }

  public synchronized Deque<Position> snapshot() {
    return new ArrayDeque<>(body);
  }

  public synchronized void advance(Position newHead, boolean grow) {
    body.addFirst(newHead);
    if (grow)
      maxLength++;
    while (body.size() > maxLength)
      body.removeLast();
  }

  public synchronized boolean contains(Position position) {
    return body.contains(position);
  }

  public synchronized void die() {
    if (alive) {
      alive = false;
      deathOrder = DEATH_SEQUENCE.incrementAndGet();
    }
  }

  public synchronized boolean isAlive() {
    return alive;
  }

  public synchronized int length() {
    return body.size();
  }

  public synchronized int deathOrder() {
    return deathOrder;
  }
}
