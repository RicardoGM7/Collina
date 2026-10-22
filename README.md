# ⚽ Collina — Sistema de Gestão de Clube Poliesportivo

Aplicação web integrada para automação e gestão dos fluxos operacionais, financeiros, administrativos, de associados, reservas de espaços e escalas de um clube poliesportivo.

> **Projeto Acadêmico**  
> Universidade Federal de Sergipe (UFS) — Departamento de Computação (DCOMP)  
> **Arquitetura:** Monólito Modular em Camadas (Spring Boot + React + PostgreSQL)

---

## 🛠️ Tecnologias Utilizadas

* **Back-end:** Java 21, Spring Boot, Spring Data JPA, Spring Security, Flyway Migration, Lombok.
* **Front-end:** React 18, TypeScript, Vite, React Router DOM, Axios, Lucide React.
* **Banco de Dados:** PostgreSQL 16+.
* **Autenticação:** Firebase Auth (Identity Provider) integrado ao Spring Security.

---

## 🏛️ Arquitetura do Back-end (Como usar as pastas)

O fluxo de dados da nossa API segue uma divisão clara de responsabilidades:

$$\text{Requisição HTTP (Front-end)} \longrightarrow \text{Controller} \longrightarrow \text{Service} \longrightarrow \text{Repository} \longrightarrow \text{PostgreSQL (Model)}$$

Todas as classes do back-end devem ser criadas dentro de `backend/src/main/java/br/ufs/dcomp/collina/` nas seguintes camadas:

* 📁 **`controller/`** ➔ **Porta de entrada da API:** Recebe as requisições HTTP do front-end (`@GetMapping`, `@PostMapping`, etc.), faz validações básicas de parâmetros e chama a Service correspondente.  
  *⚠️ Não coloque regras de negócio ou cálculos aqui!*
* 📁 **`service/`** ➔ **Cérebro da aplicação:** Onde ficam todas as regras de negócio e validações lógicas (`@Service`).  
  *Exemplo:* Verificar se o associado está inadimplente antes de confirmar uma reserva; calcular o total de uma mensalidade.
* 📁 **`repository/`** ➔ **Comunicação com o Banco:** Interfaces do Spring Data JPA (`JpaRepository`). Onde ficam as operações de persistência e consultas (`findById`, `save`, queries personalizadas).
* 📁 **`model/`** ➔ **Tabelas do Banco:** Nossas entidades JPA (`@Entity`). Representam as tabelas físicas do PostgreSQL (ex: `Associado.java`, `Espaco.java`, `Mensalidade.java`).
* 📁 **`dto/`** ➔ **Dados de Entrada e Saída:** Classes simples (Records ou classes comuns) que definem o que o front-end pode enviar (Request) e o que a API devolve (Response). Evita expor diretamente a entidade do banco na API.
* 📁 **`exception/`** ➔ **Tratamento Global de Erros:** Onde criamos exceções personalizadas e o `@RestControllerAdvice` para capturar falhas e devolver mensagens amigáveis em JSON ao front-end (ex: erro 400 ou 404 estruturado).
* 📁 **`config/`** ➔ **Configurações Gerais:** Onde configuramos os Beans do sistema, filtros do Spring Security, integração com o Firebase e permissões de CORS para o front-end.

---

## 💻 Arquitetura do Front-end (Como usar as pastas)

O front-end utiliza uma **Arquitetura Modular baseada em Features (Domínios)**. Isso permite que os 7 integrantes desenvolvam telas e componentes simultaneamente sem causar conflitos de código no Git.

Tudo fica dentro de `frontend/src/`:

* 📁 **`features/`** ➔ **🚀 O coração do projeto (Módulos do Clube):**  
  Cada módulo do sistema tem sua própria pasta isolada:
  * 📂 `auth/`: Telas e formulários de login e recuperação de senha.
  * 📂 `associados/`: Gestão de associados titulares, dependentes e planos.
  * 📂 `financeiro/`: Mensalidades, fluxo de caixa e relatórios.
  * 📂 `reservas/`: Grade de horários e agendamento de espaços esportivos.
  * 📂 `estoque/`: Produtos, pedidos e movimentação de inventário.
  * 📂 `funcionarios/`: Gestão de colaboradores, escalas e turnos.  
  *(Dentro da pasta da sua feature, você pode criar suas próprias telas e componentes específicos sem mexer na pasta do colega!)*

* 📁 **`components/`** ➔ **Componentes Visuais Globais (Reutilizáveis):**  
  Elementos de interface padrão que são usados em vários lugares do sistema para manter o visual uniforme (ex: `Button/`, `Input/`, `Modal/`, `Table/`, `Navbar/`, `Sidebar/`).

* 📁 **`routes/`** ➔ **Navegação e Proteção por Perfil (RBAC):**  
  * `index.tsx`: Onde declaramos todas as rotas da aplicação via `react-router-dom`.
  * `ProtectedRoute.tsx`: Componente que verifica o perfil do usuário logado (ex: `ADMIN`, `FINANCEIRO`, `ASSOCIADO`) e impede que uma pessoa acesse telas que não são do seu perfil.

* 📁 **`services/`** ➔ **Comunicação com o Back-end:**  
  * `api.ts`: Instância do Axios que aponta para `http://localhost:8080` e possui um interceptor para anexar automaticamente o token do Firebase em cada requisição.

* 📁 **`config/`** ➔ **Configurações Externas:**  
  * `firebase.ts`: Inicialização das credenciais do Firebase Auth.

* 📁 **`context/`** ➔ **Estado Global:**  
  * `AuthContext.tsx`: Mantém na memória quem é o usuário logado, seu token e seu perfil para que qualquer tela possa consultar.

* 📁 **`types/`** ➔ **Tipagens Globais TypeScript:**  
  Definições de interfaces e tipos comuns (ex: tipos de usuário e enum de perfis).

---

## ⚙️ Pré-requisitos para Rodar o Projeto

Antes de executar, certifique-se de ter instalado em sua máquina:
1. **Java JDK 21**
2. **Node.js 20+** e **npm**
3. **PostgreSQL 16+**

### 🗄️ Configuração do Banco de Dados Local
Para garantir que o código funcione em todas as máquinas sem alterar arquivos de configuração, usem as seguintes credenciais locais no PostgreSQL:

* **Porta:** `5432`
* **Nome do Banco:** `collina_db` *(crie essa base no pgAdmin antes da primeira execução)*
* **Usuário:** `postgres`
* **Senha:** `postgres`

> As migrações de tabelas são gerenciadas automaticamente pelo **Flyway** a partir de scripts `.sql` em `src/main/resources/db/migration/`.

---

## 🚀 Como Executar

### 1. Back-end (Spring Boot)
Entre na pasta `backend`:
```bash
cd backend
./mvnw spring-boot:run
```
*(Ou execute a classe `CollinaApiApplication.java` diretamente pelo Play da sua IDE).*  
A API estará rodando em: `http://localhost:8080`.

### 2. Front-end (React + Vite)
Entre na pasta `frontend`:
```bash
cd frontend
npm install
npm run dev
```
Acesse a aplicação no navegador pelo endereço indicado (geralmente `http://localhost:5173`).

---

## 🌿 Fluxo de Trabalho do Time (Git & GitHub)

Para mantermos o projeto organizado e evitarmos conflitos de código entre os 7 integrantes:

1. **Nunca faça commit direto na branch `main`:** A branch `main` sempre deve conter código estável.
2. **Crie uma branch para cada tarefa:**
   * `feat/associados-cadastro-titular`
   * `feat/financeiro-emissao-mensalidade`
   * `feat/reservas-grade-horarios`
   * `fix/ajuste-validacao-cpf`
3. **Abra um Pull Request (PR):** Quando terminar a funcionalidade, abra um PR para a `main` e peça a revisão de pelo menos um colega do grupo antes de realizar o merge.
