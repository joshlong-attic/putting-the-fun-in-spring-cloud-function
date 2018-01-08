package org.springframework.cloud.aws

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.aws.lambda.discovery.LambdaDiscoveryClient
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class AwsAutoConfiguration(val env: Environment) {

	val region = Regions.valueOf(env.getProperty("cloud.aws.region", Regions.US_EAST_1.name))

	@Bean
	@ConditionalOnMissingBean
	fun region() = this.region

	@Bean
	@ConditionalOnMissingBean
	fun awsLambda(): AWSLambda = AWSLambdaClientBuilder.standard()
			.withCredentials(awsCredentialsProvider())
			.withRegion(region)
			.build()

	@Bean
	@ConditionalOnMissingBean
	fun awsCredentialsProvider() = AWSStaticCredentialsProvider(BasicAWSCredentials(env.getProperty("cloud.aws.credentials.accessKey", System.getenv("AWS_ACCESS_KEY_ID")),
			env.getProperty("cloud.aws.credentials.secretKey", System.getenv("AWS_SECRET_ACCESS_KEY"))))

	@Bean
	@ConditionalOnMissingBean
	fun amazonApiGateway(): AmazonApiGateway = AmazonApiGatewayClientBuilder.standard()
			.withRegion(region)
			.withCredentials(awsCredentialsProvider())
			.build()

	@Bean
	@ConditionalOnMissingBean
	fun lambdaDiscoveryClient(): DiscoveryClient = LambdaDiscoveryClient(region(), amazonApiGateway(), awsLambda())
}