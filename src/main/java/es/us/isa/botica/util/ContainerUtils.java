package es.us.isa.botica.util;

import es.us.isa.botica.BoticaConstants;

public final class ContainerUtils {
  private ContainerUtils() {}

  /**
   * Returns the hostname of the given bot's container.
   *
   * @param botId the ID of the bot instance
   * @return the bot container's hostname
   */
  public static String getHostname(String botId) {
    return BoticaConstants.CONTAINER_PREFIX + botId;
  }
}
