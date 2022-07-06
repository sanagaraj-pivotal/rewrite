package org.openrewrite.yaml

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe

class XChangePropertyKeyTest : YamlRecipeTest {
    override val recipe: Recipe
        get() = XChangePropertyKey(
            "management.metrics.binders.*.enabled",
            "management.metrics.enable.process.files",
            null,
            null
        )

    @Issue("https://github.com/openrewrite/rewrite/issues/1873")
    @Test
    fun `shorter new key with indented config`() = assertChanged(
        recipe = XChangePropertyKey("a.b.c.d.e", "x.y", null, null),
        before =
        """
        a:
          b:
            c:
              d:
                e:
                  child: true
        """,
        after = """

        x:
          y:
            child: true
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1873")
    @Test
    fun `longer new key with indented config`() = assertChanged(
        recipe = XChangePropertyKey("x.y", "a.b.c.d.e",  null, null),
        before =
        """
        x:
          y:
            child: true
        """,
        after = """

        a:
          b:
            c:
              d:
                e:
                  child: true
        """
    )

    @Test
    fun singleEntry() = assertChanged(
        before = "management.metrics.binders.files.enabled: true",
        after = "management.metrics.enable.process.files: true"
    )

    @Test
    fun wildcardHierarchyEntry() = assertChanged(
        before = """
            management:
              metrics:
                binders:
                  files:
                    enabled: true""",
        after = """
        management:
          metrics:
            enable:
              process:
                files: true"""
    )

    @Test
    fun nestedEntry() = assertChanged(
        recipe = ChangePropertyKey(
            "management.metrics.binders.files.enabled",
            "management.metrics.enable.process.files",
            null,
            null
        ),
        before = """
            unrelated.property: true
            management.metrics:
                binders:
                    jvm.enabled: true
                    files.enabled: true
        """,
        after = """
            unrelated.property: true
            management.metrics:
                binders:
                    jvm.enabled: true
                enable:
                  process:
                    files: true
        """
    )


}
