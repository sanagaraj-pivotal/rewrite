package org.openrewrite.yaml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import org.openrewrite.Recipe
import java.nio.file.Path

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
        recipe = XChangePropertyKey(
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

    @Test
    fun nestedEntryEmptyPartialPathRemoved() = assertChanged(
        before = """
            unrelated.property: true
            management.metrics:
                binders:
                    files.enabled: true
        """,
        after = """
            unrelated.property: true
            management.metrics:
                enable:
                    process:
                        files: true
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1114")
    fun `change path to one path longer`() = assertChanged(
        recipe = XChangePropertyKey("a.b.c", "a.b.c.d", null, null),
        before = "a.b.c: true",
        after = "a.b.c.d: true"
    )

    @Test
    fun `change path to one path shorter`() = assertChanged(
        recipe = XChangePropertyKey("a.b.c.d", "a.b.c", null, null),
        before = "a.b.c.d: true",
        after = "a.b.c: true"
    )

    @Test
    fun changeOnlyMatchingFile(@TempDir tempDir: Path) {
        val matchingFile = tempDir.resolve("a.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("management.metrics.binders.files.enabled: true")
        }.toFile()
        val nonMatchingFile = tempDir.resolve("b.yml").apply {
            toFile().parentFile.mkdirs()
            toFile().writeText("management.metrics.binders.files.enabled: true")
        }.toFile()
        val recipe = XChangePropertyKey(
            "management.metrics.binders.files.enabled",
            "management.metrics.enable.process.files",
            null,
            "**/a.yml"
        )
        assertChanged(recipe = recipe, before = matchingFile, after = "management.metrics.enable.process.files: true")
        assertUnchanged(recipe = recipe, before = nonMatchingFile)
    }


    @ParameterizedTest
    @ValueSource(
        strings = [
            "acme.my-project.person.first-name",
            "acme.myProject.person.firstName",
            "acme.my_project.person.first_name",
        ]
    )
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    fun relaxedBinding(propertyKey: String) = assertChanged(
        recipe = ChangePropertyKey(propertyKey, "acme.my-project.person.changed-first-name-key", true, null),
        before = """
            unrelated.root: true
            acme.my-project:
                unrelated: true
                person:
                    unrelated: true
                    first-name: example
        """,
        after = """
            unrelated.root: true
            acme.my-project:
                unrelated: true
                person:
                    unrelated: true
                    changed-first-name-key: example
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    fun exactMatch() = assertChanged(
        recipe = ChangePropertyKey(
            "acme.my-project.person.first-name",
            "acme.my-project.person.changed-first-name-key",
            false,
            null
        ),
        before = """
            acme.myProject.person.firstName: example
            acme.my_project.person.first_name: example
            acme.my-project.person.first-name: example
        """,
        after = """
            acme.myProject.person.firstName: example
            acme.my_project.person.first_name: example
            acme.my-project.person.changed-first-name-key: example
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1249")
    @Test
    fun updateKeyAndDoesNotMergeToSibling() = assertChanged(
        recipe = ChangePropertyKey(
            "i",
            "a.b.c",
            false,
            null
        ),
        before = """
            a:
              b:
                f0: v0
                f1: v1
            i:
              f0: v0
              f1: v1
        """,
        after = """
            a:
              b:
                f0: v0
                f1: v1
            a.b.c:
              f0: v0
              f1: v1
        """
    )

}
