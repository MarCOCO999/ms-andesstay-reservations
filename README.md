# ms-andesstay-reservations

Microservicio núcleo del proyecto **AndesStay** (caso DSY1107 – Desarrollo Cloud Native I, Duoc UC): gestiona el ciclo de vida completo de una reserva, calcula su monto, coordina la disponibilidad con `catalog` y dispara las notificaciones y el registro de auditoría/reportería del resto del sistema.

## Rol en la arquitectura

- Expone la API de reservas detrás del BFF (`ms-andesstay-bff`), que reenvía el JWT de Azure AD ya validado; este servicio lo vuelve a validar de forma independiente.
- Al **crear** una reserva, consulta la tarifa vigente a `ms-andesstay-catalog` (nunca confía en un monto enviado por el cliente) y calcula `totalAmount = tarifa_por_noche × noches`.
- Al **confirmar** una reserva (`CREADA → CONFIRMADA`), llama a `catalog` para reservar atómicamente una unidad (`POST /api/catalog/units/{id}/reserve`); si no hay disponibilidad, la transacción se aborta y la reserva no cambia de estado.
- Al hacer **checkout** o **cancelar** una reserva ya confirmada, libera la unidad en `catalog` (`POST /api/catalog/units/{id}/release`), best-effort.
- Publica eventos de dominio a **Kafka** (`reservations.events`) para que `ms-andesstay-audit` y `ms-andesstay-report` los consuman de forma independiente.
- Publica comandos a **RabbitMQ** (`cmd.direct` / `cmd.topic`) para que `ms-andesstay-notify` envíe el email/voucher/ticket de housekeeping correspondiente. Los nombres de exchanges y routing keys están en `application.yml` (`andesstay.messaging`), y `ReservationCommandPublisher` es la única clase que publica (el servicio de negocio no conoce `RabbitTemplate`). Con *publisher confirms* y *returns* (`mandatory`), un comando que el broker rechaza o que no llega a ninguna cola queda registrado en el log.
- Incluye una **pasarela de pago simulada** (sin integración real con ningún procesador): el huésped puede pagar de inmediato o al hacer check-in, y el pago genera un `transactionId` ficticio a modo de comprobante.

## Máquina de estados

```
CREADA ─────► CONFIRMADA ─────► CHECKIN_PENDIENTE ─────► EN_ESTADIA ─────► CHECKOUT
  │               │                     │
  └──────────► CANCELADA ◄──────────────┘
```

No se puede hacer check-in sin haber confirmado la reserva antes (regla explícita del caso). `CHECKOUT` y `CANCELADA` son estados terminales.

## Stack

- Spring Boot 4.1.1 / Java 21
- Spring Data JPA + Oracle Database (driver `ojdbc17`)
- Spring Security OAuth2 Resource Server (JWT de Azure AD)
- Spring Kafka (productor de `reservations.events`)
- Spring AMQP / RabbitMQ (productor de comandos de notificación)
- `RestClient` para la integración síncrona con `catalog`

## Configuración

| Variable | Descripción | Default |
|---|---|---|
| `SERVER_PORT` | Puerto HTTP | `8082` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Conexión a Oracle | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` / `andesstay` / `changeit` |
| `JPA_DDL_AUTO` | Estrategia de Hibernate | `update` |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker de Kafka | `localhost:9092` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | Conexión a RabbitMQ | `localhost` / `5672` / `guest` / `guest` |
| `CATALOG_SERVICE_URL` | URL base de `ms-andesstay-catalog` | `http://localhost:8083` |
| `AZURE_ISSUER_URI` / `AZURE_API_AUDIENCE` | Validación del JWT de Azure AD | tenant/App ID URI del caso |

## Endpoints principales

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| `POST` | `/api/reservations` | Cliente, Operador, Admin | Crea una reserva; calcula el monto server-side |
| `GET` | `/api/reservations` | Cliente, Operador, Admin | Lista reservas (un Cliente solo ve las suyas) |
| `GET` | `/api/reservations/{id}` | Cliente, Operador, Admin | Detalle de una reserva (un Cliente solo puede ver las propias) |
| `PUT` | `/api/reservations/{id}/status` | Operador, Admin | Cambia el estado siguiendo la máquina de estados |
| `POST` | `/api/reservations/{id}/pay` | Cliente, Operador, Admin | Pasarela de pago simulada; un Cliente solo puede pagar las suyas |

## Cómo ejecutarlo en local

Requiere Java 21 y la infraestructura de [andesstay-infra](https://github.com/MarCOCO999/andesstay-infra) levantada (Oracle, RabbitMQ, Kafka).

```bash
./mvnw spring-boot:run
```

## Repos relacionados

[frontend-andesstay](https://github.com/MarCOCO999/frontend-andesstay) · [ms-andesstay-bff](https://github.com/MarCOCO999/ms-andesstay-bff) · [ms-andesstay-catalog](https://github.com/MarCOCO999/ms-andesstay-catalog) · [ms-andesstay-notify](https://github.com/MarCOCO999/ms-andesstay-notify) · [ms-andesstay-audit](https://github.com/MarCOCO999/ms-andesstay-audit) · [ms-andesstay-report](https://github.com/MarCOCO999/ms-andesstay-report) · [ms-andesstay-rabbit-admin](https://github.com/MarCOCO999/ms-andesstay-rabbit-admin) · [andesstay-infra](https://github.com/MarCOCO999/andesstay-infra)
