#!/usr/bin/env bash
# Arquitectura B: API Gateway (integración Lambda proxy) -> una sola Lambda con un adaptador HTTP.
# API Gateway solo enruta; la Lambda recibe el evento HTTP completo y decide estado y cabeceras.
#
# Uso (desde CloudShell, con target/orders-api.jar ya disponible):  bash infra/deploy-architecture-b.sh

source "$(dirname "$0")/common.sh"

API_NAME="orders-api-proxy"
FUNCTION="orders-proxy"

deploy_function "$FUNCTION" "edu.eci.tdse.orders.lambda.proxy.ApiGatewayProxyHandler::handleRequest"

API_ID="$(recreate_rest_api "$API_NAME")"
ROOT_ID="$(root_resource_id "$API_ID")"
ORDERS_ID="$(create_resource "$API_ID" "$ROOT_ID" orders)"
ORDER_ID="$(create_resource "$API_ID" "$ORDERS_ID" '{id}')"

proxy_method() { # <resource-id> <método>
  aws apigateway put-method --rest-api-id "$API_ID" --resource-id "$1" --http-method "$2" \
    --authorization-type NONE >/dev/null
  aws apigateway put-integration --rest-api-id "$API_ID" --resource-id "$1" --http-method "$2" \
    --type AWS_PROXY --integration-http-method POST --uri "$(lambda_uri "$FUNCTION")" >/dev/null
}

proxy_method "$ORDERS_ID" POST
proxy_method "$ORDERS_ID" GET
proxy_method "$ORDER_ID" GET
proxy_method "$ORDER_ID" DELETE # reto final: solo se añade el método, sin plantillas

allow_api_gateway "$API_ID" "$FUNCTION"

echo "API B desplegada en: $(deploy_stage "$API_ID")"
