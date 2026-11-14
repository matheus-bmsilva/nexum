/**
 * NEXUM — Aplicação Frontend Reativa e Assíncrona (Vanilla JS / Fetch API)
 */

const TOKEN_KEY = 'nexum_auth_token';

/**
 * RBT12 do perfil do usuário logado, carregado automaticamente após autenticação.
 * Utilizado nos cálculos de Simples Nacional sem necessidade de input manual na tela de precificação.
 */
let usuarioRbt12 = 0;

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

/**
 * Fetch wrapper que anexa automaticamente o cabeçalho Authorization
 * e intercepta erros 401 para redirecionar à tela de autenticação.
 */
async function authFetch(url, options = {}) {
  const token = getToken();
  const headers = options.headers ? { ...options.headers } : {};

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const mergedOptions = { ...options, headers };
  const resp = await fetch(url, mergedOptions);

  if (resp.status === 401) {
    clearToken();
    mostrarTelaAuth();
    showToast('Sessão expirada. Por favor, acesse novamente.', 'error');
  }

  return resp;
}

document.addEventListener('DOMContentLoaded', () => {
  // Inicialização de componentes e eventos
  initAuth();
  configurarVisoresSenha();
  configurarFechamentoGlobalModais();
  initTabs();
  initModalidadeSelector();
  initEtapasManager();
  initCalculoSimulacao();
  configurarCamposMoeda();
  initFormServico();
  initClientes();
  initOrcamentos();
  initFinanceiro();
  initBuscaDinamica();

  // Preenche datas de hoje por padrão (fuso horário local, mantendo yyyy-MM-dd para <input type="date">)
  const hoje = obterDataLocalISO();
  const elDataServico = document.getElementById('servico-data');
  if (elDataServico) elDataServico.value = hoje;
  const elDataEmissao = document.getElementById('orcamento-data-emissao');
  if (elDataEmissao) elDataEmissao.value = hoje;

  // Vencimento padrão: +15 dias
  const dataVenc = new Date();
  dataVenc.setDate(dataVenc.getDate() + 15);
  const elDataVenc = document.getElementById('cobranca-vencimento');
  if (elDataVenc) elDataVenc.value = obterDataLocalISO(dataVenc);

  // Verifica estado de autenticação inicial
  verificarAutenticacao();
});

/* ==========================================================================
   1. SISTEMA DE AUTENTICAÇÃO, ONBOARDING & PERFIL
   ========================================================================== */
function initAuth() {
  const btnToggleLogin = document.getElementById('btn-toggle-login');
  const btnToggleRegister = document.getElementById('btn-toggle-register');
  const formLogin = document.getElementById('form-login');
  const formRegister = document.getElementById('form-register');
  const btnLogout = document.getElementById('btn-logout');
  const btnAbrirPerfil = document.getElementById('btn-abrir-perfil');
  const modalPerfil = document.getElementById('modal-perfil');
  const btnFecharPerfil = document.getElementById('btn-fechar-modal-perfil');
  const btnCancelarPerfil = document.getElementById('btn-cancelar-modal-perfil');
  const formPerfil = document.getElementById('form-perfil');

  // Alternância suave entre Login e Cadastro
  if (btnToggleLogin && btnToggleRegister) {
    btnToggleLogin.addEventListener('click', () => {
      btnToggleLogin.classList.add('active');
      btnToggleRegister.classList.remove('active');
      formLogin.classList.remove('hidden');
      formRegister.classList.add('hidden');
    });

    btnToggleRegister.addEventListener('click', () => {
      btnToggleRegister.classList.add('active');
      btnToggleLogin.classList.remove('active');
      formRegister.classList.remove('hidden');
      formLogin.classList.add('hidden');
    });
  }

  // Submit Login
  if (formLogin) {
    formLogin.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        identificador: document.getElementById('login-identificador').value,
        senha: document.getElementById('login-senha').value
      };

      try {
        const resp = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        const data = await resp.json();
        if (!resp.ok) {
          showToast(data.error || 'Erro ao efetuar login.', 'error');
          return;
        }

        setToken(data.token);
        mostrarPainelApp(data.usuario);
        showToast(`Bem-vindo ao Nexum, ${data.usuario.nome}!`);
        formLogin.reset();
      } catch (err) {
        showToast('Falha na comunicação: ' + err.message, 'error');
      }
    });
  }

  // Submit Registro
  if (formRegister) {
    formRegister.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        nome: document.getElementById('reg-nome').value,
        cnpj: document.getElementById('reg-cnpj').value,
        email: document.getElementById('reg-email').value,
        whatsApp: document.getElementById('reg-whatsapp').value,
        rbt12: parseNumeroBRL(document.getElementById('reg-rbt12').value),
        senha: document.getElementById('reg-senha').value
      };

      try {
        const resp = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        const data = await resp.json();
        if (!resp.ok) {
          showToast(data.error || 'Erro ao cadastrar escritório.', 'error');
          return;
        }

        setToken(data.token);
        mostrarPainelApp(data.usuario);
        showToast(`Conta criada com sucesso! Bem-vindo, ${data.usuario.nome}.`);
        formRegister.reset();
      } catch (err) {
        showToast('Falha na comunicação: ' + err.message, 'error');
      }
    });
  }

  // Logout
  if (btnLogout) {
    btnLogout.addEventListener('click', async () => {
      try {
        await authFetch('/api/auth/logout', { method: 'POST' });
      } catch (ignored) {}
      clearToken();
      mostrarTelaAuth();
      showToast('Sessão encerrada com sucesso.');
    });
  }

  // Modal Perfil
  if (btnAbrirPerfil && modalPerfil) {
    btnAbrirPerfil.addEventListener('click', async () => {
      try {
        const resp = await authFetch('/api/auth/perfil');
        if (!resp.ok) return;
        const u = await resp.json();

        document.getElementById('perfil-nome').value = u.nome || '';
        document.getElementById('perfil-cnpj').value = u.cnpj || '';
        document.getElementById('perfil-email').value = u.email || '';
        document.getElementById('perfil-whatsapp').value = u.whatsApp || '';
        const perfilRbt12 = document.getElementById('perfil-rbt12');
        if (perfilRbt12) {
          perfilRbt12.value = (u.rbt12 !== undefined && u.rbt12 !== null) ? formatarMoeda(u.rbt12) : '';
        }
        document.getElementById('perfil-nova-senha').value = '';

        modalPerfil.classList.remove('hidden');
      } catch (err) {
        showToast('Erro ao carregar dados do perfil.', 'error');
      }
    });

    const fecharPerfil = () => modalPerfil.classList.add('hidden');
    if (btnFecharPerfil) btnFecharPerfil.addEventListener('click', fecharPerfil);
    if (btnCancelarPerfil) btnCancelarPerfil.addEventListener('click', fecharPerfil);

    if (formPerfil) {
      formPerfil.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
          nome: document.getElementById('perfil-nome').value,
          email: document.getElementById('perfil-email').value,
          whatsApp: document.getElementById('perfil-whatsapp').value,
          rbt12: parseNumeroBRL(document.getElementById('perfil-rbt12').value),
          novaSenha: document.getElementById('perfil-nova-senha').value
        };

        try {
          const resp = await authFetch('/api/auth/perfil', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
          });

          const data = await resp.json();
          if (!resp.ok) {
            showToast(data.error || 'Erro ao atualizar perfil.', 'error');
            return;
          }

          document.getElementById('user-display-name').textContent = data.nome;
          usuarioRbt12 = (data && data.rbt12 !== undefined) ? data.rbt12 : 0;
          simularCalculo();
          showToast('Perfil atualizado com sucesso!');
          fecharPerfil();
        } catch (err) {
          showToast('Falha ao atualizar perfil: ' + err.message, 'error');
        }
      });
    }
  }
}

/**
 * Configura botões de alternância da visibilidade da senha (olho/visor)
 */
function configurarVisoresSenha() {
  const botoesVisor = document.querySelectorAll('.btn-toggle-senha');
  botoesVisor.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const wrapper = btn.closest('.password-wrapper');
      if (!wrapper) return;
      const input = wrapper.querySelector('input');
      if (!input) return;

      const iconOpen = btn.querySelector('.eye-icon-open');
      const iconClosed = btn.querySelector('.eye-icon-closed');

      if (input.type === 'password') {
        input.type = 'text';
        if (iconOpen) iconOpen.classList.add('hidden');
        if (iconClosed) iconClosed.classList.remove('hidden');
      } else {
        input.type = 'password';
        if (iconOpen) iconOpen.classList.remove('hidden');
        if (iconClosed) iconClosed.classList.add('hidden');
      }
      input.focus();
    });
  });
}

/**
 * Adiciona listeners globais para fechamento de modais com clique fora ou tecla Esc.
 */
function configurarFechamentoGlobalModais() {
  document.addEventListener('click', (e) => {
    if (e.target && e.target.classList && e.target.classList.contains('modal')) {
      e.target.classList.add('hidden');
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal:not(.hidden)').forEach(modal => {
        modal.classList.add('hidden');
      });
    }
  });
}

async function verificarAutenticacao() {
  const token = getToken();
  if (!token) {
    mostrarTelaAuth();
    return;
  }

  try {
    const resp = await authFetch('/api/auth/perfil');
    if (resp.ok) {
      const u = await resp.json();
      mostrarPainelApp(u);
    } else {
      clearToken();
      mostrarTelaAuth();
    }
  } catch (err) {
    clearToken();
    mostrarTelaAuth();
  }
}

function mostrarPainelApp(usuario) {
  document.getElementById('auth-section').classList.add('hidden');
  document.getElementById('app-container').classList.remove('hidden');
  document.getElementById('nav-user-panel').classList.remove('hidden');

  if (usuario && usuario.nome) {
    document.getElementById('user-display-name').textContent = usuario.nome;
  }

  // Carrega o RBT12 do perfil do usuário para cálculos tributários
  usuarioRbt12 = (usuario && usuario.rbt12) ? usuario.rbt12 : 0;
  simularCalculo();

  // Carrega os dados operacionais das abas
  carregarClientes();
  carregarOrcamentos();
  carregarCobrancas();
  carregarExtrato();
}

function mostrarTelaAuth() {
  document.getElementById('app-container').classList.add('hidden');
  document.getElementById('nav-user-panel').classList.add('hidden');
  document.getElementById('auth-section').classList.remove('hidden');
}

/* ==========================================================================
   2. SISTEMA DE NOTIFICAÇÕES (TOAST)
   ========================================================================== */
function showToast(mensagem, tipo = 'success') {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.textContent = mensagem;
  toast.className = `toast toast-${tipo}`;
  toast.classList.remove('hidden');

  setTimeout(() => {
    toast.classList.add('hidden');
  }, 4000);
}

/* ==========================================================================
   3. GERENCIAMENTO DE ABAS
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
      const targetContent = document.getElementById(targetId);
      if (targetContent) targetContent.classList.add('active');

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
   4. SELETOR DE MODALIDADE POLIMÓRFICA & CAMPOS DINÂMICOS
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
        if (memoriaTipo) memoriaTipo.textContent = selecionado.label;
      }
      simularCalculo();
    });
  });
}

/* ==========================================================================
   5. GERENCIAMENTO DE ETAPAS DINÂMICAS (SERVICO_POR_ETAPA)
   ========================================================================== */
function initEtapasManager() {
  const lista = document.getElementById('lista-etapas');
  const btnAdd = document.getElementById('btn-add-etapa');
  if (!btnAdd || !lista) return;

  btnAdd.addEventListener('click', () => {
    const row = document.createElement('div');
    row.className = 'etapa-item-row';
    row.innerHTML = `
      <select class="etapa-tipo">
        <option value="" selected>Selecione uma opção</option>
        <option value="LEVANTAMENTO">1. Levantamento Cadastral</option>
        <option value="ESTUDO_PRELIMINAR">2. Estudo Preliminar</option>
        <option value="ANTEPROJETO">3. Anteprojeto</option>
        <option value="PROJETO_LEGAL">4. Projeto Legal / Prefeitura</option>
        <option value="PROJETO_EXECUTIVO">5. Projeto Executivo Detalhado</option>
        <option value="EXECUCAO_ACOMPANHAMENTO">6. Execução e Acompanhamento</option>
      </select>
      <input type="text" class="etapa-valor input-moeda" placeholder="R$ 0,00" inputmode="decimal">
      <button type="button" class="btn-remove-etapa" title="Remover etapa">&times;</button>
    `;
    lista.appendChild(row);
    const novoInput = row.querySelector('.etapa-valor');
    aplicarMascaraMoeda(novoInput);

    row.querySelector('.btn-remove-etapa').addEventListener('click', () => {
      row.remove();
      simularCalculo();
    });
    row.querySelector('.etapa-tipo').addEventListener('change', simularCalculo);
    novoInput.addEventListener('input', simularCalculo);
  });

  lista.querySelectorAll('.btn-remove-etapa').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.target.closest('.etapa-item-row').remove();
      simularCalculo();
    });
  });
  lista.querySelectorAll('.etapa-tipo').forEach(select => {
    select.addEventListener('change', simularCalculo);
  });
  lista.querySelectorAll('.etapa-valor').forEach(input => {
    aplicarMascaraMoeda(input);
    input.addEventListener('input', simularCalculo);
  });
}

/* ==========================================================================
   6. CÁLCULO E SIMULAÇÃO DE HONORÁRIOS EM TEMPO REAL
   (Sem botão redundante: cálculo automático instantâneo com Simples Nacional)
   ========================================================================== */
function initCalculoSimulacao() {
  const inputsParaMonitorar = [
    'm2-area', 'm2-valor', 'hora-horas', 'hora-valor', 'perc-custo', 'perc-taxa'
  ];
  inputsParaMonitorar.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('input', simularCalculo);
  });
}

function coletarDadosModalidade() {
  const radioChecked = document.querySelector('input[name="modalidade"]:checked');
  const modalidade = radioChecked ? radioChecked.value : 'METRO_QUADRADO';
  const payload = { tipo: modalidade, rbt12: usuarioRbt12, imposto: 0.06 };

  if (modalidade === 'METRO_QUADRADO') {
    payload.area = parseNumeroBRL(document.getElementById('m2-area').value);
    payload.valorMetroQuadrado = parseNumeroBRL(document.getElementById('m2-valor').value);
  } else if (modalidade === 'ETAPA') {
    const rows = document.querySelectorAll('#lista-etapas .etapa-item-row');
    const itens = [];
    rows.forEach(r => {
      const select = r.querySelector('.etapa-tipo');
      const etapa = select ? select.value : '';
      const valor = parseNumeroBRL(r.querySelector('.etapa-valor').value);
      if (etapa) {
        itens.push({ etapa, valor });
      }
    });
    payload.itensEtapas = itens;
  } else if (modalidade === 'HORA') {
    payload.horasTrabalhadas = parseNumeroBRL(document.getElementById('hora-horas').value);
    payload.valorHora = parseNumeroBRL(document.getElementById('hora-valor').value);
  } else if (modalidade === 'PERCENTUAL_OBRA') {
    payload.custoTotalObra = parseNumeroBRL(document.getElementById('perc-custo').value);
    const taxaInput = parseNumeroBRL(document.getElementById('perc-taxa').value);
    payload.percentual = taxaInput / 100.0;
  }

  return payload;
}

async function simularCalculo() {
  const payload = coletarDadosModalidade();

  try {
    const resp = await authFetch('/api/servicos/calcular', {
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
  const elBase = document.getElementById('memoria-base');
  const elFaixa = document.getElementById('memoria-faixa');
  const elAliquota = document.getElementById('memoria-aliquota');
  const elImposto = document.getElementById('memoria-imposto');
  const elTotal = document.getElementById('memoria-total');

  if (elBase) elBase.textContent = formatarMoeda(m.valorBase || 0);
  if (elFaixa) elFaixa.textContent = m.descricaoFaixa || (m.faixaSimples ? `${m.faixaSimples}ª Faixa` : '1ª Faixa');
  if (elAliquota) {
    const aliqPct = ((m.aliquotaImposto || 0) * 100).toFixed(2).replace('.', ',');
    elAliquota.textContent = `${aliqPct}%`;
  }
  if (elImposto) elImposto.textContent = formatarMoeda(m.valorImposto || 0);
  if (elTotal) elTotal.textContent = formatarMoeda(m.valorTotalFinal || 0);
}

/* ==========================================================================
   7. SALVAR SERVIÇO PRECIFICADO
   ========================================================================== */
function initFormServico() {
  const form = document.getElementById('form-servico');
  if (!form) return;

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
      const resp = await authFetch('/api/servicos', {
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
      abrirModalOrcamentoComServico(resData.id, payload.clienteId);
    } catch (err) {
      showToast('Falha na comunicação com o servidor: ' + err.message, 'error');
    }
  });
}

/* ==========================================================================
   8. GESTÃO DE CLIENTES & MODAL DE CADASTRO
   ========================================================================== */
function initClientes() {
  const form = document.getElementById('form-cliente');
  const modalCliente = document.getElementById('modal-cliente');
  const btnAbrirModal = document.getElementById('btn-abrir-modal-cliente');
  const btnFecharModal = document.getElementById('btn-fechar-modal-cliente');
  const btnCancelarModal = document.getElementById('btn-cancelar-modal-cliente');

  const fecharModalCliente = () => {
    if (modalCliente) modalCliente.classList.add('hidden');
    if (form) form.reset();
  };

  if (btnAbrirModal && modalCliente) {
    btnAbrirModal.addEventListener('click', () => {
      if (form) form.reset();
      modalCliente.classList.remove('hidden');
      const inputNome = document.getElementById('cliente-nome');
      if (inputNome) inputNome.focus();
    });
  }

  if (btnFecharModal) {
    btnFecharModal.addEventListener('click', fecharModalCliente);
  }

  if (btnCancelarModal) {
    btnCancelarModal.addEventListener('click', fecharModalCliente);
  }

  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const payload = {
      nome: document.getElementById('cliente-nome').value,
      cpf: document.getElementById('cliente-cpf').value,
      whatsApp: document.getElementById('cliente-whatsapp').value
    };

    try {
      const resp = await authFetch('/api/clientes', {
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
      fecharModalCliente();
      carregarClientes();
    } catch (err) {
      showToast('Falha ao conectar ao servidor: ' + err.message, 'error');
    }
  });
}

let cacheClientes = [];

async function carregarClientes() {
  try {
    const resp = await authFetch('/api/clientes');
    if (!resp.ok) {
      cacheClientes = [];
      filtrarClientes();
      return;
    }

    const data = await resp.json();
    const clientes = Array.isArray(data) ? data : (data && Array.isArray(data.clientes) ? data.clientes : []);
    cacheClientes = clientes;
    filtrarClientes();
    atualizarDropdownsClientes(clientes);
  } catch (err) {
    console.error('Erro ao listar clientes:', err);
    cacheClientes = [];
    filtrarClientes();
  }
}

function filtrarClientes() {
  const inputBusca = document.getElementById('busca-clientes');
  const termo = normalizarTextoBusca(inputBusca ? inputBusca.value : '');
  if (!termo) {
    renderTabelaClientes(cacheClientes, false);
    return;
  }

  const filtrados = cacheClientes.filter(c => {
    const cpfDigitos = String(c.cpf || '').replace(/\D/g, '');
    const whatsDigitos = String(c.whatsApp || '').replace(/\D/g, '');
    const alvo = normalizarTextoBusca(`${c.nome || ''} ${c.cpf || ''} ${cpfDigitos} ${c.whatsApp || ''} ${whatsDigitos}`);
    return alvo.includes(termo);
  });

  renderTabelaClientes(filtrados, true);
}

function renderTabelaClientes(clientes, filtroAtivo = false) {
  const tbody = document.getElementById('tabela-clientes-corpo');
  if (!tbody) return;

  if (!clientes || clientes.length === 0) {
    const msg = filtroAtivo
      ? 'Nenhum cliente encontrado para a busca informada.'
      : 'Não há clientes cadastrados.';
    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">${msg}</td></tr>`;
    return;
  }

  tbody.innerHTML = clientes.map(c => `
    <tr>
      <td><strong>${escapeHtml(c.nome)}</strong></td>
      <td>${escapeHtml(c.cpf)}</td>
      <td>
        <a href="${gerarLinkWhatsAppContatoRapido(c.whatsApp, c.nome)}" target="_blank" class="btn btn-whatsapp btn-sm">
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
    const resp = await authFetch(`/api/clientes/${id}`, { method: 'DELETE' });
    if (resp.ok) {
      showToast('Cliente removido com sucesso.');
      await sincronizarDadosAposExclusaoCliente();
      return;
    }

    const errData = await resp.json().catch(() => ({}));
    if (resp.status === 409 || errData.possuiVinculos) {
      const msgAviso = errData.erro || errData.error ||
        'Atenção: Este cliente possui serviços, propostas comerciais ou cobranças vinculadas. Se você confirmar a exclusão, todos esses registros relacionados também serão permanentemente excluídos.';
      if (!confirm(`${msgAviso}\n\nDeseja confirmar a exclusão permanente deste cliente e de todo o seu histórico?`)) {
        return;
      }

      const respForcada = await authFetch(`/api/clientes/${id}?forcar=true`, { method: 'DELETE' });
      if (respForcada.ok) {
        showToast('Cliente e todo o seu histórico vinculado foram removidos com sucesso.');
        await sincronizarDadosAposExclusaoCliente();
      } else {
        const errForcado = await respForcada.json().catch(() => ({}));
        showToast(errForcado.error || errForcado.erro || 'Erro ao remover cliente e vínculos.', 'error');
      }
    } else {
      showToast(errData.error || errData.erro || 'Erro ao remover cliente.', 'error');
    }
  } catch (e) {
    showToast('Falha na operação: ' + e.message, 'error');
  }
}

async function sincronizarDadosAposExclusaoCliente() {
  await Promise.all([
    carregarClientes(),
    carregarOrcamentos(),
    carregarCobrancas(),
    carregarExtrato(),
    carregarServicosParaOrcamento()
  ]);
}

/* ==========================================================================
   9. EMISSÃO DE PROPOSTAS / ORÇAMENTOS & DISPARO WHATSAPP
   ========================================================================== */
function initOrcamentos() {
  const btnAbrir = document.getElementById('btn-abrir-modal-orcamento');
  const modal = document.getElementById('modal-orcamento');
  const btnFechar = document.getElementById('btn-fechar-modal-orcamento');
  const btnCancelar = document.getElementById('btn-cancelar-modal-orcamento');
  const form = document.getElementById('form-novo-orcamento');

  if (btnAbrir && modal) {
    btnAbrir.addEventListener('click', () => {
      carregarServicosParaOrcamento();
      modal.classList.remove('hidden');
    });

    const fechar = () => modal.classList.add('hidden');
    if (btnFechar) btnFechar.addEventListener('click', fechar);
    if (btnCancelar) btnCancelar.addEventListener('click', fechar);

    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
          clienteId: document.getElementById('orcamento-cliente-select').value,
          servicoId: document.getElementById('orcamento-servico-select').value,
          validade: document.getElementById('orcamento-validade').value,
          dataEmissao: document.getElementById('orcamento-data-emissao').value
        };

        try {
          const resp = await authFetch('/api/orcamentos', {
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

          if (resData.whatsappUrl && confirm('Proposta emitida com sucesso! Deseja abrir o WhatsApp agora para enviar a proposta ao cliente?')) {
            window.open(resData.whatsappUrl, '_blank');
          }
        } catch (err) {
          showToast('Erro ao emitir proposta: ' + err.message, 'error');
        }
      });
    }
  }
}

let cacheServicosOrcamento = [];

async function carregarServicosParaOrcamento() {
  try {
    const resp = await authFetch('/api/servicos');
    if (!resp.ok) return;
    const servicos = await resp.json();
    cacheServicosOrcamento = Array.isArray(servicos) ? servicos : [];
    const select = document.getElementById('orcamento-servico-select');
    if (select) {
      select.innerHTML = '<option value="">Selecione o serviço...</option>' +
        cacheServicosOrcamento.map(s => `<option value="${s.id}">#${s.id} - ${escapeHtml(s.descricao)} (${formatarMoeda(s.valorTotal)})</option>`).join('');
      
      select.onchange = () => atualizarPreviewTributarioOrcamento(select.value);
    }
  } catch (err) {
    console.error('Erro ao carregar serviços:', err);
  }
}

function atualizarPreviewTributarioOrcamento(servicoId) {
  const box = document.getElementById('orcamento-detalhes-tributarios');
  if (!box) return;

  const servico = cacheServicosOrcamento.find(s => String(s.id) === String(servicoId));
  if (!servico) {
    box.style.display = 'none';
    return;
  }

  const base = servico.valorBase || 0;
  const impostoAliq = servico.imposto || 0;
  const total = servico.valorTotal || (base * (1 + impostoAliq));
  const valorImposto = total - base;

  document.getElementById('orcamento-prev-base').textContent = formatarMoeda(base);
  document.getElementById('orcamento-prev-aliq').textContent = ((impostoAliq * 100).toFixed(2).replace('.', ',')) + '%';
  document.getElementById('orcamento-prev-imposto').textContent = formatarMoeda(valorImposto);
  document.getElementById('orcamento-prev-total').textContent = formatarMoeda(total);
  box.style.display = 'block';
}

async function abrirModalOrcamentoComServico(servicoId, clienteId) {
  await carregarServicosParaOrcamento();
  const modal = document.getElementById('modal-orcamento');
  if (modal) {
    document.getElementById('orcamento-cliente-select').value = clienteId;
    document.getElementById('orcamento-servico-select').value = servicoId;
    atualizarPreviewTributarioOrcamento(servicoId);
    modal.classList.remove('hidden');
  }
}

let cacheOrcamentos = [];

function isPropostaRecusada(status) {
  if (!status) return false;
  const norm = String(status).trim().toUpperCase();
  return norm === 'RECUSADO' || norm === 'RECUSADA';
}

async function carregarOrcamentos() {
  try {
    const resp = await authFetch('/api/orcamentos');
    if (!resp.ok) return;
    const orcamentos = await resp.json();
    cacheOrcamentos = Array.isArray(orcamentos) ? orcamentos : [];
    filtrarOrcamentos();
    atualizarDropdownOrcamentos(cacheOrcamentos);
  } catch (err) {
    console.error('Erro ao carregar orçamentos:', err);
  }
}

function filtrarOrcamentos() {
  const inputBusca = document.getElementById('busca-orcamentos');
  const termo = normalizarTextoBusca(inputBusca ? inputBusca.value : '');
  if (!termo) {
    renderTabelaOrcamentos(cacheOrcamentos, false);
    return;
  }

  const filtrados = cacheOrcamentos.filter(o => {
    const clienteNome = o.cliente ? o.cliente.nome : '';
    const servicoDesc = o.servico ? o.servico.descricao : '';
    const servicoTipo = o.servico ? o.servico.tipo : '';
    const dataEmissaoBR = formatarDataBR(o.dataEmissao);
    const dataValidadeBR = calcularDataValidadeBR(o.dataEmissao, o.validade);
    const valorFmt = formatarMoeda(o.valorTotal);
    const statusRaw = String(o.statusOrcamento || o.status || 'PENDENTE');
    const statusLabel = statusRaw.toUpperCase().startsWith('APROVAD')
      ? 'Aprovado Aprovada'
      : (statusRaw.toUpperCase().startsWith('RECUSAD') ? 'Recusado Recusada' : 'Pendente');

    const alvo = normalizarTextoBusca(
      `#${o.id} ${clienteNome} ${servicoDesc} ${servicoTipo} ${dataEmissaoBR} ${o.dataEmissao || ''} ${dataValidadeBR} ${valorFmt} ${o.valorTotal || ''} ${statusRaw} ${statusLabel}`
    );
    return alvo.includes(termo);
  });

  renderTabelaOrcamentos(filtrados, true);
}

function renderTabelaOrcamentos(orcamentos, filtroAtivo = false) {
  const tbody = document.getElementById('tabela-orcamentos-corpo');
  if (!tbody) return;

  if (!orcamentos || orcamentos.length === 0) {
    const msg = filtroAtivo
      ? 'Nenhuma proposta comercial encontrada para a busca informada.'
      : 'Nenhuma proposta emitida até o momento.';
    tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted">${msg}</td></tr>`;
    return;
  }

  tbody.innerHTML = orcamentos.map(o => {
    const clienteNome = o.cliente ? o.cliente.nome : 'N/A';
    const servicoDesc = o.servico ? o.servico.descricao : 'N/A';
    const statusRaw = String(o.statusOrcamento || o.status || 'PENDENTE').trim().toUpperCase();
    const isPendente = statusRaw === 'PENDENTE';
    const isRecusada = isPropostaRecusada(statusRaw);
    const statusBadge = getStatusBadge(statusRaw);
    const servico = o.servico;
    const temImposto = servico && (servico.imposto !== undefined || servico.valorBase !== undefined);

    return `
      <tr>
        <td>#${o.id}</td>
        <td><strong>${escapeHtml(clienteNome)}</strong></td>
        <td>${escapeHtml(servicoDesc)}</td>
        <td>${escapeHtml(formatarDataBR(o.dataEmissao))}</td>
        <td>Válida até ${escapeHtml(calcularDataValidadeBR(o.dataEmissao, o.validade))}</td>
        <td>
          <strong>${formatarMoeda(o.valorTotal)}</strong>
          ${temImposto ? `
            <div class="text-muted" style="font-size: 0.75rem; margin-top: 3px; line-height: 1.2;">
              <span>Líq: ${formatarMoeda(servico.valorBase || 0)}</span><br>
              <span>Simples (${((servico.imposto || 0) * 100).toFixed(2).replace('.', ',')}%) : ${formatarMoeda((servico.valorTotal - (servico.valorBase || 0)))}</span>
            </div>
          ` : ''}
        </td>
        <td>${statusBadge}</td>
        <td>
          <button class="btn btn-whatsapp btn-sm" onclick="abrirWhatsAppOrcamento(${o.id})">
            WhatsApp
          </button>
        </td>
        <td>
          ${isPendente ? `
            <button class="btn btn-secondary btn-sm" onclick="mudarStatusOrcamento(${o.id}, 'APROVADO')">Aprovar</button>
            <button class="btn btn-secondary btn-sm" onclick="mudarStatusOrcamento(${o.id}, 'RECUSADO')">Recusar</button>
          ` : (!isRecusada ? `
            <button class="btn btn-secondary btn-sm" onclick="abrirModalCobrancaParaOrcamento(${o.id})">Gerar Cobrança</button>
          ` : `
            <span class="text-muted" style="font-size: 0.85rem;">—</span>
          `)}
        </td>
      </tr>
    `;
  }).join('');
}

async function abrirWhatsAppOrcamento(id) {
  try {
    const resp = await authFetch(`/api/orcamentos/${id}/whatsapp`);
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
    const resp = await authFetch(`/api/orcamentos/${id}/status`, {
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
   10. FLUXO FINANCEIRO & COBRANÇAS
   ========================================================================== */
function initFinanceiro() {
  const btnAbrir = document.getElementById('btn-abrir-modal-cobranca');
  const modal = document.getElementById('modal-cobranca');
  const btnFechar = document.getElementById('btn-fechar-modal-cobranca');
  const btnCancelar = document.getElementById('btn-cancelar-modal-cobranca');
  const form = document.getElementById('form-nova-cobranca');

  if (btnAbrir && modal) {
    btnAbrir.addEventListener('click', () => modal.classList.remove('hidden'));
    const fechar = () => modal.classList.add('hidden');
    if (btnFechar) btnFechar.addEventListener('click', fechar);
    if (btnCancelar) btnCancelar.addEventListener('click', fechar);

    if (form) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
          orcamentoId: document.getElementById('cobranca-orcamento-select').value,
          dataVencimento: document.getElementById('cobranca-vencimento').value
        };

        try {
          const resp = await authFetch('/api/cobrancas', {
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
  }
}

function abrirModalCobrancaParaOrcamento(orcamentoId) {
  const orc = cacheOrcamentos.find(o => String(o.id) === String(orcamentoId));
  if (orc && isPropostaRecusada(orc.statusOrcamento || orc.status)) {
    showToast('Não é permitido gerar cobrança para uma proposta recusada.', 'error');
    return;
  }
  const modal = document.getElementById('modal-cobranca');
  if (modal) {
    document.getElementById('cobranca-orcamento-select').value = orcamentoId;
    modal.classList.remove('hidden');
  }
}

function atualizarDropdownOrcamentos(orcamentos) {
  const sel = document.getElementById('cobranca-orcamento-select');
  if (!sel) return;
  const elegiveis = (orcamentos || []).filter(o => !isPropostaRecusada(o.statusOrcamento || o.status));
  sel.innerHTML = '<option value="">Selecione um orçamento...</option>' +
    elegiveis.map(o => {
      const clienteNome = o.cliente ? o.cliente.nome : '';
      return `<option value="${o.id}">#${o.id} - ${escapeHtml(clienteNome)} (${formatarMoeda(o.valorTotal)})</option>`;
    }).join('');
}

async function carregarExtrato() {
  try {
    const resp = await authFetch('/api/cobrancas/extrato');
    if (!resp.ok) return;
    const extrato = await resp.json();

    const elFat = document.getElementById('kpi-faturado');
    if (elFat) elFat.textContent = formatarMoeda(extrato.totalFaturado || 0);

    const elPagas = document.getElementById('kpi-pagas-count');
    if (elPagas) elPagas.textContent = `${extrato.cobrancasPagas || 0} recebimentos`;

    const elAberto = document.getElementById('kpi-em-aberto');
    if (elAberto) elAberto.textContent = formatarMoeda(extrato.totalEmAberto || 0);

    const elAbertoCount = document.getElementById('kpi-aberto-count');
    if (elAbertoCount) elAbertoCount.textContent = `${extrato.cobrancasEmAberto || 0} pendentes`;

    const elTotal = document.getElementById('kpi-total-cobrancas');
    if (elTotal) elTotal.textContent = extrato.totalCobrancas || 0;
  } catch (err) {
    console.error('Erro ao carregar extrato:', err);
  }
}

let cacheCobrancas = [];

async function carregarCobrancas() {
  try {
    const resp = await authFetch('/api/cobrancas');
    if (!resp.ok) return;
    const cobrancas = await resp.json();
    cacheCobrancas = Array.isArray(cobrancas) ? cobrancas : [];
    filtrarCobrancas();
  } catch (err) {
    console.error('Erro ao carregar cobranças:', err);
  }
}

function filtrarCobrancas() {
  const inputBusca = document.getElementById('busca-cobrancas');
  const termo = normalizarTextoBusca(inputBusca ? inputBusca.value : '');
  if (!termo) {
    renderTabelaCobrancas(cacheCobrancas, false);
    return;
  }

  const filtrados = cacheCobrancas.filter(c => {
    const orc = c.orcamento;
    const clienteNome = (orc && orc.cliente) ? orc.cliente.nome : '';
    const servicoDesc = (orc && orc.servico) ? orc.servico.descricao : '';
    const orcIdStr = orc ? `#${orc.id}` : '';
    const vencBR = formatarDataBR(c.dataVencimento);
    const pagBR = c.dataPagamento ? formatarDataBR(c.dataPagamento) : '';
    const valorFmt = formatarMoeda(c.valor);
    const statusRaw = String(c.statusCobranca || 'EM_ABERTO');
    const statusLabel = statusRaw === 'PAGO'
      ? 'Pago Liquidado'
      : (statusRaw === 'ATRASADO' ? 'Atrasado Vencido' : 'Em Aberto Pendente');

    const alvo = normalizarTextoBusca(
      `#${c.id} ${orcIdStr} ${clienteNome} ${servicoDesc} ${vencBR} ${c.dataVencimento || ''} ${pagBR} ${c.dataPagamento || ''} ${valorFmt} ${c.valor || ''} ${statusRaw} ${statusLabel}`
    );
    return alvo.includes(termo);
  });

  renderTabelaCobrancas(filtrados, true);
}

function renderTabelaCobrancas(cobrancas, filtroAtivo = false) {
  const tbody = document.getElementById('tabela-cobrancas-corpo');
  if (!tbody) return;

  if (!cobrancas || cobrancas.length === 0) {
    const msg = filtroAtivo
      ? 'Nenhuma cobrança encontrada para a busca informada.'
      : 'Nenhum registro financeiro encontrado.';
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">${msg}</td></tr>`;
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
        <td>${escapeHtml(formatarDataBR(c.dataVencimento))}</td>
        <td>${c.dataPagamento ? escapeHtml(formatarDataBR(c.dataPagamento)) : '<span class="text-muted">—</span>'}</td>
        <td><strong>${formatarMoeda(c.valor)}</strong></td>
        <td>${statusBadge}</td>
        <td>
          <div style="display:flex; gap:0.4rem; flex-wrap:wrap;">
            ${c.statusCobranca !== 'PAGO' ? `
              <button class="btn btn-primary btn-sm" onclick="registrarPagamentoCobranca(${c.id})">Liquidar</button>
              <button class="btn btn-whatsapp btn-sm" onclick="abrirWhatsAppCobranca(${c.id})">Lembrete</button>
            ` : `
              <span style="color:var(--success); font-weight:600; font-size:0.85rem; align-self:center;">Liquidado</span>
            `}
            <button class="btn btn-danger btn-sm" onclick="deletarCobranca(${c.id})">Excluir</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

async function registrarPagamentoCobranca(id) {
  const dataHoje = obterDataLocalISO();
  try {
    const resp = await authFetch(`/api/cobrancas/${id}/pagar`, {
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
    const resp = await authFetch(`/api/cobrancas/${id}/whatsapp`);
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
    const resp = await authFetch(`/api/cobrancas/${id}`, { method: 'DELETE' });
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
   11. UTILITÁRIOS GERAIS & MÁSCARAS BRL
   ========================================================================== */
function parseNumeroBRL(val) {
  if (val === null || val === undefined) return 0;
  if (typeof val === 'number') return isNaN(val) ? 0 : val;
  let str = String(val).trim().replace(/^R\$\s?/, '').trim();
  if (!str) return 0;

  // Trata formato brasileiro com pontos de milhar e vírgula decimal (ex: 2.500.000,00)
  if (str.includes('.') && str.includes(',')) {
    str = str.replace(/\./g, '').replace(',', '.');
  } else if (str.includes(',')) {
    // Apenas vírgula decimal (ex: 150,50 ou 12,5)
    str = str.replace(',', '.');
  } else if (str.includes('.')) {
    // Pode ser decimal (150.50) ou padrão de milhar (2.500.000)
    const partes = str.split('.');
    if (partes.length > 2) {
      str = str.replace(/\./g, '');
    }
  }

  const num = parseFloat(str);
  return isNaN(num) ? 0 : num;
}

const formatadorMoedaBRL = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
});

function formatarMoeda(val) {
  const num = parseNumeroBRL(val);
  return formatadorMoedaBRL.format(num).replace(/\u00a0/g, ' ');
}

/**
 * Converte uma data ISO (yyyy-MM-dd) para o padrão brasileiro (dd/MM/yyyy).
 * Realiza o split manual das partes da string para evitar retrocesso de 1 dia no fuso horário (-03:00).
 */
function formatarDataBR(dataIso) {
  if (!dataIso) return '';
  const limpa = String(dataIso).trim();
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(limpa)) {
    return limpa;
  }
  const parteData = limpa.split('T')[0];
  const partes = parteData.split('-');
  if (partes.length === 3 && partes[0].length === 4) {
    const [ano, mes, dia] = partes;
    return `${dia.padStart(2, '0')}/${mes.padStart(2, '0')}/${ano}`;
  }
  return limpa;
}

/**
 * Calcula a data final exata de vencimento da proposta somando os dias de validade à data de emissão,
 * utilizando data local new Date(ano, mes - 1, dia) para evitar desvios de fuso horário (-03:00).
 */
function calcularDataValidadeBR(dataEmissao, diasValidade) {
  if (!dataEmissao) return '';
  const limpa = String(dataEmissao).trim().split('T')[0];
  let ano, mes, dia;
  if (/^\d{2}\/\d{2}\/\d{4}$/.test(limpa)) {
    const partes = limpa.split('/');
    dia = parseInt(partes[0], 10);
    mes = parseInt(partes[1], 10);
    ano = parseInt(partes[2], 10);
  } else {
    const partes = limpa.split('-');
    if (partes.length !== 3) return formatarDataBR(limpa);
    ano = parseInt(partes[0], 10);
    mes = parseInt(partes[1], 10);
    dia = parseInt(partes[2], 10);
  }
  const dias = parseInt(diasValidade, 10) || 0;
  const dataLimite = new Date(ano, mes - 1, dia);
  dataLimite.setDate(dataLimite.getDate() + dias);
  const d = String(dataLimite.getDate()).padStart(2, '0');
  const m = String(dataLimite.getMonth() + 1).padStart(2, '0');
  const y = dataLimite.getFullYear();
  return `${d}/${m}/${y}`;
}

/**
 * Retorna a data local no formato yyyy-MM-dd compatível com o atributo value de <input type="date">.
 */
function obterDataLocalISO(data = new Date()) {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

/**
 * Extrai apenas o primeiro nome do cliente com fallback defensivo para "Cliente".
 */
function extrairPrimeiroNome(nomeCompleto) {
  if (!nomeCompleto || typeof nomeCompleto !== 'string') return 'Cliente';
  const limpo = nomeCompleto.trim();
  if (!limpo) return 'Cliente';
  return limpo.split(/\s+/)[0] || 'Cliente';
}

/**
 * Gera link de contato rápido para o WhatsApp utilizando apenas o primeiro nome na saudação.
 */
function gerarLinkWhatsAppContatoRapido(whatsApp, nomeCompleto) {
  const primeiroNome = extrairPrimeiroNome(nomeCompleto);
  const saudacao = encodeURIComponent(`Olá, ${primeiroNome}! Tudo bem?`);
  const numero = String(whatsApp || '').replace(/\D/g, '');
  return `https://wa.me/${numero}?text=${saudacao}`;
}

function configurarCamposMoeda() {
  document.querySelectorAll('.input-moeda').forEach(aplicarMascaraMoeda);
}

function aplicarMascaraMoeda(input) {
  if (!input || input.dataset.mascaraAplicada) return;
  input.dataset.mascaraAplicada = 'true';

  // Se já possuir valor numérico inicial, formata para BRL
  if (input.value && !input.value.startsWith('R$')) {
    const n = parseNumeroBRL(input.value);
    if (n > 0) input.value = formatarMoeda(n);
  }

  input.addEventListener('focus', () => {
    const raw = input.value.trim();
    if (!raw) return;
    const num = parseNumeroBRL(raw);
    if (num >= 0) {
      input.value = num.toFixed(2).replace('.', ',');
      input.select();
    } else {
      input.value = '';
    }
  });

  input.addEventListener('blur', () => {
    const raw = input.value.trim();
    if (!raw) {
      input.value = '';
    } else {
      const num = parseNumeroBRL(raw);
      if (num >= 0) {
        input.value = formatarMoeda(num);
      } else {
        input.value = '';
      }
    }
    simularCalculo();
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}

function getStatusBadge(status) {
  const s = String(status || '').trim().toUpperCase();
  if (s === 'APROVADO' || s === 'APROVADA') return '<span class="badge badge-success">Aprovado</span>';
  if (s === 'RECUSADO' || s === 'RECUSADA') return '<span class="badge badge-danger">Recusado</span>';
  return '<span class="badge badge-warning">Pendente</span>';
}

function getStatusCobrancaBadge(status) {
  if (status === 'PAGO') return '<span class="badge badge-success">Pago</span>';
  if (status === 'ATRASADO') return '<span class="badge badge-danger">Atrasado</span>';
  return '<span class="badge badge-warning">Em Aberto</span>';
}

/* ==========================================================================
   12. BUSCA E FILTRO DINÂMICO EM TEMPO REAL (CLIENT-SIDE)
   ========================================================================== */
function normalizarTextoBusca(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

function initBuscaDinamica() {
  const inputClientes = document.getElementById('busca-clientes');
  if (inputClientes) {
    inputClientes.addEventListener('input', filtrarClientes);
  }

  const inputOrcamentos = document.getElementById('busca-orcamentos');
  if (inputOrcamentos) {
    inputOrcamentos.addEventListener('input', filtrarOrcamentos);
  }

  const inputCobrancas = document.getElementById('busca-cobrancas');
  if (inputCobrancas) {
    inputCobrancas.addEventListener('input', filtrarCobrancas);
  }
}
