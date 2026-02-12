# Running your bot

This document guides you through the process of building and deploying your Botica Java bot,
transforming your code into a container image ready to be orchestrated by the Botica Director.

## Bot entry point

Each Botica Java bot requires a designated entry point that starts the Botica runtime. This is
typically a `main` method in a launcher class that calls `BotLauncher.run(...)`.

```java
package com.myorg;

public class MyBotBootstrap {

  public static void main(String[] args) {
    // Replace 'new MyBot()' with an instance of your BaseBot implementation
    BotLauncher.run(new MyBot(), args);
  }
}
```

> [!IMPORTANT]
> Botica bots are designed to run exclusively within a Botica environment, not as standalone
> applications. You cannot simply run the `main` method of your `BotBootstrap` class manually.

## How to run your bot

How you run your bot depends on how you have structured your project.

### Option 1: Inside a Botica Project (Recommended)

This is the standard and easiest way to develop with Botica. In this scenario, your bot's source
code resides in a subdirectory within your main Botica project (a "monorepo" structure).

1. **Configure `environment.yml`**: Point the `build` property to your bot's directory.

   ```yaml
   bots:
     my_java_bot:
       build: "./my-bot-directory" # Path to the directory containing the Dockerfile
       replicas: 1
       subscribe:
         - key: "data_channel"
           strategy: distributed
   ```

2. **Run the Director**:

   Execute `./botica-director` on **Linux**/**macOS**, or `botica-director.cmd` on **Windows** in
   your project's directory.

The Director will automatically detect the `build` configuration, build the Docker image from the
source code in `./my-bot-directory`, and launch the container.

### Option 2: Separate Repository (Advanced)

This approach is suitable if you prefer to manage your bot in a completely separate Git repository,
or if you are building a **heavy bot** with complex dependencies, large files, or long compilation
times that you don't want to rebuild frequently.

In this scenario, you must build the Docker image yourself and tell Botica to use that pre-built
image.

1. **Build your bot image**:
   You need to compile your code and package it into a Docker image.

   <details>
   <summary>Using the official template (Easy)</summary>

   If you used the official `botica-seed-java` template, the `Dockerfile` and `pom.xml` are already
   configured for you. You simply need to run the Docker build command.

   Run this from your project root:

   ```bash
   docker build -t my-org/my-bot:latest .
   ```

   Ensure the tag you use (e.g., `my-org/my-bot:latest`) matches what you put in `environment.yml`.
   </details>

   <details>
   <summary>Manually (Custom/Existing Projects)</summary>

   If you have a custom setup, you'll need to create a `Dockerfile` and build it manually.

    1. **Create a Dockerfile**:
       ```dockerfile
       FROM eclipse-temurin:21
       WORKDIR /app
       COPY target/bot.jar /app/bot.jar
       ENTRYPOINT ["java", "-jar", "bot.jar"]
       ```

    2. **Build the image**:
       ```bash
       mvn clean install
       docker build -t my-org/heavy-bot .
       ```
   </details>

2. **Configure `environment.yml`**:
   Use the `image` property instead of `build`.

   ```yaml
   bots:
     my_heavy_bot:
       image: "my-org/heavy-bot" # Must match the tag you built
       replicas: 1
       subscribe:
         - key: "data_channel"
           strategy: distributed
   ```

3. **Run the Director**:

   Execute `./botica-director` on **Linux**/**macOS**, or `botica-director.cmd` on **Windows** in
   your project's directory.

The Director will skip the build step and directly use the local Docker image `my-org/heavy-bot`.

[Back to documentation index](0-index.md)
