package com.github.lburgazzoli.gradle.plugin.karaf

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.util.zip.ZipFile

/**
 * Runs the plugin in a real Gradle build, so Groovy build scripts reach the DSL through
 * Gradle's closure-to-Action conversion, as users' builds do.
 */
class EndToEndTest {
    @TempDir
    lateinit var projectDir: File

    private fun build(
        buildScript: String,
        vararg tasks: String,
        gradleVersion: String = "8.14.4",
        buildFileName: String = "build.gradle",
        expectFailure: Boolean = false,
        settings: String = "rootProject.name = 'sample'\n"
    ): BuildResult {
        projectDir.resolve("settings.gradle").writeText(settings)
        projectDir.resolve(buildFileName).writeText(buildScript)
        return GradleRunner.create()
            .withPluginClasspath()
            .withGradleVersion(gradleVersion)
            .withProjectDir(projectDir)
            .withArguments(*tasks, "--stacktrace")
            .forwardOutput()
            .let { if (expectFailure) it.buildAndFail() else it.build() }
    }

    private fun groovyBuild(karafBlock: String, dependencies: String = "implementation 'commons-lang:commons-lang:2.6'") =
        """
        plugins {
            id 'java'
            id 'com.github.lburgazzoli.karaf'
        }

        group = 'com.example'
        version = '1.2.3'

        repositories {
            mavenCentral()
        }

        dependencies {
            $dependencies
        }

        $karafBlock
        """.trimIndent()

    private fun featuresXml() = projectDir.resolve("build/karaf/features/sample-1.2.3-features.xml").readText()

    @ParameterizedTest
    @ValueSource(strings = ["8.0.2", "8.10.2", "8.14.4", "9.9.2"])
    fun `groovy dsl writes nested blocks into the features file`(gradleVersion: String) {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        xsdVersion = '1.3'
                        repository 'mvn:org.foo/bar/1.0/xml/features'
                        feature {
                            name = 'f1'
                            description = 'd'
                            feature('dep-true') { dependency = true }
                            feature('dep-false') { dependency = false }
                            config('my.pid') { content = 'a=b' }
                            bundle('commons-lang') {
                                wrap { instruction 'Bundle-Version', '2.6' }
                            }
                            bundle('com.google.guava') {
                                remap { version = '19' }
                            }
                            conditional('webconsole') {
                                config('cond.pid') { content = 'x=y' }
                                bundle 'com.google.guava'
                            }
                            capability('osgi.service') { effective = 'active' }
                        }
                    }
                }
                """.trimIndent(),
                dependencies = """
                implementation 'commons-lang:commons-lang:2.6'
                implementation 'com.google.guava:guava:19.0'
                """.trimIndent()
            ),
            "generateFeatures",
            gradleVersion = gradleVersion
        )

        assertThat(featuresXml()).isEqualTo(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <features xmlns="http://karaf.apache.org/xmlns/features/v1.3.0" name="sample-1.2.3">
              <repository>mvn:org.foo/bar/1.0/xml/features</repository>
              <feature name="f1" version="1.2.3" description="d">
                <config name="my.pid">a=b</config>
                <feature dependency="true">dep-true</feature>
                <feature>dep-false</feature>
                <bundle>wrap:mvn:commons-lang/commons-lang/2.6${'$'}Bundle-Version=2.6</bundle>
                <conditional>
                  <condition>webconsole</condition>
                  <config name="cond.pid">x=y</config>
                  <bundle>mvn:com.google.guava/guava/19</bundle>
                </conditional>
                <capability>osgi.service;effective:='active';resolution:='mandatory'</capability>
              </feature>
            </features>
            """.trimIndent()
        )
    }

    @Test
    fun `remap with a map sets the dependency properties`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            bundle('commons-lang') {
                                remap version: '2.6.1', classifier: 'osgi'
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml())
            .contains("<bundle>mvn:commons-lang/commons-lang/2.6.1/jar/osgi</bundle>")
    }

    @Test
    fun `features without a name are all written, with no name attribute`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { description = 'a' }
                        feature { description = 'b' }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains(
            """
              <feature version="1.2.3" description="a">
                <bundle>mvn:commons-lang/commons-lang/2.6</bundle>
              </feature>
              <feature version="1.2.3" description="b">
                <bundle>mvn:commons-lang/commons-lang/2.6</bundle>
              </feature>
            """.trimIndent().prependIndent("  ")
        )
    }

    @Test
    fun `a feature-level includeProject overrides a false extension-level one`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        includeProject = false
                        feature {
                            name = 'f1'
                            includeProject = true
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<bundle>wrap:mvn:com.example/sample/1.2.3</bundle>")
    }

    @Test
    fun `outputFile accepts a path string`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        outputFile = 'build/custom/features.xml'
                        feature { name = 'f1' }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(projectDir.resolve("build/custom/features.xml")).content().contains("<feature name=\"f1\"")
    }

    @Test
    fun `configurations del removes a configuration added before it`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            configurations {
                                add 'runtimeClasspath'
                                del 'runtimeClasspath'
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<feature name=\"f1\" version=\"1.2.3\" />")
    }

    @Test
    fun `generateKar lays out the repository as Maven files`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { name = 'f1' }
                    }
                    kar {}
                }
                """.trimIndent()
            ),
            "generateKar"
        )

        val repo = projectDir.resolve("build/karaf/repo")
        assertThat(repo.walk().filter { it.isFile }.map { it.relativeTo(repo).invariantSeparatorsPath }.sorted().toList())
            .containsExactly(
                "com/example/sample/1.2.3/sample-1.2.3-features.xml",
                "commons-lang/commons-lang/2.6/commons-lang-2.6.jar"
            )
        ZipFile(projectDir.resolve("build/karaf/kar/sample-1.2.3.kar")).use { kar ->
            assertThat(kar.entries().asSequence().filter { !it.isDirectory }.map { it.name }.toList())
                .contains(
                    "repository/com/example/sample/1.2.3/sample-1.2.3-features.xml",
                    "repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar"
                )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["8.0.2", "8.10.2", "8.14.4", "9.9.2"])
    fun `kotlin dsl configures a feature`(gradleVersion: String) {
        build(
            """
            plugins {
                `java-library`
                id("com.github.lburgazzoli.karaf")
            }

            group = "com.example"
            version = "1.2.3"

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation("commons-lang:commons-lang:2.6")
            }

            karaf {
                features {
                    xsdVersion = "1.3.0"
                    feature {
                        name = "f1"
                        description = "d"
                        details = "details"
                        feature("dep-true") {
                            dependency = true
                        }
                        bundle {
                            match {
                                name = "commons-lang"
                            }
                            wrap = false
                        }
                        conditional("webconsole") {
                            config("cond.pid") {
                                content = "x=y"
                            }
                        }
                    }
                }
            }
            """.trimIndent(),
            "generateFeatures",
            gradleVersion = gradleVersion,
            buildFileName = "build.gradle.kts"
        )

        assertThat(featuresXml()).isEqualTo(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <features xmlns="http://karaf.apache.org/xmlns/features/v1.3.0" name="sample-1.2.3">
              <feature name="f1" version="1.2.3" description="d">
                <details>details</details>
                <feature dependency="true">dep-true</feature>
                <bundle>mvn:commons-lang/commons-lang/2.6</bundle>
                <conditional>
                  <condition>webconsole</condition>
                  <config name="cond.pid">x=y</config>
                </conditional>
              </feature>
            </features>
            """.trimIndent()
        )
    }

    /**
     * A build script is compiled without the Kotlin metadata version check, so only a compiled
     * class such as this buildSrc plugin shows whether the Kotlin compiler of that Gradle version
     * reads the plugin's classes.
     */
    @ParameterizedTest
    @ValueSource(strings = ["8.0.2", "9.9.2"])
    fun `a kotlin buildSrc plugin configures the features through the plugin's classes`(gradleVersion: String) {
        val pluginClasspath = javaClass.classLoader.getResourceAsStream("plugin-under-test-metadata.properties")!!
            .use { java.util.Properties().apply { load(it) } }
            .getProperty("implementation-classpath")
            .split(File.pathSeparator)
        projectDir.resolve("buildSrc/src/main/kotlin").mkdirs()
        projectDir.resolve("buildSrc/plugin-classpath.txt").writeText(pluginClasspath.joinToString("\n"))
        projectDir.resolve("buildSrc/build.gradle.kts").writeText(
            """
            plugins {
                `kotlin-dsl`
            }

            repositories {
                gradlePluginPortal()
            }

            dependencies {
                implementation(files(file("plugin-classpath.txt").readLines()))
            }
            """.trimIndent()
        )
        projectDir.resolve("buildSrc/src/main/kotlin/KarafConvention.kt").writeText(
            """
            import com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension
            import org.gradle.api.Plugin
            import org.gradle.api.Project

            class KarafConvention : Plugin<Project> {
                override fun apply(project: Project) {
                    project.pluginManager.apply("com.github.lburgazzoli.karaf")
                    project.extensions.getByType(KarafPluginExtension::class.java).features {
                        feature {
                            name = "f1"
                        }
                    }
                }
            }
            """.trimIndent()
        )

        build(
            """
            plugins {
                id 'java'
            }

            group = 'com.example'
            version = '1.2.3'

            apply plugin: KarafConvention

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'commons-lang:commons-lang:2.6'
            }
            """.trimIndent(),
            "generateFeatures",
            gradleVersion = gradleVersion
        )

        assertThat(featuresXml()).isEqualTo(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <features xmlns="http://karaf.apache.org/xmlns/features/v1.2.0" name="sample-1.2.3">
              <feature name="f1" version="1.2.3">
                <bundle>mvn:commons-lang/commons-lang/2.6</bundle>
              </feature>
            </features>
            """.trimIndent()
        )
    }

    @Test
    fun `reading karaf settings in a groovy script returns their values`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        name = "${'$'}{project.name}-${'$'}{version}"
                        outputFile = "build/out/${'$'}{name}.xml"
                        feature { name = 'f1' }
                    }
                    repo { enabled = false }
                }

                tasks.register('printKaraf') {
                    doLast {
                        if (karaf.repo.enabled || karaf.repo.isEnabled() || karaf.kar.isEnabled()) {
                            println 'repo or kar is enabled'
                        }
                        println "features file: ${'$'}{karaf.features.outputFile.name}"
                        println "repo dir: ${'$'}{karaf.repo.outputDir.name}"
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures", "printKaraf"
        ).also { result ->
            assertThat(result.output)
                .doesNotContain("repo or kar is enabled")
                .contains("features file: sample-1.2.3.xml", "repo dir: repo")
        }

        assertThat(projectDir.resolve("build/out/sample-1.2.3.xml")).content()
            .contains("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.2.0\" name=\"sample-1.2.3-1.2.3\">")
    }

    @Test
    fun `match with a map selects the bundle to configure`() {
        build(
            groovyBuild(
                """
                def artifact = 'commons-lang'
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            bundle {
                                match name: "${'$'}{artifact}"
                                wrap { instruction 'Bundle-Version', '2.6' }
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml())
            .contains("<bundle>wrap:mvn:commons-lang/commons-lang/2.6${'$'}Bundle-Version=2.6</bundle>")
    }

    @Test
    fun `remap with a file copies that file into the repository`() {
        projectDir.resolve("custom.jar").writeText("custom")
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            bundle('commons-lang') {
                                remap file: file('custom.jar')
                            }
                        }
                    }
                    repo {}
                }
                """.trimIndent()
            ),
            "generateRepo"
        )

        assertThat(projectDir.resolve("build/karaf/repo/commons-lang/commons-lang/2.6/commons-lang-2.6.jar"))
            .hasContent("custom")
    }

    @Test
    fun `configurations del removes one of two identical entries`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            configurations {
                                add 'runtimeClasspath'
                                add 'runtimeClasspath'
                                del 'runtimeClasspath'
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<bundle>mvn:commons-lang/commons-lang/2.6</bundle>")
    }

    @Test
    fun `generateRepo without a project group starts the path at the project name`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { name = 'f1' }
                    }
                    repo {}
                }
                """.trimIndent()
            ).replace("group = 'com.example'\n", ""),
            "generateRepo"
        )

        assertThat(projectDir.resolve("build/karaf/repo/sample/1.2.3/sample-1.2.3-features.xml")).exists()
    }

    @Test
    fun `features settings accept a number version and a File output`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        version = 2
                        outputFile = file('build/custom/features.xml')
                        feature { name = 'f1' }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(projectDir.resolve("build/custom/features.xml")).content()
            .contains("name=\"sample-2\"")
    }

    @ParameterizedTest
    @ValueSource(strings = ["8.0.2", "9.9.2"])
    fun `outputs follow a version and build directory changed after the plugin's afterEvaluate`(gradleVersion: String) {
        val result = build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { name = 'f1' }
                    }
                    repo {}
                }

                afterEvaluate {
                    version = '9.9'
                    buildDir = file('out')
                }

                tasks.register('printOutputs') {
                    doLast {
                        def relative = { File f -> projectDir.toPath().relativize(f.toPath()).toString().replace(File.separator, '/') }
                        println "features output: " + relative(tasks.generateFeatures.outputs.files.singleFile)
                        println "repo output: " + relative(tasks.generateRepo.outputs.files.singleFile)
                        configurations.archives.artifacts.each { println "artifact: " + relative(it.file) }
                    }
                }
                """.trimIndent()
            ),
            "generateRepo", "printOutputs",
            gradleVersion = gradleVersion
        )

        assertThat(projectDir.resolve("out/karaf/features/sample-9.9-features.xml")).exists()
        assertThat(result.output).contains(
            "features output: out/karaf/features/sample-9.9-features.xml",
            "repo output: out/karaf/repo",
            "artifact: out/karaf/features/sample-9.9-features.xml",
            "artifact: out/karaf/repo"
        )
    }

    @Test
    fun `feature configurations is a list a groovy script can change`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            assert configurations.size() == 1
                            configurations.clear()
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<feature name=\"f1\" version=\"1.2.3\" />")
    }

    @Test
    fun `empty optional settings are treated as unset`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            details = ''
                            configFile {
                                filename = ''
                                uri = 'mvn:org.foo/bar/1.0/cfg'
                            }
                            configFile {
                                filename = 'etc/a.cfg'
                                uri = ''
                            }
                            conditional('') {
                                config('cond.pid') { content = 'x=y' }
                            }
                        }
                    }
                    kar {
                        archiveName = ''
                    }
                }
                """.trimIndent()
            ),
            "generateKar"
        )

        assertThat(featuresXml()).contains(
            """
              <feature name="f1" version="1.2.3">
                <bundle>mvn:commons-lang/commons-lang/2.6</bundle>
                <conditional>
                  <config name="cond.pid">x=y</config>
                </conditional>
              </feature>
            """.trimIndent().prependIndent("  ")
        )
        assertThat(projectDir.resolve("build/karaf/kar/sample-1.2.3.kar")).exists()
    }

    @Test
    fun `a task configured with its own karaf block writes its own features file`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { name = 'main' }
                    }
                }

                tasks.register('otherFeatures', com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask) {
                    karaf {
                        features {
                            name = 'other'
                            feature { name = 'o1' }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures", "otherFeatures"
        )

        assertThat(featuresXml()).contains("<feature name=\"main\"").doesNotContain("o1")
        assertThat(projectDir.resolve("build/karaf/features/other-1.2.3-features.xml")).content()
            .contains("<feature name=\"o1\"")
            .doesNotContain("main")
    }

    @Test
    fun `a dependency on an unnamed feature takes its name from the dependency block`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { description = 'base' }
                        feature {
                            name = 'f2'
                            feature(karaf.features.featureDescriptors[0]) { name = 'base-name' }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<feature>base-name</feature>")
    }

    @Test
    fun `a dependency on an unnamed feature without a name fails the build`() {
        val result = build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { description = 'base' }
                        feature {
                            name = 'f2'
                            feature(karaf.features.featureDescriptors[0])
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures",
            expectFailure = true
        )

        assertThat(result.output).contains("cannot be referenced as a dependency")
    }

    @Test
    fun `a feature version reads back as a string`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            assert version == '1.2.3'
                            version = 2
                            assert version instanceof String
                            assert version == '2'
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<feature name=\"f1\" version=\"2\">")
    }

    @Test
    fun `a feature takes the project version set after it unless it sets its own`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { name = 'f1' }
                        feature {
                            name = 'f2'
                            version = '5'
                        }
                        feature {
                            name = 'f3'
                            version = null
                        }
                    }
                }

                version = '2.0'
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(projectDir.resolve("build/karaf/features/sample-2.0-features.xml")).content()
            .contains(
                "<feature name=\"f1\" version=\"2.0\">",
                "<feature name=\"f2\" version=\"5\">",
                "<feature name=\"f3\">"
            )
    }

    @Test
    fun `repo and kar tasks given another extension stage and pack its features`() {
        build(
            groovyBuild(
                """
                def otherKaraf = objects.newInstance(com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension, project)
                otherKaraf.features {
                    name = 'other'
                    feature { name = 'o1' }
                }
                otherKaraf.repo { outputDir = file('build/other-repo') }
                otherKaraf.kar { outputDir = file('build/other-kar') }

                def otherFeatures = tasks.register('otherFeatures', com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask) {
                    karaf(otherKaraf)
                }
                def otherRepo = tasks.register('otherRepo', com.github.lburgazzoli.gradle.plugin.karaf.repo.KarafRepoTask) {
                    karaf(otherKaraf)
                    dependsOn otherFeatures
                }
                tasks.register('otherKar', com.github.lburgazzoli.gradle.plugin.karaf.kar.KarafKarTask) {
                    karaf(otherKaraf)
                    dependsOn otherRepo
                }
                """.trimIndent()
            ),
            "otherKar"
        )

        ZipFile(projectDir.resolve("build/other-kar/other-1.2.3.kar")).use { kar ->
            assertThat(kar.entries().asSequence().filter { !it.isDirectory }.map { it.name }.toList())
                .contains(
                    "repository/com/example/other/1.2.3/other-1.2.3-features.xml",
                    "repository/commons-lang/commons-lang/2.6/commons-lang-2.6.jar"
                )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["8.0.2", "8.14.4"])
    fun `configuring the build resolves no configuration and realizes no unrelated task`(gradleVersion: String) {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature { name = 'f1' }
                    }
                }

                tasks.register('unused') {
                    throw new GradleException('the unused task was realized')
                }

                afterEvaluate {
                    dependencies {
                        runtimeOnly 'com.google.guava:guava:19.0'
                    }
                }
                """.trimIndent()
            ),
            "help",
            gradleVersion = gradleVersion
        )
    }

    @Test
    fun `a features task with its own karaf block declares and publishes the file it writes`() {
        val result = build(
            groovyBuild(
                """
                tasks.named('generateFeatures') {
                    karaf {
                        features {
                            name = 'other'
                            feature { name = 'o1' }
                        }
                    }
                }

                tasks.register('printOutputs') {
                    doLast {
                        def relative = { File f -> projectDir.toPath().relativize(f.toPath()).toString().replace(File.separator, '/') }
                        println "features output: " + relative(tasks.generateFeatures.outputs.files.singleFile)
                        configurations.archives.artifacts.each { println "artifact: " + relative(it.file) }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures", "printOutputs"
        )

        assertThat(projectDir.resolve("build/karaf/features/other-1.2.3-features.xml")).exists()
        assertThat(result.output).contains(
            "features output: build/karaf/features/other-1.2.3-features.xml",
            "artifact: build/karaf/features/other-1.2.3-features.xml"
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["8.0.2", "8.14.4", "9.9.2"])
    fun `a registered features task builds the war of a project it depends on`(gradleVersion: String) {
        projectDir.resolve("lib").mkdirs()
        projectDir.resolve("lib/build.gradle").writeText(
            """
            apply plugin: 'war'
            group = 'com.example'
            version = '1.2.3'
            """.trimIndent()
        )
        val result = build(
            groovyBuild(
                """
                tasks.register('otherFeatures', com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask) {
                    karaf {
                        features {
                            name = 'other'
                            feature { name = 'f1' }
                        }
                    }
                }
                """.trimIndent(),
                dependencies = "implementation project(':lib')"
            ),
            "otherFeatures",
            gradleVersion = gradleVersion,
            settings = "rootProject.name = 'sample'\ninclude 'lib'\n"
        )

        assertThat(result.task(":lib:war")).`as`("task :lib:war in the build").isNotNull
        assertThat(projectDir.resolve("build/karaf/features/other-1.2.3-features.xml")).content()
            .contains("<bundle>war:mvn:com.example/lib/1.2.3/war</bundle>")
    }

    @Test
    fun `includeProject set in a task's own karaf block adds the project bundle`() {
        build(
            groovyBuild(
                """
                tasks.named('generateFeatures') {
                    karaf {
                        features {
                            includeProject = true
                            feature { name = 'f1' }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<bundle>wrap:mvn:com.example/sample/1.2.3</bundle>")
    }

    @Test
    fun `a task accepts settings created with new KarafPluginExtension`() {
        build(
            groovyBuild(
                """
                def otherKaraf = new com.github.lburgazzoli.gradle.plugin.karaf.KarafPluginExtension(project)
                otherKaraf.features.name = 'other'
                otherKaraf.features.feature { name = 'o1' }

                tasks.register('otherFeatures', com.github.lburgazzoli.gradle.plugin.karaf.features.KarafFeaturesTask) {
                    karaf(otherKaraf)
                }
                """.trimIndent()
            ),
            "otherFeatures"
        )

        assertThat(projectDir.resolve("build/karaf/features/other-1.2.3-features.xml")).content()
            .contains("<feature name=\"o1\"")
    }

    @Test
    fun `remap and match accept interpolated keys`() {
        build(
            groovyBuild(
                """
                def versionKey = 'version'
                def nameKey = 'name'
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            bundle {
                                match([("${'$'}{nameKey}"): 'commons-lang'])
                                remap([("${'$'}{versionKey}"): '2.6.1'])
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<bundle>mvn:commons-lang/commons-lang/2.6.1</bundle>")
    }

    @Test
    fun `bundle instructions accept interpolated values`() {
        build(
            groovyBuild(
                """
                def bundleVersion = '2.6'
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            bundle('commons-lang') {
                                wrap {
                                    instruction(['Bundle-Version': "${'$'}{bundleVersion}"])
                                }
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml())
            .contains("<bundle>wrap:mvn:commons-lang/commons-lang/2.6${'$'}Bundle-Version=2.6</bundle>")
    }

    @Test
    fun `hasClassifier() in a groovy remap block is true for an empty classifier and false for none`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        includeProject = true
                        feature {
                            name = 'f1'
                            bundle('com.example:sample') {
                                remap { name = hasClassifier() ? 'with-classifier' : 'without-classifier' }
                            }
                            bundle('commons-lang') {
                                remap { name = hasClassifier() ? 'with-classifier' : 'without-classifier' }
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml())
            .`as`("the project jar has an empty classifier, and 0.5.7 counted it as a classifier")
            .contains(
                "<bundle>mvn:com.example/with-classifier/1.2.3</bundle>",
                "<bundle>mvn:commons-lang/without-classifier/2.6</bundle>"
            )
    }

    @Test
    fun `configuration given a missing configuration keeps the feature's default configuration`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            configuration project.configurations.findByName('absent')
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<bundle>mvn:commons-lang/commons-lang/2.6</bundle>")
    }

    @Test
    fun `configurations add and del given a missing configuration change nothing`() {
        build(
            groovyBuild(
                """
                karaf {
                    features {
                        feature {
                            name = 'f1'
                            configurations {
                                add project.configurations.findByName('absent')
                                add 'runtimeClasspath'
                                del project.configurations.findByName('absent')
                            }
                        }
                    }
                }
                """.trimIndent()
            ),
            "generateFeatures"
        )

        assertThat(featuresXml()).contains("<bundle>mvn:commons-lang/commons-lang/2.6</bundle>")
    }

    @Test
    fun `a groovy script builds dependency matchers through the model's static factories`() {
        val result = build(
            groovyBuild(
                """
                import com.github.lburgazzoli.gradle.plugin.karaf.features.model.BundleDescriptor
                import com.github.lburgazzoli.gradle.plugin.karaf.features.model.Dependency
                import com.github.lburgazzoli.gradle.plugin.karaf.features.model.DependencyMatcher

                tasks.register('printMatches') {
                    doLast {
                        def lang = new Dependency()
                        lang.group = 'commons-lang'
                        lang.name = 'commons-lang'
                        def guava = new Dependency()
                        guava.group = 'com.google.guava'
                        guava.name = 'guava'
                        def matcher = DependencyMatcher.from('commons-lang:commons-lang')
                        def bundle = BundleDescriptor.fromPattern('commons-lang:commons-lang')
                        println "from: " + [matcher.matches(lang), matcher.matches(guava)]
                        println "fromPattern: " + [bundle.matcher.matches(lang), bundle.matcher.matches(guava)]
                    }
                }
                """.trimIndent()
            ),
            "printMatches"
        )

        assertThat(result.output).contains("from: [true, false]", "fromPattern: [true, false]")
    }
}
