/**
 * NEXUM — Aplicação Frontend Reativa e Assíncrona (Vanilla JS / Fetch API)
 */

document.addEventListener('DOMContentLoaded', () => {
  // Inicialização de componentes e dados
  initTabs();
  initModalidadeSelector();
  initEtapasManager();
  initCalculoSimulacao();
  initFormServico();
  initClientes();
  initOrcamentos();
  initFinanceiro();

  // Carregamento inicial de dados
  carregarClientes();
  carregarOrcamentos();
  carregarCobrancas();
  carregarExtrato();

  // Preenche datas de hoje por padrão
  const hoje = new Date().toISOString().split('T')[0];
  document.getElementById('servico-data').value = hoje;
  document.getElementById('orcamento-data-emissao').value = hoje;
  
  // Vencimento padrão: +15 dias
  const dataVenc = new Date();
  dataVenc.setDate(dataVenc.getDate() + 15);
  document.getElementById('cobranca-vencimento').value = dataVenc.toISOString().split('T')[0];
});

/* ==========================================================================
   1. SISTEMA DE NOTIFICAÇÕES (TOAST)
   ========================================================================== */
function showToast(mensagem, tipo = 'success') {
  const toast = document.getElementById('toast');
  toast.textContent = mensagem;
  toast.className = `toast toast-${tipo}`;
  toast.classList.remove('hidden');

  setTimeout(() => {
    toast.classList.add('hidden');
  }, 4000);
}

/* ==========================================================================
   2. GERENCIAMENTO DE ABAS
   ========================================================================== */
function initTabs() {
  const tabButtons = document.querySelectorAll('.tab-btn');
  const tabContents = document.querySelectorAll('.tab-content');

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      tabButtons.forEach(b => b.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const targetId = btn.dataset.tab;
      document.getElementById(targetId).classList.add('active');

      if (targetId === 'tab-clientes') carregarClientes();
      if (targetId === 'tab-orcamentos') carregarOrcamentos();
      if (targetId === 'tab-financeiro') {
        carregarCobrancas();
        carregarExtrato();
      }
    });
  });
}

/* ==========================================================================
   3. SELETOR DE MODALIDADE POLIMÓRFICA & CAMPOS DINÂMICOS
   ========================================================================== */
function initModalidadeSelector() {
  const radios = document.querySelectorAll('input[name="modalidade"]');
  const painelM2 = document.getElementById('campos-m2');
  const painelEtapa = document.getElementById('campos-etapa');
  const painelHora = document.getElementById('campos-hora');
  const painelPerc = document.getElementById('campos-perc');
  const memoriaTipo = document.getElementById('memoria-tipo');

  const paineis = {
    METRO_QUADRADO: { el: painelM2, label: 'Metro Quadrado (m²)' },
    ETAPA: { el: painelEtapa, label: 'Etapas de Projeto' },
    HORA: { el: painelHora, label: 'Hora Técnica' },
    PERCENTUAL_OBRA: { el: painelPerc, label: 'Percentual da Obra' }
  };

  radios.forEach(radio => {
    radio.addEventListener('change', () => {
      Object.values(paineis).forEach(p => p.el.classList.add('hidden'));

      const selecionado = paineis[radio.value];
      if (selecionado) {
        selecionado.el.classList.remove('hidden');
        memoriaTipo.textContent = selecionado.label;
      }
      simularCalculo();
    });
  });
}

/* ==========================================================================
   4. GERENCIAMENTO DE ETAPAS DINÂMICAS (SERVICO_POR_ETAPA)
   ========================================================================== */
function initEtapasManager() {
  const lista = document.getElementById('lista-etapas');
  const btnAdd = document.getElementById('btn-add-etapa');

  btnAdd.addEventListener('click', () => {
    const row = document.createElement('div');
    row.className = 'etapa-item-row';
    row.innerHTML = `
      <select class="etapa-tipo">
        <option value="LEVANTAMENTO">1. Levantamento Cadastral</option>
        <option value="ESTUDO_PRELIMINAR">2. Estudo Preliminar</option>
        <option value="ANTEPROJETO">3. Anteprojeto</option>
        <option value="PROJETO_LEGAL">4. Projeto Legal / Prefeitura</option>
        <option value="PROJETO_EXECUTIVO" selected>5. Projeto Executivo Detalhado</option>
        <option value="EXECUCAO_ACOMPANHAMENTO">6. Execução e Acompanhamento</option>
      </select>
      <input type="number" class="etapa-valor" placeholder="Valor (R$)" step="50" min="0">
      <button type="button" class="btn-remove-etapa" title="Remover etapa">Remover</button>
    `;
    lista.appendChild(row);
    row.querySelector('.btn-remove-etapa').addEventListener('click', () => {
      row.remove();
      simularCalculo();
    });
    row.querySelector('.etapa-valor').addEventListener('input', simularCalculo);
  });

  // Listener para itens iniciais
  lista.querySelectorAll('.btn-remove-etapa').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.target.closest('.etapa-item-row').remove();
      simularCalculo();
    });
  });
  lista.querySelectorAll('.etapa-valor').forEach(input => {
    input.addEventListener('input', simularCalculo);
  });
}

/* ==========================================================================
   5. CÁLCULO E SIMULAÇÃO DE HONORÁRIOS EM TEMPO REAL
   ========================================================================== */
function initCalculoSimulacao() {
  const inputsParaMonitorar = [
    'm2-area', 'm2-valor', 'hora-horas', 'hora-valor', 'perc-custo', 'perc-taxa'
  ];
  inputsParaMonitorar.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('input', simularCalculo);
  });

  document.getElementById('btn-calcular').addEventListener('click', simularCalculo);
}

function coletarDadosModalidade() {
  const modalidade = document.querySelector('input[name="modalidade"]:checked').value;
  const payload = { tipo: modalidade, imposto: 0.06 };

  if (modalidade === 'METRO_QUADRADO') {
    payload.area = parseFloat(document.getElementById('m2-area').value) || 0;
    payload.valorMetroQuadrado = parseFloat(document.getElementById('m2-valor').value) || 0;
  } else if (modalidade === 'ETAPA') {
    const rows = document.querySelectorAll('#lista-etapas .etapa-item-row');
    const itens = [];
    rows.forEach(r => {
      const etapa = r.querySelector('.etapa-tipo').value;
      const valor = parseFloat(r.querySelector('.etapa-valor').value) || 0;
      itens.push({ etapa, valor });
    });
    payload.itensEtapas = itens;
  } else if (modalidade === 'HORA') {
    payload.horasTrabalhadas = parseFloat(document.getElementById('hora-horas').value) || 0;
    payload.valorHora = parseFloat(document.getElementById('hora-valor').value) || 0;
  } else if (modalidade === 'PERCENTUAL_OBRA') {
    payload.custoTotalObra = parseFloat(document.getElementById('perc-custo').value) || 0;
    const taxaInput = parseFloat(document.getElementById('perc-taxa').value) || 0;
    payload.percentual = taxaInput / 100.0;
  }

  return payload;
}

async function simularCalculo() {
  const payload = coletarDadosModalidade();

  try {
    const resp = await fetch('/api/servicos/calcular', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (resp.ok) {
      const memoria = await resp.json();
      atualizarPainelMemoria(memoria);
    }
  } catch (err) {
    console.error('Erro na simulação:', err);
  }
}

function atualizarPainelMemoria(m) {
  document.getElementById('memoria-base').textContent = formatarMoeda(m.valorBase || 0);
  document.getElementById('memoria-imposto').textContent = formatarMoeda(m.valorImposto || 0);
  document.getElementById('memoria-total').textContent = formatarMoeda(m.valorTotalFinal || 0);
}

/* ==========================================================================
   6. SALVAR SERVIÇO PRECIFICADO
   ========================================================================== */
function initFormServico() {
  const form = document.getElementById('form-servico');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const clienteId = document.getElementById('servico-cliente').value;
    if (!clienteId) {
      showToast('Por favor, selecione um cliente cadastrado.', 'error');
      return;
    }

    const payload = coletarDadosModalidade();
    payload.clienteId = parseInt(clienteId);
    payload.descricao = document.getElementById('servico-descricao').value;
    payload.data = document.getElementById('servico-data').value;
    payload.padrao = document.getElementById('servico-padrao').value;

    try {
      const resp = await fetch('/api/servicos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const resData = await resp.json();
      if (!resp.ok) {
        showToast(resData.error || 'Erro ao salvar serviço.', 'error');
        return;
      }

      showToast('Serviço de arquitetura precificado e gravado com sucesso!');
      // Abre modal de emissão de orçamento com esse serviço pré-selecionado
      abrirModalOrcamentoComServico(resData.id, payload.clienteId);
    } catch (err) {
      showToast('Falha na comunicação com o servidor: ' + err.message, 'error');
    }
  });
}

/* ==========================================================================
   7. GESTÃO DE CLIENTES
   ========================================================================== */
function initClientes() {
  const form = document.getElementById('form-cliente');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const payload = {
      nome: document.getElementById('cliente-nome').value,
      cpf: document.getElementById('cliente-cpf').value,
      whatsApp: document.getElementById('cliente-whatsapp').value
    };

    try {
      const resp = await fetch('/api/clientes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const resData = await resp.json();
      if (!resp.ok) {
        showToast(resData.error || 'Erro ao cadastrar cliente.', 'error');
        return;
      }

      showToast(`Cliente "${resData.nome}" cadastrado com sucesso!`);
      form.reset();
      carregarClientes();
    } catch (err) {
      showToast('Falha ao conectar ao servidor: ' + err.message, 'error');
    }
  });
}

async function carregarClientes() {
  try {
    const resp = await fetch('/api/clientes');
    if (!resp.ok) return;

    const clientes = await resp.json();
    renderTabelaClientes(clientes);
    atualizarDropdownsClientes(clientes);
  } catch (err) {
    console.error('Erro ao listar clientes:', err);
  }
}

function renderTabelaClientes(clientes) {
  const tbody = document.getElementById('tabela-clientes-corpo');
  if (!clientes || clientes.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" class="text-center">Nenhum cliente cadastrado ainda.</td></tr>';
    return;
  }

  tbody.innerHTML = clientes.map(c => `
    <tr>
      <td><strong>${escapeHtml(c.nome)}</strong></td>
      <td>${escapeHtml(c.cpf)}</td>
      <td>
        <a href="https://wa.me/${c.whatsApp}" target="_blank" class="btn btn-whatsapp btn-sm">
          WhatsApp
        </a>
      </td>
      <td>
        <button class="btn btn-danger btn-sm" onclick="deletarCliente(${c.id})">Excluir</button>
      </td>
    </tr>
  `).join('');
}

function atualizarDropdownsClientes(clientes) {
  const selects = [
    document.getElementById('servico-cliente'),
    document.getElementById('orcamento-cliente-select')
  ];

  selects.forEach(sel => {
    if (!sel) return;
    const atual = sel.value;
    sel.innerHTML = '<option value="">Selecione um cliente...</option>' +
      clientes.map(c => `<option value="${c.id}">${escapeHtml(c.nome)} (${escapeHtml(c.cpf)})</option>`).join('');
    if (atual) sel.value = atual;
  });
}

async function deletarCliente(id) {
  if (!confirm('Deseja realmente remover este cliente?')) return;
  try {
    const resp = await fetch(`/api/clientes/${id}`, { method: 'DELETE' });
    if (resp.ok) {
      showToast('Cliente removido com sucesso.');
      carregarClientes();
    } else {
      const err = await resp.json();
      showToast(err.error || 'Erro ao remover cliente.', 'error');
    }
  } catch (e) {
    showToast('Falha na operação: ' + e.message, 'error');
  }
}

/* ==========================================================================
   8. EMISSÃO DE ORÇAMENTOS & DISPARO WHATSAPP
   ========================================================================== */
function initOrcamentos() {
  const btnAbrir = document.getElementById('btn-abrir-modal-orcamento');
  const modal = document.getElementById('modal-orcamento');
  const btnFechar = document.getElementById('btn-fechar-modal-orcamento');
  const btnCancelar = document.getElementById('btn-cancelar-modal-orcamento');
  const form = document.getElementById('form-novo-orcamento');

  btnAbrir.addEventListener('click', () => {
    carregarServicosParaOrcamento();
    modal.classList.remove('hidden');
  });

  const fechar = () => modal.classList.add('hidden');
  btnFechar.addEventListener('click', fechar);
  btnCancelar.addEventListener('click', fechar);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
      clienteId: document.getElementById('orcamento-cliente-select').value,
      servicoId: document.getElementById('orcamento-servico-select').value,
      validade: document.getElementById('orcamento-validade').value,
      dataEmissao: document.getElementById('orcamento-data-emissao').value
    };

    try {
      const resp = await fetch('/api/orcamentos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const resData = await resp.json();
      if (!resp.ok) {
        showToast(resData.error || 'Erro ao emitir proposta.', 'error');
        return;
      }

      showToast('Proposta emitida com sucesso!');
      fechar();
      carregarOrcamentos();

      // Se retornou link de WhatsApp, oferece para abrir imediatamente
      if (resData.whatsappUrl && confirm('Proposta emitida com sucesso! Deseja abrir o WhatsApp agora para enviar a proposta ao cliente?')) {
        window.open(resData.whatsappUrl, '_blank');
      }
    } catch (err) {
      showToast('Erro ao emitir proposta: ' + err.message, 'error');
    }
  });
}

async function carregarServicosParaOrcamento() {
  try {
    const resp = await fetch('/api/servicos');
    if (!resp.ok) return;
    const servicos = await resp.json();
    const select = document.getElementById('orcamento-servico-select');
    select.innerHTML = '<option value="">Selecione o serviço...</option>' +
      servicos.map(s => `<option value="${s.id}">#${s.id} - ${escapeHtml(s.descricao)} (${formatarMoeda(s.valorTotal)})</option>`).join('');
  } catch (err) {
    console.error('Erro ao carregar serviços:', err);
  }
}

async function abrirModalOrcamentoComServico(servicoId, clienteId) {
  await carregarServicosParaOrcamento();
  const modal = document.getElementById('modal-orcamento');
  document.getElementById('orcamento-cliente-select').value = clienteId;
  document.getElementById('orcamento-servico-select').value = servicoId;
  modal.classList.remove('hidden');
}

async function carregarOrcamentos() {
  try {
    const resp = await fetch('/api/orcamentos');
    if (!resp.ok) return;
    const orcamentos = await resp.json();
    renderTabelaOrcamentos(orcamentos);
    atualizarDropdownOrcamentos(orcamentos);
  } catch (err) {
    console.error('Erro ao carregar orçamentos:', err);
  }
}

function renderTabelaOrcamentos(orcamentos) {
  const tbody = document.getElementById('tabela-orcamentos-corpo');
  if (!orcamentos || orcamentos.length === 0) {
    tbody.innerHTML = '<tr><td colspan="9" class="text-center">Nenhuma proposta emitida até o momento.</td></tr>';
    return;
  }

  tbody.innerHTML = orcamentos.map(o => {
    const clienteNome = o.cliente ? o.cliente.nome : 'N/A';
    const servicoDesc = o.servico ? o.servico.descricao : 'N/A';
    const statusBadge = getStatusBadge(o.statusOrcamento);

    return `
      <tr>
        <td>#${o.id}</td>
        <td><strong>${escapeHtml(clienteNome)}</strong></td>
        <td>${escapeHtml(servicoDesc)}</td>
        <td>${escapeHtml(o.dataEmissao)}</td>
        <td>${o.validade} dias</td>
        <td><strong>${formatarMoeda(o.valorTotal)}</strong></td>
        <td>${statusBadge}</td>
        <td>
          <button class="btn btn-whatsapp btn-sm" onclick="abrirWhatsAppOrcamento(${o.id})">
            WhatsApp
          </button>
        </td>
        <td>
          ${o.statusOrcamento === 'PENDENTE' ? `
            <button class="btn btn-secondary btn-sm" onclick="mudarStatusOrcamento(${o.id}, 'APROVADO')">Aprovar</button>
            <button class="btn btn-secondary btn-sm" onclick="mudarStatusOrcamento(${o.id}, 'RECUSADO')">Recusar</button>
          ` : `
            <button class="btn btn-secondary btn-sm" onclick="abrirModalCobrancaParaOrcamento(${o.id})">Gerar Cobrança</button>
          `}
        </td>
      </tr>
    `;
  }).join('');
}

async function abrirWhatsAppOrcamento(id) {
  try {
    const resp = await fetch(`/api/orcamentos/${id}/whatsapp`);
    const data = await resp.json();
    if (data.whatsappUrl) {
      window.open(data.whatsappUrl, '_blank');
    }
  } catch (e) {
    showToast('Erro ao obter link do WhatsApp.', 'error');
  }
}

async function mudarStatusOrcamento(id, novoStatus) {
  try {
    const resp = await fetch(`/api/orcamentos/${id}/status`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: novoStatus })
    });
    if (resp.ok) {
      showToast(`Orçamento marcado como ${novoStatus}!`);
      carregarOrcamentos();
      if (novoStatus === 'APROVADO') {
        abrirModalCobrancaParaOrcamento(id);
      }
    }
  } catch (e) {
    showToast('Erro ao atualizar status: ' + e.message, 'error');
  }
}

/* ==========================================================================
   9. FLUXO FINANCEIRO & COBRANÇAS
   ========================================================================== */
function initFinanceiro() {
  const btnAbrir = document.getElementById('btn-abrir-modal-cobranca');
  const modal = document.getElementById('modal-cobranca');
  const btnFechar = document.getElementById('btn-fechar-modal-cobranca');
  const btnCancelar = document.getElementById('btn-cancelar-modal-cobranca');
  const form = document.getElementById('form-nova-cobranca');

  btnAbrir.addEventListener('click', () => modal.classList.remove('hidden'));
  const fechar = () => modal.classList.add('hidden');
  btnFechar.addEventListener('click', fechar);
  btnCancelar.addEventListener('click', fechar);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
      orcamentoId: document.getElementById('cobranca-orcamento-select').value,
      dataVencimento: document.getElementById('cobranca-vencimento').value
    };

    try {
      const resp = await fetch('/api/cobrancas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const resData = await resp.json();
      if (!resp.ok) {
        showToast(resData.error || 'Erro ao gerar cobrança.', 'error');
        return;
      }

      showToast('Cobrança gerada com sucesso!');
      fechar();
      carregarCobrancas();
      carregarExtrato();
    } catch (err) {
      showToast('Erro ao gerar cobrança: ' + err.message, 'error');
    }
  });
}

function abrirModalCobrancaParaOrcamento(orcamentoId) {
  const modal = document.getElementById('modal-cobranca');
  document.getElementById('cobranca-orcamento-select').value = orcamentoId;
  modal.classList.remove('hidden');
}

function atualizarDropdownOrcamentos(orcamentos) {
  const sel = document.getElementById('cobranca-orcamento-select');
  if (!sel) return;
  sel.innerHTML = '<option value="">Selecione um orçamento...</option>' +
    orcamentos.map(o => {
      const clienteNome = o.cliente ? o.cliente.nome : '';
      return `<option value="${o.id}">#${o.id} - ${escapeHtml(clienteNome)} (${formatarMoeda(o.valorTotal)})</option>`;
    }).join('');
}

async function carregarExtrato() {
  try {
    const resp = await fetch('/api/cobrancas/extrato');
    if (!resp.ok) return;
    const extrato = await resp.json();

    document.getElementById('kpi-faturado').textContent = formatarMoeda(extrato.totalFaturado || 0);
    document.getElementById('kpi-pagas-count').textContent = `${extrato.cobrancasPagas || 0} recebimentos`;

    document.getElementById('kpi-em-aberto').textContent = formatarMoeda(extrato.totalEmAberto || 0);
    document.getElementById('kpi-aberto-count').textContent = `${extrato.cobrancasEmAberto || 0} pendentes`;

    document.getElementById('kpi-total-cobrancas').textContent = extrato.totalCobrancas || 0;
  } catch (err) {
    console.error('Erro ao carregar extrato:', err);
  }
}

async function carregarCobrancas() {
  try {
    const resp = await fetch('/api/cobrancas');
    if (!resp.ok) return;
    const cobrancas = await resp.json();
    renderTabelaCobrancas(cobrancas);
  } catch (err) {
    console.error('Erro ao carregar cobranças:', err);
  }
}

function renderTabelaCobrancas(cobrancas) {
  const tbody = document.getElementById('tabela-cobrancas-corpo');
  if (!cobrancas || cobrancas.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="text-center">Nenhum registro financeiro encontrado.</td></tr>';
    return;
  }

  tbody.innerHTML = cobrancas.map(c => {
    const orc = c.orcamento;
    const clienteNome = (orc && orc.cliente) ? orc.cliente.nome : 'N/A';
    const orcDesc = orc ? `#${orc.id}` : 'N/A';
    const statusBadge = getStatusCobrancaBadge(c.statusCobranca);

    return `
      <tr>
        <td>#${c.id}</td>
        <td><strong>${escapeHtml(clienteNome)}</strong></td>
        <td>${orcDesc}</td>
        <td>${escapeHtml(c.dataVencimento)}</td>
        <td>${c.dataPagamento ? escapeHtml(c.dataPagamento) : '<span class="text-muted">—</span>'}</td>
        <td><strong>${formatarMoeda(c.valor)}</strong></td>
        <td>${statusBadge}</td>
        <td>
          <div style="display:flex; gap:0.4rem; flex-wrap:wrap;">
            ${c.statusCobranca !== 'PAGO' ? `
              <button class="btn btn-primary btn-sm" onclick="registrarPagamentoCobranca(${c.id})">Liquidar</button>
              <button class="btn btn-whatsapp btn-sm" onclick="abrirWhatsAppCobranca(${c.id})">Lembrete</button>
            ` : `
              <span style="color:var(--color-success); font-weight:600; font-size:0.85rem; align-self:center;">Liquidado</span>
            `}
            <button class="btn btn-danger btn-sm" onclick="deletarCobranca(${c.id})">Excluir</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

async function registrarPagamentoCobranca(id) {
  const dataHoje = new Date().toISOString().split('T')[0];
  try {
    const resp = await fetch(`/api/cobrancas/${id}/pagar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ dataPagamento: dataHoje })
    });
    if (resp.ok) {
      showToast('Pagamento registrado com sucesso!');
      carregarCobrancas();
      carregarExtrato();
    } else {
      const err = await resp.json();
      showToast(err.error || 'Erro ao liquidar cobrança.', 'error');
    }
  } catch (e) {
    showToast('Erro ao liquidar cobrança: ' + e.message, 'error');
  }
}

async function abrirWhatsAppCobranca(id) {
  try {
    const resp = await fetch(`/api/cobrancas/${id}/whatsapp`);
    const data = await resp.json();
    if (data.whatsappUrl) {
      window.open(data.whatsappUrl, '_blank');
    }
  } catch (e) {
    showToast('Erro ao gerar link de mensagem.', 'error');
  }
}

async function deletarCobranca(id) {
  if (!confirm('Deseja realmente excluir este registro financeiro?')) return;
  try {
    const resp = await fetch(`/api/cobrancas/${id}`, { method: 'DELETE' });
    if (resp.ok) {
      showToast('Registro financeiro excluído com sucesso.');
      carregarCobrancas();
      carregarExtrato();
    } else {
      const err = await resp.json();
      showToast(err.error || 'Erro ao excluir cobrança.', 'error');
    }
  } catch (e) {
    showToast('Falha na operação: ' + e.message, 'error');
  }
}

/* ==========================================================================
   UTILITÁRIOS GERAIS
   ========================================================================== */
function formatarMoeda(val) {
  return (val || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}

function getStatusBadge(status) {
  if (status === 'APROVADO') return '<span class="badge badge-success">Aprovado</span>';
  if (status === 'RECUSADO') return '<span class="badge badge-danger">Recusado</span>';
  return '<span class="badge badge-warning">Pendente</span>';
}

function getStatusCobrancaBadge(status) {
  if (status === 'PAGO') return '<span class="badge badge-success">Pago</span>';
  if (status === 'ATRASADO') return '<span class="badge badge-danger">Atrasado</span>';
  return '<span class="badge badge-warning">Em Aberto</span>';
}
