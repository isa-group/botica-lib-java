# Botica Java Library

This library provides the official Java support for developing bots that run inside
a [Botica](https://github.com/isa-group/botica) environment.

## Installation

### Using the official template (recommended)

If you are starting from scratch, we recommend using the official template to set up your project:  
https://github.com/isa-group/botica-seed-java

This template contains:

- A Maven project configured for Botica bots
- Scripts for building and packaging your bot:
    - `build.sh` (Linux/macOS)
    - `build.bat` (Windows)
- A Dockerfile preconfigured for Botica environments
- Example bots implemented with the library
- A `pom.xml` file exposing the `imageTag` property, used by the build scripts

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
> We **really encourage** creating your bot's repository **using the official template**. It
> contains build scripts that simplify the entire build process of your bot into a single step, from
> compilation to Docker image creation.

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
