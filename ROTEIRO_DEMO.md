# Roteiro de Demonstração (execução manual)

Todos os comandos são para **PowerShell**, a partir da pasta do projeto:

```powershell
cd C:\dev\java\ada_backIII\flash-sale\flash-sale
```

Use **4 janelas/abas de terminal** (A, B, C, D) lado a lado, mais o navegador.

| Janela | Uso |
|---|---|
| A | `order-processor-service` (logs do processor) |
| B | `checkout-service` (logs do checkout) |
| C | comandos da demonstração (curl/PowerShell) |
| D | Redis e RabbitMQ via `docker exec` |

---

## 0. Preparação (fazer antes da apresentação)

### 0.1 Docker Desktop aberto ("Engine running"), depois:

```powershell
docker compose up -d
docker compose ps
```

Esperado: `redis` e `rabbitmq` em `running`.

### 0.2 Subir o processor (janela A) — **primeiro**

```powershell
.\mvnw.cmd -pl order-processor-service quarkus:dev "-Dquarkus.console.enabled=false"
```

Aguarde `Listening on: http://localhost:8081`.

### 0.3 Subir o checkout (janela B)

```powershell
.\mvnw.cmd -pl checkout-service quarkus:dev "-Dquarkus.console.enabled=false"
```

Aguarde `Listening on: http://localhost:8080`.

> Suba os dois serviços antes de criar pedidos: cada consumer declara a sua fila.
> Sem isso, uma mensagem publicada antes de a fila existir é perdida.

### 0.4 Abrir o console do RabbitMQ

http://localhost:15672 (usuário `guest`, senha `guest`) → aba **Queues**.
Devem aparecer `order-created` e `order-compensation`, cada uma com **1 consumer**.

### 0.5 Variáveis úteis (janela C)

```powershell
$u = 'http://localhost:8080'
$h = @{ 'Content-Type' = 'application/json' }
```

---

## 1. Estoque inicial (item 1)

Defina um estoque conhecido e mostre-o:

```powershell
Invoke-RestMethod -Method Put "$u/v1/stock/PROD-001" -Headers $h -Body '{"quantidade":10}'
Invoke-RestMethod "$u/v1/stock/PROD-001"
```

Esperado: `quantidade = 10`.

Mostre que o saldo está **no Redis** (janela D):

```powershell
docker exec flash-sale-redis-1 redis-cli GET stock:PROD-001
```

Esperado: `10`.

---

## 2. Pedido com sucesso (itens 2 e 3)

```powershell
$body = '{"produto_id":"PROD-001","quantidade":2,"cliente_cpf":"50329291076","cliente_nome":"Fernando Augusto"}'
$r = Invoke-RestMethod -Method Post "$u/v1/orders" -Headers $h -Body $body
$r
$id = $r.pedido_id
```

Esperado: **HTTP 202**, `status = PROCESSING`, `pedido_id` (UUID v4) e `data_hora_requisicao`.

Para exibir o código HTTP explicitamente, use no lugar:

```powershell
$resp = Invoke-WebRequest -Method Post "$u/v1/orders" -Headers $h -Body $body -UseBasicParsing
$resp.StatusCode
$resp.Content
$id = ($resp.Content | ConvertFrom-Json).pedido_id
```

Mostre o estoque já decrementado (janela D): `docker exec flash-sale-redis-1 redis-cli GET stock:PROD-001` → `8`.

---

## 3. Publicação na fila `order-created` (item 4)

A mensagem é consumida em milissegundos, então para **vê-la na fila**, pare o consumer por um instante:

1. Na janela A, pressione **Ctrl+C** (para o processor). A fila continua existindo.
2. Na janela C, crie outro pedido:

```powershell
$r2 = Invoke-RestMethod -Method Post "$u/v1/orders" -Headers $h -Body $body
$id2 = $r2.pedido_id
```

3. Mostre a mensagem parada na fila (janela D):

```powershell
docker exec flash-sale-rabbitmq-1 rabbitmqctl list_queues name messages consumers
```

Esperado: `order-created  1  0` (1 mensagem, 0 consumers).
No console web, em **Queues → order-created → Get messages** (Ack mode: *Nack message requeue true*, Messages: 1 → *Get Message(s)*), mostre o JSON com `pedido_id`, `produto_id`, `quantidade`, `cliente_cpf`, `cliente_nome`, `data_hora_criacao`.

> Use *requeue true* para não consumir a mensagem.

Estoque neste momento: 6.

---

## 4. Consumo pelo processor e `CONFIRMED` (itens 5 e 6)

Suba o processor novamente (janela A):

```powershell
.\mvnw.cmd -pl order-processor-service quarkus:dev "-Dquarkus.console.enabled=false"
```

Assim que subir, ele consome a mensagem pendente. Mostre:

- Janela A: log `Pedido ... CONFIRMED`.
- Janela D: `docker exec flash-sale-rabbitmq-1 rabbitmqctl list_queues name messages consumers` → `order-created  0  1`.
- Status direto no banco do processor (banco próprio, porta 8081):

```powershell
Invoke-RestMethod "http://localhost:8081/internal/orders/$id2"
```

Esperado: `status = CONFIRMED`.

---

## 5. `GET /v1/orders/{id}` e uso do Redis (itens 7 e 8)

Primeiro mostre que **ainda não há cache** para o pedido `$id2` (janela D):

```powershell
docker exec flash-sale-redis-1 redis-cli EXISTS "order:$id2"
```

Esperado: `0`.

Consulte o pedido (1ª vez: vai ao banco do processor e grava no Redis):

```powershell
Invoke-RestMethod "$u/v1/orders/$id2"
```

Esperado: **200**, `status = CONFIRMED`, `data_hora_atualizacao`.

Prove o Cache-Aside (janela D):

```powershell
docker exec flash-sale-redis-1 redis-cli GET "order:$id2"
docker exec flash-sale-redis-1 redis-cli TTL "order:$id2"
```

Esperado: o JSON do pedido e TTL ≈ `300` (5 min).

Consulte de novo (2ª vez: servida pelo Redis, sem ir ao banco):

```powershell
Invoke-RestMethod "$u/v1/orders/$id2"
```

(Opcional) Para mostrar que a 2ª resposta não toca o processor, observe que o log da janela A não registra nova consulta interna.

---

## 6. Falha no processamento e compensação (itens 9 a 12)

A falha é simulada quando o `cliente_nome` contém **FALHA**.

Anote o estoque antes (janela C):

```powershell
Invoke-RestMethod "$u/v1/stock/PROD-001"     # esperado: 6
```

Crie o pedido que vai falhar (3 unidades):

```powershell
$bodyF = '{"produto_id":"PROD-001","quantidade":3,"cliente_cpf":"50329291076","cliente_nome":"Cliente FALHA"}'
$rf = Invoke-RestMethod -Method Post "$u/v1/orders" -Headers $h -Body $bodyF
$rf
$idF = $rf.pedido_id
```

Logo em seguida (em poucos segundos) acompanhe:

1. **Janela A** (processor): `Falha ao processar pedido ...` e `Pedido ... CANCELLED; compensação publicada` → item 10 (publicação em `order-compensation`).
2. **Janela B** (checkout): log do consumer de compensação devolvendo o estoque → item 11.
3. **Console do RabbitMQ → Overview → Message rates** (ou aba Queues → `order-compensation`): picos de *publish* e *deliver/ack*.
4. Estoque devolvido (janela C):

```powershell
Invoke-RestMethod "$u/v1/stock/PROD-001"     # volta a 6 (foi 6 -> 3 -> 6)
```

5. Compensação registrada e idempotente (janela D):

```powershell
docker exec flash-sale-redis-1 redis-cli GET "compensation:$idF"
```

Esperado: `DONE`.

6. Pedido cancelado (janela C) → item 12:

```powershell
Invoke-RestMethod "$u/v1/orders/$idF"
```

Esperado: `status = CANCELLED`.

---

## 7. Extras que valem mostrar

### 7.1 Estoque insuficiente → 409

```powershell
try {
  Invoke-RestMethod -Method Post "$u/v1/orders" -Headers $h `
    -Body '{"produto_id":"PROD-001","quantidade":9999,"cliente_cpf":"50329291076","cliente_nome":"X"}'
} catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }
```

Esperado: `409` e `{"mensagem":"Estoque insuficiente para o produto PROD-001"}`.

### 7.2 Validação → 400

```powershell
try { Invoke-RestMethod -Method Post "$u/v1/orders" -Headers $h `
  -Body '{"produto_id":"PROD-001","quantidade":0,"cliente_cpf":"50329291076","cliente_nome":"X"}' } `
catch { $_.Exception.Response.StatusCode.value__ }
```

Esperado: `400` (repita com `produto_id` vazio e com `cliente_cpf` vazio).

### 7.3 Pedido inexistente → 404 após 3 tentativas

```powershell
try { Invoke-RestMethod "$u/v1/orders/00000000-0000-4000-8000-000000000000" } `
catch { $_.Exception.Response.StatusCode.value__; $_.ErrorDetails.Message }
```

Esperado: `404` e `{"mensagem":"Pedido não encontrado"}` (leva ~600 ms: 3 tentativas com 300 ms entre elas).

### 7.4 Nunca vender além do estoque (concorrência)

```powershell
Invoke-RestMethod -Method Put "$u/v1/stock/PROD-002" -Headers $h -Body '{"quantidade":5}'
$jobs = 1..15 | ForEach-Object {
  Start-Job { try { (Invoke-WebRequest -Method Post 'http://localhost:8080/v1/orders' `
    -Headers @{'Content-Type'='application/json'} -UseBasicParsing `
    -Body '{"produto_id":"PROD-002","quantidade":1,"cliente_cpf":"50329291076","cliente_nome":"C"}').StatusCode } `
    catch { [int]$_.Exception.Response.StatusCode } }
}
$codes = $jobs | Wait-Job | Receive-Job
$codes | Group-Object | Select-Object Name, Count
Invoke-RestMethod "$u/v1/stock/PROD-002"
```

Esperado: 5 × `202`, 10 × `409` e estoque final `0` (nunca negativo — reserva atômica por script Lua).

---

## 8. Testes automatizados (para fechar a apresentação)

Pare os serviços (Ctrl+C nas janelas A e B), pois os testes usam a porta 8081, e rode:

```powershell
.\mvnw.cmd clean test
```

Esperado: `BUILD SUCCESS` com 25 testes (18 no checkout, 7 no processor). Eles não exigem Docker.

---

## 9. Encerramento

```powershell
docker compose down
```

---

## Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| `dockerDesktopLinuxEngine ... cannot find the file` | Docker Desktop fechado; abra-o e aguarde "Engine running" |
| `Prefixo URI não reconhecido` | Faltou o `http://` na URL |
| Mensagem de pedido nunca é consumida | Processor não subiu, ou foi subido depois de publicar sem a fila existir; reinicie na ordem 0.2 → 0.3 |
| `-Dquarkus.console.enabled=false` dá "Unknown lifecycle phase" | No PowerShell, mantenha as **aspas** em volta do `-D...` |
| `Failed to delete ...jar` no `clean` | Há um `quarkus:dev` rodando; pare-o antes |
| Porta 8080/8081 ocupada | `netstat -ano \| findstr "8080 8081"` e encerre o processo |

