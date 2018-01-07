#!/bin/bash

mvn -DskipTests=true clean package

## REFERENCES
## https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-custom-integrations.html

FUNCTION_NAME=d6  # ${RANDOM}
METHOD=ANY
JAR_NAME=./target/demo-1.0.0.BUILD-SNAPSHOT-aws.jar
HANDLER_NAME=example.HelloHandler
ENDPOINT_PATH_PART=${FUNCTION_NAME}
REGION=us-east-1
REST_API_NAME=${FUNCTION_NAME}-apigateway
FUNCTION_ROLE=arn:aws:iam::${AWS_ACCOUNT_ID}:role/lambda-role

## todo update this to inspect that the function is currently deployed and, if deployed, to update instead of create a function
aws lambda list-functions --region $REGION | jq -r '.Functions[].FunctionName' | grep $FUNCTION_NAME &&  \
     aws lambda delete-function --function-name $FUNCTION_NAME --region $REGION ;

FUNCTION_ARN=$(
    aws lambda create-function \
        --region ${REGION} \
        --timeout 300 \
        --function-name ${FUNCTION_NAME} \
        --zip-file fileb://${JAR_NAME} \
        --memory-size 512 \
        --role  ${FUNCTION_ROLE} \
        --handler ${HANDLER_NAME}  \
        --runtime java8 |  jq -r '.FunctionArn'
)

REST_API_ID=$( aws apigateway create-rest-api --name ${REST_API_NAME} --region ${REGION} | jq -r '.id' )

RESOURCE_ID=$( aws apigateway get-resources --rest-api-id ${REST_API_ID} --region ${REGION} | jq -r '.items[].id' )
RESOURCE_ID=$( aws apigateway create-resource --rest-api-id ${REST_API_ID} --region ${REGION} --parent-id ${RESOURCE_ID} --path-part ${FUNCTION_NAME} | jq -r '.id' )


METHOD_RESULT=$( aws apigateway put-method --rest-api-id $REST_API_ID  --region $REGION  --resource-id $RESOURCE_ID --http-method $METHOD --authorization-type "NONE" )
METHOD_RESPONSE_RESULT=$( aws apigateway put-method-response --rest-api-id $REST_API_ID --region $REGION --resource-id $RESOURCE_ID  --http-method $METHOD --status-code 200 )

INTEGRATION_URI=arn:aws:apigateway:${REGION}:lambda:path/2015-03-31/functions/arn:aws:lambda:${REGION}:${AWS_ACCOUNT_ID}:function:${FUNCTION_NAME}/invocations
ROLE_ID=arn:aws:iam::960598786046:role/lambda-role

PUT_INTEGRATION_RESULT=$(
    aws apigateway put-integration \
        --region ${REGION} \
        --rest-api-id ${REST_API_ID} \
        --resource-id ${RESOURCE_ID} \
        --http-method ${METHOD} \
        --type AWS \
        --integration-http-method POST \
        --uri ${INTEGRATION_URI} \
        --request-templates file://`pwd`/request-template.json \
        --credentials $ROLE_ID
)

PUT_INTEGRATION_RESPONSE_RESULT=$(
    aws apigateway put-integration-response \
        --region ${REGION} \
        --rest-api-id ${REST_API_ID} \
        --resource-id ${RESOURCE_ID} \
        --http-method ANY \
        --status-code 200 \
        --selection-pattern ""
)

DEPLOY=$( aws apigateway create-deployment --rest-api-id ${REST_API_ID} --stage-name prod --region ${REGION} )

echo Finished deployment.