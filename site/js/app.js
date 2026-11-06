/* =========================================================
   Site Paella ABADEUS - comunicacao com a API
   - Cardapio publico:  GET /api/eventos  e  /api/eventos/{id}/cardapio
   - Conta:             POST /api/auth/login  |  POST /api/auth/registrar
   - Compra:            POST /api/pedidos  ->  POST /api/pedidos/{id}/pagamentos
                        ->  POST /api/pagamentos/{id}/simular-aprovacao (pagamento simulado)
   - Meus pedidos:      GET /api/pedidos/meus  e  /api/pedidos/{id}/comprovante (QR Code)
   ========================================================= */
(function () {
  'use strict';

  const API = (window.API_URL || 'http://localhost:8080').replace(/\/$/, '');
  const CHAVE_TOKEN = 'abadeus_token';
  const $ = (id) => document.getElementById(id);

  const estado = {
    evento: null,
    vendasAbertas: false,
    produtos: [],
    carrinho: new Map(),      // produtoId -> quantidade
    modoCadastro: false,
    finalizarDepoisDoLogin: false,
    pedidoAtual: null,
    pagamentoAtual: null
  };

  // ---------------- Utilitarios ----------------
  const dinheiro = (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

  function data(iso, opcoes) {
    if (!iso) return '';
    const d = new Date(iso);
    return isNaN(d) ? '' : d.toLocaleString('pt-BR', opcoes);
  }
  const dataLonga = (iso) => {
    const s = data(iso, { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' });
    return s.charAt(0).toUpperCase() + s.slice(1);
  };
  const hora = (iso) => data(iso, { hour: '2-digit', minute: '2-digit' });
  const dataHora = (iso) => data(iso, { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });

  function esc(t) {
    const d = document.createElement('div');
    d.textContent = t == null ? '' : String(t);
    return d.innerHTML;
  }

  function guardar(chave, valor) {
    try { valor == null ? sessionStorage.removeItem(chave) : sessionStorage.setItem(chave, valor); } catch (e) { /* sem storage */ }
  }
  function ler(chave) {
    try { return sessionStorage.getItem(chave); } catch (e) { return null; }
  }
  const logado = () => !!ler(CHAVE_TOKEN);

  async function api(caminho, opcoes = {}) {
    const headers = { 'Accept': 'application/json' };
    if (opcoes.corpo) headers['Content-Type'] = 'application/json';
    const token = ler(CHAVE_TOKEN);
    if (token && opcoes.auth) headers['Authorization'] = 'Bearer ' + token;
    const resp = await fetch(API + caminho, {
      method: opcoes.metodo || 'GET',
      headers,
      body: opcoes.corpo ? JSON.stringify(opcoes.corpo) : undefined
    });
    let json = null;
    try { json = await resp.json(); } catch (e) { /* sem corpo */ }
    if (!resp.ok) {
      let msg = (json && json.mensagem) || 'Erro ' + resp.status;
      if (json && json.campos) msg = Object.values(json.campos)[0] || msg;
      const erro = new Error(msg);
      erro.status = resp.status;
      throw erro;
    }
    return json;
  }
  const mensagemErro = (e) => e.status ? e.message : 'Servidor indisponível. Tente novamente mais tarde.';

  const STATUS = {
    AGUARDANDO_PAGAMENTO: ['Aguardando pagamento', 'etiqueta'],
    PAGO: ['Pago · pronto p/ retirar', 'etiqueta etiqueta--oliva'],
    RETIRADO: ['Retirado', 'etiqueta etiqueta--grafite'],
    CANCELADO: ['Cancelado', 'etiqueta etiqueta--terracota'],
    EXPIRADO: ['Expirado', 'etiqueta etiqueta--terracota'],
    REEMBOLSADO: ['Reembolsado', 'etiqueta etiqueta--terracota'],
    VENDAS_ABERTAS: ['Vendas abertas', 'etiqueta etiqueta--oliva'],
    VENDAS_ENCERRADAS: ['Vendas encerradas', 'etiqueta etiqueta--terracota'],
    FINALIZADO: ['Evento finalizado', 'etiqueta etiqueta--grafite'],
    RASCUNHO: ['Em breve', 'etiqueta']
  };
  const etiqueta = (s) => {
    const [texto, classe] = STATUS[s] || [s, 'etiqueta'];
    return `<span class="${classe}">${esc(texto)}</span>`;
  };

  // ---------------- Menu mobile ----------------
  const menu = $('menu');
  const menuBotao = $('menuBotao');
  menuBotao.addEventListener('click', () => {
    const aberto = menu.classList.toggle('aberto');
    menuBotao.setAttribute('aria-expanded', String(aberto));
  });
  menu.querySelectorAll('a').forEach((a) => a.addEventListener('click', () => {
    menu.classList.remove('aberto');
    menuBotao.setAttribute('aria-expanded', 'false');
  }));

  $('ano').textContent = new Date().getFullYear();
  $('botaoApp').addEventListener('click', (e) => {
    e.preventDefault();
    alert('Versão de demonstração: o aplicativo é instalado pelo Android Studio (projeto acadêmico).');
  });

  // =========================================================
  // Evento + cardapio
  // =========================================================
  async function carregarCardapio() {
    try {
      const eventos = await api('/api/eventos');
      $('statusApi').textContent = 'online';
      if (!eventos || !eventos.length) {
        mostrarAvisoCardapio('Nenhum evento publicado no momento. Volte em breve!');
        $('produtos').innerHTML = '';
        return;
      }
      const evento = eventos.find((e) => e.status === 'VENDAS_ABERTAS') || eventos[0];
      mostrarEvento(evento);
      const cardapio = await api('/api/eventos/' + evento.id + '/cardapio');
      mostrarProdutos(cardapio);
    } catch (e) {
      $('statusApi').textContent = 'offline';
      mostrarAvisoCardapio('Não foi possível carregar o cardápio agora. Verifique se o servidor da ABADEUS está no ar e tente novamente.');
      $('produtos').innerHTML = '';
    }
  }

  function mostrarAvisoCardapio(texto) {
    const aviso = $('cardapioAviso');
    aviso.textContent = texto;
    aviso.hidden = !texto;
  }

  function mostrarEvento(ev) {
    estado.evento = ev;
    const st = $('heroStatus');
    if (st) st.outerHTML = `<span id="heroStatus">${etiqueta(ev.status)}</span>`;
    $('heroData').textContent = dataLonga(ev.dataEvento) + ' · ' + hora(ev.dataEvento);
    $('heroLocal').textContent = ev.local;
    const retirada = ev.retiradaInicio
      ? 'Retirada das ' + hora(ev.retiradaInicio) + (ev.retiradaFim ? ' às ' + hora(ev.retiradaFim) : '')
      : 'Retirada no dia do evento';
    $('heroRetirada').textContent = retirada;
    $('cardapioTitulo').textContent = ev.titulo;
    // Informacoes do evento
    $('infoData').textContent = dataLonga(ev.dataEvento) + ', ' + hora(ev.dataEvento);
    $('infoLocal').textContent = ev.local;
    $('infoRetirada').textContent = ev.retiradaInicio
      ? data(ev.retiradaInicio, { day: '2-digit', month: '2-digit' }) + ', das ' + hora(ev.retiradaInicio)
        + (ev.retiradaFim ? ' às ' + hora(ev.retiradaFim) : '')
      : 'No dia do evento';
    if (ev.contato) $('infoContato').textContent = ev.contato;
    if (ev.regras) $('infoRegras').textContent = ev.regras;
  }

  function mostrarProdutos(c) {
    estado.vendasAbertas = !!c.vendasAbertas;
    estado.produtos = c.itens || [];
    mostrarAvisoCardapio(c.vendasAbertas ? '' : 'As vendas deste evento não estão abertas no momento.');
    // remove do carrinho o que nao existe mais
    for (const id of [...estado.carrinho.keys()]) {
      if (!estado.produtos.some((p) => p.produtoId === id)) estado.carrinho.delete(id);
    }
    desenharProdutos();
    atualizarCarrinho();
  }

  function desenharProdutos() {
    if (!estado.produtos.length) {
      $('produtos').innerHTML = '<p class="vazio">Cardápio em preparação.</p>';
      return;
    }
    $('produtos').innerHTML = estado.produtos.map((p) => {
      let estoque = '<span class="produto__estoque">Disponível</span>';
      if (p.esgotado) {
        estoque = '<span class="produto__estoque produto__estoque--pouco">Esgotado</span>';
      } else if (p.disponivel != null) {
        const pouco = p.disponivel <= 20;
        estoque = `<span class="produto__estoque${pouco ? ' produto__estoque--pouco' : ''}">${pouco ? 'Últimas ' + p.disponivel + ' unidades!' : p.disponivel + ' disponíveis'}</span>`;
      }
      const qtd = estado.carrinho.get(p.produtoId) || 0;
      const podeMais = estado.vendasAbertas && !p.esgotado && (p.disponivel == null || qtd < p.disponivel) && qtd < 50;
      return `
        <article class="produto${p.esgotado ? ' produto--esgotado' : ''}">
          <div class="produto__topo">
            <div class="produto__selo"><img src="img/simbolo.svg" alt=""></div>
            ${p.loteNome ? `<span class="etiqueta etiqueta--oliva">${esc(p.loteNome)}</span>` : ''}
          </div>
          <h3>${esc(p.nome)}</h3>
          <p>${esc(p.descricao || '')}</p>
          <div class="produto__preco">${dinheiro(p.precoAtual)}</div>
          ${estoque}
          <div class="qtd">
            <button type="button" data-acao="menos" data-id="${p.produtoId}" aria-label="Remover ${esc(p.nome)}" ${qtd > 0 ? '' : 'disabled'}>−</button>
            <span aria-live="polite">${qtd}</span>
            <button type="button" data-acao="mais" data-id="${p.produtoId}" aria-label="Adicionar ${esc(p.nome)}" ${podeMais ? '' : 'disabled'}>+</button>
          </div>
        </article>`;
    }).join('');
  }

  $('produtos').addEventListener('click', (e) => {
    const b = e.target.closest('button[data-acao]');
    if (!b) return;
    const id = Number(b.dataset.id);
    const atual = estado.carrinho.get(id) || 0;
    if (b.dataset.acao === 'mais') estado.carrinho.set(id, atual + 1);
    else if (atual <= 1) estado.carrinho.delete(id);
    else estado.carrinho.set(id, atual - 1);
    desenharProdutos();
    atualizarCarrinho();
  });

  function itensDoCarrinho() {
    return [...estado.carrinho.entries()].map(([id, qtd]) => {
      const p = estado.produtos.find((x) => x.produtoId === id);
      return { produto: p, qtd, subtotal: p ? p.precoAtual * qtd : 0 };
    }).filter((i) => i.produto);
  }

  function atualizarCarrinho() {
    const itens = itensDoCarrinho();
    const qtd = itens.reduce((s, i) => s + i.qtd, 0);
    const total = itens.reduce((s, i) => s + i.subtotal, 0);
    $('carrinho').hidden = qtd === 0;
    document.body.style.paddingBottom = qtd ? '96px' : '';
    $('carrinhoQtd').textContent = qtd + (qtd === 1 ? ' item' : ' itens');
    $('carrinhoTotal').textContent = dinheiro(total);
  }

  // =========================================================
  // Checkout
  // =========================================================
  const dialogo = $('checkout');

  function mostrarPasso(id) {
    ['passoRevisar', 'passoPagar', 'passoPronto'].forEach((p) => { $(p).hidden = p !== id; });
  }

  $('botaoFinalizar').addEventListener('click', () => {
    if (!logado()) {
      estado.finalizarDepoisDoLogin = true;
      $('loginErro').textContent = 'Entre ou crie sua conta para finalizar o pedido.';
      document.getElementById('meus-pedidos').scrollIntoView({ behavior: 'smooth' });
      setTimeout(() => $('email').focus(), 500);
      return;
    }
    abrirCheckout();
  });

  function abrirCheckout() {
    const itens = itensDoCarrinho();
    if (!itens.length) return;
    $('resumoItens').innerHTML = itens.map((i) =>
      `<li><span>${i.qtd}x ${esc(i.produto.nome)}</span><strong>${dinheiro(i.subtotal)}</strong></li>`).join('');
    $('resumoTotal').textContent = dinheiro(itens.reduce((s, i) => s + i.subtotal, 0));
    $('checkoutErro').textContent = '';
    $('botaoConfirmar').disabled = false;
    mostrarPasso('passoRevisar');
    dialogo.showModal();
  }

  $('fecharCheckout').addEventListener('click', () => dialogo.close());
  ['botaoVerPedidos', 'botaoConcluir'].forEach((id) => $(id).addEventListener('click', () => dialogo.close()));
  dialogo.addEventListener('close', () => { if (logado()) mostrarPedidos(); });

  // 1) cria o pedido  2) inicia o pagamento
  $('botaoConfirmar').addEventListener('click', async () => {
    const botao = $('botaoConfirmar');
    botao.disabled = true;
    $('checkoutErro').textContent = '';
    const metodo = document.querySelector('input[name="metodo"]:checked').value;
    try {
      const pedido = await api('/api/pedidos', {
        metodo: 'POST', auth: true,
        corpo: {
          eventoId: estado.evento.id,
          origem: 'SITE',
          itens: itensDoCarrinho().map((i) => ({ produtoId: i.produto.produtoId, quantidade: i.qtd })),
          observacao: $('observacao').value.trim() || null
        }
      });
      estado.pedidoAtual = pedido;
      estado.carrinho.clear();
      atualizarCarrinho();
      carregarCardapio();
      const pagamento = await api('/api/pedidos/' + pedido.id + '/pagamentos', { metodo: 'POST', auth: true, corpo: { metodo } });
      estado.pagamentoAtual = pagamento;
      mostrarPagamento(pedido, pagamento);
    } catch (e) {
      if (e.status === 401) return sessaoExpirada();
      $('checkoutErro').textContent = mensagemErro(e);
      botao.disabled = false;
    }
  });

  function mostrarPagamento(pedido, pg) {
    $('pagNumero').textContent = pedido.numero;
    $('pagTotal').textContent = dinheiro(pedido.valorTotal);
    $('pagErro').textContent = '';
    $('blocoPix').hidden = pg.metodo !== 'PIX';
    $('pixCodigo').textContent = pg.codigoPix || '';
    const dinheiroNoCaixa = pg.metodo === 'DINHEIRO';
    $('botaoSimular').hidden = dinheiroNoCaixa;
    $('botaoSimular').disabled = false;
    $('botaoSimular').textContent = pg.metodo === 'PIX' ? 'Simular Pix pago' : 'Simular cartão aprovado';
    $('pagInstrucao').textContent = dinheiroNoCaixa
      ? 'Vá ao caixa do evento e informe o número ' + pedido.numero + '. Assim que a organização confirmar, o QR Code aparece em Meus pedidos.'
      : 'Pague até as ' + hora(pedido.expiraEm) + ' para manter a reserva. (Simulação: o botão abaixo faz o papel do banco.)';
    mostrarPasso('passoPagar');
  }

  $('copiarPix').addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText($('pixCodigo').textContent);
      $('copiarPix').textContent = 'Copiado!';
      setTimeout(() => { $('copiarPix').textContent = 'Copiar código'; }, 1500);
    } catch (e) { /* sem acesso a area de transferencia */ }
  });

  // 3) aprova (simulado) e mostra o QR Code
  $('botaoSimular').addEventListener('click', async () => {
    const botao = $('botaoSimular');
    botao.disabled = true;
    try {
      await api('/api/pagamentos/' + estado.pagamentoAtual.id + '/simular-aprovacao', { metodo: 'POST', auth: true });
      const c = await api('/api/pedidos/' + estado.pedidoAtual.id + '/comprovante', { auth: true });
      $('qrPronto').src = 'data:image/png;base64,' + c.qrCodeBase64;
      $('codigoPronto').textContent = c.codigoRetirada;
      mostrarPasso('passoPronto');
    } catch (e) {
      if (e.status === 401) return sessaoExpirada();
      $('pagErro').textContent = mensagemErro(e);
      botao.disabled = false;
    }
  });

  // =========================================================
  // Conta: entrar / criar conta
  // =========================================================
  function alternarModo(cadastro) {
    estado.modoCadastro = cadastro;
    $('abaEntrar').classList.toggle('ativa', !cadastro);
    $('abaCriar').classList.toggle('ativa', cadastro);
    $('abaEntrar').setAttribute('aria-selected', String(!cadastro));
    $('abaCriar').setAttribute('aria-selected', String(cadastro));
    $('camposCadastro').hidden = !cadastro;
    $('senha').autocomplete = cadastro ? 'new-password' : 'current-password';
    $('botaoEntrar').textContent = cadastro ? 'Criar conta' : 'Entrar';
    $('loginErro').textContent = '';
  }
  $('abaEntrar').addEventListener('click', () => alternarModo(false));
  $('abaCriar').addEventListener('click', () => alternarModo(true));

  $('formLogin').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = $('email').value.trim();
    const senha = $('senha').value;
    const nome = $('nome').value.trim();
    $('loginErro').textContent = '';
    if (!email || !senha || (estado.modoCadastro && !nome)) {
      $('loginErro').textContent = estado.modoCadastro ? 'Informe nome, e-mail e senha.' : 'Informe e-mail e senha.';
      return;
    }
    if (estado.modoCadastro && senha.length < 6) {
      $('loginErro').textContent = 'A senha deve ter pelo menos 6 caracteres.';
      return;
    }
    const botao = $('botaoEntrar');
    const textoOriginal = botao.textContent;
    botao.disabled = true;
    botao.textContent = estado.modoCadastro ? 'Criando conta…' : 'Entrando…';
    try {
      const r = estado.modoCadastro
        ? await api('/api/auth/registrar', { metodo: 'POST', corpo: { nome, email, telefone: $('telefone').value.trim() || null, senha } })
        : await api('/api/auth/login', { metodo: 'POST', corpo: { login: email, senha } });
      if (r.usuario.perfil !== 'COMPRADOR') {
        $('loginErro').textContent = 'Esta conta é da equipe. Use o aplicativo para acessar o painel.';
        return;
      }
      guardar(CHAVE_TOKEN, r.token);
      guardar('abadeus_nome', r.usuario.nome);
      guardar('abadeus_email', r.usuario.email);
      $('senha').value = '';
      await mostrarPedidos();
      if (estado.finalizarDepoisDoLogin && estado.carrinho.size) {
        estado.finalizarDepoisDoLogin = false;
        abrirCheckout();
      }
    } catch (err) {
      $('loginErro').textContent = mensagemErro(err);
    } finally {
      botao.disabled = false;
      botao.textContent = textoOriginal;
    }
  });

  function sair() {
    guardar(CHAVE_TOKEN, null);
    guardar('abadeus_nome', null);
    guardar('abadeus_email', null);
    $('painelPedidos').hidden = true;
    $('painelLogin').hidden = false;
  }
  $('botaoSair').addEventListener('click', sair);

  function sessaoExpirada() {
    if (dialogo.open) dialogo.close();
    sair();
    $('loginErro').textContent = 'Sua sessão expirou. Entre novamente.';
    document.getElementById('meus-pedidos').scrollIntoView({ behavior: 'smooth' });
  }

  // =========================================================
  // Meus pedidos
  // =========================================================
  async function mostrarPedidos() {
    $('painelLogin').hidden = true;
    $('painelPedidos').hidden = false;
    $('nomeUsuario').textContent = 'Olá, ' + (ler('abadeus_nome') || '').split(' ')[0] + '!';
    $('emailUsuario').textContent = ler('abadeus_email') || '';
    const lista = $('listaPedidos');
    lista.innerHTML = '<p class="vazio">Carregando pedidos…</p>';
    try {
      const pedidos = await api('/api/pedidos/meus', { auth: true });
      if (!pedidos.length) {
        lista.innerHTML = '<p class="vazio">Você ainda não fez nenhum pedido.<br>Escolha sua paella no <a href="#cardapio">cardápio</a>!</p>';
        return;
      }
      lista.innerHTML = pedidos.map((p) => `
        <article class="pedido" data-id="${p.id}">
          <div class="pedido__topo"><span class="pedido__numero">${esc(p.numero)}</span>${etiqueta(p.status)}</div>
          <p class="pedido__itens">${p.itens.map((i) => esc(i.quantidade + 'x ' + i.produtoNome)).join(' · ')}</p>
          <div class="pedido__rodape"><span>${dataHora(p.criadoEm)} · ${esc(p.origem)}</span><span class="pedido__total">${dinheiro(p.valorTotal)}</span></div>
          ${p.qrCodeDisponivel ? '<div class="pedido__qr-espaco"></div>' : ''}
          ${pagarDepois(p)}
        </article>`).join('');
      pedidos.filter((p) => p.qrCodeDisponivel).forEach(carregarQr);
    } catch (err) {
      if (err.status === 401) {
        sessaoExpirada();
      } else {
        lista.innerHTML = '<p class="vazio">Não foi possível carregar seus pedidos.</p>';
      }
    }
  }

  /** Pedido aguardando pagamento com Pix/cartao pendente: botao para concluir. */
  function pagarDepois(p) {
    if (p.status !== 'AGUARDANDO_PAGAMENTO') return '';
    const pg = (p.pagamentos || []).find((x) => x.status === 'PENDENTE');
    if (!pg || pg.metodo === 'DINHEIRO') {
      return '<p class="pequeno">Pague no caixa do evento informando o número do pedido.</p>';
    }
    return `<button class="botao botao--pequeno" type="button" data-pagar="${p.id}" data-pagamento="${pg.id}">Concluir pagamento</button>`;
  }

  $('listaPedidos').addEventListener('click', async (e) => {
    const b = e.target.closest('button[data-pagar]');
    if (!b) return;
    try {
      const pedido = await api('/api/pedidos/' + b.dataset.pagar, { auth: true });
      const pg = (pedido.pagamentos || []).find((x) => String(x.id) === b.dataset.pagamento);
      estado.pedidoAtual = pedido;
      estado.pagamentoAtual = pg;
      mostrarPagamento(pedido, pg);
      dialogo.showModal();
    } catch (err) {
      if (err.status === 401) sessaoExpirada();
    }
  });

  async function carregarQr(pedido) {
    try {
      const c = await api('/api/pedidos/' + pedido.id + '/comprovante', { auth: true });
      const espaco = document.querySelector(`.pedido[data-id="${pedido.id}"] .pedido__qr-espaco`);
      if (!espaco) return;
      const retirado = c.status === 'RETIRADO';
      espaco.outerHTML = `
        <div class="pedido__qr${retirado ? ' pedido__qr--retirado' : ''}">
          <img src="data:image/png;base64,${c.qrCodeBase64}" alt="QR Code do pedido ${esc(c.numero)}">
          <div>
            <strong>${retirado ? 'Já retirado' : 'Apresente na retirada'}</strong><br>
            <code>${esc(c.codigoRetirada)}</code><br>
            <small>${esc(c.local || '')}</small>
          </div>
        </div>`;
    } catch (e) { /* QR indisponivel */ }
  }

  // ---------------- Inicio ----------------
  carregarCardapio();
  if (logado()) mostrarPedidos();
})();
