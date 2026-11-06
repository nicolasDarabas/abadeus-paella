# Paella Beneficente ABADEUS — Site institucional

HTML + CSS + JavaScript puro (sem instalação). Mesma identidade do app e do Manual da Marca.

## Como abrir
1. Deixe a **API** rodando no IntelliJ (porta 8080).
2. No VS Code, clique com o botão direito em `index.html` → **Open with Live Server**.

Sem a API o site abre normalmente, mas o cardápio mostra o aviso de servidor indisponível.

## Seções
- **Início**: slogan, data, local e horário de retirada (vindos da API)
- **A causa** e **Como funciona**
- **Cardápio com compra**: preço do lote vigente e estoque em tempo real; botões + e − montam o carrinho
- **Checkout**: resumo, Pix / cartão / dinheiro, pagamento simulado e QR Code na hora (pedido com origem `SITE`)
- **Informações do evento**: data, endereço, horário de retirada, contato e regras
- **Meus pedidos**: entrar ou criar conta (a mesma do app), status, QR Code e "Concluir pagamento"
- **Aplicativo**: telas reais do app

## Arquivos
```
index.html        página
css/estilo.css    visual (cores e fontes da marca)
js/config.js      endereço da API  ← troque aqui se publicar a API
js/app.js         cardápio, login e pedidos
img/              símbolo da marca e telas do app
fontes/           Baloo 2 e Nunito (Google Fonts, licença OFL)
```
