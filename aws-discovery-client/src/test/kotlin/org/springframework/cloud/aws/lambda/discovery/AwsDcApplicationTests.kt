package org.springframework.cloud.aws.lambda.discovery

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder
import com.amazonaws.services.apigateway.model.*
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.invoke.LambdaFunction
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory
import com.amazonaws.services.lambda.model.DeleteFunctionRequest
import com.amazonaws.services.lambda.model.GetFunctionRequest
import org.apache.commons.logging.LogFactory
import org.assertj.core.api.Assertions
import org.junit.Ignore
import org.junit.Test

class AwsApiGatewayScratchTests {

	val log = LogFactory.getLog(AwsApiGatewayScratchTests::class.java)

	val basicAWSCredentials = BasicAWSCredentials(
			System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY"))

	val region = Regions.US_EAST_1

	val credentialsProvider = AWSStaticCredentialsProvider(this.basicAWSCredentials)

	val amazonLambda = AWSLambdaClientBuilder
			.standard()
			.withCredentials(credentialsProvider)
			.withRegion(region)
			.build()

	val amazonApiGateway = AmazonApiGatewayClientBuilder
			.standard()
			.withRegion(region)
			.withCredentials(credentialsProvider)
			.build()

	fun urlByFunctionName(functionName: String): String? {

		data class PathContext(val resource: Resource,
		                       val integrationResult: GetIntegrationResult,
		                       val restApi: RestApi)


		val fnArn = amazonLambda.getFunction(GetFunctionRequest()
				.withFunctionName(functionName))
				.configuration
				.functionArn

		return amazonApiGateway.getRestApis(GetRestApisRequest())
				.items
				.flatMap { restApi ->
					amazonApiGateway.getResources(GetResourcesRequest().withRestApiId(restApi.id))
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

	@Test
	fun urlByFunctionName() {
		val url = this.urlByFunctionName("uppercase")
		println("the function URI is:\n $url ")
	}

	@Test
	@Ignore
	fun cleanup() {

		fun deleteFunctions() {
			this.amazonLambda.listFunctions().functions.forEach {
				println("deleting ${it.functionName}")
				val deleteFunction = this.amazonLambda.deleteFunction(DeleteFunctionRequest()
						.withFunctionName(it.functionName))
				println("\tresult:\t${deleteFunction.sdkResponseMetadata}")
			}
		}

		fun deleteRestApis() {
			this.amazonApiGateway.getRestApis(GetRestApisRequest()).items.forEach {
				println("deleting ${it.name}")

				fun delete(id: String) {
					val deleteRestApi = this.amazonApiGateway.deleteRestApi(DeleteRestApiRequest()
							.withRestApiId(id))
					println("\tresult:\t ${deleteRestApi.sdkResponseMetadata}")
				}

				var deleted = false

				while (!deleted)
					try {
						Thread.sleep(5000)
						delete(it.id)
						deleted = true
					} catch (tme: TooManyRequestsException) {
						//
					}
			}
		}

		deleteFunctions()
		deleteRestApis()
	}


	fun restApiUrl(restApiId: String): String =
			this.amazonApiGateway.getRestApis(GetRestApisRequest()).items
					.filter {
						it.name == restApiId
					}
					.map {
						this.amazonApiGateway.getRestApi(GetRestApiRequest().withRestApiId(it.id))
					}
					.flatMap { restApiResult ->
						this.amazonApiGateway.getResources(GetResourcesRequest().withRestApiId(restApiResult.id)).items
								.filter { x ->
									println("rest api id: ${restApiResult.id} ${restApiResult.name}")
									log.info("${x.path} ${x.id} ${x.pathPart} ${x.resourceMethods}")
									x.resourceMethods != null && x.pathPart != null
								}
								.map {
									""" https://${restApiResult.id}.execute-api.${region.getName()}.amazonaws.com/prod${it.path} """.trim()
								}
					}
					.first()


	@Test
	fun testRestApi() {
		val url = restApiUrl("uppercase")
		println("the URL is ${url}")
	}

	interface UppercaseService {

		@LambdaFunction
		fun uppercase(request: UppercaseRequest): UppercaseResponse
	}

	data class UppercaseRequest(var incoming: String? = null)
	data class UppercaseResponse(var outgoing: String? = null)

	@Test
	fun invokeFunction() {
		val uppercaseService = LambdaInvokerFactory
				.builder()
				.lambdaClient(this.amazonLambda)
				.build(UppercaseService::class.java)
		val message = uppercaseService.uppercase(UppercaseRequest(incoming = "hello"))
		Assertions.assertThat(message.outgoing).isEqualTo("hello".toUpperCase())
	}
}