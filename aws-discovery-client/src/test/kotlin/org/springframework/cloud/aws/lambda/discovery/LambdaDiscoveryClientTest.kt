package org.springframework.cloud.aws.lambda.discovery

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest
@RunWith(SpringRunner::class)
class LambdaDiscoveryClientTest {

	@SpringBootApplication
	class MyApp

	@Autowired
	val ldc: LambdaDiscoveryClient? = null

	@Test
	fun getServices() {
		ldc!!.services.forEach {
			println("the service ${it} is available.")
		}
	}

	@Test
	fun getInstances() {
		val dc = ldc!!
		dc.getInstances("uppercase").forEach {
			println("found: $it.uri}")
		}
	}

	@Test
	fun description() {
		println(ldc!!.description())
	}
}