package lxol.custom.filter

import _root_.io.swagger.v3.core.filter.OpenAPISpecFilter
import _root_.io.swagger.v3.core.model.ApiDescription
import _root_.io.swagger.v3.oas.models._
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.{Parameter, RequestBody}
import io.swagger.v3.oas.models.responses.ApiResponse
import java.util
import java.util.Optional
import scala.jdk.CollectionConverters._

class RemoveDescriptionAndExampleOpenAPISpecFilter extends OpenAPISpecFilter {

  // (A) Remove the top-level "description" in `info`
  override def filterOpenAPI(
      openAPI: OpenAPI,
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[OpenAPI] = {
    if (openAPI != null && openAPI.getInfo != null) {
      openAPI.getInfo.setDescription(null)
    }
    Optional.ofNullable(openAPI)
  }

  // (B) Pass through PathItems (do not filter out)
  override def filterPathItem(
      pathItem: PathItem,
      api: ApiDescription,
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[PathItem] =
    Optional.ofNullable(pathItem)

  // (C) Remove an operation's description and summary
  override def filterOperation(
      operation: Operation,
      api: ApiDescription,
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[Operation] = {
    if (operation != null) {
      operation.setDescription(null)
      operation.setSummary(null)
    }
    Optional.ofNullable(operation)
  }

  // (D) Remove a parameter's description and example
  override def filterParameter(
      parameter: Parameter,
      operation: Operation,
      api: ApiDescription,
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[Parameter] = {
    if (parameter != null) {
      parameter.setDescription(null)
      parameter.setExample(null)
      if (parameter.getExamples != null) {
        parameter.getExamples.clear()
      }
    }
    Optional.ofNullable(parameter)
  }

  // (E) Remove a RequestBody's description and clear examples
  override def filterRequestBody(
      requestBody: RequestBody,
      operation: Operation,
      api: ApiDescription,
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[RequestBody] = {
    if (requestBody != null) {
      requestBody.setDescription(null)
      val content = requestBody.getContent()
      if (content != null) {
        content.values().forEach { mediaType =>
          if (mediaType != null) {
            mediaType.setExample(null)
            mediaType.setExamples(null)
            val schema = mediaType.getSchema()
            if (schema != null) {
              removeSchemaDescriptionsAndExamples(schema)
            }
          }
        }
      }
    }
    Optional.ofNullable(requestBody)
  }

  // (F) Remove a Response's description and clear examples
  override def filterResponse(
      response: ApiResponse,
      operation: Operation,
      api: ApiDescription,
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[ApiResponse] = {
    if (response != null) {
      response.setDescription(null)
      val content = response.getContent()
      if (content != null) {
        content.values().forEach { mediaType =>
          if (mediaType != null) {
            mediaType.setExample(null)
            mediaType.setExamples(null)
            val schema = mediaType.getSchema()
            if (schema != null) {
              removeSchemaDescriptionsAndExamples(schema)
            }
          }
        }
      }
    }
    Optional.ofNullable(response)
  }

  // (G) Remove descriptions and examples from an entire Schema
  override def filterSchema(
      schema: Schema[_],
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[Schema[_]] = {
    if (schema != null) {
      removeSchemaDescriptionsAndExamples(schema)
    }
    Optional.ofNullable(schema)
  }

  // (H) Remove description and example from a schema property
  override def filterSchemaProperty(
      schema: Schema[_],
      propertySchema: Schema[_],
      propertyName: String,
      params: util.Map[String, util.List[String]],
      cookies: util.Map[String, String],
      headers: util.Map[String, util.List[String]]
  ): Optional[Schema[_]] = {
    if (propertySchema != null) {
      propertySchema.setDescription(null)
      propertySchema.setExample(null)
      if (propertySchema.getExamples != null) {
        propertySchema.getExamples.clear()
      }
      removeSchemaDescriptionsAndExamples(propertySchema)
    }
    Optional.ofNullable(propertySchema)
  }

  // (I) Do not remove unreferenced definitions
  override def isRemovingUnreferencedDefinitions(): Boolean = false

  // Helper method to remove descriptions, examples,
  // **and** any `type: "object"` recursively.
  private def removeSchemaDescriptionsAndExamples(schema: Schema[_]): Unit = {
    // Clear out type if it is "object"
    if ("object".equalsIgnoreCase(schema.getType)) {
      schema.setType(null)
    }

    // Remove description and examples
    schema.setDescription(null)
    schema.setExample(null)
    if (schema.getExamples != null) {
      schema.getExamples.clear()
    }

    // For each property, do the same
    if (schema.getProperties != null) {
      schema.getProperties.values().forEach { propSchema =>
        if (propSchema != null) {
          if ("object".equalsIgnoreCase(propSchema.getType)) {
            propSchema.setType(null)
          }
          propSchema.setDescription(null)
          propSchema.setExample(null)
          if (propSchema.getExamples != null) {
            propSchema.getExamples.clear()
          }
          removeSchemaDescriptionsAndExamples(propSchema)
        }
      }
    }

    // Recursively check items
    schema.getItems match {
      case itemSchema: Schema[_] =>
        removeSchemaDescriptionsAndExamples(itemSchema)
      case _ =>
    }

    // Recursively check additionalProperties if it's a Schema
    if (
      schema.getAdditionalProperties != null && schema.getAdditionalProperties
        .isInstanceOf[Schema[_]]
    ) {
      removeSchemaDescriptionsAndExamples(
        schema.getAdditionalProperties.asInstanceOf[Schema[_]]
      )
    }

    // Handle oneOf, anyOf, allOf, not
    Option(schema.getOneOf).foreach(
      _.asScala.foreach(removeSchemaDescriptionsAndExamples)
    )
    Option(schema.getAnyOf).foreach(
      _.asScala.foreach(removeSchemaDescriptionsAndExamples)
    )
    Option(schema.getAllOf).foreach(
      _.asScala.foreach(removeSchemaDescriptionsAndExamples)
    )
    Option(schema.getNot).foreach(removeSchemaDescriptionsAndExamples)
  }
}
