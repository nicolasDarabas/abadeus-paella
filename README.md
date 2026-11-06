# Paella Beneficente ABADEUS

Sistema de venda e retirada da Paella Beneficente da ABADEUS.

| Pasta | Conteúdo |
|---|---|
| `backend/paella-api` | API REST em Java 21 + Spring Boot com banco SQLite. Inclui a coleção do Postman. |
| `app-android` | Aplicativo Android em Java com perfis de comprador, administrador e voluntário, e banco local para retirada offline. |
| `site` | Site institucional em HTML, CSS e JavaScript, com compra on-line e "Meus pedidos". |
| `docs` | Manual da marca ABADEUS (PDF). |

## Como rodar

1. **API:** abra `backend/paella-api` no IntelliJ e execute a aplicação (ou `./mvnw spring-boot:run`). Ela sobe em `http://localhost:8080`.
2. **App:** abra `app-android` no Android Studio e rode no emulador. O app acessa a API por `http://10.0.2.2:8080/`.
3. **Site:** abra `site/index.html` com o Live Server do VS Code.

Cada pasta tem um README com mais detalhes.

## Acessos de teste

| Perfil | Login | Senha |
|---|---|---|
| Administrador | ADMINISTRADOR | ADM123 |
| Voluntário | VOLUNTARIO | VOL123 |
| Comprador | comprador@teste.com | 123456 |
