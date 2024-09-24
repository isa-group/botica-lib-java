package es.us.isa.botica.bot.shutdown;

public class ShutdownRequest {
  private final boolean forced;
  private boolean canceled = false;

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

  /**
   * Attempts to cancel this shutdown.
   *
   * <p>Note that if this shutdown request is {@link #isForced() forced}, canceling will have no
   * effect, and the director may proceed with the shutdown regardless.
   */
  public void cancel() {
    this.setCanceled(true);
  }

  /**
   * Sets whether this shutdown should be canceled.
   *
   * <p>Note that if this shutdown request is {@link #isForced() forced}, canceling will have no
   * effect, and the director may proceed with the shutdown regardless.
   */
  public void setCanceled(boolean canceled) {
    this.canceled = canceled;
  }

  /**
   * Returns whether this shutdown request is canceled. This will have no real effect if the
   * shutdown request is forced.
   */
  public boolean isCanceled() {
    return canceled;
  }

  @Override
  public String toString() {
    return "ShutdownRequest{" + "forced=" + forced + ", canceled=" + canceled + '}';
  }
}
