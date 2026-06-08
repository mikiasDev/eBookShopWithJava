# eBookShop

A JavaFX + MySQL book store application for browsing books, managing a cart, placing orders, and administering inventory.

## Features

- User login and registration
- Browse and search books
- Shopping cart and checkout flow
- Order history and receipt generation
- Admin dashboard for inventory and customer overview
- MySQL-backed persistence

## Requirements

- JDK 17
- MySQL 8+
- VS Code with the Java extension pack, or another IDE that can run JavaFX apps

## Google Sign-In Setup

The login and registration screens now include a Google sign-in option.

To enable it, create an OAuth client in Google Cloud and set the following before launching the app:

- `GOOGLE_OAUTH_CLIENT_ID`
- `GOOGLE_OAUTH_CLIENT_SECRET` (optional for some client setups)

Your OAuth client must allow a loopback redirect such as `http://127.0.0.1`.

## Database Setup

1. Create a MySQL database named `ebookshop`.
2. Run `schema.sql` to create the tables and seed the default data.
3. If you use a different MySQL username or password, update `src/main/java/com/mycompany/ebookshop/database/DBConnection.java`.

Default seeded admin account:

- Username: `admin`
- Password: `admin123`
# eBookShop

eBookShop is a lightweight JavaFX desktop storefront demonstrating a small online bookshop: browsing books, cart/checkout, user accounts, order history, and a small admin dashboard. It is intentionally compact and suitable as a learning project or starter template for JavaFX + MySQL desktop apps.

## Quick Overview

- UI: JavaFX (FXML + CSS)
- Persistence: MySQL (JDBC) for production; H2 available for tests
- Build: Maven

## What's included

- User registration and login (includes Google sign-in integration hooks)
- Shopping cart and checkout flow
- Order history and receipts
- Admin screens for inventory and user management
- Email sending via Jakarta Mail (GreenMail used for local dev/testing)

## Requirements

- JDK 17 installed
- Maven (or use the included build instructions)
- MySQL 8.x (recommended) or adjust `DBConnection` for other DBs

## Security fixes applied (2026-06-06)

During the most recent security audit I updated the following dependencies to patched versions to resolve public CVEs:

- `com.mysql:mysql-connector-j`: `8.0.33` → `8.2.0` (fixes CVE-2023-22102)
- `com.google.protobuf:protobuf-java`: added `3.25.5` (overrides transitive `3.21.9`, fixes CVE-2024-7254)

These changes are committed on branch `appmod/security-fix-20260606194219`.

## Setup

1. Clone the repository.
2. Create a MySQL database (example name `ebookshop`) and user with appropriate privileges.
3. Run the SQL schema to create tables and seed data:

```ps1
mysql -u youruser -p ebookshop < src/main/java/schema.sql
```

4. Configure database connection in `src/main/java/com/mycompany/ebookshop/database/DBConnection.java` or set JVM system properties:

- `db.url` (example: `jdbc:mysql://localhost:3306/ebookshop`)
- `db.user`
- `db.password`

You can also provide a local `oauth.properties` file for Google OAuth keys (see OAuth section below).

## Running

From the project root:

```ps1
# run using Maven
mvn javafx:run

# or compile and run the packaged JAR
mvn clean package
java -jar target/eBookShop-1.0-SNAPSHOT.jar
```

If you use an IDE (VS Code, IntelliJ, NetBeans), run `com.mycompany.ebookshop.MainApp` as the main class.

## Google OAuth (optional)

To enable Google Sign-In:

1. Create OAuth 2.0 credentials in Google Cloud Console (type: Desktop app).
2. Provide `GOOGLE_OAUTH_CLIENT_ID` and `GOOGLE_OAUTH_CLIENT_SECRET` via one of:
	- JVM system properties (`-Dgoogle.oauth.clientId=... -Dgoogle.oauth.clientSecret=...`)
	- Environment variables
	- A local `~/.ebookshop/oauth.properties` file (copy `oauth.properties.example` and fill in)

The app reads configuration in this priority order: system properties → env vars → local properties file.

## Development notes

- Tests use H2 in-memory database where possible. Run `mvn test` to execute unit tests.
- Local email sending for dev/testing uses GreenMail (no real emails sent).
- Sensitive values (DB passwords, OAuth secrets) should never be committed to the repo.

## Contributing

If you want me to push this repository to your GitHub account and open a PR from the `appmod/security-fix-20260606194219` branch, please confirm:

1. The target repository URL (e.g. `https://github.com/username/repo.git`).
2. That I should push the branch and create a pull request (I will ask before performing remote operations).

If you confirm, I will add the remote (if missing), push the branch, and open a PR with a descriptive title and the summary of changes.

## License

This project does not include an explicit license. If you want one, I can add an `MIT` or other license file.

---
Updated: 2026-06-06 — security fixes applied and build verified.
