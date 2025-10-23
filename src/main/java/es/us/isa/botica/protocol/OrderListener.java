package es.us.isa.botica.protocol;

@FunctionalInterface
public interface OrderListener {
  void onMessageReceived(String action, String payload);
}
