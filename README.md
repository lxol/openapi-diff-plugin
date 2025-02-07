# openapi-diff-plugin

**Version:** 0.1.4

An SBT plugin to compare two OpenAPI specification files and generate a diff report. It leverages the [OpenAPI Diff](https://github.com/OpenAPITools/openapi-diff) library to detect changes between two API specifications, and applies a custom filter to remove descriptions and examples for a cleaner comparison.

## Features

- **Spec Comparison:** Compare two OpenAPI spec files (YAML or JSON) and generate a detailed diff.
- **Custom Filtering:** Uses a custom filter (`RemoveDescriptionAndExampleOpenAPISpecFilter`) to strip out descriptions, summaries, and examples from the specs before comparison.
- **Multiple Report Formats:** Render diff reports in various formats including text, HTML, Markdown, or JSON.
- **Output Files:** Automatically writes unfiltered and filtered versions of the spec files to YAML.
- **SBT Tasks:** Provides convenient SBT tasks:
  - `openapiDiff` – Compares two OpenAPI spec files and generates the diff report.
  - `printSpecUnfiltered` – Prints (and writes to file) the unfiltered YAML version of the first spec.
  - `printSpecFiltered` – Prints (and writes to file) the filtered YAML version of the first spec.

## Installation

To use the plugin in your SBT project, add the following line to your `project/plugins.sbt`:

```scala
addSbtPlugin("org.lxol" % "openapi-diff-plugin" % "0.1.4")
```

## Usage

### Default Setup

By default, the plugin expects your OpenAPI spec files to be located in your project’s base directory:

- **Spec 1:** `spec1.yaml`
- **Spec 2:** `spec2.yaml`

And it will write reports and output files under the `target/swagger` directory.

### Running the Diff Task

In the SBT shell, run the main diff task:

```bash
> openapiDiff
```

This task will:

1. Check that both spec files exist.
2. Parse each file using the OpenAPIV3Parser.
3. Apply the custom filter to remove descriptions, summaries, and examples.
4. Write both the unfiltered and filtered versions of each spec to YAML files.
5. Compare the filtered specs using the OpenAPI Diff library.
6. Render a diff report (using the default format, Markdown) and either log it or write it to the file specified by the `diffReportOutput` key.

### Printing the Specs

To simply view the YAML (unfiltered or filtered) for the first spec, use:

```bash
> printSpecUnfiltered
> printSpecFiltered
```

## Configuration

The plugin exposes several SBT keys that you can override in your `build.sbt` file:

- **Input Spec Files:**
  - `inputSpec1`: File path for the first OpenAPI spec.
  - `inputSpec2`: File path for the second OpenAPI spec.
- **Diff Report Options:**
  - `diffReportFormat`: Format of the diff report. Acceptable values are `"text"`, `"html"`, `"markdown"`, or `"json"`. *(Default: `"markdown"`).*
  - `diffReportOutput`: Optional file path where the diff report will be written.
- **Output Files for Specs:**
  - `spec1UnfilteredOutput` / `spec1FilteredOutput`
  - `spec2UnfilteredOutput` / `spec2FilteredOutput`

### Example Custom Configuration

You can override the defaults in your `build.sbt` as follows:

```scala
// Specify custom paths for your spec files
inputSpec1 := baseDirectory.value / "path" / "to" / "your_spec1.yaml"
inputSpec2 := baseDirectory.value / "path" / "to" / "your_spec2.yaml"

// Choose the diff report format and output file
diffReportFormat := "markdown"
diffReportOutput := Some(baseDirectory.value / "target" / "swagger" / s"diff-report.${diffReportFormat.value}")

// Optionally, customize the output file names for the unfiltered/filtered specs
spec1UnfilteredOutput := baseDirectory.value / "target" / "swagger" / "spec1.unfiltered.yaml"
spec1FilteredOutput   := baseDirectory.value / "target" / "swagger" / "spec1.filtered.yaml"
spec2UnfilteredOutput := baseDirectory.value / "target" / "swagger" / "spec2.unfiltered.yaml"
spec2FilteredOutput   := baseDirectory.value / "target" / "swagger" / "spec2.filtered.yaml"
```

## How It Works

1. **Parsing:** The plugin uses the OpenAPIV3Parser to read and parse your OpenAPI specification files.
2. **Filtering:** It applies the `RemoveDescriptionAndExampleOpenAPISpecFilter` (located in `lxol.custom.filter`) to remove extra details (like descriptions and examples) that may not be needed in the diff.
3. **Output Generation:** The unfiltered and filtered specs are written out as YAML files.
4. **Diffing:** The plugin compares the filtered specs using the OpenAPI Diff library and selects the appropriate renderer based on the configured report format.
5. **Reporting:** Finally, it writes the diff report to the configured file (or logs it if no file is specified) and fails the build if breaking changes are detected.

## Contributing

Contributions, feedback, and bug reports are welcome! Please open an issue or submit a pull request if you have any suggestions or fixes.

## License

This project is licensed under the Apache License 2.0.
