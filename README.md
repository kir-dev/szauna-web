# Szauna-Web
The booking management system for Szauna Kör.

---

| Service   | Default URL           |
|-----------|-----------------------|
| Backend   | http://localhost:8080 |
| Swagger   | http://localhost:8080/swagger-ui/index.html |

---

## Prerequisites

- **JDK 25**
- AuthSCH OAuth Client ([auth.sch.bme.hu console](https://auth.sch.bme.hu/console))

### 1. AuthSCH + local config

1. Create an OAuth client at [auth.sch.bme.hu](https://auth.sch.bme.hu/console/).
2. Set *Átirányítási cím* (redirect URI) to: `http://localhost:8080/login/oauth2/code/authsch`
3. Create **`backend/src/main/resources/config/application-local.yml`**

```yml
### SPECIFY AUTHSCH
spring:
  security:
    oauth2:
      client:
        registration:
          authsch:
            client-id: "YOUR_CLIENT_ID"
            client-secret: "YOUR_CLIENT_KEY"
```

### 2. Create secret string for JWT

1. Generate the secret string with this command.

```bash
openssl rand -base64 32
```

2. Edit **`backend/src/main/resources/config/application-local.yml`**

```yml
szaunaWeb:
  jwt:
    secret: YOUR_SECRET_STRING
```

### 3. ADMIN role (optiaonal)

If you want to give **ADMIN** privileges to someone who does not have a "körvezető" position, put their **internal id** in the list of admins.

Edit: **`backend/src/main/resources/config/application-local.yml`**

*You can add multiple internal ids separated by commas.*

```yml
### SPECIFY ADMIN PRIVILEGES
szaunaWeb:
  admins: "YOUR_INTERNAL_ID,INTERNAL_ID,INTERNAL_ID"
```