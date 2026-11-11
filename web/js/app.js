/**
 * NEXUM — Aplicação Frontend Reativa e Assíncrona (Vanilla JS / Fetch API)
 */

const TOKEN_KEY = 'nexum_auth_token';

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

  // Preenche datas de hoje por padrão
  const hoje = new Date().toISOString().split('T')[0];
  const elDataServico = document.getElementById('servico-data');
  if (elDataServico) elDataServico.value = hoje;
  const elDataEmissao = document.getElementById('orcamento-data-emissao');
  if (elDataEmissao) elDataEmissao.value = hoje;

  // Vencimento padrão: +15 dias
  const dataVenc = new Date();
  dataVenc.setDate(dataVenc.getDate() + 15);
  const elDataVenc = document.getElementById('cobranca-vencimento');
  if (elDataVenc) elDataVenc.value = dataVenc.toISOString().split('T')[0];

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
    'servico-rbt12', 'm2-area', 'm2-valor', 'hora-horas', 'hora-valor', 'perc-custo', 'perc-taxa'
  ];
  inputsParaMonitorar.forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('input', simularCalculo);
  });
}

function coletarDadosModalidade() {
  const radioChecked = document.querySelector('input[name="modalidade"]:checked');
  const modalidade = radioChecked ? radioChecked.value : 'METRO_QUADRADO';
  const rbt12Input = document.getElementById('servico-rbt12');
  const rbt12 = rbt12Input ? parseNumeroBRL(rbt12Input.value) : 0;
  const payload = { tipo: modalidade, rbt12: rbt12, imposto: 0.06 };

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

async function carregarClientes() {
  try {
    const resp = await authFetch('/api/clientes');
    if (!resp.ok) {
      renderTabelaClientes([]);
      return;
    }

    const data = await resp.json();
    const clientes = Array.isArray(data) ? data : (data && Array.isArray(data.clientes) ? data.clientes : []);
    renderTabelaClientes(clientes);
    atualizarDropdownsClientes(clientes);
  } catch (err) {
    console.error('Erro ao listar clientes:', err);
    renderTabelaClientes([]);
  }
}

function renderTabelaClientes(clientes) {
  const tbody = document.getElementById('tabela-clientes-corpo');
  if (!tbody) return;

  if (!clientes || clientes.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">Não há clientes cadastrados.</td></tr>';
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
    const resp = await authFetch(`/api/clientes/${id}`, { method: 'DELETE' });
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

async function carregarOrcamentos() {
  try {
    const resp = await authFetch('/api/orcamentos');
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
  if (!tbody) return;

  if (!orcamentos || orcamentos.length === 0) {
    tbody.innerHTML = '<tr><td colspan="9" class="text-center">Nenhuma proposta emitida até o momento.</td></tr>';
    return;
  }

  tbody.innerHTML = orcamentos.map(o => {
    const clienteNome = o.cliente ? o.cliente.nome : 'N/A';
    const servicoDesc = o.servico ? o.servico.descricao : 'N/A';
    const statusBadge = getStatusBadge(o.statusOrcamento);
    const servico = o.servico;
    const temImposto = servico && (servico.imposto !== undefined || servico.valorBase !== undefined);

    return `
      <tr>
        <td>#${o.id}</td>
        <td><strong>${escapeHtml(clienteNome)}</strong></td>
        <td>${escapeHtml(servicoDesc)}</td>
        <td>${escapeHtml(o.dataEmissao)}</td>
        <td>${o.validade} dias</td>
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
  const modal = document.getElementById('modal-cobranca');
  if (modal) {
    document.getElementById('cobranca-orcamento-select').value = orcamentoId;
    modal.classList.remove('hidden');
  }
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

async function carregarCobrancas() {
  try {
    const resp = await authFetch('/api/cobrancas');
    if (!resp.ok) return;
    const cobrancas = await resp.json();
    renderTabelaCobrancas(cobrancas);
  } catch (err) {
    console.error('Erro ao carregar cobranças:', err);
  }
}

function renderTabelaCobrancas(cobrancas) {
  const tbody = document.getElementById('tabela-cobrancas-corpo');
  if (!tbody) return;

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
  const dataHoje = new Date().toISOString().split('T')[0];
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

function formatarMoeda(val) {
  const num = parseNumeroBRL(val);
  const partes = Math.abs(num).toFixed(2).split('.');
  const inteiraComPontos = partes[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  const prefixo = num < 0 ? '-R$ ' : 'R$ ';
  return `${prefixo}${inteiraComPontos},${partes[1]}`;
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
    const num = parseNumeroBRL(input.value);
    if (num > 0) {
      input.value = num.toFixed(2).replace('.', ',');
      input.select();
    } else {
      input.value = '';
    }
  });

  input.addEventListener('blur', () => {
    const num = parseNumeroBRL(input.value);
    if (num > 0) {
      input.value = formatarMoeda(num);
    } else {
      input.value = '';
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
  if (status === 'APROVADO') return '<span class="badge badge-success">Aprovado</span>';
  if (status === 'RECUSADO') return '<span class="badge badge-danger">Recusado</span>';
  return '<span class="badge badge-warning">Pendente</span>';
}

function getStatusCobrancaBadge(status) {
  if (status === 'PAGO') return '<span class="badge badge-success">Pago</span>';
  if (status === 'ATRASADO') return '<span class="badge badge-danger">Atrasado</span>';
  return '<span class="badge badge-warning">Em Aberto</span>';
}
