# Proactive bots

Proactive bots represent a design pattern for bots that execute work automatically, primarily on a
scheduled basis, without requiring explicit external interaction like an incoming order. Botica
provides native support for this pattern, abstracting away the complexities of schedulers, thread
management, and configuration. This allows developers to easily implement bots that, for example,
generate data periodically, initiate process chains by publishing orders, or perform routine
maintenance tasks, without hardcoding delay or period values.

## Configuration in the environment file

The execution schedule of a proactive bot's task is defined in the bot's lifecycle configuration
within your Botica environment file. This centralizes scheduling configuration and avoids hardcoding
it within your bot's code.

Example configuration for a proactive bot:

```yaml
bots:
  my_proactive_bot_type:
    image: "my_org/my_proactive_bot_image"
    lifecycle:
      type: proactive
      initialDelay: 5      # (Optional) Time in seconds before the first execution. Defaults to 0.
      period: 60           # (Optional) Time in seconds between executions. Defaults to 1.
    # ... other configurations
```

- **`type: proactive`**: This is a mandatory setting that identifies the bot type as proactive,
  enabling the execution of its proactive task. If this is not set, a proactive task defined in the
  bot will not execute.
- **`initialDelay`**: Specifies the time in seconds to wait before the first execution of the
  proactive task. It defaults to `0` if not specified.
- **`period`**: Defines the interval in seconds between repeated executions of the task.
    - If `period` is a positive value (e.g., `60`), the task will execute repeatedly every `60`
      seconds after its `initialDelay`.
    - If `period` is set to `-1`, the task will execute only once after its `initialDelay`.
      Following this single execution, the bot will automatically shut down if there are no other
      active user threads.

## Defining a proactive task

Once the bot type is configured as `proactive` in the environment file, you can define the actual
task within your `BaseBot` implementation.

### Annotation-based proactive task

Mark a method with `@ProactiveTask`. This method will be executed according to the timing
configuration in your environment file.

```java
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.ProactiveTask;
import java.time.Instant;
import java.util.UUID;

public class DataGeneratorBot extends BaseBot {

  @ProactiveTask
  public void generateAndPublishData() {
    System.out.println("Executing proactive data generation...");
    String generatedData = createRandomData();
    publishOrder("raw_data", "process_generated_data", generatedData);
  }

  private String createRandomData() {
    return "{ \"id\": \"" + UUID.randomUUID() + "\", \"timestamp\": \"" + Instant.now() + "\" }";
  }
}
```

Only **one** method per bot can be annotated with `@ProactiveTask`. If multiple methods are found,
an `IllegalStateException` will be thrown. The method must not have any parameters.

Proactive bots can still listen for and respond to incoming orders (making them suitable for
mixed-initiative workflows).

> [!NOTE]
> When using IoC frameworks like Spring or Guice, annotation-based configuration might not be
> detected on proxied instances. For best results, instantiate bot classes manually or use factory
> methods that return the actual implementation. Alternatively, proactive tasks can also be
> registered programmatically, as shown below.

### Programmatic proactive task

You can also set the proactive task programmatically by calling `setProactiveTask(Runnable task)`
within the `configure()` method. This offers flexibility for dynamic task assignment or integration
with other frameworks.

```java
import es.us.isa.botica.bot.BaseBot;
import java.time.Instant;
import java.util.UUID;

public class ProgrammaticProactiveBot extends BaseBot {

  @Override
  public void configure() {
    setProactiveTask(() -> {
      String generatedData = createRandomData();
      publishOrder("data_feed", "new_entry", generatedData);
    });
  }

  private String createRandomData() {
    return "{ \"task_id\": \"" + UUID.randomUUID() + "\", \"exec_time\": \"" + Instant.now()
        + "\" }";
  }
}
```

If a proactive task is set programmatically and a method is also annotated with `@ProactiveTask`,
an `IllegalStateException` will be thrown. Ensure you use only one approach to define the
proactive task.

## Best practices for proactive bots

- **Single Responsibility**: A proactive bot's primary role is often to initiate work. If its
  proactive task involves extensive processing, consider if the work can be split into smaller,
  independent tasks. These can then be delegated to reactive bots by publishing orders, potentially
  allowing for parallelization and better resource utilization.
- **Resource Utilization**: Be mindful of the `period` setting. Very frequent tasks might consume
  excessive resources. Adjust the timing based on the actual needs of the workflow.

[Back to documentation index](0-index.md)
