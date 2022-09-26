# **Mi**nimalistic **A**synchronous framework

Minimalistic Asynchronous: Small framework for asynchronous java application.

MiA has been designed to ease the creation of applications such as front or middle servers that communicate over a network and requires to be Asynchronous and rely on state machines.

MiA is event-driven and relies on Netty for asynchronous networking operations. It features a tasking system which uses @FunctionalInterface and Futures to manage asynchronous operations.

## MiA Event handling

In a way, MiA event handling resembles the `ExecutorService` interface provided by java as it relies on the use of `Callable` and `Future`. But there are fundamental differences that justify that we do not use java executors and the `ExecutorService` interface. In MiA this is called the `ExecutionStage` interface.

Improvements brought by `ExecutionStage`:

1. Real-time: MiA provides a `TimeLimit` argument to contrain the execution of Callable delegated to an ExecutionStage.
2. CompletableFuture: java base interface `Future<V>` does not support any callback mechanism on completion. This is added by `CompletableFuture` or by `io.netty.util.concurrent.Future<T>`. That's why MiA requires these higher-level implementations of Future at its interface level.

These improvements are offered through the method `CompletableFuture<R> callBefore(TimeLimit limit, Callable<R> callable)`

To make asynchronism more comprehensive, MiA provides 3 levels of `ExecutionStage`.

1. `Task`: A Task is a Thread. It's the single point of synchronization that you'll use to avoid any synchronization issues.
2. `Terminal`: The terminal is a sub-task which is executed by the Thread of its parent Task. Why is it called Terminal? A Terminal should hold a termination point to something like a Socket, a File, a database connection... If a termination point generates events then we'll handle them on the corresponding `Terminal`.
3. `State`: A state can be either a TerminalState or a TaskState. It's used to handle events only when the state is active. Otherwise, the state will reject the execution of the event with an `InactiveStateException`.

This 3 stage design fits most cases you could find when implementing server-based applications.

See [http2-server sample](../mia-samples/http2-server/) for an example of 3 stage design with MiA.
