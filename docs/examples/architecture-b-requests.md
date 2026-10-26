# Arquitectura B: integración Lambda proxy + adaptador HTTP

Peticiones reales ejecutadas con `curl -i` contra `https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev` (fecha: 2026-10-26).
Las cabeceras de AWS (`x-amzn-*`, `x-amz-*`, `Date`, `Connection`, `Content-Length`) se omiten por brevedad.

## GET de un pedido existente (200)

```http
$ curl -i https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders/ORD-001
HTTP/1.1 200 OK
Content-Type: application/json

{"id":"ORD-001","customerId":"CUST-42","product":"MacBook Air","quantity":1}

```

## GET de un pedido inexistente (404)

```http
$ curl -i https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders/ORD-999
HTTP/1.1 404 Not Found
Content-Type: application/json

{"error":"ORDER_NOT_FOUND","message":"Order ORD-999 does not exist"}

```

## Búsqueda por cliente con pedidos (200)

```http
$ curl -i 'https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders?customerId=CUST-42'
HTTP/1.1 200 OK
Content-Type: application/json

[{"id":"ORD-001","customerId":"CUST-42","product":"MacBook Air","quantity":1},{"id":"ORD-003","customerId":"CUST-42","product":"USB-C Hub","quantity":3}]

```

## Búsqueda por cliente sin pedidos (200, lista vacía)

```http
$ curl -i 'https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders?customerId=CUST-100'
HTTP/1.1 200 OK
Content-Type: application/json

[]

```

## Búsqueda sin customerId (400)

```http
$ curl -i https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"INVALID_ORDER","message":"customerId is required"}

```

## POST válido (201)

```http
$ curl -i -X POST https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders -H 'Content-Type: application/json' -d '{"customerId":"CUST-42","product":"MacBook Air","quantity":1}'
HTTP/1.1 201 Created
Content-Type: application/json
Location: /orders/ORD-004

{"id":"ORD-004","customerId":"CUST-42","product":"MacBook Air","quantity":1}

```

## POST con quantity = 0 (400)

```http
$ curl -i -X POST https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders -H 'Content-Type: application/json' -d '{"customerId":"CUST-42","product":"MacBook Air","quantity":0}'
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"INVALID_ORDER","message":"quantity must be greater than 0"}

```

## POST con product vacío (400)

```http
$ curl -i -X POST https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders -H 'Content-Type: application/json' -d '{"customerId":"CUST-42","product":"   ","quantity":1}'
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"INVALID_ORDER","message":"product is required"}

```

## POST con JSON mal formado (400)

```http
$ curl -i -X POST https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders -H 'Content-Type: application/json' -d '{not json'
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"INVALID_ORDER","message":"Request body must be a valid order JSON document"}

```

## DELETE de un pedido existente (204)

```http
$ curl -i -X DELETE https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders/ORD-002
HTTP/1.1 204 No Content
Content-Type: application/json


```

## DELETE del mismo pedido otra vez (404)

```http
$ curl -i -X DELETE https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders/ORD-002
HTTP/1.1 404 Not Found
Content-Type: application/json

{"error":"ORDER_NOT_FOUND","message":"Order ORD-002 does not exist"}

```

## DELETE de un pedido inexistente (404)

```http
$ curl -i -X DELETE https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders/ORD-999
HTTP/1.1 404 Not Found
Content-Type: application/json

{"error":"ORDER_NOT_FOUND","message":"Order ORD-999 does not exist"}

```

## GET del pedido borrado

```http
$ curl -i https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders/ORD-002
HTTP/1.1 404 Not Found
Content-Type: application/json

{"error":"ORDER_NOT_FOUND","message":"Order ORD-002 does not exist"}

```

## GET del pedido creado por el POST (misma Lambda, misma memoria)

```http
$ curl -i https://znv3i6grrj.execute-api.us-east-1.amazonaws.com/dev/orders/ORD-004
HTTP/1.1 200 OK
Content-Type: application/json

{"id":"ORD-004","customerId":"CUST-42","product":"MacBook Air","quantity":1}

```
