# SalesMind API

Sistema de gestão de vendas com análise de IA via Gemini para transcrição e análise de chamadas de vendas.

## 🛠️ Stack Tecnológica

- **Java 24** - Linguagem principal
- **HttpServer** - Servidor HTTP nativo do Java (sem framework)
- **PostgreSQL** - Banco de dados relacional
- **HikariCP** - Connection pooling
- **Gemini 2.5 Flash** - IA para transcrição e análise
- **Maven** - Gerenciamento de dependências

## 📐 Arquitetura e Modelagem

### Diagrama UML
![UML - SalesMind](docs/UML%20-%20SalesMind.png)

### Diagrama DER
![DER - SalesMind](docs/DER%20-%20SalesMind.png)

### Script SQL
O script completo de criação do banco de dados, incluindo tabelas, índices, triggers e views úteis está disponível em:
- [Schema SQL](database/schema.sql)

## 📋 Pré-requisitos

- Java 24 ou superior
- PostgreSQL 14+ instalado e rodando
- Maven 3.9+
- Gemini API Key (para funcionalidades de IA)

## 🚀 Setup do Banco de Dados

### 1. Instalar PostgreSQL

### 2. Criar Database

```bash
# Opção 1: Usando o script SQL completo (recomendado)
psql -U postgres -c "CREATE DATABASE salesmind;"
psql -U postgres -d salesmind -f database/schema.sql

# Opção 2: Deixar a aplicação criar as tabelas automaticamente
# (apenas conecte ao database, as tabelas serão criadas no primeiro start)
```

```sql
-- Conectar ao PostgreSQL manualmente
psql -U postgres

-- Criar database
CREATE DATABASE salesmind;

-- Conectar ao database
\c salesmind

-- As tabelas serão criadas automaticamente pela aplicação
-- Ou execute: \i database/schema.sql
```

### 3. Configurar Variáveis de Ambiente

Copie `.env.example` para `.env` e configure:

```bash
cp .env.example .env
```

Edite `.env`:
```properties
# Database
DB_URL=jdbc:postgresql://localhost:5432/salesmind
DB_USER=postgres
DB_PASSWORD=sua_senha_aqui

# Gemini AI
GEMINI_API_KEY=sua_api_key_aqui

# File Upload
UPLOAD_DIR=./uploads
```

## 📦 Compilar e Executar

```bash
# Compilar
mvn clean compile

# Executar
mvn exec:java

# Ou executar JAR
mvn package
java -jar target/salesmind-1.0-SNAPSHOT.jar
```

## 📄 Documentação da API

A documentação interativa está disponível em:

```
http://localhost:8080/api/docs
```
