#!/usr/bin/env bash

REGION=us-east-1

#aws lambda list-functions --region $REGION | jq -r '.Functions[].FunctionName' | grep $FUNCTION_NAME &&  \
#     aws lambda delete-function --function-name $FUNCTION_NAME --region $REGION ;


aws lambda list-functions --region $REGION | jq -r '.Functions[].FunctionName' | while read   functionName ; do
    aws lambda delete-function --function-name $functionName --region $REGION
done


aws apigateway get-rest-apis --region $REGION | jq -r '.items[].id' | while read l ; do
    RID=$l
    aws apigateway delete-rest-api --region $REGION --rest-api-id $RID || echo "can't delete $RID ";
done

