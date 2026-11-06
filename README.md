# Cookie + OAuth2/OIDC Lab

Учебный проект React + Vite 7 и Kotlin/Spring Boot: вход через OpenID Connect (Keycloak), серверная сессия, OAuth2 refresh token и CSRF-защита изменяющих запросов.

Это BFF-подход: браузер получает только HttpOnly session cookie; OAuth access/refresh tokens остаются на backend и не попадают в React.

- `frontend/` — React + TypeScript + Vite 7.
- `backend/` — Spring Boot + Kotlin, Gradle Kotlin DSL.
- `keycloak/realm-cookie-lab.json` — локальный OIDC provider и учебный клиент.
- `compose.yaml` — поднимает локальный Keycloak.
- `start.txt` — полное описание, запуск, Cloudflare/Render настройки, OAuth, refresh и CSRF.

Быстрый старт: в корне `docker compose up -d keycloak`, затем в `backend/` выполнить `gradle bootRun`, а в `frontend/` — `npm install && npm run dev`.

> Учебная конфигурация и realm secret не предназначены для production. Не публикуйте client secret, signing keys или токены.
