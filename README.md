# Flash Sale – Checkout e Reserva de Estoque Relâmpago
Solução com dois microsserviços **Quarkus** (Java 21, Maven multi-módulo), Redis, RabbitMQ e SAGA coreografada simplificada.
| Módulo | Porta | Responsabilidade |
|---|---|---|
| `checkout-service` | 8080 | `POST /v1/orders`, `GET /v1/orders/{id}` (Cache-Aside), reserva de estoque no Redis, consumer de `order-compensation` |
| `order-processor-service` | 8081 | Consumer de `order-created`, persistência (H2 próprio), simulação de falhas, publica `order-compensation` |
```
Cliente -> Checkout -> Redis (reserva atômica via Lua) -> fila order-created -> Order Processor -> DB
                                       ^                                              |
                                       +------ fila order-compensation <--------------+ (falha)
```
## Pré-requisitos
- JDK 21 e Docker (para Redis e RabbitMQ). Maven wrapper incluso.
## Executando
```shell
docker compose up -d                       # Redis (6379) e RabbitMQ (5672 / console 15672, guest/guest)
# terminal 1
./mvnw -pl order-processor-service quarkus:dev
# terminal 2
./mvnw -pl checkout-service quarkus:dev
```
(No Windows use `mvnw.cmd`.) Suba os dois serviços antes de criar pedidos: cada consumer declara a sua fila.
Em dev, o checkout cria o estoque inicial `PROD-001 = 100` se ainda não existir.
## Demonstração
```shell
# 1. estoque
curl localhost:8080/v1/stock/PROD-001
curl -X PUT localhost:8080/v1/stock/PROD-001 -H "Content-Type: application/json" -d '{"quantidade":10}'
# 2. sucesso -> 202
curl -i -X POST localhost:8080/v1/orders -H "Content-Type: application/json" \
  -d '{"produto_id":"PROD-001","quantidade":2,"cliente_cpf":"50329291076","cliente_nome":"Fernando Augusto"}'
# 3. consulta (1ª vez banco, depois cache Redis por 5 min)
curl localhost:8080/v1/orders/<pedido_id>
# 4. falha simulada: nome do cliente contendo "FALHA" -> CANCELLED + compensação + estoque devolvido
curl -i -X POST localhost:8080/v1/orders -H "Content-Type: application/json" \
  -d '{"produto_id":"PROD-001","quantidade":2,"cliente_cpf":"50329291076","cliente_nome":"Cliente FALHA"}'
curl localhost:8080/v1/stock/PROD-001
# 5. estoque insuficiente -> 409
curl -i -X POST localhost:8080/v1/orders -H "Content-Type: application/json" \
  -d '{"produto_id":"PROD-001","quantidade":9999,"cliente_cpf":"50329291076","cliente_nome":"X"}'
```
Falhas também podem ser aleatórias com `processor.failure-rate` (0 a 1) e a palavra-chave com `processor.failure-keyword`.
## Decisões de projeto
- **Estoque**: script Lua no Redis (`GET` + comparação + `DECRBY`) → atômico, nunca fica negativo.
- **Banco segregado**: só o order-processor acessa o banco de pedidos; o checkout o consulta via `GET /internal/orders/{id}`.
- **Cache-Aside**: Redis (`order:{id}`, TTL 300s) → banco → grava no cache; até 3 tentativas (300 ms entre elas) antes do `404`.
- **Compensação idmpotente**: chave `compensation:{pedido_id}` evita devolver estoque duas vezes. Se a publicação em `order-created` falhar, o estoque é devolvido imediatamente.
- Mensagens são JSON (snake_case, conforme o enunciado) trafegando como texto.
## Testes
```shell
./mvnw test
```
Unitários (regras de negócio com fakes) e de integração (`@QuarkusTest`, canais RabbitMQ substituídos pelo conector em memória; não exigem Docker).
## Checklist do MVP
- [x] `checkout-service` e `order-processor-service`
- [x] `POST /v1/orders` com validação (produto_id, quantidade > 0, CPF)
- [x] Reserva atômica no Redis sem estoque negativo
- [x] Publicação em `order-created` e consumo no order-processor
- [x] Persistência com status `CONFIRMED` / `CANCELLED`
- [x] Fila `order-compensation` + devolução do estoque
- [x] `GET /v1/orders/{id}` com Cache-Aside e 3 tentativas antes do `404`
- [x] Testes de sucesso e falha
- [x] README
