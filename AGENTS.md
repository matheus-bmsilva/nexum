# Diretrizes Operacionais para o Agente - Sistema MEI Arquitetura (Java)

## 1. Stack Tecnológica & Arquitetura
* **Linguagem:** Java (versão 21 ou compatível).
* **Arquitetura Base:** Padrão em camadas (Controller, Service, Repository, Model/Entity).
* **Precificação Polimórfica:** O cálculo de preços utiliza uma classe de serviço base e subclasses especializadas. Respeite estritamente a hierarquia polimórfica existente. Caso o fluxo de execução ou a instanciação concreta ainda não estejam totalmente mapeados, crie pontos de extensão limpos (como Factory Pattern ou injeção via interface) sem inventar regras de negócio arbitrárias.

## 2. Princípios SOLID Obrigatórios
* **S (Single Responsibility Principle):** Cada classe e método deve ter uma única responsabilidade clara (ex: a lógica de cálculo de RRT/RT não deve misturar-se com persistência de dados).
* **O (Open/Closed Principle):** As entidades e serviços de cálculo devem estar abertos para extensão (novos tipos de projetos de arquitetura ou métricas) mas fechados para modificação direta do núcleo.
* **L (Liskov Substitution Principle):** As subclasses de precificação devem poder substituir a classe base de serviço sem quebrar o comportamento esperado pelo sistema.
* **I (Interface Segregation Principle):** Prefira interfaces coesas e específicas em vez de contratos genéricos gigantescos.
* **D (Dependency Inversion Principle):** Dependa de abstrações (interfaces/classes abstratas) e não de implementações concretas. Use injeção de dependência via construtor.

## 3. Padrões de Clean Code & Segurança
* **Legibilidade:** Nomes descritivos em inglês ou português consistente (`PascalCase` para classes, `camelCase` para métodos/variáveis). Métodos curtos e focados.
* **Tratamento de Erros:** Lance exceções de negócio customizadas e descritivas; nunca deixe blocos `catch` vazios.
* **Segurança de Dados:** Dados sensíveis de faturamento MEI e de clientes jamais devem ser expostos em logs (`System.out.println` ou loggers públicos). Valide rigorosamente todas as entradas (valores monetários, metragens, etc.).

## 4. Regras de Modus Operandi para o Agente
1. **Execução Incremental:** Implemente uma funcionalidade ou camada por vez. Aguarde a minha revisão antes de avançar para o próximo bloco lógico.
2. **Preservação de Código:** Não reescreva arquivos ou o esqueleto existente a menos que seja estritamente necessário para integração ou correção de bugs.
3. **Comunicação Técnica:** Justifique sempre as decisões de código adotadas, apontando como elas respeitam o polimorfismo de cálculo ou os princípios SOLID definidos neste arquivo.