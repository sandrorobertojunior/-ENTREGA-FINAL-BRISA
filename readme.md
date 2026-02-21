
# OBECI — Repositório do Sistema (Backend + Frontend)

Este repositório contém o sistema OBECI completo, dividido em:

- **Backend (API + WebSocket)**: `back-end-obeci/` (Spring Boot)
- **Frontend (Aplicação Web)**: `front-end-obeci/` (Next.js)
- **Documentos**: `documentos/`

O objetivo deste documento é descrever o conteúdo do repositório e padronizar as **regras de compilação e execução** para ambientes de desenvolvimento e produção.

---

## 1) Estrutura do repositório

- `back-end-obeci/`
	- API REST e colaboração em tempo real via WebSocket (STOMP)
	- Build com Maven Wrapper (`mvnw`, `mvnw.cmd`)
	- Configurações em `src/main/resources/application*.yml`
- `front-end-obeci/`
	- Aplicação web em Next.js (React + TypeScript)
	- Scripts em `package.json` (`dev`, `build`, `start`, `lint`)
	- Configuração da API via variável `NEXT_PUBLIC_API_URL`
- `documentos/`
	- PDFs e planilhas de requisitos e casos de teste

---

## 2) Portas e URLs (padrão)

- **Backend**: `http://localhost:9090`
- **Frontend**: `http://localhost:3000`

Observação: o backend usa cookies (`credentials: "include"` no frontend). Em produção, isso exige configuração correta de CORS e políticas de cookie.

---

## 3) Pré-requisitos

### Backend

- **Java (JDK) 21**
- PostgreSQL (para perfil `dev` e `prod`)
- (Opcional) Maven instalado: o repositório já inclui Maven Wrapper

### Frontend

- **Node.js** (recomendado usar uma versão LTS)
- npm (ou outro gerenciador, mas os comandos abaixo assumem npm)

---

## 4) Configuração de ambiente

## 4.1 Backend — perfis e variáveis

O backend possui perfis:

- `dev` (perfil padrão)
- `prod`

Arquivos:

- `back-end-obeci/src/main/resources/application.yml` (base, define `dev` como padrão)
- `back-end-obeci/src/main/resources/application-dev.yml`
- `back-end-obeci/src/main/resources/application-prod.yml`

### Banco de dados (DEV)

Por padrão (ver `application-dev.yml`):

- `jdbc:postgresql://localhost:5432/coesterdb`
- usuário: `admin`
- senha: `admin`

Se necessário, ajuste esses valores no YAML de dev.

### Variáveis para produção (PROD)

No perfil `prod`, o backend suporta (ver `application-prod.yml`):

- `SPRING_PROFILES_ACTIVE=prod`
- `APP_JWT_SECRET` (**obrigatória em produção**)
- `OBECI_DB_URL`
- `OBECI_DB_USERNAME`
- `OBECI_DB_PASSWORD`

Recomendação: em produção, **não utilizar valores default** do YAML; sempre fornecer via ambiente/secret manager.

### CORS

O CORS do backend é configurado em `app.cors.allowed-origins` no YAML.

- Em DEV já existem origens locais (`http://localhost:3000` e `http://localhost:3001`).
- Em PROD, ajuste para o domínio/porta reais do frontend.

---

## 4.2 Frontend — variável de API

O frontend resolve a base URL do backend via a variável:

- `NEXT_PUBLIC_API_URL`

Crie o arquivo `front-end-obeci/.env.local` (desenvolvimento) com, por exemplo:

```bash
NEXT_PUBLIC_API_URL=http://localhost:9090
```

Importante: variáveis `NEXT_PUBLIC_*` são usadas no build do Next.js; se a URL da API mudar, normalmente é necessário **rebuild** para refletir a alteração.

---

## 5) Como compilar e executar (desenvolvimento)

## 5.1 Subir o backend (DEV)

Em PowerShell:

```powershell
cd back-end-obeci
./mvnw.cmd spring-boot:run
```

O backend inicia em `http://localhost:9090`.

## 5.2 Subir o frontend (DEV)

Em outro terminal:

```powershell
cd front-end-obeci
npm install
npm run dev
```

Abra `http://localhost:3000`.

---

## 6) Como compilar e executar (produção)

## 6.1 Backend — gerar JAR

```powershell
cd back-end-obeci
./mvnw.cmd test
./mvnw.cmd package
```

O artefato gerado fica em `back-end-obeci/target/` (ex.: `platform-0.0.1-SNAPSHOT.jar`).

### Rodar o JAR com perfil `prod`

```powershell
cd back-end-obeci
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:APP_JWT_SECRET = "defina-um-segredo-forte-com-32+-caracteres"
# Configure também o banco, se necessário:
# $env:OBECI_DB_URL = "jdbc:postgresql://..."
# $env:OBECI_DB_USERNAME = "..."
# $env:OBECI_DB_PASSWORD = "..."

java -jar .\target\platform-0.0.1-SNAPSHOT.jar
```

## 6.2 Frontend — build e start

```powershell
cd front-end-obeci
npm install
npm run build
npm run start
```

Por padrão, o Next.js sobe em `http://localhost:3000`.

---

## 7) Testes e verificação

### Backend

```powershell
cd back-end-obeci
./mvnw.cmd test
```

### Frontend

```powershell
cd front-end-obeci
npm run lint
```

---

## 8) Observações importantes (cookies, CORS e HTTPS)

- O frontend faz `fetch` com `credentials: "include"`, portanto o backend precisa estar com `allow-credentials: true` e com `allowed-origins` correto.
- Em produção, quando o frontend e o backend estão em domínios diferentes, normalmente é necessário:
	- HTTPS no backend
	- Cookie com `secure: true` e `same-site: None` (já previsto no `application-prod.yml`)

---

## 9) O que **não** existe neste repositório

- Não há `Dockerfile`/`docker-compose.yml` prontos, usuario vai ter que criar o próprio

