package com.example.awsdc

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder
import com.amazonaws.services.apigateway.model.DeleteRestApiRequest
import com.amazonaws.services.apigateway.model.GetResourcesRequest
import com.amazonaws.services.apigateway.model.GetRestApisRequest
import com.amazonaws.services.apigateway.model.TooManyRequestsException
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.DeleteFunctionRequest
import org.apache.commons.logging.LogFactory
import org.junit.Test


class AwsDcApplicationTests {

	// this worked
	// curl -XPOST -d{"incoming": "Hi"} https://vs84ravw45.execute-api.us-east-1.amazonaws.com/prod/uppercase

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

	@Test
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

	val log = LogFactory.getLog(AwsDcApplicationTests::class.java)

	@Test
	fun urlsForRestApi() {

		val list = this.amazonApiGateway
				.getRestApis(GetRestApisRequest())
				.items
				.flatMap { ri ->
					this.amazonApiGateway.getResources(GetResourcesRequest().withRestApiId(ri.id)).items
							.filter { x ->
								log.info("${x.path} ${x.id} ${x.pathPart} ${x.resourceMethods}")
								x.resourceMethods != null && x.pathPart != null
							}
							.map {
								""" https://${ri.id}.execute-api.${region.getName()}.amazonaws.com/prod${it.path} """.trim()
							}
				}


		list.forEach { resource ->
			println(resource)
		}
	}

}
