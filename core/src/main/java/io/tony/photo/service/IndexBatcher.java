package io.tony.photo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class IndexBatcher<T> implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(IndexBatcher.class);

  private ExecutorService executor;
  private AtomicInteger threadIndex = new AtomicInteger();
  private volatile boolean run = false;
  private Consumer<T> messageConsumer;
  private Queue<T> messageQueue = new LinkedBlockingQueue<>();
  private Thread mainThread;
  private final T stopSignal;

  public IndexBatcher(Consumer<T> messageConsumer, T stopSignal) {
    this.messageConsumer = messageConsumer;
    this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "thread-index-batch-" + threadIndex.incrementAndGet());
      }
    });
    this.mainThread = new Thread(() -> {
      doIndex();
    }, "thread-index-batcher-master");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      this.run = false;
      add(stopSignal);
    }));
    this.stopSignal = stopSignal;
  }

  public void start() {
    if (!run) {
      synchronized (this) {
        if (!run) {
          this.run = true;
          this.mainThread.start();
        }
      }
    }
  }

  private void doIndex() {
    while (run) {
      try {
        T poll = messageQueue.poll();
        if (null == poll) {
          if (log.isDebugEnabled()) {
            log.debug("Message queue is empty, waiting now...");
          }
          synchronized (messageQueue) {
            messageQueue.wait();
          }
        } else {
          if (this.stopSignal == poll) {
            break;
          }
          executor.submit(() -> messageConsumer.accept(poll));
        }
      } catch (Exception e) {
        log.error("Process message failed with exception.", e);
      }
    }
  }

  public void add(T message) {
    if (run) {
      messageQueue.add(message);
      synchronized (messageQueue) {
        messageQueue.notifyAll();
      }
    }
  }

  public void stop() {
    this.run = false;
    messageQueue.add(stopSignal);
    try {
      executor.shutdown();
      executor.awaitTermination(5000, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("Close executor failed.");
    } finally {
      messageQueue.clear();
    }
  }

  @Override
  public void close() throws IOException {
    stop();
  }
}
