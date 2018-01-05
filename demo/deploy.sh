#!/bin/bash

## REFERENCES
## https://docs.aws.amazon.com/lambda/latest/dg/with-on-demand-https-example-configure-event-source.html
## https://docs.aws.amazon.com/cli/latest/reference/lambda/index.html


JAR_NAME=./target/demo-1.0.0.BUILD-SNAPSHOT-aws.jar
HANDLER_NAME=example.HelloHandler
FUNCTION_NAME=hw
ENDPOINT_PATH_PART=${FUNCTION_NAME}
REGION=us-east-1


REST_API_NAME=${FUNCTION_NAME}-apigateway


# 3.0 create the API gateway itself.

API_ID=$( aws apigateway create-rest-api --name ${REST_API_NAME} --region ${REGION} )
API_ID=$( echo $API_ID | jq -r '.id' )


# 3.1 Now we're defining the surface of the API gateway. Create the root resource.

RESOURCE_ROOT_ID=$( aws apigateway get-resources --rest-api-id $API_ID  )
RESOURCE_ROOT_ID=$( echo ${RESOURCE_ROOT_ID} | jq -r '.items[].id'   )


# 3.2 Create the path for the resource.

RESOURCE_ID=$( aws apigateway create-resource  --rest-api-id ${API_ID} --parent-id ${RESOURCE_ROOT_ID}  --path-part ${ENDPOINT_PATH_PART} )
RESOURCE_ID=$( echo ${RESOURCE_ID} |  jq -r '.id' )


# 3.3 Add the method GET to the resource.

aws apigateway put-method --rest-api-id ${API_ID} --resource-id ${RESOURCE_ID} --http-method GET --authorization-type NONE


# 3.4 set the lambda function as the destination for the POST method

INTEGRATION_METHOD=GET
INTEGRATION_URI=arn:aws:apigateway:${REGION}:lambda:path/2015-03-31/functions/arn:aws:lambda:${REGION}:${AWS_ACCOUNT_ID}:function:${FUNCTION_NAME}/invocations

echo $INTEGRATION_URI

aws apigateway put-integration \
    --rest-api-id ${API_ID} \
    --resource-id ${RESOURCE_ID} \
    --http-method ${INTEGRATION_METHOD} \
    --type AWS \
    --integration-http-method ${INTEGRATION_METHOD}  \
    --uri ${INTEGRATION_URI}
