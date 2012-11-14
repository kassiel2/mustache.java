package com.github.mustachejavabenchmarks;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.mustachejavabenchmarks.BenchmarkTest.skip;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class JsonInterpreterTest extends TestCase {
  private static final int TIME = 2;

  protected File root;

  public static Object toObject(final JsonNode node) {
    if (node.isArray()) {
      return new ArrayList() {{
        for (JsonNode jsonNodes : node) {
          add(toObject(jsonNodes));
        }
      }};
    } else if (node.isObject()) {
      return new HashMap() {{
        for (Iterator<Map.Entry<String, JsonNode>> i = node.getFields(); i.hasNext(); ) {
          Map.Entry<String, JsonNode> next = i.next();
          Object o = toObject(next.getValue());
          put(next.getKey(), o);
        }
      }};
    } else if (node.isNull()) {
      return null;
    } else {
      return node.asText();
    }
  }

  public void testSingleThreaded() throws MustacheException, IOException, InterruptedException {
    if (skip()) return;
    final Mustache parse = getMustache();
    final Object parent = getScope();

    singlethreaded(parse, parent);
  }

  public void testCompiler() throws MustacheException, IOException, InterruptedException {
    if (skip()) return;
    for (int i = 0; i < 3; i++) {
      {
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          DefaultMustacheFactory mb = createMustacheFactory();
          final Mustache parse = mb.compile("timeline.mustache");
          total++;
          if (System.currentTimeMillis() - start > TIME * 1000) break;
        }
        System.out.println("Compilations: " + total / TIME + "/s");
      }
    }
  }

  protected DefaultMustacheFactory createMustacheFactory() {
    return new DefaultMustacheFactory(root);
  }

  public void testMultithreaded() throws IOException, InterruptedException {
    if (skip()) return;
    final Mustache parse = getMustache();
    final Object parent = getScope();

    final AtomicInteger runs = new AtomicInteger(0);
    ExecutorService es = Executors.newCachedThreadPool();
    int range = (int) Math.round(Runtime.getRuntime().availableProcessors() * 1.5 + 1);
    for (int threads = 1; threads < range; threads++) {
      final Semaphore semaphore = new Semaphore(threads);
      {
        long start = System.currentTimeMillis();
        while (true) {
          semaphore.acquire();
          es.submit(new Runnable() {
            @Override
            public void run() {
              parse.execute(new NullWriter(), new Object[] { parent });
              runs.incrementAndGet();
              semaphore.release();
            }
          });
          if (System.currentTimeMillis() - start > TIME * 1000) {
            break;
          }
        }
        System.out.println("NullWriter Serial with " + threads + " threads: " + runs.intValue() / TIME + "/s " + " per thread: " + (runs.intValue() / TIME / threads));
        runs.set(0);
        Thread.sleep(100);
      }
    }
  }

  private Object getScope() throws IOException {
    MappingJsonFactory jf = new MappingJsonFactory();
    InputStream json = getClass().getClassLoader().getResourceAsStream("hogan.json");
    final Map node = (Map) toObject(jf.createJsonParser(json).readValueAsTree());
    System.out.println(node);
    return new Object() {
      int uid = 0;
      List tweets = new ArrayList() {{
        for (int i = 0; i < 50; i++) {
          add(node);
        }
      }};
    };
  }

  private Mustache getMustache() {
    DefaultMustacheFactory mb = createMustacheFactory();
    return mb.compile("timeline.mustache");
  }

  private void singlethreaded(Mustache parse, Object parent) {
    long start = System.currentTimeMillis();
    System.out.println(System.currentTimeMillis() - start);
    start = System.currentTimeMillis();
    StringWriter writer = new StringWriter();
    parse.execute(writer, parent);
    writer.flush();

    start = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      parse.execute(new StringWriter(), parent);
    }
    System.out.println((System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      parse.execute(new StringWriter(), parent);
    }
    System.out.println((System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      parse.execute(new StringWriter(), parent);
    }
    System.out.println((System.currentTimeMillis() - start));

    System.out.println("timeline.html evaluations:");
    for (int i = 0; i < 2; i++) {
      {
        start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          parse.execute(new NullWriter(), parent);
          total++;
          if (System.currentTimeMillis() - start > TIME * 1000) break;
        }
        System.out.println("NullWriter Serial: " + total / TIME + "/s");
      }
      {
        start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          parse.execute(new StringWriter(), parent);
          total++;
          if (System.currentTimeMillis() - start > TIME * 1000) break;
        }
        System.out.println("StringWriter Serial: " + total / TIME + "/s");
      }
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

}
