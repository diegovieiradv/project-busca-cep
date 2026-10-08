# Busca CEP

Aplicacao full-stack para consulta de enderecos via CEP, com backend Spring Boot e frontend Next.js.

## Descricao

O Busca CEP permite consultar enderecos completos a partir de um CEP brasileiro, integrando-se a API ViaCEP. A aplicacao possui interface web responsiva com identidade visual profissional.

## Arquitetura

- **Backend:** Java 21 + Spring Boot 3.5 + Maven Wrapper (Spring Web, CORS, Docker)
- **Frontend:** Next.js 16 (TypeScript) + Tailwind CSS + Jest
- **Integracao:** API REST (`/api/cep/{cep}`) consumida pelo frontend via cliente API configurado com `NEXT_PUBLIC_API_BASE_URL`
- **Deploy:** Backend em container (Render) / Frontend (Vercel)

## Funcionalidades

- Consulta de CEP via API ViaCEP
- Interface responsiva (desktop / mobile)
- Identidade visual com paleta profissional (`#0f3d7a`, `#c5a065`)
- Favicon SVG personalizado
- Estados visuais: carregamento, erro e resultado
- Acessibilidade basica (labels, roles, aria-label)

## Tecnologias

- Java 21 / Spring Boot 3.5 / Maven
- Next.js 16 / TypeScript / Tailwind CSS
- Jest (testes frontend)
- Docker (backend)

## Instalacao e Execucao

### Backend
```bash
cd .
./mvnw spring-boot:run
```
Ou: `mvn clean compile spring-boot:run`

### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Variaveis de Ambiente (`frontend/.env.local`)
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## Testes

- Backend: `mvn test` (58 testes)
- Frontend: `npm test` (9 testes)
- E2E: validar build (`npm run build`) + smoke test no deploy

## Deploy

- Backend: Render (container via Dockerfile)
- Frontend: Vercel
- Variaveis de ambiente configuradas nas plataformas (sem credenciais expostas)

## Licenca
MIT License - Diego Vieira

