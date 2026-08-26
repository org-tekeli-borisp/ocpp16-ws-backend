@file:JvmName("BootstrapClockPatch")

package org.tekeli.borisp.ocpp16.diagnostics

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.loading.ClassInjector
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.This
import net.bytebuddy.matcher.ElementMatchers
import org.mockito.Answers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mockStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.lang.instrument.ClassFileTransformer
import java.nio.file.Files
import java.nio.file.Path
import java.security.ProtectionDomain
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

fun toMillis(@This unit: TimeUnit?, @Argument(0) duration: Long): Long {
    val millisPerUnit = when (unit) {
        TimeUnit.NANOSECONDS -> 1_000_000L
        TimeUnit.MICROSECONDS -> 1_000L
        TimeUnit.MILLISECONDS -> 1L
        TimeUnit.SECONDS -> 1_000L
        TimeUnit.MINUTES -> 60_000L
        TimeUnit.HOURS -> 3_600_000L
        else -> 86_400_000L
    }
    val result = duration * millisPerUnit
    val target = System.getProperty("ocpp.test.clockpatch.target")
    if (target != null) {
        try {
            Path.of(target).toFile().setLastModified(System.currentTimeMillis() - result)
        } catch (e: Exception) {
        }
    }
    return result
}

class FileSystemStorageSurvivingMutantsTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var storage: FileSystemStorage

    @BeforeEach
    fun setup() {
        storage = FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L)
    }

    private class LogCapture {
        val messages = mutableListOf<String>()
        private val logger = Logger.getLogger("org.tekeli.borisp.ocpp16.diagnostics.FileSystemStorage")
        private val handler = object : Handler() {
            override fun publish(record: LogRecord) {
                record.message?.let { messages.add(it) }
            }

            override fun flush() {}
            override fun close() {}
        }

        init {
            logger.addHandler(handler)
        }

        fun stop() {
            logger.removeHandler(handler)
        }
    }

    companion object {
        private const val BOOTSTRAP_PATCH_CLASS = "org.tekeli.borisp.ocpp16.diagnostics.BootstrapClockPatch"
        private const val TARGET_PROPERTY = "ocpp.test.clockpatch.target"
    }

    private fun injectBootstrapHelper() {
        val instrumentation = ByteBuddyAgent.install()
        try {
            Class.forName(BOOTSTRAP_PATCH_CLASS, false, null)
            return
        } catch (e: ClassNotFoundException) {
        }
        val facadeClass = Class.forName(BOOTSTRAP_PATCH_CLASS)
        val whenMappingsName = BOOTSTRAP_PATCH_CLASS + "\$WhenMappings"
        val classes = mapOf(
            BOOTSTRAP_PATCH_CLASS to facadeClass.getResourceAsStream("BootstrapClockPatch.class")!!.readAllBytes(),
            whenMappingsName to facadeClass.getResourceAsStream("BootstrapClockPatch\$WhenMappings.class")!!.readAllBytes()
        )
        val injector = ClassInjector.UsingInstrumentation.of(
            Files.createTempDirectory("clockpatch-inject").toFile(),
            ClassInjector.UsingInstrumentation.Target.BOOTSTRAP,
            instrumentation
        )
        injector.injectRaw(classes)
    }

    @Test
    fun `deleteFile returns false when the underlying deleteIfExists returns false`() {
        val storedName = storage.uploadFile("CP-001", "x.log", byteArrayOf(1).inputStream())

        mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
            mock.`when`<Boolean> { Files.deleteIfExists(any<Path>()) }.thenReturn(false)
            assertFalse(storage.deleteFile("CP-001", storedName))
        }
    }

    @Test
    fun `cleanupExpired deletes a file whose lastModified equals the cutoff exactly`() {
        val base = tempDir.resolve("exact-boundary")
        val retentionDays = 30
        val boundaryStorage = FileSystemStorage(base.toString(), 1024)
        val counts = mutableListOf<Int>()

        injectBootstrapHelper()
        val instrumentation = ByteBuddyAgent.install()
        val patchClass = Class.forName(BOOTSTRAP_PATCH_CLASS)
        val patcher = object : ClassFileTransformer {
            override fun transform(
                loader: ClassLoader?,
                className: String?,
                classBeingRedefined: Class<*>?,
                protectionDomain: ProtectionDomain?,
                classfileBuffer: ByteArray?
            ): ByteArray? {
                if (className != "java/util/concurrent/TimeUnit") return null
                return try {
                    ByteBuddy()
                        .redefine<TimeUnit>(
                            TypeDescription.ForLoadedType.of(TimeUnit::class.java),
                            ClassFileLocator.ForClassLoader.ofSystemLoader()
                        )
                        .method(
                            ElementMatchers.named<MethodDescription>("toMillis")
                                .and(ElementMatchers.takesArgument(0, Long::class.javaPrimitiveType))
                        )
                        .intercept(MethodDelegation.to(patchClass))
                        .make()
                        .bytes
                } catch (e: Exception) {
                    null
                }
            }
        }
        instrumentation.addTransformer(patcher, true)
        try {
            instrumentation.retransformClasses(TimeUnit::class.java)
            repeat(3) {
                val cp = Files.createDirectories(base.resolve("CP-1"))
                val file = Files.write(cp.resolve("old.log"), byteArrayOf(1))
                System.setProperty(TARGET_PROPERTY, file.toString())
                try {
                    counts.add(boundaryStorage.cleanupExpired(retentionDays))
                } finally {
                    System.clearProperty(TARGET_PROPERTY)
                }
            }
        } finally {
            instrumentation.removeTransformer(patcher)
            instrumentation.retransformClasses(TimeUnit::class.java)
        }

        assertTrue(
            counts.count { it == 1 } >= 2,
            "a file whose lastModified equals the cutoff exactly must be deleted, counts: $counts"
        )
    }

    @Test
    fun `cleanupExpired logs the exception message when deleting an expired file fails`() {
        val capture = LogCapture()
        try {
            val base = tempDir.resolve("file-delete-fail")
            val cp = Files.createDirectories(base.resolve("CP-1"))
            val file = Files.write(cp.resolve("old.log"), byteArrayOf(1))
            file.toFile().setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(367))
            val failingStorage = FileSystemStorage(base.toString(), 1024)

            mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
                mock.`when`<Boolean> { Files.deleteIfExists(any<Path>()) }
                    .thenAnswer { throw IOException("delete-boom") }
                assertEquals(0, failingStorage.cleanupExpired(30))
            }
            assertTrue(
                capture.messages.any { it.contains("delete-boom") },
                "warning log must contain the exception message, got: ${capture.messages}"
            )
        } finally {
            capture.stop()
        }
    }

    @Test
    fun `cleanupExpired only attempts to delete empty top-level directories`() {
        val base = tempDir.resolve("dir-filter")
        val cp1 = Files.createDirectories(base.resolve("cp1"))
        val sub = Files.createDirectory(cp1.resolve("sub"))
        val cp2 = Files.createDirectories(base.resolve("cp2"))
        val stray = Files.write(base.resolve("stray.txt"), byteArrayOf(1))
        val old1 = Files.write(cp1.resolve("old.txt"), byteArrayOf(1))
        val recent = Files.write(sub.resolve("new.txt"), byteArrayOf(1))
        val old2 = Files.write(cp2.resolve("old2.txt"), byteArrayOf(1))
        val oldTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(367)
        old1.toFile().setLastModified(oldTime)
        old2.toFile().setLastModified(oldTime)
        recent.toFile().setLastModified(System.currentTimeMillis())
        stray.toFile().setLastModified(System.currentTimeMillis())
        val filteringStorage = FileSystemStorage(base.toString(), 1024)
        val attempts = mutableListOf<Path>()

        mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
            mock.`when`<Boolean> { Files.deleteIfExists(any<Path>()) }.thenAnswer { invocation ->
                val path = invocation.getArgument(0, Path::class.java)
                attempts.add(path)
                path.toFile().delete()
            }
            assertEquals(2, filteringStorage.cleanupExpired(30))
        }

        assertEquals(setOf(old1, old2, cp2), attempts.toSet())
    }

    @Test
    fun `cleanupExpired logs the exception message when deleting an empty directory fails`() {
        val capture = LogCapture()
        try {
            val base = tempDir.resolve("dir-delete-fail")
            val cp = Files.createDirectories(base.resolve("CP-1"))
            val file = Files.write(cp.resolve("old.log"), byteArrayOf(1))
            file.toFile().setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(367))
            val failingStorage = FileSystemStorage(base.toString(), 1024)

            mockStatic(Files::class.java, Answers.CALLS_REAL_METHODS).use { mock ->
                mock.`when`<Boolean> { Files.deleteIfExists(any<Path>()) }.thenAnswer { invocation ->
                    val path = invocation.getArgument(0, Path::class.java)
                    if (path == cp) {
                        throw IOException("dir-delete-boom")
                    }
                    path.toFile().delete()
                }
                failingStorage.cleanupExpired(30)
            }
            assertTrue(
                capture.messages.any { it.contains("dir-delete-boom") },
                "warning log must contain the exception message, got: ${capture.messages}"
            )
        } finally {
            capture.stop()
        }
    }

    @Test
    fun `listFiles returns two files ordered by uploadedAt descending`() {
        val dir = storage.ensureDirectory("CP-SORT-2")
        val a = Files.write(dir.resolve("A.log"), byteArrayOf(1))
        val b = Files.write(dir.resolve("B.log"), byteArrayOf(2))
        a.toFile().setLastModified(1000)
        b.toFile().setLastModified(2000)

        assertEquals(listOf("B.log", "A.log"), storage.listFiles("CP-SORT-2").map { it.storedName })
    }

    @Test
    fun `listFiles returns three files ordered by uploadedAt descending`() {
        val dir = storage.ensureDirectory("CP-SORT-3")
        val a = Files.write(dir.resolve("A.log"), byteArrayOf(1))
        val c = Files.write(dir.resolve("C.log"), byteArrayOf(2))
        val b = Files.write(dir.resolve("B.log"), byteArrayOf(3))
        a.toFile().setLastModified(1000)
        c.toFile().setLastModified(3000)
        b.toFile().setLastModified(2000)

        assertEquals(listOf("C.log", "B.log", "A.log"), storage.listFiles("CP-SORT-3").map { it.storedName })
    }

    @Test
    fun `listFiles returns an empty list when the CP directory does not exist`() {
        assertEquals(emptyList<DiagnosticsFileInfo>(), storage.listFiles("CP-ABSENT"))
    }
}
