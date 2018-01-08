package org.springframework.cloud.aws.lambda.discovery

import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.*
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.GetFunctionRequest
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties
import java.net.URI

/**
 * A {@link DiscoveryClient} implementation that provides URLs for
 * functions that have been registered in AWS Lambda and then exposed
 * through an AWS API Gateway trigger. This implementation returns the URL
 * for the AWS API Gateway.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 *
 */
class LambdaDiscoveryClient(private val region: Regions,
                            private val amazonApiGateway: AmazonApiGateway,
                            private val lambda: AWSLambda) : DiscoveryClient {

	override fun getServices(): MutableList<String> =
			lambda
					.listFunctions()
					.functions
					.map { it.functionName }
					.toMutableList()

	override fun getInstances(serviceId: String): MutableList<ServiceInstance> =
			mutableListOf(SimpleDiscoveryProperties.SimpleServiceInstance(URI.create(urlByFunctionName(serviceId))))

	override fun description(): String = ("A discovery client that returns URIs " +
			"for AWS Lambda functions mapped to API Gateway endpoints")
			.trim()

	private fun urlByFunctionName(functionName: String): String? {

		data class PathContext(val resource: Resource,
		                       val integrationResult: GetIntegrationResult,
		                       val restApi: RestApi)

		val fnArn = lambda.getFunction(GetFunctionRequest()
				.withFunctionName(functionName))
				.configuration
				.functionArn

		return amazonApiGateway
				.getRestApis(GetRestApisRequest())
				.items
				.flatMap { restApi ->
					amazonApiGateway
							.getResources(GetResourcesRequest().withRestApiId(restApi.id))
							.items
							.flatMap { resource ->
								val resourceId = resource.id
								val integration: GetIntegrationResult? =
										try {
											val integrationRequest = GetIntegrationRequest()
													.withHttpMethod("ANY")
													.withRestApiId(restApi.id)
													.withResourceId(resourceId)

											amazonApiGateway.getIntegration(integrationRequest)
										} catch (e: Exception) {
											null
										}

								if (null == integration)
									emptyList()
								else
									listOf(PathContext(resource, integration, restApi))
							}
				}
				.map { ctx ->
					if (ctx.integrationResult.uri.contains(fnArn)) {
						"https://${ctx.restApi.id}.execute-api.${region.getName()}.amazonaws.com/prod/${ctx.resource.pathPart}"
					} else
						null
				}
				.first { it != null }
	}
}