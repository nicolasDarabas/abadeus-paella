# Paella Beneficente ABADEUS — API (backend)

API em **Java 21 + Spring Boot 4 + SQLite** usada pelo aplicativo Android e pelo site.

## Como rodar

1. Abra a pasta `paella-api` no IntelliJ e espere o Maven baixar as dependências
   (se não começar sozinho: aba **Maven** → botão de recarregar).
2. Rode a classe `PaellaApiApplication` (botão ▶ ao lado do `main`).
3. No console deve aparecer `Tomcat started on port 8080`.
4. O banco **`paella.db`** é criado na pasta do projeto, com as 12 tabelas
   (script em `src/main/resources/schema.sql`). Abra no DB Browser para conferir.

Na primeira execução são criados automaticamente:

| Login | Senha | Perfil |
|---|---|---|
| `ADMINISTRADOR` | `ADM123` | Administrador |
| `comprador@teste.com` | `123456` | Comprador (teste) |

Também é criado um **evento de exemplo** com 4 produtos e um lote promocional.
Para não criar dados de exemplo: `app.seed.dados-exemplo=false` no `application.properties`.

> Para recomeçar do zero, pare a API e apague o arquivo `paella.db`.

## Testar no Postman

Importe `Paella-ABADEUS.postman_collection.json` (Postman → **Import**) e execute as
pastas em ordem. Tokens e IDs são salvos sozinhos nas variáveis da coleção.

## Fluxo principal

```
Comprador: registrar/login → cardápio → criar pedido (reserva o estoque por 30 min)
         → iniciar pagamento (PIX/CARTÃO/DINHEIRO) → aprovação → QR Code liberado
Administrador: lê o QR → consultar → validar retirada → comprovante de retirada
               segunda leitura do mesmo QR → 409 "JÁ RETIRADO"
```

## Endpoints

Envie o token no cabeçalho `Authorization: Bearer <token>`.

| Método | Rota | Quem | O que faz |
|---|---|---|---|
| GET | `/api/status` | público | Servidor no ar (splash do app) |
| POST | `/api/auth/login` | público | Login (login ou e-mail + senha) |
| POST | `/api/auth/registrar` | público | Cadastro de comprador |
| GET | `/api/auth/me` | logado | Dados e **perfil** do usuário |
| GET | `/api/eventos` | público | Lista eventos |
| GET | `/api/eventos/{id}/cardapio` | público | Produtos com preço do lote e estoque |
| POST/PUT | `/api/eventos`, `/api/eventos/{id}` | admin | Cria/edita evento |
| PATCH | `/api/eventos/{id}/status` | admin | Abre/encerra vendas |
| POST | `/api/eventos/{id}/produtos` | admin | Cria produto |
| PUT/DELETE | `/api/produtos/{id}` | admin | Edita/desativa produto |
| POST | `/api/eventos/{id}/lotes` | admin | Cria lote |
| POST | `/api/lotes/{id}/produtos` | admin | Preço/quantidade do produto no lote |
| POST | `/api/pedidos` | logado | Cria pedido |
| GET | `/api/pedidos/meus` | logado | Meus pedidos |
| GET | `/api/pedidos/{id}` | dono/admin | Detalhe |
| POST | `/api/pedidos/{id}/cancelar` | dono/admin | Cancela |
| GET | `/api/pedidos/{id}/historico` | dono/admin | Histórico de status |
| POST | `/api/pedidos/{id}/pagamentos` | dono/admin | Inicia pagamento |
| POST | `/api/pagamentos/{id}/simular-aprovacao` | dono/admin | Simula aprovação PIX/cartão |
| POST | `/api/pagamentos/{id}/confirmar` | admin | Confirma (ex.: dinheiro) |
| POST | `/api/pagamentos/{id}/recusar` | admin | Recusa |
| GET | `/api/pedidos/{id}/comprovante` | dono/admin | Comprovante + QR em Base64 |
| GET | `/api/pedidos/{id}/qrcode` | dono/admin | QR Code em PNG |
| GET | `/api/admin/retiradas/consultar?codigo=` | admin | Pré-visualiza o QR lido |
| POST | `/api/admin/retiradas/validar` | admin | Confirma a retirada |
| GET | `/api/admin/eventos/{id}/resumo` | admin | Painel: vendas, arrecadação, retiradas |
| GET | `/api/admin/pedidos` | admin | Pedidos (filtros `eventoId`, `status`) |
| GET | `/api/admin/usuarios` | admin | Usuários |
| GET | `/api/sync/eventos/{id}/retiradas-pendentes` | admin | Lista para o banco local (offline) |
| POST | `/api/sync/retiradas` | admin | Envia retiradas feitas offline |

## Regras de negócio implementadas

- Pedido nasce **AGUARDANDO_PAGAMENTO** e reserva o estoque por 30 min
  (`app.pedido.minutos-reserva`); se não for pago, vira **EXPIRADO** automaticamente.
- QR Code só é liberado depois do pagamento aprovado (**PAGO**).
- **Retirada única**: `retirada.pedido_id` é `UNIQUE` no banco + validação no código.
- Preço por **lote**: vale o primeiro lote vigente com quantidade; senão, o preço do produto.
- Toda mudança de status gera registro em `pedido_status_historico`.
- Todas as tabelas têm `criado_em`, `atualizado_em`, `criado_por`, `atualizado_por`.
- Senhas com **BCrypt**; autenticação por **token JWT** (validade de 12 h).
- Pagamento **simulado** (projeto acadêmico). Integração real (Mercado Pago, Asaas...) fica
  como evolução futura, no lugar de `/simular-aprovacao` (webhook do provedor).

## Status

| Pedido | Pagamento | Evento |
|---|---|---|
| AGUARDANDO_PAGAMENTO, PAGO, RETIRADO, CANCELADO, EXPIRADO | PENDENTE, APROVADO, RECUSADO, CANCELADO | RASCUNHO, VENDAS_ABERTAS, VENDAS_ENCERRADAS, FINALIZADO |

## Estrutura

```
src/main/java/br/com/abadeus/paellaapi
├── config/        SecurityConfig, DataSeeder (admin inicial e dados de exemplo)
├── controller/    Rotas REST
├── domain/entity  Entidades (12 tabelas)
├── domain/enums   Status, perfis, métodos de pagamento
├── dto/           Objetos de entrada e saída (JSON)
├── exception/     Erros padronizados
├── repository/    Acesso ao banco (Spring Data JPA)
├── security/      Token JWT
└── service/       Regras de negócio
```

## Novidades (itens da especificação)

| Método | Rota | Quem | O que faz |
|---|---|---|---|
| POST | `/api/admin/usuarios` | admin | Cadastra usuário (padrão: **VOLUNTARIO**) |
| POST | `/api/admin/pedidos/{id}/reembolsar` | admin | Reembolsa pedido pago (status **REEMBOLSADO**) |
| GET | `/api/admin/relatorios/pedidos.csv?eventoId=` | admin | Exporta pedidos em CSV |
| GET | `/api/admin/relatorios/arrecadacao.csv?eventoId=` | admin | Exporta arrecadação por produto e forma de pagamento |
| GET | `/api/admin/notificacoes` | admin | Todos os e-mails enviados (simulados) |
| GET | `/api/notificacoes/minhas` | logado | Avisos recebidos pelo comprador |

- **Perfil VOLUNTARIO** (equipe de entrega): acessa somente a validação de retirada e o modo contingência.
  Login inicial: `VOLUNTARIO` / `VOL123`.
- **E-mails simulados**: pedido criado, pagamento confirmado, lembrete de retirada (24 h antes),
  retirada, cancelamento, expiração e reembolso. Ficam na tabela `notificacao` e aparecem no console
  como `[E-MAIL SIMULADO]`.
- **Evento** agora tem `contato` e `regras` de retirada.
- Bancos antigos são atualizados sozinhos ao iniciar (linhas `ALTER TABLE` no fim do `schema.sql`).
