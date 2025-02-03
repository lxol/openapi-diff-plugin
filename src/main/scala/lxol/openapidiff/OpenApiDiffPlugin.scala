// openapi-diff-plugin/src/main/scala/lxol/openapidiff/OpenApiDiffPlugin.scala
package lxol.openapidiff

import sbt._
import sbt.Keys._
import java.io.{ ByteArrayOutputStream, File, OutputStreamWriter }
import scala.util.Using

import org.openapitools.openapidiff.core.OpenApiCompare
import org.openapitools.openapidiff.core.model.ChangedOpenApi
import org.openapitools.openapidiff.core.output.{ ConsoleRender, HtmlRender, JsonRender, MarkdownRender }
import _root_.io.swagger.v3.oas.models.OpenAPI
import _root_.io.swagger.v3.parser.OpenAPIV3Parser
import _root_.io.swagger.v3.parser.core.models.ParseOptions
import _root_.io.swagger.v3.core.util.Yaml
import _root_.io.swagger.v3.core.filter.SpecFilter

object OpenApiDiffPlugin extends AutoPlugin {

  // Expose keys for build.sbt users
  object autoImport {
    // Input spec files: they can be YAML or JSON and be located anywhere
    val inputSpec1 = settingKey[File]("Path to the first OpenAPI spec file (JSON or YAML)")
    val inputSpec2 = settingKey[File]("Path to the second OpenAPI spec file (JSON or YAML)")

    // Diff report formatting and output
    val diffReportFormat = settingKey[String]("Format of the diff report (text, html, markdown, json)")
    val diffReportOutput = settingKey[Option[File]]("Optional file to write the diff report")

    // Output files for the unfiltered and filtered specs (in YAML)
    val spec1UnfilteredOutput = settingKey[File]("Output file for the unfiltered first OpenAPI spec in YAML format")
    val spec1FilteredOutput   = settingKey[File]("Output file for the filtered first OpenAPI spec in YAML format")
    val spec2UnfilteredOutput = settingKey[File]("Output file for the unfiltered second OpenAPI spec in YAML format")
    val spec2FilteredOutput   = settingKey[File]("Output file for the filtered second OpenAPI spec in YAML format")

    // Main diff task: compare two specs
    val openapiDiff = taskKey[Unit]("Compare two OpenAPI spec files and generate a diff report")

    // New tasks: print the YAML of the first spec, unfiltered and filtered
    val printSpecUnfiltered = taskKey[Unit]("Print the unfiltered YAML of the first OpenAPI spec")
    val printSpecFiltered   = taskKey[Unit]("Print the filtered YAML of the first OpenAPI spec")
  }
  import autoImport._

  // Automatically enable this plugin for all projects
  override def trigger = allRequirements

  override def projectSettings: Seq[Setting[_]] = Seq(
    // Default settings for input files
    inputSpec1 := baseDirectory.value / "spec1.yaml",
    inputSpec2 := baseDirectory.value / "spec2.yaml",

    // Default diff report format and output file (placed in target/swagger)
    diffReportFormat := "markdown",
    diffReportOutput := Some(
      baseDirectory.value / "target" / "swagger" / s"diff-report.${diffReportFormat.value}"
    ),

    // Compute output file names based on the input spec file names.
    // The base name is the file name with its extension removed.
    spec1UnfilteredOutput := {
      val baseName = inputSpec1.value.getName.replaceFirst("\\.[^.]+$", "")
      baseDirectory.value / "target" / "swagger" / s"${baseName}.unfiltered.yaml"
    },
    spec1FilteredOutput := {
      val baseName = inputSpec1.value.getName.replaceFirst("\\.[^.]+$", "")
      baseDirectory.value / "target" / "swagger" / s"${baseName}.filtered.yaml"
    },
    spec2UnfilteredOutput := {
      val baseName = inputSpec2.value.getName.replaceFirst("\\.[^.]+$", "")
      baseDirectory.value / "target" / "swagger" / s"${baseName}.unfiltered.yaml"
    },
    spec2FilteredOutput := {
      val baseName = inputSpec2.value.getName.replaceFirst("\\.[^.]+$", "")
      baseDirectory.value / "target" / "swagger" / s"${baseName}.filtered.yaml"
    },

    // Main diff task: compares the two filtered specs and writes a diff report.
    openapiDiff := {
      val log = streams.value.log

      // Use the two input spec files
      val file1 = inputSpec1.value
      val file2 = inputSpec2.value
      val format = diffReportFormat.value.toLowerCase
      val reportOutput = diffReportOutput.value

      if (!file1.exists() || !file2.exists()) {
        sys.error(s"Missing spec files:\n- Spec 1: $file1\n- Spec 2: $file2")
      }

      val parseOptions = new ParseOptions()
      parseOptions.setResolve(true)
      parseOptions.setResolveFully(true)

      // Parse the OpenAPI spec files (they can be YAML or JSON)
      val spec1Unfiltered: OpenAPI =
        new OpenAPIV3Parser().read(file1.getAbsolutePath, null, parseOptions)
      val spec2Unfiltered: OpenAPI =
        new OpenAPIV3Parser().read(file2.getAbsolutePath, null, parseOptions)

      if (spec1Unfiltered == null || spec2Unfiltered == null) {
        sys.error("Failed to parse one or both OpenAPI spec files")
      }

      // Apply a custom filter to remove descriptions and examples
      val specFilter = new SpecFilter()
      val customFilter = new lxol.custom.filter.RemoveDescriptionAndExampleOpenAPISpecFilter()
      val spec1Filtered: OpenAPI =
        specFilter.filter(spec1Unfiltered, customFilter, null, null, null)
      val spec2Filtered: OpenAPI =
        specFilter.filter(spec2Unfiltered, customFilter, null, null, null)

      // Write the unfiltered and filtered specs to YAML files
      IO.write(spec1UnfilteredOutput.value, Yaml.pretty(spec1Unfiltered))
      IO.write(spec1FilteredOutput.value, Yaml.pretty(spec1Filtered))
      IO.write(spec2UnfilteredOutput.value, Yaml.pretty(spec2Unfiltered))
      IO.write(spec2FilteredOutput.value, Yaml.pretty(spec2Filtered))
      log.info(s"Wrote first spec (unfiltered) to: ${spec1UnfilteredOutput.value.getAbsolutePath}")
      log.info(s"Wrote first spec (filtered)   to: ${spec1FilteredOutput.value.getAbsolutePath}")
      log.info(s"Wrote second spec (unfiltered) to: ${spec2UnfilteredOutput.value.getAbsolutePath}")
      log.info(s"Wrote second spec (filtered)   to: ${spec2FilteredOutput.value.getAbsolutePath}")

      // Compare the two filtered specs
      val diff: ChangedOpenApi = OpenApiCompare.fromSpecifications(spec1Filtered, spec2Filtered)
      val renderer = format match {
        case "html"     => new HtmlRender()
        case "markdown" => new MarkdownRender()
        case "json"     => new JsonRender()
        case _          => new ConsoleRender()
      }

      // Render the diff report into a string
      val byteStream = new ByteArrayOutputStream()
      Using.resource(new OutputStreamWriter(byteStream)) { writer =>
        renderer.render(diff, writer)
      }
      val report = byteStream.toString("UTF-8")

      // Write diff report to a file (if set) or log it
      reportOutput match {
        case Some(file) =>
          IO.write(file, report)
          log.info(s"Diff report written to: ${file.getAbsolutePath}")
        case None =>
          log.info(s"OpenAPI Diff Report ($format):\n$report")
      }

      if (!diff.isCompatible) {
        log.error("OpenAPI Diff found breaking changes!")
        sys.error("Schemas are NOT compatible")
      } else if (diff.isUnchanged) {
        log.info("Schemas are identical")
      } else {
        log.info("Schemas have non-breaking changes")
      }
    },

    // Task to print the unfiltered YAML of the first spec
    printSpecUnfiltered := {
      val log = streams.value.log
      val file = inputSpec1.value
      if (!file.exists()) sys.error(s"Missing spec file: $file")
      val parseOptions = new ParseOptions()
      parseOptions.setResolve(true)
      parseOptions.setResolveFully(true)
      val spec: OpenAPI = new OpenAPIV3Parser().read(file.getAbsolutePath, null, parseOptions)
      if (spec == null) sys.error(s"Failed to parse spec file: $file")
      IO.write(spec1UnfilteredOutput.value, Yaml.pretty(spec))
      log.info("First Spec (Unfiltered):")
      log.info(Yaml.pretty(spec))
    },

    // Task to print the filtered YAML of the first spec
    printSpecFiltered := {
      val log = streams.value.log
      val file = inputSpec1.value
      if (!file.exists()) sys.error(s"Missing spec file: $file")
      val parseOptions = new ParseOptions()
      parseOptions.setResolve(true)
      parseOptions.setResolveFully(true)
      val spec: OpenAPI = new OpenAPIV3Parser().read(file.getAbsolutePath, null, parseOptions)
      if (spec == null) sys.error(s"Failed to parse spec file: $file")
      val specFilter = new SpecFilter()
      val customFilter = new lxol.custom.filter.RemoveDescriptionAndExampleOpenAPISpecFilter()
      val specFiltered: OpenAPI = specFilter.filter(spec, customFilter, null, null, null)
      IO.write(spec1FilteredOutput.value, Yaml.pretty(specFiltered))
      log.info("First Spec (Filtered):")
      log.info(Yaml.pretty(specFiltered))
    }
  )
}
