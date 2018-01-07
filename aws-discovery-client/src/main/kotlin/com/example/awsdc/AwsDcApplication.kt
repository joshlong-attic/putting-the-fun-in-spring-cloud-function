package com.example.awsdc

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.support.beans

@SpringBootApplication
class AwsDcApplication

fun main(args: Array<String>) {
  SpringApplicationBuilder()
      .sources(AwsDcApplication::class.java)
      .initializers(beans {
          bean {



            ApplicationRunner {

















            }


          }
      })
      .run( *args)

}
