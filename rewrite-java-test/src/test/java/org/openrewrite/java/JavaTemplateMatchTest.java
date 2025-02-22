/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateMatchTest implements RewriteTest {

    // IMPORTANT: This test needs to stay at the top, so that the name of `JavaTemplateMatchTest$1_Equals1` matches the expectations
    @DocumentExample
    @SuppressWarnings("ConstantValue")
    @Test
    void matchBinaryUsingCompile() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              // matches manually written class JavaTemplateMatchTest$1_Equals1 below
              private final JavaTemplate template = JavaTemplate.compile(this, "Equals1", (Integer i) -> 1 == i).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return template.matches(binary) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 3;

                  boolean b3 = 2 == 1;
              }
              """,
            """
              class Test {
                  boolean b1 = /*~~>*/1 == 2;
                  boolean b2 = /*~~>*/1 == 3;

                  boolean b3 = 2 == 1;
              }
              """
          ));
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void matchBinary() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(this::getCursor, "1 == #{any(int)}").build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return template.matches(binary) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 3;

                  boolean b3 = 2 == 1;
              }
              """,
            """
              class Test {
                  boolean b1 = /*~~>*/1 == 2;
                  boolean b2 = /*~~>*/1 == 3;

                  boolean b3 = 2 == 1;
              }
              """
          ));
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void extractParameterUsingMatcher() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate template = JavaTemplate.builder(this::getCursor, "1 == #{any(int)}").build();
              final JavaTemplate replacement = JavaTemplate.builder(this::getCursor, "Objects.equals(#{any()}, 1)")
                .imports("java.util.Objects")
                .build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  JavaTemplate.Matcher matcher = template.matcher(binary);
                  if (matcher.find()) {
                      maybeAddImport("java.util.Objects");
                      return binary.withTemplate(replacement, binary.getCoordinates().replace(), matcher.parameter(0));
                  }
                  return super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  boolean b1 = 1 == 2;
                  boolean b2 = 1 == 3;

                  boolean b3 = 2 == 1;
              }
              """,
            """
              import java.util.Objects;

              class Test {
                  boolean b1 = Objects.equals(2, 1);
                  boolean b2 = Objects.equals(3, 1);

                  boolean b3 = 2 == 1;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"ObviousNullCheck"})
    void matchAgainstQualifiedReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate miTemplate = JavaTemplate.builder(this::getCursor, "java.util.Objects.requireNonNull(#{any(String)})").build();
              private final JavaTemplate faTemplate = JavaTemplate.builder(this::getCursor, "java.util.regex.Pattern.UNIX_LINES").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return miTemplate.matches(method) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.Import) {
                      return fieldAccess;
                  }
                  return faTemplate.matches(fieldAccess) ? SearchResult.found(fieldAccess) : super.visitFieldAccess(fieldAccess, ctx);
              }

              @Override
              public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                  if (ident.getFieldType() == null) {
                      return ident;
                  }
                  return faTemplate.matches(ident) ? SearchResult.found(ident) : super.visitIdentifier(ident, ctx);
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = java.util.Objects.requireNonNull("");
                  String s2 = Objects.requireNonNull("");
                  String s3 = requireNonNull("");

                  int i1 = java.util.regex.Pattern.UNIX_LINES;
                  int i2 = Pattern.UNIX_LINES;
                  int i3 = UNIX_LINES;
              }
              """,
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = /*~~>*/java.util.Objects.requireNonNull("");
                  String s2 = /*~~>*/Objects.requireNonNull("");
                  String s3 = /*~~>*/requireNonNull("");

                  int i1 = /*~~>*/java.util.regex.Pattern.UNIX_LINES;
                  int i2 = /*~~>*/Pattern.UNIX_LINES;
                  int i3 = /*~~>*/UNIX_LINES;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"ObviousNullCheck"})
    void matchAgainstUnqualifiedReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate miTemplate = JavaTemplate.builder(this::getCursor, "Objects.requireNonNull(#{any(String)})")
                .imports("java.util.Objects").build();
              private final JavaTemplate faTemplate = JavaTemplate.builder(this::getCursor, "Pattern.UNIX_LINES")
                .imports("java.util.regex.Pattern").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return miTemplate.matches(method) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.Import) {
                      return fieldAccess;
                  }
                  return faTemplate.matches(fieldAccess) ? SearchResult.found(fieldAccess) : super.visitFieldAccess(fieldAccess, ctx);
              }

              @Override
              public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                  if (ident.getFieldType() == null) {
                      return ident;
                  }
                  return faTemplate.matches(ident) ? SearchResult.found(ident) : super.visitIdentifier(ident, ctx);
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = java.util.Objects.requireNonNull("");
                  String s2 = Objects.requireNonNull("");
                  String s3 = requireNonNull("");

                  int i1 = java.util.regex.Pattern.UNIX_LINES;
                  int i2 = Pattern.UNIX_LINES;
                  int i3 = UNIX_LINES;
              }
              """,
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = /*~~>*/java.util.Objects.requireNonNull("");
                  String s2 = /*~~>*/Objects.requireNonNull("");
                  String s3 = /*~~>*/requireNonNull("");

                  int i1 = /*~~>*/java.util.regex.Pattern.UNIX_LINES;
                  int i2 = /*~~>*/Pattern.UNIX_LINES;
                  int i3 = /*~~>*/UNIX_LINES;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"ObviousNullCheck"})
    void matchAgainstStaticallyImportedReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate miTemplate = JavaTemplate.builder(this::getCursor, "requireNonNull(#{any(String)})")
                .staticImports("java.util.Objects.requireNonNull").build();
              private final JavaTemplate faTemplate = JavaTemplate.builder(this::getCursor, "UNIX_LINES")
                .staticImports("java.util.regex.Pattern.UNIX_LINES").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return miTemplate.matches(method) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.Import) {
                      return fieldAccess;
                  }
                  return faTemplate.matches(fieldAccess) ? SearchResult.found(fieldAccess) : super.visitFieldAccess(fieldAccess, ctx);
              }

              @Override
              public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                  if (ident.getFieldType() == null) {
                      return ident;
                  }
                  return faTemplate.matches(ident) ? SearchResult.found(ident) : super.visitIdentifier(ident, ctx);
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = java.util.Objects.requireNonNull("");
                  String s2 = Objects.requireNonNull("");
                  String s3 = requireNonNull("");

                  int i1 = java.util.regex.Pattern.UNIX_LINES;
                  int i2 = Pattern.UNIX_LINES;
                  int i3 = UNIX_LINES;
              }
              """,
            """
              import java.util.Objects;
              import java.util.regex.Pattern;

              import static java.util.Objects.requireNonNull;
              import static java.util.regex.Pattern.UNIX_LINES;

              class Test {
                  String s1 = /*~~>*/java.util.Objects.requireNonNull("");
                  String s2 = /*~~>*/Objects.requireNonNull("");
                  String s3 = /*~~>*/requireNonNull("");

                  int i1 = /*~~>*/java.util.regex.Pattern.UNIX_LINES;
                  int i2 = /*~~>*/Pattern.UNIX_LINES;
                  int i3 = /*~~>*/UNIX_LINES;
              }
              """
          ));
    }

    @Test
    @SuppressWarnings({"UnnecessaryCallToStringValueOf", "RedundantCast"})
    void matchCompatibleTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(this::getCursor, "#{any(long)}").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return method.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(method) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  void m() {
                      System.out.println(new Object().hashCode());
                      System.out.println((int) new Object().hashCode());
                      System.out.println(Long.parseLong("123"));
                      System.out.println(String.valueOf(Long.parseLong("123")));

                      System.out.println(new Object());
                      System.out.println(1L);
                  }
              }
              """,
            """
              class Test {
                  void m() {
                      System.out.println(/*~~>*/new Object().hashCode());
                      System.out.println((int) /*~~>*/new Object().hashCode());
                      System.out.println(/*~~>*/Long.parseLong("123"));
                      System.out.println(String.valueOf(/*~~>*/Long.parseLong("123")));

                      System.out.println(new Object());
                      System.out.println(1L);
                  }
              }
              """)
        );
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void matchMethodInvocationParameter() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                this::getCursor,
                "#{any(java.sql.Statement)}.executeUpdate(#{any(java.lang.String)})"
              ).build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  return method.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(method) ? SearchResult.found(method) : super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              import java.sql.*;
              class Test {
                  void m(StringBuilder sb) throws SQLException {
                      try (Connection c = null) {
                          try (Statement s = c.createStatement()) {
                              s.executeUpdate("foo");
                              s.executeUpdate(sb.toString());
                          }
                      }
                  }
              }
              """,
            """
              import java.sql.*;
              class Test {
                  void m(StringBuilder sb) throws SQLException {
                      try (Connection c = null) {
                          try (Statement s = c.createStatement()) {
                              /*~~>*/s.executeUpdate("foo");
                              /*~~>*/s.executeUpdate(sb.toString());
                          }
                      }
                  }
              }
              """)
        );
    }

    @Test
    void matchExpressionInAssignmentOperation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                this::getCursor,
                "1 + 1"
              ).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return binary.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(binary) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  int m() {
                      int i = 1;
                      i += 1 + 1;
                      return i;
                  }
              }
              """,
            """
              class Test {
                  int m() {
                      int i = 1;
                      i += /*~~>*/1 + 1;
                      return i;
                  }
              }
              """)
        );
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void matchExpressionInThrow() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                this::getCursor,
                "\"a\" + \"b\""
              ).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return template.matches(binary) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              class Test {
                  int m() {
                      if (true)
                          throw new IllegalArgumentException("a" + "b");
                      else
                          return ("a" + "b").length();
                  }
                  int f = 1;
              }
              """,
            """
              class Test {
                  int m() {
                      if (true)
                          throw new IllegalArgumentException(/*~~>*/"a" + "b");
                      else
                          return (/*~~>*/"a" + "b").length();
                  }
                  int f = 1;
              }
              """)
        );
    }

    @Test
    void matchExpressionInAnnotationAssignment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder(
                this::getCursor,
                "\"a\" + \"b\""
              ).build();

              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return binary.getMarkers().findFirst(SearchResult.class).isEmpty() && template.matches(binary) ? SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          java(
            """
              @SuppressWarnings(value = {"a" + "b", "c"})
              class Test {
              }
              """,
            """
              @SuppressWarnings(value = {/*~~>*/"a" + "b", "c"})
              class Test {
              }
              """)
        );
    }
}

/**
 * This class looks like a class which would be generated by the `rewrite-templating` annotation processor
 * and is used by the test {@link JavaTemplateMatchTest#matchBinaryUsingCompile()}.
 */
@SuppressWarnings("unused")
class JavaTemplateMatchTest$1_Equals1 {
    static JavaTemplate.Builder getTemplate(JavaVisitor<?> visitor) {
        return JavaTemplate.builder(visitor::getCursor, "1 == #{any(int)}");
    }

}
