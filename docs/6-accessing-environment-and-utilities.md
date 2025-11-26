# Accessing environment and utilities

Bots can access crucial runtime information about themselves and the Botica environment. The
`BaseBot` class provides several utility methods that allow retrieving the bot's identity,
communicating with other bots, or interacting with the shared file system.

## Accessing bot identity and hostnames

Every bot instance within a Botica environment has a unique identity and runs within its own
container. You can retrieve details about your bot or other bots using the following methods:

- **`getBotId()`**: Returns the unique ID of the current bot instance. This ID is assigned by the
  Botica Director and is guaranteed to be unique within the environment.

  ```java
  public class MyBot extends BaseBot {
    @Override
    public void onStart() {
      String myId = getBotId();
      System.out.println("My bot ID is: " + myId);
    }
  }
  ```

- **`getBotType()`**: Returns the ID of the bot type this instance belongs to. A bot type groups
  multiple instances that share the same Docker image and general configuration.

  ```java
  public class MyBot extends BaseBot {
    @Override
    public void onStart() {
      String myType = getBotType();
      System.out.println("I am an instance of bot type: " + myType);
    }
  }
  ```

- **`getHostname()`**: Returns the hostname of the Docker container where the current bot instance
  is running. This can be useful for network-related tasks or logging.

  ```java
  public class MyBot extends BaseBot {
    @Override
    public void onStart() {
      String myHostname = getHostname();
      System.out.println("My container hostname is: " + myHostname);
    }
  }
  ```

- **`getBotHostname(String botId)`**: Returns the hostname of the Docker container for a specified
  bot ID. This is particularly useful when you need to establish direct network communication (e.g.,
  HTTP requests) with another specific bot instance.

  ```java
  public class MyBot extends BaseBot {
    public void connectToAnotherBot(String targetBotId) {
      String targetHostname = getBotHostname(targetBotId);
      System.out.println("Hostname for bot '" + targetBotId + "': " + targetHostname);
      // Example: use targetHostname to make an HTTP call
      // HttpClient.newHttpClient().send(HttpRequest.newBuilder().uri(URI.create("http://" + targetHostname + ":8080/api")).build(), HttpResponse.BodyHandlers.ofString());
    }
  }
  ```

## Accessing the shared directory

Botica provides a common, shared `/shared` directory mounted across all bot containers in an
environment. This facilitates inter-bot file sharing for large datasets, binaries, or any data not
suitable for message broker payloads.

**`getSharedDirectory()`** returns a `java.io.File` object representing the root of the shared
directory. You can then use standard Java `File` I/O operations to read from or write to this
directory.

  ```java
  import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileSharingBot extends BaseBot {

  @Override
  public void onStart() {
    File sharedDir = getSharedDirectory();
    System.out.println("Shared directory path: " + sharedDir.getAbsolutePath());

    // Example: Write a file to the shared directory
    Path myFile = sharedDir.toPath().resolve("my-bot-output.txt");
    try {
      Files.writeString(myFile, "Hello from " + getBotId() + "!", StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
      System.out.println("Wrote to shared file: " + myFile.getFileName());
    } catch (IOException e) {
      System.err.println("Error writing to shared file: " + e.getMessage());
    }
  }
}
  ```

## Advanced: The underlying `Bot` instance

While `BaseBot` provides a convenient, high-level API, in advanced scenarios you might need direct
access to the underlying `Bot` instance that `botica-lib-java` manages.

- **`getBot()`**: Returns the internal `Bot` instance. This object holds the core configuration and
  low-level client for interacting with the Botica protocol.

  **Note**: Direct interaction with the `Bot` instance is generally not recommended for most
  applications, as `BaseBot` exposes all common functionality through more user-friendly methods.
  Use this only if you need to access very specific, low-level features not exposed by `BaseBot`.

[Back to documentation index](0-index.md)
