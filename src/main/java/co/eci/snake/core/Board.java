package co.eci.snake.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class Board {
  private final int width;
  private final int height;

  private final Set<Position> mice = new HashSet<>();
  private final Set<Position> obstacles = new HashSet<>();
  private final Set<Position> turbo = new HashSet<>();
  private final Map<Position, Position> teleports = new HashMap<>();

  public enum MoveResult {
    MOVED, ATE_MOUSE, HIT_OBSTACLE, ATE_TURBO, TELEPORTED, DIED
  }

  public Board(int width, int height) {
    if (width <= 0 || height <= 0)
      throw new IllegalArgumentException("Board dimensions must be positive");
    this.width = width;
    this.height = height;
    for (int i = 0; i < 6; i++)
      mice.add(randomEmpty());
    for (int i = 0; i < 4; i++)
      obstacles.add(randomEmpty());
    for (int i = 0; i < 3; i++)
      turbo.add(randomEmpty());
    createTeleportPairs(2);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public synchronized Set<Position> mice() {
    return new HashSet<>(mice);
  }

  public synchronized Set<Position> obstacles() {
    return new HashSet<>(obstacles);
  }

  public synchronized Set<Position> turbo() {
    return new HashSet<>(turbo);
  }

  public synchronized Map<Position, Position> teleports() {
    return new HashMap<>(teleports);
  }

  public MoveResult step(Snake snake) {
    Objects.requireNonNull(snake, "snake");
    if (!snake.isAlive())
      return MoveResult.DIED;
    var head = snake.head();
    var dir = snake.direction();
    Position next = new Position(head.x() + dir.dx, head.y() + dir.dy).wrap(width, height);

    boolean teleported = false;
    boolean ateMouse = false;
    boolean ateTurbo = false;
    synchronized (this) {
      if (obstacles.contains(next))
        return MoveResult.HIT_OBSTACLE;
      if (teleports.containsKey(next)) {
        next = teleports.get(next);
        teleported = true;
      }
      ateMouse = mice.remove(next);
      ateTurbo = turbo.remove(next);
      if (ateMouse) {
        mice.add(randomEmpty());
        obstacles.add(randomEmpty());
        if (ThreadLocalRandom.current().nextDouble() < 0.2)
          turbo.add(randomEmpty());
      }
    }
    if (snake.contains(next)) {
      snake.die();
      return MoveResult.DIED;
    }
    snake.advance(next, ateMouse);
    if (ateTurbo)
      return MoveResult.ATE_TURBO;
    if (ateMouse)
      return MoveResult.ATE_MOUSE;
    if (teleported)
      return MoveResult.TELEPORTED;
    return MoveResult.MOVED;
  }

  private void createTeleportPairs(int pairs) {
    for (int i = 0; i < pairs; i++) {
      Position a = randomEmpty();
      Position b = randomEmpty();
      teleports.put(a, b);
      teleports.put(b, a);
    }
  }

  private Position randomEmpty() {
    var rnd = ThreadLocalRandom.current();
    Position p;
    int guard = 0;
    do {
      p = new Position(rnd.nextInt(width), rnd.nextInt(height));
      guard++;
      if (guard > width * height * 2)
        break;
    } while (mice.contains(p) || obstacles.contains(p) || turbo.contains(p) || teleports.containsKey(p));
    return p;
  }
}
