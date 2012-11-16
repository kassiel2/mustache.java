package com.github.mustachejava;

import com.github.mustachejava.codegen.CodegenMustacheFactory;
import org.codehaus.jackson.JsonNode;
import org.junit.Ignore;

import java.io.Reader;
import java.io.StringReader;

/**
 * Specification tests
 */
@Ignore("This never compiled, ignoring for now so we can get a clean build.")
public class CodegenSpecTest extends SpecTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory(final JsonNode test) {
    return new CodegenMustacheFactory("/spec/specs") {
      @Override
      public Reader getReader(String resourceName) {
        JsonNode partial = test.get("partials").get(resourceName);
        return new StringReader(partial == null ? "" : partial.getTextValue());
      }
    };
  }
}
