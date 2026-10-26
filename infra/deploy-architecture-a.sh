#!/usr/bin/env bash
# Arquitectura A: API Gateway (integración no-proxy + plantillas de mapeo) -> una Lambda POJO
# por operación. API Gateway extrae parámetros, transforma peticiones/respuestas y elige el
# estado HTTP con expresiones regulares sobre el errorMessage de la Lambda.
#
# Uso (desde CloudShell, con target/orders-api.jar ya disponible):  bash infra/deploy-architecture-a.sh

source "$(dirname "$0")/common.sh"

API_NAME="orders-api-mapping"
HANDLER="edu.eci.tdse.orders.lambda.mapping.OrderFunctions"

deploy_function orders-create-order "${HANDLER}::createOrder"
deploy_function orders-get-order "${HANDLER}::getOrder"
deploy_function orders-find-orders "${HANDLER}::getOrdersByCustomer"
deploy_function orders-delete-order "${HANDLER}::deleteOrder"

API_ID="$(recreate_rest_api "$API_NAME")"
ROOT_ID="$(root_resource_id "$API_ID")"
ORDERS_ID="$(create_resource "$API_ID" "$ROOT_ID" orders)"
ORDER_ID="$(create_resource "$API_ID" "$ORDERS_ID" '{id}')"

# Validación del cuerpo del POST en API Gateway (JSON Schema) y respuesta 400 con el formato
# de error de la API cuando el cuerpo no cumple el esquema.
aws apigateway create-model --rest-api-id "$API_ID" --name CreateOrder --content-type application/json \
  --schema "file://$INFRA_DIR/models/create-order.schema.json" >/dev/null
VALIDATOR_ID="$(aws apigateway create-request-validator --rest-api-id "$API_ID" --name body-only \
  --validate-request-body --no-validate-request-parameters --query id --output text)"
aws apigateway put-gateway-response --rest-api-id "$API_ID" --response-type BAD_REQUEST_BODY \
  --status-code 400 --response-parameters '{"gatewayresponse.header.Content-Type":"'"'"'application/json'"'"'"}' \
  --response-templates '{"application/json":"{\"error\":\"INVALID_ORDER\",\"message\":\"Request body must contain customerId, product and a quantity greater than 0\"}"}' >/dev/null

# integrate <resource-id> <método> <lambda> <plantilla> <estado-éxito> <regex:estado>...
integrate() {
  local resource_id="$1" method="$2" function="$3" template="$4" success="$5"
  shift 5
  aws apigateway put-integration --rest-api-id "$API_ID" --resource-id "$resource_id" \
    --http-method "$method" --type AWS --integration-http-method POST \
    --uri "$(lambda_uri "$function")" --passthrough-behavior NEVER \
    --request-templates "$(template_json "templates/$template")" >/dev/null

  # Respuesta por defecto (la Lambda terminó sin error): el JSON devuelto pasa tal cual.
  aws apigateway put-method-response --rest-api-id "$API_ID" --resource-id "$resource_id" \
    --http-method "$method" --status-code "$success" \
    --response-models '{"application/json":"Empty"}' >/dev/null
  aws apigateway put-integration-response --rest-api-id "$API_ID" --resource-id "$resource_id" \
    --http-method "$method" --status-code "$success" >/dev/null

  # Respuestas de error: la regex se evalúa contra errorMessage ("CODIGO: mensaje").
  local mapping code status
  for mapping in "$@"; do
    code="${mapping%%:*}"
    status="${mapping##*:}"
    aws apigateway put-method-response --rest-api-id "$API_ID" --resource-id "$resource_id" \
      --http-method "$method" --status-code "$status" \
      --response-models '{"application/json":"Error"}' >/dev/null
    aws apigateway put-integration-response --rest-api-id "$API_ID" --resource-id "$resource_id" \
      --http-method "$method" --status-code "$status" --selection-pattern "${code}: .*" \
      --response-templates "$(template_json templates/response-error.vtl)" >/dev/null
  done
}

# POST /orders
aws apigateway put-method --rest-api-id "$API_ID" --resource-id "$ORDERS_ID" --http-method POST \
  --authorization-type NONE --request-validator-id "$VALIDATOR_ID" \
  --request-models '{"application/json":"CreateOrder"}' >/dev/null
integrate "$ORDERS_ID" POST orders-create-order request-create-order.vtl 201 \
  INVALID_ORDER:400 INTERNAL_ERROR:500

# GET /orders?customerId=...
aws apigateway put-method --rest-api-id "$API_ID" --resource-id "$ORDERS_ID" --http-method GET \
  --authorization-type NONE --request-parameters method.request.querystring.customerId=false >/dev/null
integrate "$ORDERS_ID" GET orders-find-orders request-customer-query.vtl 200 \
  INVALID_ORDER:400 INTERNAL_ERROR:500

# GET /orders/{id}
aws apigateway put-method --rest-api-id "$API_ID" --resource-id "$ORDER_ID" --http-method GET \
  --authorization-type NONE --request-parameters method.request.path.id=true >/dev/null
integrate "$ORDER_ID" GET orders-get-order request-order-id.vtl 200 \
  ORDER_NOT_FOUND:404 INVALID_ORDER:400 INTERNAL_ERROR:500

# DELETE /orders/{id} (reto final): 204 sin cuerpo si existe, 404 si no.
aws apigateway put-method --rest-api-id "$API_ID" --resource-id "$ORDER_ID" --http-method DELETE \
  --authorization-type NONE --request-parameters method.request.path.id=true >/dev/null
integrate "$ORDER_ID" DELETE orders-delete-order request-order-id.vtl 204 \
  ORDER_NOT_FOUND:404 INVALID_ORDER:400 INTERNAL_ERROR:500
aws apigateway put-integration-response --rest-api-id "$API_ID" --resource-id "$ORDER_ID" \
  --http-method DELETE --status-code 204 \
  --response-templates "$(template_json templates/response-no-content.vtl)" >/dev/null

for fn in orders-create-order orders-get-order orders-find-orders orders-delete-order; do
  allow_api_gateway "$API_ID" "$fn"
done

echo "API A desplegada en: $(deploy_stage "$API_ID")"
