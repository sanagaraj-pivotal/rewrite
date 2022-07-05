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
}
