#!/usr/bin/env bash
# Funciones compartidas por los scripts de despliegue. Pensado para ejecutarse en AWS CloudShell
# (AWS Academy Learner Lab): usa el rol LabRole porque el laboratorio no permite crear roles IAM.

set -euo pipefail

INFRA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REGION="${AWS_REGION:-us-east-1}"
JAR="${JAR:-$INFRA_DIR/../target/orders-api.jar}"
STAGE="${STAGE:-dev}"
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
ROLE_ARN="${ROLE_ARN:-arn:aws:iam::${ACCOUNT_ID}:role/LabRole}"

export AWS_DEFAULT_REGION="$REGION"
export AWS_PAGER=""

# deploy_function <nombre> <handler>: crea la Lambda o actualiza su código si ya existe.
deploy_function() {
  local name="$1" handler="$2"
  if aws lambda get-function --function-name "$name" >/dev/null 2>&1; then
    aws lambda update-function-code --function-name "$name" --zip-file "fileb://$JAR" >/dev/null
    aws lambda wait function-updated-v2 --function-name "$name"
    aws lambda update-function-configuration --function-name "$name" --handler "$handler" >/dev/null
  else
    aws lambda create-function --function-name "$name" --runtime java17 --handler "$handler" \
      --role "$ROLE_ARN" --zip-file "fileb://$JAR" --memory-size 512 --timeout 15 \
      --architectures x86_64 >/dev/null
    aws lambda wait function-active-v2 --function-name "$name"
  fi
  aws lambda wait function-updated-v2 --function-name "$name"
  echo "Lambda lista: $name ($handler)"
}

# recreate_rest_api <nombre>: borra la API REST con ese nombre (si existe) y crea una nueva.
recreate_rest_api() {
  local name="$1" existing
  existing="$(aws apigateway get-rest-apis --query "items[?name=='$name'].id" --output text)"
  for id in $existing; do
    aws apigateway delete-rest-api --rest-api-id "$id"
    sleep 30 # límite de cuota de API Gateway para DeleteRestApi
  done
  aws apigateway create-rest-api --name "$name" --endpoint-configuration types=REGIONAL \
    --query id --output text
}

root_resource_id() {
  aws apigateway get-resources --rest-api-id "$1" --query "items[?path=='/'].id" --output text
}

create_resource() { # <api-id> <parent-id> <path-part>
  aws apigateway create-resource --rest-api-id "$1" --parent-id "$2" --path-part "$3" \
    --query id --output text
}

lambda_arn() {
  echo "arn:aws:lambda:${REGION}:${ACCOUNT_ID}:function:$1"
}

lambda_uri() {
  echo "arn:aws:apigateway:${REGION}:lambda:path/2015-03-31/functions/$(lambda_arn "$1")/invocations"
}

# allow_api_gateway <api-id> <función>: permite que la API invoque la Lambda.
allow_api_gateway() {
  local api_id="$1" name="$2"
  aws lambda remove-permission --function-name "$name" --statement-id "apigw-$api_id" >/dev/null 2>&1 || true
  aws lambda add-permission --function-name "$name" --statement-id "apigw-$api_id" \
    --action lambda:InvokeFunction --principal apigateway.amazonaws.com \
    --source-arn "arn:aws:execute-api:${REGION}:${ACCOUNT_ID}:${api_id}/*/*/*" >/dev/null
}

# template_json <archivo>: {"application/json": "<contenido del archivo>"} para la CLI.
template_json() {
  jq -n --rawfile t "$INFRA_DIR/$1" '{"application/json": $t}'
}

deploy_stage() { # <api-id>
  aws apigateway create-deployment --rest-api-id "$1" --stage-name "$STAGE" >/dev/null
  echo "https://$1.execute-api.${REGION}.amazonaws.com/${STAGE}"
}
