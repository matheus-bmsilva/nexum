# 🚀 Nexum — Gestão e Orçamentação para Arquitetos e Profissionais Liberais

> Projeto desenvolvido para o **Startup Challenge UMC 2026** (Universidade de Mogi das Cruzes em parceria com a Mestres da Web).

---

## 👥 Equipe / Autores
* **Matheus Barros Moreira da Silva** — Desenvolvedor e Idealizador
* **Pablo Estevam Godoy Miranda** — Desenvolvedor e Idealizador

---

## 📌 Sobre o Projeto
O **Nexum** é uma solução web projetada para simplificar a rotina de arquitetos autônomos e pequenos escritórios de arquitetura e engenharia. 

O sistema resolve os principais gargalos operacionais mapeados em pesquisas de mercado com profissionais da área: **a complexidade na precificação flexível de projetos personalizados, a lentidão na emissão de orçamentos detalhados e o controle ineficiente de recebimentos e inadimplência**.

---

## 🛠️ Tecnologias e Conceitos Aplicados
O backend da aplicação foi estruturado em **Java** aplicando rigorosos conceitos de **Orientação a Objetos (POO)**:
* **Polimorfismo:** Implementado na regra de cálculo de serviços para abranger diferentes modelos de cobrança utilizados na arquitetura.
* **Encapsulamento e Tipagem Segura:** Uso extensivo de `Enums` para padronização de status, padrões de acabamento e etapas de projeto.
* **Arquitetura Limpa:** Separação clara entre Usuários/Profissionais, Clientes, Orçamentos, Serviços e Cobranças.

---

## 📐 Modelagem do Sistema (UML)
O projeto conta com modelagem orientada a objetos estruturada em diagramas de **Casos de Uso** e **Diagrama de Classes**:
* **Polimorfismo de Serviços:** A classe abstrata `Servico` possui subclasses especializadas para suportar 4 modalidades distintas de precificação na arquitetura:
  1. `ServicoPorMetroQuadrado` (Área projetada $\times$ Valor do $m^2$ ajustado por padrão)
  2. `ServicoPorEtapa` (Soma de itens customizados como levantamento, estudo preliminar, anteprojeto, projeto executivo, etc.)
  3. `ServicoPorHora` (Horas técnicas gastas $\times$ Valor da hora)
  4. `ServicoPorPercentualObra` (Percentual aplicado sobre o custo global estimado da obra)

---

## ⚙️ Principais Funcionalidades (Requisitos Funcionais)
* **Autenticação de Profissionais:** Área de login com controle seguro de credenciais para acesso à plataforma web pelo arquiteto.
* **Gestão de Clientes:** Cadastro e controle ágil de clientes (Nome, CPF e WhatsApp).
* **Emissão de Orçamentos:** Emissão de propostas comerciais vinculadas a serviços parametrizados, calculando impostos e valores totais de forma automatizada.
* **Integração com WhatsApp:** Geração de mensagens amigáveis para envio direto de propostas e avisos de cobrança via links universais (`wa.me`).
* **Controle Financeiro e Extratos:** Acompanhamento de status de cobranças (*Em Aberto*, *Pago*, *Atrasado*) e geração de extratos simplificados para apoio ao fluxo de caixa.
