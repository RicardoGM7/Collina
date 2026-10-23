# âš½ Collina â€” Sistema de GestÃ£o de Clube Poliesportivo

AplicaÃ§Ã£o web integrada para automaÃ§Ã£o e gestÃ£o dos fluxos operacionais, financeiros, administrativos, de associados, reservas de espaÃ§os e escalas de um clube poliesportivo.

> **Projeto AcadÃªmico**  
> Universidade Federal de Sergipe (UFS) â€” Departamento de ComputaÃ§Ã£o (DCOMP)  
> **Arquitetura:** MonÃ³lito Modular em Camadas (Spring Boot + React + PostgreSQL)

---

## ðŸ› ï¸ Tecnologias Utilizadas

* **Back-end:** Java 21, Spring Boot, Spring Data JPA, Spring Security, Hibernate DDL (Auto-update), Lombok.
* **Front-end:** React 18, TypeScript, Vite, React Router DOM, Axios, Lucide React.
* **Banco de Dados:** PostgreSQL 16+.
* **AutenticaÃ§Ã£o:** Firebase Auth (Identity Provider) integrado ao Spring Security.

---

## ðŸ›ï¸ Arquitetura do Back-end (Como usar as pastas)

O fluxo de dados da nossa API segue uma divisÃ£o clara de responsabilidades:

$$\text{RequisiÃ§Ã£o HTTP (Front-end)} \longrightarrow \text{Controller} \longrightarrow \text{Service} \longrightarrow \text{Repository} \longrightarrow \text{PostgreSQL (Model)}$$

Todas as classes do back-end devem ser criadas dentro de `backend/src/main/java/br/ufs/dcomp/collina/` nas seguintes camadas:

* ðŸ“ **`controller/`** âž” **Porta de entrada da API:** Recebe as requisiÃ§Ãµes HTTP do front-end (`@GetMapping`, `@PostMapping`, etc.), faz validaÃ§Ãµes bÃ¡sicas de parÃ¢metros e chama a Service correspondente.  
  *âš ï¸ NÃ£o coloque regras de negÃ³cio ou cÃ¡lculos aqui!*
* ðŸ“ **`service/`** âž” **CÃ©rebro da aplicaÃ§Ã£o:** Onde ficam todas as regras de negÃ³cio e validaÃ§Ãµes lÃ³gicas (`@Service`).  
  *Exemplo:* Verificar se o associado estÃ¡ inadimplente antes de confirmar uma reserva; calcular o total de uma mensalidade.
* ðŸ“ **`repository/`** âž” **ComunicaÃ§Ã£o com o Banco:** Interfaces do Spring Data JPA (`JpaRepository`). Onde ficam as operaÃ§Ãµes de persistÃªncia e consultas (`findById`, `save`, queries personalizadas).
* ðŸ“ **`model/`** âž” **Tabelas do Banco:** Nossas entidades JPA (`@Entity`). Representam as tabelas fÃ­sicas do PostgreSQL (ex: `Associado.java`, `Espaco.java`, `Mensalidade.java`).
* ðŸ“ **`dto/`** âž” **Dados de Entrada e SaÃ­da:** Classes simples (Records ou classes comuns) que definem o que o front-end pode enviar (Request) e o que a API devolve (Response). Evita expor diretamente a entidade do banco na API.
* ðŸ“ **`exception/`** âž” **Tratamento Global de Erros:** Onde criamos exceÃ§Ãµes personalizadas e o `@RestControllerAdvice` para capturar falhas e devolver mensagens amigÃ¡veis em JSON ao front-end (ex: erro 400 ou 404 estruturado).
* ðŸ“ **`config/`** âž” **ConfiguraÃ§Ãµes Gerais:** Onde configuramos os Beans do sistema, filtros do Spring Security, integraÃ§Ã£o com o Firebase e permissÃµes de CORS para o front-end.

---

## ðŸ’» Arquitetura do Front-end (Como usar as pastas)

O front-end utiliza uma **Arquitetura Modular baseada em Features (DomÃ­nios)**. Isso permite que os 7 integrantes desenvolvam telas e componentes simultaneamente sem causar conflitos de cÃ³digo no Git.

Tudo fica dentro de `frontend/src/`:

* ðŸ“ **`features/`** âž” **ðŸš€ O coraÃ§Ã£o do projeto (MÃ³dulos do Clube):**  
  Cada mÃ³dulo do sistema tem sua prÃ³pria pasta isolada:
  * ðŸ“‚ `auth/`: Telas e formulÃ¡rios de login e recuperaÃ§Ã£o de senha.
  * ðŸ“‚ `associados/`: GestÃ£o de associados titulares, dependentes e planos.
  * ðŸ“‚ `financeiro/`: Mensalidades, fluxo de caixa e relatÃ³rios.
  * ðŸ“‚ `espacos/`: GestÃ£o de espaÃ§os fÃ­sicos, quadras, grade de horÃ¡rios e agendamento de reservas.
  * ðŸ“‚ `estoque/`: Produtos, pedidos e movimentaÃ§Ã£o de inventÃ¡rio.
  * ðŸ“‚ `funcionarios/`: GestÃ£o de colaboradores, escalas e turnos.  
  *(Dentro da pasta da sua feature, vocÃª pode criar suas prÃ³prias telas e componentes especÃ­ficos sem mexer na pasta do colega!)*

* ðŸ“ **`components/`** âž” **Componentes Visuais Globais (ReutilizÃ¡veis):**  
  Elementos de interface padrÃ£o que sÃ£o usados em vÃ¡rios lugares do sistema para manter o visual uniforme (ex: `Button/`, `Input/`, `Modal/`, `Table/`, `Navbar/`, `Sidebar/`).

* ðŸ“ **`routes/`** âž” **NavegaÃ§Ã£o e ProteÃ§Ã£o por Perfil (RBAC):**  
  * `index.tsx`: Onde declaramos todas as rotas da aplicaÃ§Ã£o via `react-router-dom`.
  * `ProtectedRoute.tsx`: Componente que verifica o perfil do usuÃ¡rio logado (ex: `ADMIN`, `FINANCEIRO`, `ASSOCIADO`) e impede que uma pessoa acesse telas que nÃ£o sÃ£o do seu perfil.

* ðŸ“ **`services/`** âž” **ComunicaÃ§Ã£o com o Back-end:**  
  * `api.ts`: InstÃ¢ncia do Axios que aponta para `http://localhost:8080` e possui um interceptor para anexar automaticamente o token do Firebase em cada requisiÃ§Ã£o.

* ðŸ“ **`config/`** âž” **ConfiguraÃ§Ãµes Externas:**  
  * `firebase.ts`: InicializaÃ§Ã£o das credenciais do Firebase Auth.

* ðŸ“ **`context/`** âž” **Estado Global:**  
  * `AuthContext.tsx`: MantÃ©m na memÃ³ria quem Ã© o usuÃ¡rio logado, seu token e seu perfil para que qualquer tela possa consultar.

* ðŸ“ **`types/`** âž” **Tipagens Globais TypeScript:**  
  DefiniÃ§Ãµes de interfaces e tipos comuns (ex: tipos de usuÃ¡rio e enum de perfis).

---

## âš™ï¸ PrÃ©-requisitos para Rodar o Projeto

Antes de executar, certifique-se de ter instalado em sua mÃ¡quina:
1. **Java JDK 21**
2. **Node.js 20+** e **npm**
3. **PostgreSQL 16+**

### ðŸ—„ï¸ ConfiguraÃ§Ã£o do Banco de Dados Local
Para garantir que o cÃ³digo funcione em todas as mÃ¡quinas sem alterar arquivos de configuraÃ§Ã£o, usem as seguintes credenciais locais no PostgreSQL:

* **Porta:** `5432`
* **Nome do Banco:** `collina_db` *(crie essa base no pgAdmin antes da primeira execuÃ§Ã£o)*
* **UsuÃ¡rio:** `postgres`
* **Senha:** `postgres`

> As tabelas sÃ£o criadas e atualizadas automaticamente pelo **Hibernate / Spring Data JPA** (`ddl-auto=update`) a partir das classes anotadas com `@Entity`.

---

## ðŸš€ Como Executar

### 1. Back-end (Spring Boot)
Entre na pasta `backend`:
```bash
cd backend
./mvnw spring-boot:run
```
*(Ou execute a classe `CollinaApiApplication.java` diretamente pelo Play da sua IDE).*  
A API estarÃ¡ rodando em: `http://localhost:8080`.

### 2. Front-end (React + Vite)
Entre na pasta `frontend`:
```bash
cd frontend
npm install
npm run dev
```
Acesse a aplicaÃ§Ã£o no navegador pelo endereÃ§o indicado (geralmente `http://localhost:5173`).

---

## ðŸŒ¿ Fluxo de Trabalho do Time (Git & GitHub)

Para mantermos o projeto organizado e evitarmos conflitos de cÃ³digo entre os 7 integrantes:

1. **Nunca faÃ§a commit direto na branch `main`:** A branch `main` sempre deve conter cÃ³digo estÃ¡vel.
2. **Crie uma branch para cada tarefa:**
   * `feat/associados-cadastro-titular`
   * `feat/financeiro-emissao-mensalidade`
   * `feat/reservas-grade-horarios`
   * `fix/ajuste-validacao-cpf`
3. **Abra um Pull Request (PR):** Quando terminar a funcionalidade, abra um PR para a `main` e peÃ§a a revisÃ£o de pelo menos um colega do grupo antes de realizar o merge.
