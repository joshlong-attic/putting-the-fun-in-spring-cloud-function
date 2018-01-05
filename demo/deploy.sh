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
FUNCTION_ROLE=arn:aws:iam::${AWS_ACCOUNT_ID}:role/lambda-role

## todo update this to inspect that the function is currently deployed and, if deployed, to update instead of create a function
aws lambda delete-function --function-name $FUNCTION_NAME --region $REGION || echo "No function named ${FUNCTION_NAME} to delete."

FUNCTION_ARN=$(
    aws lambda create-function \
        --region ${REGION} \
        --function-name ${FUNCTION_NAME} \
        --zip-file fileb://${JAR_NAME} \
        --role  ${FUNCTION_ROLE} \
        --handler ${HANDLER_NAME}  \
        --runtime java8 |  jq -r '.FunctionArn'
)

# 3.0 create the API gateway itself.

REST_API_ID=$( aws apigateway create-rest-api --name ${REST_API_NAME} --region ${REGION} )
REST_API_ID=$( echo $REST_API_ID | jq -r '.id' )


# 3.1 Now we're defining the surface of the API gateway. Create the root resource.

RESOURCE_ROOT_ID=$( aws apigateway get-resources --rest-api-id $REST_API_ID --region ${REGION} )
RESOURCE_ROOT_ID=$( echo ${RESOURCE_ROOT_ID} | jq -r '.items[].id'   )


# 3.2 Create the path for the resource.

RESOURCE_ID=$( aws apigateway create-resource  --rest-api-id ${REST_API_ID} --parent-id ${RESOURCE_ROOT_ID}  --path-part ${ENDPOINT_PATH_PART} --region ${REGION} )
RESOURCE_ID=$( echo ${RESOURCE_ID} |  jq -r '.id' )


# 3.3 Add the method GET to the resource.

aws apigateway put-method --rest-api-id ${REST_API_ID} --resource-id ${RESOURCE_ID} --http-method GET --authorization-type NONE --region ${REGION}


# 3.4 set the lambda function as the destination for the POST method

METHOD=GET
INTEGRATION_URI=arn:aws:apigateway:${REGION}:lambda:path/2015-03-31/functions/arn:aws:lambda:${REGION}:${AWS_ACCOUNT_ID}:function:${FUNCTION_NAME}/invocations

aws apigateway put-integration \
    --rest-api-id ${REST_API_ID} \
    --region ${REGION} \
    --resource-id ${RESOURCE_ID} \
    --http-method ${METHOD} \
    --type AWS \
    --integration-http-method ${METHOD}  \
    --uri ${INTEGRATION_URI}


aws apigateway put-method-response \
    --rest-api-id ${REST_API_ID} \
    --resource-id ${RESOURCE_ID} \
    --http-method ${METHOD} \
    --region ${REGION} \
    --status-code 200 \
    --response-models "{\"application/json\": \"Empty\"}"

aws apigateway put-integration-response \
    --rest-api-id ${REST_API_ID} \
    --resource-id ${RESOURCE_ID} \
    --http-method ${METHOD} \
    --region ${REGION} \
    --status-code 200 \
    --response-templates "{\"application/json\": \"\"}"


# 3.5 deploy the API
aws apigateway create-deployment --rest-api-id ${REST_API_ID} --stage-name prod --region ${REGION}

# 3.6 grant permissions

aws lambda add-permission \
    --function-name ${FUNCTION_NAME} \
    --statement-id ${REST_API_NAME}-dev  \
    --action lambda:InvokeFunction \
    --principal apigateway.amazonaws.com \
    --region ${REGION} \
    --source-arn "arn:aws:execute-api:${REGION}:${AWS_ACCOUNT_ID}:${REST_API_ID}/*/${METHOD}/${ENDPOINT_PATH_PART}"

aws lambda add-permission \
    --function-name ${FUNCTION_NAME} \
    --statement-id ${REST_API_NAME}-prod  \
    --action lambda:InvokeFunction \
    --principal apigateway.amazonaws.com \
    --region ${REGION} \
    --source-arn "arn:aws:execute-api:${REGION}:${AWS_ACCOUNT_ID}:${REST_API_ID}/*/${METHOD}/${ENDPOINT_PATH_PART}"


# 3.7
#done!