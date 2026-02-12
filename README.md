# Botica Java Library

This library provides the official Java support for developing bots that run inside
a [Botica](https://github.com/isa-group/botica) environment.

## Installation

### With botica-director

The easiest way to start is by using the `botica-director` CLI to initialize a new bot directly inside your Botica project. This sets up the directory structure, Dockerfile, and build configuration automatically.

```bash
# On Linux/macOS
./botica-director init java my-bot-name

# On Windows
botica-director.cmd init java my-bot-name
```

### Using the official template

If you prefer to maintain your bot in a separate repository, you can use the official template to
set up your project:  
https://github.com/isa-group/botica-seed-java

### Using Maven

Add the library dependency in your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.isa-group.botica</groupId>
  <artifactId>botica-lib-java</artifactId>
  <version>0.6.0</version>
</dependency>
```

> [!TIP]
> We **really encourage** creating your bot's repository **using botica-director or the official
> template**. They provide the necessary structure and Dockerfile to easily build and run your bot
> within a Botica environment.

## Creating your first bot

To implement a bot, extend `BaseBot` and define its behavior either through annotations or through
the functional API in the `configure()` method.

### Example: reactive bot

This bot defines an order handler for `process_data` actions using the `@OrderHandler` annotation.

```java
public class MyBot extends BaseBot {

  @OrderHandler("process_data")
  public void onProcessData(String payload) {
    System.out.println("Processing data: " + payload);
    String processedResult = process(payload);
    publishOrder("results_key", "store_processed_results", processedResult);
  }

  private String process(String data) {
    return data.toUpperCase(); // Example processing
  }
}
```

### Example: proactive bot

This bot defines a proactive task using the `@ProactiveTask` annotation, which will run periodically
as configured in the environment file.

```java
public class GeneratorBot extends BaseBot {

  @ProactiveTask
  public void generateData() {
    publishOrder("raw_data", "process_data", "sample");
  }
}
```

## Entry point

Each bot requires an entry point class that starts the Botica runtime.  
The `BotLauncher.run(...)` method establishes the connection with the Botica Director and
initializes your bot.

```java
public class BotBootstrap {

  public static void main(String[] args) {
    BotLauncher.run(new MyBot(), args);
  }
}
```

> [!IMPORTANT]
> Botica bots are designed to run exclusively within a Botica environment, not as standalone
> applications. You cannot simply run the `main` method of your `BotBootstrap` class manually.
>
> Check out [Running your bot](docs/8-running-your-bot.md) in the documentation to learn how to run
> your bot in a Botica environment.

## Further documentation

For a complete overview of `botica-lib-java` features and detailed guides, please refer to the
full documentation:

### [Read full documentation, detailed guides and example projects](docs/0-index.md)

## License

This project is distributed under the MIT License.
