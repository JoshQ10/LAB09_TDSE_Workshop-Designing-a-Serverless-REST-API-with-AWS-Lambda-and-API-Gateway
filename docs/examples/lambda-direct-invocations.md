# Invocaciones directas de Lambda

Ejecutadas desde AWS CloudShell (us-east-1) con:

```bash
aws lambda invoke --function-name <función> --cli-binary-format raw-in-base64-out \
  --payload '<json>' /tmp/o.json --query '{status:StatusCode,error:FunctionError}' --output text
cat /tmp/o.json
```

La primera columna de la salida es `FunctionError` (`None` = sin error, `Unhandled` = el handler lanzó una
excepción) y la segunda el `StatusCode` de la invocación (siempre `200`: la invocación en sí se completó). Los
archivos de entrada están en [events/](../../events).

## Arquitectura A: handlers POJO (`OrderFunctions`)

### Crear un pedido ([lambda-create-order.json](../../events/lambda-create-order.json))

```
>>> orders-create-order {"customerId":"CUST-42","product":"MacBook Air","quantity":1}
None 200
{"id":"ORD-004","customerId":"CUST-42","product":"MacBook Air","quantity":1}
```

### Segundo valor: tipos distintos y propiedad desconocida ([lambda-create-order-coercion.json](../../events/lambda-create-order-coercion.json))

`"quantity"` llega como string y `"color"` no existe en `CreateOrderRequest`:

```
>>> orders-create-order {"customerId":"CUST-100","product":"Monitor","quantity":"2","color":"black"}
None 200
{"id":"ORD-005","customerId":"CUST-100","product":"Monitor","quantity":2}
```

### Entrada inválida para el negocio (la excepción llega como `errorMessage`)

```
>>> orders-create-order {"customerId":"CUST-42","product":"Mouse","quantity":0}
Unhandled 200
{"errorMessage":"INVALID_ORDER: quantity must be greater than 0","errorType":"edu.eci.tdse.orders.lambda.mapping.OrderFunctionException","stackTrace":["edu.eci.tdse.orders.lambda.mapping.OrderFunctions.execute(OrderFunctions.java:53)","edu.eci.tdse.orders.lambda.mapping.OrderFunctions.createOrder(OrderFunctions.java:38)", "..."]}
```

### JSON que no se puede convertir al tipo Java (falla en el runtime, antes del handler)

```
>>> orders-create-order {"customerId":"CUST-42","product":"Mouse","quantity":"abc"}
Unhandled 200
{"errorMessage":"An error occurred during JSON parsing","errorType":"java.lang.RuntimeException","stackTrace":[]}
```

### Consultar un pedido ([lambda-get-order.json](../../events/lambda-get-order.json))

```
>>> orders-get-order {"id":"ORD-001"}
None 200
{"id":"ORD-001","customerId":"CUST-42","product":"MacBook Air","quantity":1}

>>> orders-get-order {"id":"ORD-999"}
Unhandled 200
{"errorMessage":"ORDER_NOT_FOUND: Order ORD-999 does not exist","errorType":"edu.eci.tdse.orders.lambda.mapping.OrderFunctionException","stackTrace":["..."]}
```

### Buscar por cliente (otra Lambda, otra memoria: no aparece `ORD-004`)

```
>>> orders-find-orders {"customerId":"CUST-42"}
None 200
[{"id":"ORD-001","customerId":"CUST-42","product":"MacBook Air","quantity":1},{"id":"ORD-003","customerId":"CUST-42","product":"USB-C Hub","quantity":3}]
```

### Eliminar un pedido (`void` se serializa como `null`)

```
>>> orders-delete-order {"id":"ORD-003"}
None 200
null

>>> orders-delete-order {"id":"ORD-003"}
Unhandled 200
{"errorMessage":"ORDER_NOT_FOUND: Order ORD-003 does not exist","errorType":"edu.eci.tdse.orders.lambda.mapping.OrderFunctionException","stackTrace":["..."]}
```

## Arquitectura B: handler proxy (`ApiGatewayProxyHandler`)

La Lambda proxy también puede invocarse directamente con un evento de API Gateway (aquí, uno mínimo). Su
respuesta ya es una respuesta HTTP (`statusCode`, `headers`, `body` como string) y los errores de negocio **no**
son errores de Lambda (`FunctionError` = `None`):

```
>>> orders-proxy {"httpMethod":"GET","resource":"/orders/{id}","path":"/orders/ORD-001","pathParameters":{"id":"ORD-001"}}
None 200
{"statusCode":200,"headers":{"Content-Type":"application/json"},"body":"{\"id\":\"ORD-001\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}"}

>>> orders-proxy {"httpMethod":"DELETE","resource":"/orders/{id}","path":"/orders/ORD-999","pathParameters":{"id":"ORD-999"}}
None 200
{"statusCode":404,"headers":{"Content-Type":"application/json"},"body":"{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}"}
```
