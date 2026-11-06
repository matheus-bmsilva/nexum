# 📋 Documento de Requisitos — Nexum

Este documento especifica os Requisitos Funcionais (RF) e Não Funcionais (RNF) do **Nexum**, sistema de gestão e orçamentação voltado para arquitetos autônomos e profissionais liberais.

---

## 1. Requisitos Funcionais (RF)

* **RF01 - Autenticação e Controle de Acesso:** O Sistema deve conter uma área de login com controle de acesso por credenciais para os profissionais entrarem na plataforma web.
* **RF02 - Gerenciamento de Clientes:** O Sistema deve permitir cadastrar, alterar, listar e excluir os dados dos clientes (nome, CPF e WhatsApp), substituindo o uso de planilhas manuais.
* **RF03 - Cadastro de Serviços Polimórficos:** O Sistema deve permitir o cadastro de serviços associados a diferentes modalidades de cálculo de preço (por metro quadrado, por etapas customizadas, por horas trabalhadas ou por percentual de obra), aplicando de forma automatizada os impostos parametrizados.
* **RF04 - Emissão de Orçamentos:** O Sistema deve permitir calcular o valor total com base nos parâmetros do serviço e gerar orçamentos estruturados contendo data de emissão, validade, status e valores.
* **RF05 - Envio de Mensagens via WhatsApp:** O Sistema deve gerar e formatar textos amigáveis de orçamentos e cobranças, permitindo o direcionamento rápido para envio via link do WhatsApp.
* **RF06 - Controle de Cobranças e Inadimplência:** O Sistema deve registrar e controlar cobranças atreladas aos orçamentos, permitindo atualizar o status de pagamento (*Em Aberto*, *Pago*, *Atrasado*) e a data de liquidação.
* **RF07 - Consulta de Extrato Financeiro:** O Sistema deve permitir a consulta consolidada de extratos e recebimentos, facilitando a gestão do fluxo de caixa pelo profissional.

---

## 2. Requisitos Não Funcionais (RNF)

* **RNF01 - Adequação (Responsividade):** O Sistema web deve conseguir se adequar a diferentes tipos de telas e navegadores de dispositivos utilizados pelos usuários finais.
* **RNF02 - Acurácia:** O Sistema deve garantir alta precisão nos cálculos monetários e matemáticos gerados pelas classes polimórficas de serviços.
* **RNF03 - Segurança de Acesso:** O Sistema deve limitar o acesso exclusivo somente a usuários autenticados, realizando o controle rigoroso via login e criptografia de credenciais.
* **RNF04 - Maturidade:** O Sistema deve ter um nível mínimo de maturidade em seu desenvolvimento para ser utilizado no ambiente de validação com clientes reais (MVP) sem falhas críticas.
* **RNF05 - Tolerância a Falhas:** O Sistema deve conseguir evitar erros que o deixem inoperável, garantindo estabilidade durante o lançamento de serviços e emissão de orçamentos.
* **RNF06 - Recuperabilidade:** O Sistema deve prever uma estrutura organizada para salvamento e integridade dos dados cadastrados no backend.
* **RNF07 - Inteligibilidade:** O Sistema deve conter uma interface web simples, intuitiva e acessível, adequada à rotina de arquitetos e profissionais independentes.
* **RNF08 - Apreensibilidade:** O Sistema deve garantir o rápido entendimento dos recursos de criação de orçamentos e controle de cobranças pelo usuário.
* **RNF09 - Operacionalibilidade:** O Sistema deve garantir que a execução de tarefas complexas de cálculo e orçamentação seja feita de forma automatizada e sem dificuldades, eliminando o uso de planilhas engessadas.
* **RNF10 - Atratividade:** O Sistema deve apresentar uma interface web agradável e limpa (*clean*), estilizada utilizando frameworks CSS modernos.
* **RNF11 - Comportamento em Relação ao Tempo:** O Sistema deve apresentar um tempo de carregamento ágil e respostas imediatas em cada ação de cálculo ou consulta de extrato (inferior a 2 segundos).
* **RNF12 - Utilização de Recursos:** O Sistema deve consumir uma quantidade otimizada e razoável de recursos do servidor e do navegador do usuário.
* **RNF13 - Analisabilidade:** O Sistema deve conter uma boa documentação baseada nos diagramas UML criados (Casos de Uso e Classes), facilitando manutenções e auditorias da banca.
* **RNF14 - Modificabilidade:** O Sistema deve manter uma base de código Java estruturada sob princípios sólidos de Orientação a Objetos (como polimorfismo e encapsulamento), tornando-a altamente passível de novas implementações futuras.
* **RNF15 - Estabilidade:** O Sistema deve manter uma boa estabilidade de conexão para consultas e persistência de dados em tempo real.
* **RNF16 - Testabilidade:** O Sistema deverá apresentar uma arquitetura modular que permita testes unitários nos módulos de cálculo de serviços e orçamentos.
* **RNF17 - Adaptabilidade:** O Sistema deve ser compatível com diferentes sistemas operacionais (Windows, Linux, macOS) e navegadores web modernos.
* **RNF18 - Capacidade para ser Instalado:** O Sistema deve conseguir rodar de forma fluida em servidores web compatíveis com os requisitos mínimos do backend em Java.
* **RNF19 - Coexistência:** O Sistema deve permitir o uso simultâneo entre diversos usuários acessando e gerenciando seus respectivos clientes e finanças.
* **RNF20 - Capacidade de Substituição:** O Sistema deve manter o desacoplamento de camadas, permitindo a fácil substituição ou migração de repositórios e banco de dados se necessário.
* **RNF21 - Conformidade (LGPD):** O Sistema deve proporcionar conformidade com a Lei Geral de Proteção de Dados (Lei 13.709/2018), garantindo segurança e privacidade às informações de profissionais e clientes finais coletadas na plataforma.
