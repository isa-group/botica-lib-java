package es.us.isa.botica.bot.shutdown;

/**
 * Hook for handling shutdown requests from the botica director.
 *
 * <p>If the bot is still running a task, it can respond by requesting extra time until the next
 * shutdown request:
 *
 * <pre>
 * getShutdownHandler().registerShutdownRequestHook((request, response) -> {
 *   if (this.isRunning()) { // some method to check if the bot is still executing a task
 *     response.cancel(); // this will have no effect if request.isForced() is true
 *   }
 * });
 * </pre>
 *
 * <p>Note that when extra time is requested, the director may send another shutdown request
 * in the future. This allows bots some grace to finish ongoing tasks, but developers should
 * still avoid very long-running tasks, as these may not be completed within the available time
 * window. The director may ultimately force a shutdown if the bot fails to complete its tasks
 * within the given time.
 *
 * <p>All registered shutdown hooks will be run before the response is sent back to the director,
 * so developers should also watch the overall execution time to ensure that the response is not
 * delayed unnecessarily.
 *
 * <p>In the case of a forced shutdown, indicated by {@link ShutdownRequest#isForced()} being
 * {@code true}, the response will be ignored, and the director may shut down the container
 * imminently. In this scenario, the hook should be used only to quickly save important data.
 */
@FunctionalInterface
public interface ShutdownRequestHook {
  void onShutdownRequest(ShutdownRequest request, ShutdownResponse response);
}
