# Postman — Lume V1

Collection versionada: [`Lume_-_V1.postman_collection.json`](Lume_-_V1.postman_collection.json)

## Base URL (Render)

`https://sboot-lume-monolith-api.onrender.com`

> Cold start no free tier: aguarde 30–60s na primeira chamada.

## Importar no Postman Desktop / Cursor

1. Abra o Postman (desktop) **ou** autentique o MCP Postman no Cursor.
2. **Import** → selecione `Lume_-_V1.postman_collection.json`.
3. Rode `Auth / cadastrarUsuario` e em seguida `Auth / autenticar` (o script de teste grava `jwt_token` no environment).
4. Demais rotas autenticadas usam o Bearer do environment.

## Pastas da collection

- Auth
- Usuários
- Catálogo
- Estabelecimentos
- Ocorrências
- Segurança (diagnóstico + mapa)
