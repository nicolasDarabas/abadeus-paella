# Paella ABADEUS — App Android

App em **Java** (telas em XML) com **2 perfis** e **banco local SQLite**.

## Como rodar
1. Deixe a **API** rodando no IntelliJ (porta 8080).
2. Abra esta pasta no **Android Studio** e espere o Gradle sincronizar.
3. Ligue o emulador e clique em ▶ **app**.
4. Login de teste: `ADMINISTRADOR` / `ADM123` ou `comprador@teste.com` / `123456`.

Servidor: o app usa `http://10.0.2.2:8080` (o computador, visto de dentro do emulador).
Em celular real, toque em **Configurar servidor** no login e use `http://IP-DO-COMPUTADOR:8080`
(celular e computador na mesma rede Wi-Fi).

## Telas
| Perfil | Telas |
|---|---|
| Todos | Splash, Login, Registrar |
| Comprador | Cardápio (com lote e estoque), Carrinho/Pagamento, Meus pedidos, Detalhe com QR Code, Perfil |
| Administrador | Painel (arrecadação, vendas, abrir/encerrar vendas, novo produto), Pedidos (filtros, confirmar dinheiro), Retirada (leitor de QR) |

## Banco local (SQLite no celular) — `data/BancoLocal.java`
- **Comprador:** salva o cardápio e o **QR Code dos pedidos pagos** → mostra o QR mesmo sem internet.
- **Administrador (modo contingência):** baixa a lista de pedidos pagos, valida retiradas sem internet
  e depois **sincroniza** com o servidor (`/api/sync/retiradas`).

## Estrutura
```
br.com.abadeus.paella
├── MainActivity        Splash
├── BaseActivity        Ajuste de margens (Android 15+)
├── data/               Sessao, Carrinho, BancoLocal
│   └── api/            ApiClient (Retrofit), ApiService (rotas), Modelos, Resposta (tratamento de erros)
├── ui/auth/            Login e Registro
├── ui/comprador/       Cardapio, Checkout, Meus pedidos, Perfil
├── ui/admin/           Painel, Pedidos, Retirada (QR + contingência)
├── ui/pedido/          Detalhe do pedido (pagamento + QR) e lista
└── util/               Formatos (R$, datas) e cores de status
```

## Identidade visual
Cores: açafrão `#E3A21A`, terracota `#B9472A`, verde-oliva `#5E7330`, areia `#F7EEDF`, grafite `#2B2622`.
Fontes: **Baloo 2** (títulos) e **Nunito** (textos) — Google Fonts, licença OFL.
