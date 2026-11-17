# Car Rental Vehicle Service

A Java/Spring Boot REST API for registering vehicles, associating them with a user identifier, and releasing them back to available status. Vehicle records and association changes are persisted in MySQL.

**Status:** historical portfolio project, with a recorded local demonstration—not a production-ready rental platform. This documentation describes [source revision `05c9778`](https://github.com/Dumpy250/car-rental-service/tree/05c977857c41073a505584839d8e8028a05145e2).

## Start here

- **See it work:** [case study and recorded API workflow](https://www.cameron-basham.com/projects/car-rental-microservices). The walkthrough displays saved requests and responses; its controls do not call a live rental service.
- **Inspect the evidence:** [original HTTP recording](https://www.cameron-basham.com/projects/vehicle-service-recording/requests.json), using fictional data.
- **Explore the implementation:** [controller](https://github.com/Dumpy250/car-rental-service/blob/05c977857c41073a505584839d8e8028a05145e2/src/main/java/com/ola/controller/VehicleController.java), [service logic](https://github.com/Dumpy250/car-rental-service/blob/05c977857c41073a505584839d8e8028a05145e2/src/main/java/com/ola/service/VehicleServiceimp.java), and [vehicle model](https://github.com/Dumpy250/car-rental-service/blob/05c977857c41073a505584839d8e8028a05145e2/src/main/java/com/ola/model/Vehicle.java).
- **Run a local reproduction:** read the requirements, configuration and limitations below first. Use only a disposable database and keep the HTTP service on loopback.

## Repository scope

This repository contains the **vehicle service only**: Spring MVC endpoints, Jakarta validation, service-layer state transitions, and Spring Data JPA persistence. It does not include the wider project's gateway or Cognito user service, a frontend, or a complete reservation/payment workflow. Obtaining this repository does not supply those separate services or an AWS environment.

The wider project's EC2/VPC configuration, RDS integration and Cognito user management are described in the case study. The recorded vehicle-service demonstration is local evidence, not verification of that cloud deployment.

### Vehicle lifecycle

1. Create a vehicle with nonblank `brand`, `model`, and `licence_number` fields. The service generates its UUID, sets `AVAILABLE`, and clears any supplied owner/association date.
2. Associate an available vehicle with a user identifier. The service saves `ASSOCIATED`, the supplied owner string, and an association timestamp.
3. Release an associated vehicle using the matching owner string. The service restores `AVAILABLE` and clears owner/date.

Null fields are omitted from JSON. The stored owner is a caller-supplied string, not a verified user identity.

## Requirements and historical versions

| Component | Source requirement / recorded environment |
| --- | --- |
| JDK | Java **23** toolchain in `build.gradle`; recorded build/run used Eclipse Temurin **23.0.2+7** |
| Build tool | Included Gradle wrapper **8.12.1** |
| Framework | Spring Boot **3.4.3**; Spring dependency-management plugin **1.1.7** |
| Database | Existing MySQL server and a separate disposable schema/account; recorded runtime used MySQL **8.0.37** |
| JDBC driver | Source declares legacy `mysql:mysql-connector-java:8.0.33`; the retained packaged artifact actually contains **Connector/J 9.1.0** |

Use an installed JDK 23 and point `JAVA_HOME` and `PATH` at it for reproduction. Do not assume an arbitrary newer JDK works: [Gradle 8.12.1 supports running on Java 23, but not Java 24+](https://docs.gradle.org/8.12.1/userguide/compatibility.html). The wrapper and dependencies require network access on a machine without their caches; the earlier offline build used pre-existing caches and is not a fresh-install guarantee.

**Maintenance warning:** [Temurin 23 is end-of-service-life](https://adoptium.net/support), and [Spring Boot 3.4.x has ended open-source support](https://spring.io/blog/2025/12/18/spring-boot-3-4-13-available-now). These pins describe the historical project, not recommended defaults for a new deployment. Production reuse needs a separately planned move to a supported LTS JDK and compatible maintained Spring Boot/Gradle versions, dependency review, and regression testing. No upgrade is implied by this README.

## Local setup

These are **source-checked reproduction instructions**, not a claim that this exact fresh-checkout procedure or every shell/platform was tested. The previously exercised isolated build and API scenario are described separately below.

### 1. Build the executable JAR

From the repository root, with JDK 23 selected:

```bash
# Bash / compatible shell
bash ./gradlew --no-daemon bootJar -x test
```

```powershell
# Windows PowerShell alternative
.\gradlew.bat --no-daemon bootJar -x test
```

The configured output is `build/libs/car-rental-service.jar`. These commands package the application without running tests; they do not start the HTTP service. Do not substitute a full `build` and describe it as database-independent: the included `@SpringBootTest` context-load test needs datasource configuration and can initialize the application context.

### 2. Supply local configuration

Create a dedicated, empty MySQL schema and an account limited to that schema, with the DDL/DML permissions needed for Hibernate schema updates and vehicle operations. Do not use root or a shared/production database.

The checked-in YAML reads `${db.url}`, `${db.username}`, and `${db.password}`. Supply these using process environment variables ([Spring Boot external configuration](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html)):

| Environment variable | Example / purpose |
| --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:<MYSQL_PORT>/<DISPOSABLE_SCHEMA>?sslMode=REQUIRED` |
| `DB_USERNAME` | `<LOCAL_SCHEMA_USER>` |
| `DB_PASSWORD` | `<LOCAL_SECRET>` |
| `SERVER_ADDRESS` | `127.0.0.1` — explicitly prevent listening on external interfaces |
| `SERVER_PORT` | `8081` — the checked-in default; choose another unused local port if needed |

Angle-bracket values are placeholders, not working credentials or an existing endpoint. Set them privately in your process environment or local run configuration; do not commit secrets. The application does not automatically load a `.env` file. Avoid inheriting old IDE/cloud configuration.

The example JDBC URL requires a TLS-capable MySQL server. `sslMode=REQUIRED` encrypts the connection but does **not** verify server identity; this is a loopback reproduction setting, not a production TLS policy.

**Startup changes the selected schema:** the source sets `spring.jpa.hibernate.ddl-auto=update` and enables SQL logging. The database must already exist; Hibernate's schema update is not a database-provisioning step or a versioned migration system.

### 3. Start locally

After setting the variables and preparing the database:

```bash
java -jar build/libs/car-rental-service.jar
```

Use `http://127.0.0.1:8081/vehicles` if you selected port 8081. Check the application startup log before issuing requests. Stop the foreground application with Ctrl+C when finished and stop your disposable database separately. No gateway, Cognito credentials or AWS access is needed for the direct local vehicle API.

## API

Paths below are relative to the local server address.

| Method | Path | Behavior / successful response |
| --- | --- | --- |
| `POST` | `/vehicles` | Create a vehicle; **200**, vehicle JSON |
| `GET` | `/vehicles` | List vehicles; **200**, JSON array |
| `GET` | `/vehicles/{vehicleId}` | Retrieve a vehicle; **200**, vehicle JSON |
| `POST` | `/vehicles/{vehicleId}/user/{userId}` | Associate an available vehicle; **201**, empty body |
| `DELETE` | `/vehicles/{vehicleId}/user/{userId}` | Release the matching association; **200**, empty body; does not delete the vehicle |

Example create request body (`Content-Type: application/json`):

```json
{
  "brand": "Fictional Motors",
  "model": "Demo Compact",
  "licence_number": "DEMO-001"
}
```

Use the returned `id` for later calls. A subsequent GET—not the empty association/release response—shows the saved state.

Blank required fields yield **400**. Missing records and rejected association/release guards use **404**, including attempting to associate an already-associated vehicle or release with a different owner string. These are the implemented historical responses, not claims of ideal HTTP API design.

## What was actually verified

The retained isolated demonstration used the pinned source above and executable JAR SHA-256:

```text
b90a9f7f67f4f96614587f656bf3033a35e86dd0530bc99cf1d8b7c7b6a66330
```

- An offline Gradle 8.12.1 `bootJar` build with Temurin 23.0.2+7 succeeded using cached dependencies; no JUnit tests were run.
- The resulting JAR ran against a disposable local MySQL 8.0.37 database with fictional data.
- Six scoped checks passed: blank-licence rejection; create/read/list; association; rejection of repeat association; rejection of wrong-owner release; and matching-owner release.
- Recorded API responses matched database readbacks. The service and disposable database were stopped afterward.

The [public recording](https://www.cameron-basham.com/projects/vehicle-service-recording/requests.json) contains 14 HTTP requests from that scenario. Its recorded local port differs from the example port above. It is evidence from a prior run, not a live endpoint or a portable automated test suite. These results do not establish why a historical environment failed, validate AWS deployment, or certify production readiness.

## Security and limitations

- **No authentication or authenticated authorization is implemented in this module.** The supplied `userId` is trusted as text; matching it to `owner` is only an equality guard. Someone who knows an owner string can supply it. Keep this service private/local; the separate Cognito service does not automatically protect direct access to this API.
- No complete booking, pricing, payment, fleet-edit/delete, or user-management API is provided here.
- The association logic has no explicit locking/version field to establish safe competing updates. Concurrency behavior was not tested.
- The repository contains a context-load test, not automated coverage of the recorded lifecycle. Its tests were not exercised in the retained demonstration.
- Schema evolution, dependency support/security, authentication and deployment hardening need additional work before production use. The recorded functional checks are not a security audit.
