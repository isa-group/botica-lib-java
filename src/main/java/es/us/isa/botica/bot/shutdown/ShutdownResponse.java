package es.us.isa.botica.bot.shutdown;

public class ShutdownResponse {

  /**
   * Returns a {@code ShutdownResponse} indicating that the bot is ready to shut down.
   *
   * @return a {@code ShutdownResponse} where shutdown proceeds normally
   */
  public static ShutdownResponse ready() {
    return new ShutdownResponse(false);
  }

  /**
   * Returns a {@code ShutdownResponse} attempting to cancel the shutdown request.
   *
   * <p><b>Note:</b> If the shutdown request is {@link ShutdownRequest#isForced() forced}, canceling
   * will have no effect, and the director may proceed with the shutdown regardless.
   *
   * <p><b>Important:</b> Even if the request is <b>not forced</b>, canceling is not guaranteed to
   * prevent the shutdown. The director may still proceed based on other conditions, such as
   * exceeding the allowed delay.
   *
   * @return a {@code ShutdownResponse} attempting to cancel the shutdown
   */
  public static ShutdownResponse cancel() {
    return new ShutdownResponse(true);
  }

  private boolean canceled = false;

  public ShutdownResponse() {
    this(false);
  }

  public ShutdownResponse(boolean canceled) {
    this.canceled = canceled;
  }

  /**
   * Sets whether this shutdown should be canceled.
   *
   * <p><b>Note:</b> If the shutdown request is {@link ShutdownRequest#isForced() forced}, canceling
   * will have no effect, and the director may proceed with the shutdown regardless.
   *
   * <p><b>Important:</b> Even if the request is <b>not forced</b>, canceling is not guaranteed to
   * prevent the shutdown. The director may still proceed based on other conditions, such as
   * exceeding the allowed delay.
   *
   * @param canceled {@code true} to attempt to cancel the shutdown, {@code false} otherwise
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
    return "ShutdownResponse{" + "canceled=" + canceled + '}';
  }
}
