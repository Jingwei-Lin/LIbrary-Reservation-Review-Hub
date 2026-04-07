# ELEC5619-Practical06-Group-4

# Library Reservation & Review Hub

---


## How to Run the Application

### 1️⃣ Configure Database

Modify the database configuration in `application.yml`:

```
datasource:
  driver-class-name: com.mysql.cj.jdbc.Driver
  url: jdbc:mysql://localhost:3306/library_reservation
  username: your-username
  password: your-password
```

------

### 2️⃣ Set Up Your Local MySQL Database

Import or run the provided SQL schema in:

```
LibraryReservationReviewHub/Schema.sql
```

------

### 3️⃣ Run the Backend Server

Use Gradle command:

```
./gradlew bootRun
```

Or run inside **IntelliJ IDEA**:

- Open the project.
- Locate `LibraryReservationReviewHubApplication.java`.
- Right-click → **Run**.

------

### 4️⃣ Run the Frontend (React)

- Navigate to frontend directory (`/frontend`):

  ```
  cd frontend
  ```

- Install dependencies:

  ```
  npm install
  ```

- Start the frontend server:

  ```
  npm run dev
  ```

- Visit the app in your browser:
   👉 http://localhost:5173



## 1. Project Overview

**Real-world problem:**  
In many libraries, users face difficulties in **reserving books**, **tracking availability**, and **sharing reviews** with the community. Existing systems are often outdated, lack a modern interface, and fail to integrate social features.

**Our solution:**  
The **Library Reservation & Review Hub** is a web-based platform that allows library members to:
- Reserve books online and join waiting lists.
- Get real-time updates on book availability.
- Share and read community book reviews.

**High-level goals:**
- Improve user experience for library book reservations.
- Enable community engagement through reviews and ratings.
- Provide an efficient backend to handle waiting lists and availability checks.

---

## 2. Key Features
### User Management

Supports secure registration, login, logout, and profile management.

Admin users can update, query, and list all users with role-based authorization.

### Book Management

Enables CRUD operations for book data (add, delete, update, restore) and advanced search by keyword, genre, or availability.

Provides pagination, sorting, and genre filtering to support efficient retrieval.

### Reservation System

Allows users to reserve and cancel books, check active reservations, and update pickup time.

Admins can manage reservation status and perform system-wide pagination queries.

### Waiting List and Notification

Automatically manages book queues when all copies are borrowed.

Users can join, cancel, or filter their waiting records.

The system supports auto-notification when a book becomes available and updates inventory accordingly.

### Review and Rating System

Users can write, edit, delete, and view reviews for books.

Ratings are automatically maintained with MySQL triggers but can also be recalculated manually.

Admins can moderate reviews and list all user-generated content.

### AI Personal Agent

Integrated AI chat assistant using Server-Sent Events (SSE) for real-time streaming.

Provides personalized book recommendations, contextual responses based on login session, and natural language interaction.

---

## 3. Team Roles & Responsibilities

| Name         | Role                   | Responsibilities                                             |
| ------------ | ---------------------- | ------------------------------------------------------------ |
| Hanchen Wang | Project Manager        | Oversees project timelines, coordinates communication, and ensures goals are met. |
| Hao Chen     | Backend Developer      | Implements server-side logic, API development, and system integration. |
| Huang Dayu   | Frontend Developer     | Designs and builds responsive user interfaces, integrates frontend with backend APIs. |
| Jingwei Lin  | UI/UX Designer         | Creates intuitive user experiences, wireframes, and design prototypes. |
| Yonghao Lyu  | Database Administrator | Designs database schema, manages data integrity, and optimizes queries. |



## Backend Libraries and Versions

| Library                | Version  | Purpose                                  |
| ---------------------- | -------- | ---------------------------------------- |
| Spring Boot            | 3.5.5    | Core web backend framework               |
| MyBatis-Plus           | 3.5.14   | ORM framework                            |
| Knife4j                | 4.6.0    | REST API documentation                   |
| Lombok                 | Latest   | Code simplification (annotations)        |
| Hutool                 | 5.8.26   | Utility functions                        |
| MySQL Connector        | Latest   | Database connection                      |
| H2 Database            | Dev-only | Testing environment                      |
| JWT (io.jsonwebtoken)  | 0.11.5   | Token-based authentication               |
| Spring Boot Validation | Built-in | Input validation                         |
| Quartz Scheduler       | Built-in | Scheduled email and waiting list updates |
| Mail Starter           | Built-in | Email notifications                      |
| Mockito                | 5.14.2   | Unit testing & mocking                   |
| AssertJ                | 3.26.3   | Assertions in testing                    |
| Jacoco                 | 0.8.12   | Code coverage report                     |
| Alibaba Cloud AI       | 1.0.0.2  | AI recommendation API                    |
| Spring AI              | 1.0.0    | AI integration framework                 |
| Jsoup                  | 1.19.1   | HTML parser                              |
| Kryo                   | 5.6.2    | Object serialization                     |

## Frontend Libraries and Versions

| Library                           | Version  | Purpose                                                      |
| --------------------------------- | -------- | ------------------------------------------------------------ |
| React                             | 18.3.1   | Core UI library for building components                      |
| React Router DOM                  | 6.26.2   | Handles client-side routing and navigation                   |
| Axios                             | 1.7.3    | Sends HTTP requests to backend APIs                          |
| Vite                              | 5.3.1    | Fast development server and build tool                       |
| Node.js                           | 18+      | Runtime environment for JavaScript (required for running Vite) |
| NPM                               | 9+       | Package manager for installing frontend dependencies         |
| Bootstrap / Tailwind (if used)    | Optional | UI styling and layout framework                              |
| Session Storage API               | Built-in | Stores authenticated user session in browser                 |
| ESLint / Prettier (optional)      | Latest   | Code linting and formatting                                  |
| React Hooks (useState, useEffect) | Built-in | Manage component state and lifecycle                         |
| React Icons / Lucide React        | Latest   | Provides vector icons for UI                                 |

