package org.tekeli.borisp.ocpp16.tools

import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow
import kotlin.system.exitProcess
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource

data class MethodCrapMetric(
    val className: String,
    val methodName: String,
    val descriptor: String,
    val complexity: Int,
    val coverage: Double,
    val crap: Double
)

object CrapMetricsAnalyzer {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size > 2) {
            usage()
        }

        val reportPath = args.getOrNull(0) ?: "target/site/jacoco/jacoco.xml"
        val threshold = args.getOrNull(1)?.toDoubleOrNull() ?: 30.0
        val reportFile = File(reportPath)

        if (!reportFile.exists()) {
            System.err.println("JaCoCo XML report not found: ${reportFile.path}")
            System.err.println("Run first: ./mvnw test")
            exitProcess(2)
        }

        val document = parseReport(reportFile)
        val metrics = extractMetrics(document)
        printReport(metrics, threshold)
    }

    private fun usage(): Nothing {
        println(
            """
            Usage:
              ./mvnw verify

            Direct Maven execution:
              ./mvnw test exec:java@crap-metrics

            Arguments:
              [jacocoXmlPath] [threshold]
            """.trimIndent()
        )
        exitProcess(2)
    }

    private fun parseReport(reportFile: File): Document {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        }

        val documentBuilder = documentBuilderFactory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }

        return documentBuilder.parse(reportFile).apply { documentElement.normalize() }
    }

    private fun extractMetrics(document: Document): List<MethodCrapMetric> {
        val metrics = mutableListOf<MethodCrapMetric>()
        val classNodes = document.getElementsByTagName("class")

        for (classIndex in 0 until classNodes.length) {
            val classElement = classNodes.item(classIndex) as? Element ?: continue
            val className = classElement.getAttribute("name").replace("/", ".")
            val methodNodes = classElement.getElementsByTagName("method")

            for (methodIndex in 0 until methodNodes.length) {
                val methodElement = methodNodes.item(methodIndex) as? Element ?: continue
                toMetric(className, methodElement)?.let { metrics += it }
            }
        }

        return metrics
    }

    private fun toMetric(className: String, methodElement: Element): MethodCrapMetric? {
        val methodName = methodElement.getAttribute("name")
        if (isSynthetic(methodName)) {
            return null
        }

        val complexity = cyclomaticComplexity(methodElement)
        val coverage = instructionCoverage(methodElement)

        return MethodCrapMetric(
            className = className,
            methodName = methodName,
            descriptor = methodElement.getAttribute("desc"),
            complexity = complexity,
            coverage = coverage,
            crap = crapScore(complexity, coverage)
        )
    }

    private fun isSynthetic(methodName: String): Boolean =
        methodName == "<init>" ||
                methodName == "<clinit>" ||
                methodName.startsWith("lambda$") ||
                methodName.startsWith("access$")

    private fun counterValues(method: Element, type: String): Pair<Int, Int> {
        val counters = method.getElementsByTagName("counter")

        for (index in 0 until counters.length) {
            val counter = counters.item(index) as? Element ?: continue

            if (counter.getAttribute("type") == type) {
                val missed = counter.getAttribute("missed").toIntOrNull() ?: 0
                val covered = counter.getAttribute("covered").toIntOrNull() ?: 0

                return missed to covered
            }
        }

        return 0 to 0
    }

    private fun instructionCoverage(method: Element): Double {
        val (missed, covered) = counterValues(method, "INSTRUCTION")
        val total = missed + covered

        return if (total == 0) {
            0.0
        } else {
            covered.toDouble() / total.toDouble()
        }
    }

    private fun cyclomaticComplexity(method: Element): Int {
        val (missed, covered) = counterValues(method, "COMPLEXITY")

        return maxOf(missed + covered, 1)
    }

    private fun crapScore(complexity: Int, coverage: Double): Double {
        return complexity.toDouble().pow(2.0) * (1.0 - coverage).pow(3.0) + complexity
    }

    private fun printReport(metrics: List<MethodCrapMetric>, threshold: Double) {
        val sortedMetrics = metrics.sortedByDescending { it.crap }
        val failingMetrics = sortedMetrics.filter { it.crap >= threshold }

        printTable(sortedMetrics, threshold)

        if (failingMetrics.isNotEmpty()) {
            printFailures(failingMetrics)
            exitProcess(1)
        }

        println()
        println("Result: OK")
    }

    private fun printTable(sortedMetrics: List<MethodCrapMetric>, threshold: Double) {
        println()
        println("CRAP Metrics")
        println("=".repeat(140))
        println("%8s %6s %8s  %s".format("CRAP", "Cmplx", "Cov%", "Method"))
        println("-".repeat(140))

        for (metric in sortedMetrics) {
            val marker = if (metric.crap >= threshold) "!" else " "
            val coveragePercent = metric.coverage * 100.0

            println(
                "%8.2f %6d %7.1f%% %s %s.%s%s".format(
                    metric.crap,
                    metric.complexity,
                    coveragePercent,
                    marker,
                    metric.className,
                    metric.methodName,
                    metric.descriptor
                )
            )
        }

        println("-".repeat(140))
        println("Threshold: %.2f".format(threshold))
    }

    private fun printFailures(failingMetrics: List<MethodCrapMetric>) {
        println()
        println("Result: FAILED")
        println("Methods exceeding threshold: ${failingMetrics.size}")

        println()
        println("Top failing methods:")
        failingMetrics
            .take(10)
            .forEach {
                println(
                    "  %.2f - %s.%s%s".format(
                        it.crap,
                        it.className,
                        it.methodName,
                        it.descriptor
                    )
                )
            }
    }
}