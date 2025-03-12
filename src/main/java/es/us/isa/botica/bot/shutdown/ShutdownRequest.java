package es.us.isa.botica.bot.shutdown;

public class ShutdownRequest {
  private final boolean forced;

  public ShutdownRequest(boolean forced) {
    this.forced = forced;
  }

  /**
   * Returns whether this shutdown request is forced. This means that the director may shut down the
   * container imminently: shutdown hooks should only save important data quickly.
   */
  public boolean isForced() {
    return forced;
  }

  @Override
  public String toString() {
    return "ShutdownRequest{" + "forced=" + forced + '}';
  }
}
