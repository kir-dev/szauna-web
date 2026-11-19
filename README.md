# Szauna-Web
The booking management system for Szauna Kör.

| Service   | Default URL           |
|-----------|-----------------------|
| Frontend  | http://localhost:5173 |
| Backend   | http://localhost:8080 |
| Swagger   | http://localhost:8080/swagger-ui/index.html |


## Prerequisites

- **JDK 25**
- **Node.js** and **npm** (tested with Node.js *v24.18.0* and npm *x.y.z*)
- **Git**
- AuthSCH OAuth Client ([auth.sch.bme.hu console](https://auth.sch.bme.hu/console))

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/kir-dev/szauna-web.git
cd szauna-web
```

### 2. Create an AuthSCH OAuth client

1. Create an OAuth client at [auth.sch.bme.hu](https://auth.sch.bme.hu/console/).
2. Set *Átirányítási cím* (redirect URI) to: `http://localhost:8080/login/oauth2/code/authsch`

### 3. Generate a JWT secret

```bash
openssl rand -base64 32
```

### 4. Create the local config

Create **`backend/src/main/resources/config/application-local.yml`** with the following content:

```yml
spring:
  application:
    name: SzaunaWeb
  datasource:
    url: jdbc:h2:mem:szaunadb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  security:
    oauth2:
      client:
        registration:
          authsch:
            client-id: "YOUR_CLIENT_ID"
            client-secret: "YOUR_CLIENT_SECRET"
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - openid
              - profile
              - email
              - pek.sch.bme.hu:profile
        provider:
          authsch:
            authorization-uri: https://auth.sch.bme.hu/site/login
            token-uri: https://auth.sch.bme.hu/oauth2/token
            user-info-uri: https://auth.sch.bme.hu/oidc/userinfo
            jwk-set-uri: https://auth.sch.bme.hu/oidc/jwks
            user-name-attribute: sub

szaunaWeb:
  szaunaKorId: 435
  # In default settings, in local environment you do not have to modify the frontend url
  frontendUrl: "http://localhost:5173/dashboard"
  # Optional: grant ADMIN role to users without a "körvezető" position.
  # Comma-separated list of AuthSCH internal ids.
  admins: "YOUR_INTERNAL_ID,test-admin-sub"
  activeMembershipsScope: pek.sch.bme.hu:activeMemberships/v1
  jwt:
    secret: "YOUR_JWT_SECRET"
    experation: 3600000 #1 hour expiration
    issuer: Kir-Dev
    authTokenCookieName: auth_token
```
> [!CAUTION]
> This file contains secrets. It is listed in `.gitignore`, never commit it.

### 5. Run the backend

The backend must run with the **`local`** profile, otherwise the config above is not loaded.

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Test data (optional):** to load the sample data, add the `test` profile as well:

```bash
./gradlew bootRun --args='--spring.profiles.active=local,test'
```

In IntelliJ IDEA you can set the same in *Run Configuration → Active profiles*: `local,test`.

### 6. Install frontend dependencies

```bash
cd frontend
npm install
```

### 7. Run the frontend in development mode

```bash
npm run dev
```

The app is now available at http://localhost:5173.