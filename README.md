# Botica Java Library

This library provides the official Java support for developing bots that run inside a Botica
environment.

## Installation

### Using the official template

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

## Creating your first bot

To implement a bot, extend `BaseBot` and define its behavior either through annotations or through
the functional API in the `configure()` method.

### Example: reactive bot using the annotation-based API

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

### Example: proactive bot using the annotation-based API

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

### Example: reactive bot using the functional API

This bot registers an order listener for `process_data` actions programmatically within its
`configure()` method.

```java
public class MyBot extends BaseBot {

  @Override
  public void configure() {
    registerOrderListener("process_data", (action, payload) -> {
      System.out.println("Processing data: " + payload);
      String processedResult = process(payload);
      publishOrder("results_key", "store_processed_results", processedResult);
    });
  }

  private String process(String data) {
    return data.toUpperCase(); // Example processing
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

At runtime, Botica provides your bot with the necessary environment details, such as its ID, type,
and broker connection information, through automatically injected configuration objects.

## Further documentation

For a complete overview of `botica-lib-java` features and detailed guides, please refer to the
full documentation:

- **[Read full documentation and detailed guides](docs/0-index.md)**

To understand how bots interact inside a Botica environment, including core platform concepts,
refer to the main Botica documentation:

- **[The concept of a bot](https://github.com/isa-group/botica/blob/main/docs/1-the-concept-of-a-bot.md)**
- **[Creating process chains](https://github.com/isa-group/botica/blob/main/docs/2-process-chains.md)**
- **[Messaging between bots](https://github.com/isa-group/botica/blob/main/docs/3-messaging-between-bots.md)**
- **[Sharing files between bots](https://github.com/isa-group/botica/blob/main/docs/4-sharing-files-between-bots.md)**
- **[The infrastructure configuration file](https://github.com/isa-group/botica/blob/main/docs/the-infrastructure-configuration-file.md)**

## Example projects

Explore these real-world and demonstrative projects built with `botica-lib-java` to see the
concepts in action.

- **[Botica Fishbowl infrastructure](https://github.com/isa-group/botica-infrastructure-fishbowl)**:
    A simulation of a 9x9 fishbowl where multiple fish bots move around and a manager bot tracks
    their positions. This project showcases proactive (fish) and reactive (manager) bots written in
    both Java and Node.js, demonstrating inter-language communication and file system interaction.
    - **[Java Fish Bot](https://github.com/isa-group/botica-bot-fishbowl-fish-java)**: A proactive
      bot that periodically publishes its position within the fishbowl.
    - **[Java Manager Bot](https://github.com/isa-group/botica-bot-fishbowl-manager)**: A reactive
      bot that listens for fish positions, logs the fishbowl state, and saves it to files.


- **[Automatic REST API testing system with RESTest](https://github.com/isa-group/botica-infrastructure-restest)**:
    A real-world application automating REST API testing. Generator bots create test cases, executor
    bots run them, and reporter bots analyze results, demonstrating distributed processing and
    complex workflow orchestration using various Java bots.
    - **[RESTest Generator Bot](https://github.com/isa-group/botica-bot-restest-generator)**:
      Generates test cases and execution plans from API specifications.
    - **[RESTest Executor Bot](https://github.com/isa-group/botica-bot-restest-executor)**: Executes
      generated test cases against a target REST API.
    - **[RESTest Reporter Bot](https://github.com/isa-group/botica-bot-restest-reporter)**: Consumes
      test results, performs analysis, and generates reports or dashboards.

## License

This project is distributed under the MIT License.
