# FreePark Cloud Simple (Smart Parking Lot Cloud Platform - Lite Edition)

## Introduction

`FreePark Cloud Simple` is a fully functional smart parking lot cloud platform management system. The project adopts a separated front-end and back-end architecture, where the back-end is built based on the **Spring Boot** framework, and the front-end is built based on the **Vue 3 + TypeScript + Vite** framework.

The system covers core functions such as parking lot management, vehicle entry and exit control (whitelist/blacklist), special date billing rule configuration, system parameter settings, and administrator account management. It is suitable for scenarios such as residential communities and commercial parking lots.

## Tech Stack

### Backend
*   **Core Framework:** Spring Boot 3.x
*   **Development Language:** Java 17+
*   **Data Persistence Layer:** Spring Data JPA (Hibernate)
*   **Security Authentication:** JWT (JSON Web Token) + Spring Security
*   **Internationalization:** Spring i18n (Supports English and Chinese)
*   **Modularity:** Maven Multi-module Architecture

### Frontend
*   **Core Framework:** Vue 3
*   **Development Language:** TypeScript
*   **Build Tool:** Vite
*   **UI Library:** Element Plus (Mainstream UI library based on Vue 3)

## Project Structure

The project is organized in a Monorepo style, containing the back-end service and two front-end applications:

```text
freepark-cloud-simple/
├── freepark-cloud-simple-backend/              # Back-end core code
│   ├── freepark-cloud-simple-startup/          # Startup entry module
│   ├── freepark-cloud-simple-common/           # Common base module (Exception handling, Auth interception, Utilities)
│   ├── freepark-cloud-simple-user/             # User & Permission module (Admin login, Account management, JWT)
│   ├── freepark-cloud-simple-parking/          # Parking business module (Spaces, Lanes, Vehicles, Session management)
│   ├── freepark-cloud-simple-billing/          # Billing module (Special date rules)
│   └── freepark-cloud-simple-settings/         # System configuration module (Timezone, Language, License plate colors)
│
├── freepark-cloud-simple-frontend-mnt/         # Administration backend frontend (Management)
└── freepark-cloud-simple-frontend-user/        # User client/Visitor portal frontend (User Portal)
```

## Core Feature Highlights

### 1. Parking Lot Basic Management
*   **Parking Lot Information:** Supports configuration of parking lot name, code, address, total number of spaces, and map data.
*   **Zones and Spaces:** Supports dividing spaces by zones, supports bulk importing space information via Excel.
*   **Lanes and Booths:** Flexibly configure lane types (Entry, Exit, Bi-directional) and associate booths.

### 2. Intelligent Access Control
*   **Whitelist / Blacklist:** Manage whitelist (VIP / Free) and blacklist (Prohibited Entry) vehicles, supports setting effective time periods.
*   **Internal Vehicles:** Manage internal vehicles (Employee vehicles), supports batch entry and deletion.
*   **Pattern Lists:** Supports configuring special license plate access rules via Regular Expressions (Pattern).
*   **Access Decision:** Provides standard API interfaces to judge whether a vehicle is blocked based on multi-dimensional rules.

### 3. Parking Record Management
*   **Parking Sessions:** Records detailed information such as vehicle entry/exit time, lane, and images.
*   **Status Tracking:** Real-time tracking of parking status (In Progress, Completed, Cancelled).

### 4. Billing and System Settings
*   **Special Dates:** Configure billing rules for holidays or special time periods.
*   **System Configuration:** Centrally manage global parameters such as system language, timezone, and allowed license plate colors.

## Quick Start

### Environment Dependencies
*   JDK 17 or higher
*   Node.js 18 or higher
*   Maven 3.8 or higher
*   MySQL 8.0 or higher

### 1. Backend Deployment

1.  **Create Database:** Create a database in MySQL (e.g., `freepark_cloud`).
2.  **Configure DataSource:** Edit `freepark-cloud-simple-startup/src/main/resources/application.yml`, configure database username, password, and connection address.
3.  **Build Project:**
    ```bash
    cd freepark-cloud-simple-backend
    mvn clean install -DskipTests
    ```
4.  **Run Service:**
    ```bash
    java -jar freepark-cloud-simple-startup/target/freepark-cloud-simple-startup-*.jar
    ```

### 2. Frontend Deployment (Administration Portal)

1.  **Enter Directory:**
    ```bash
    cd freepark-cloud-simple-frontend-mnt
    ```
2.  **Install Dependencies:**
    ```bash
    npm install
    ```
3.  **Start Development Server:**
    ```bash
    npm run dev
    ```
4.  **Build Production Package:**
    ```bash
    npm run build
    ```

## Default Accounts

Upon system startup, it will attempt to initialize a default super administrator account:

*   **Username:** `admin`
*   **Password:** `admin123`

> **Note:** Please change the default password immediately after the first login. The initialization function can be disabled via the configuration item (`freepark.user.init.enabled`) in `application.yml`.

## License

This project follows an open-source agreement. For specific agreement information, please refer to the LICENSE file in the project root directory (if available).