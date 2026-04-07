# Online Bookstore & Review Hub – Project Development Documentation

## 1. Project Overview

The **Online Bookstore & Review Hub** is a Java Spring Boot application designed to provide a seamless platform for users to browse, borrow, purchase, and review books. It also includes a simple recommendation engine and administrative management features.

### Core Features

- User Registration & Account Management (User/Admin)
- Book Listing, Searching, and Filtering
- User Ratings and Reviews
- Recommendation Engine (based on ratings/reviews)
- Borrowing & Returning Books
- Waiting List & Notifications for Availability
- Admin Features (Add/Update/Delete Books)
- Home Page (list of books with ratings & categories

## 2. Project Setup

### Frameworks & Dependencies

- Ract(frontend)
- LibraryReservationReviewHub/build.gradle

## 3. Domain Model (Entities)

### Core Entities

- ##  **User**

  - `userId (int, PK)`
  - `email (String, unique, not null)` → e.g. `"alice@example.com"`
  - `password (String, not null)` → e.g. `"hashed_password"`
  - `firstName (String, not null)` → e.g. `"Alice"`
  - `lastName (String, not null)` → e.g. `"Smith"`
  - `phone (String, length=10)` → e.g. `"0412345678"`
  - `isAdmin (boolean, default=false)` → e.g. `false`

  ------

  ## 2. **Book**

  - `bookId (int, PK)`
  - `title (String, not null)` → e.g. `"The Hunger Games"`
  - `author (String, not null)` → e.g. `"Suzanne Collins"`
  - `publisher (String)` → e.g. `"Scholastic Press"`
  - `year (int)` → e.g. `2010`
  - `price (double)` → e.g. `7.85`
  - `rating (int)` → e.g. `4`
  - `genre (FK → Genre)` → e.g. `"Action and Adventure"`
  - `quantity (int)` → e.g. `10`
  - `available (boolean)` → e.g. `true`

  ------

  ## 3. **Genre**

  - `genreId (int, PK)`
  - `name (String, unique, not null)` → e.g. `"Science Fiction"`
  - `description (Text)` → e.g. `"Books exploring futuristic science and technology."`

  ------

  ## 4. **Review**

  - `reviewId (int, PK)`
  - `user (FK → User)`
  - `book (FK → Book)`
  - `rating (int, 1–5)` → e.g. `5`
  - `comment (Text)` → e.g. `"Amazing book, loved the characters!"`
  - `reviewDate (DateTime, not null)` → e.g. `"2025-09-11 14:30:00"`
  - `phone (String, length=10)` → e.g. `"0412345678"`

  ------

  ## 5. **Reservation**

  - `reservationId (int, PK)`
  - `user (FK → User)`
  - `book (FK → Book)`
  - `reservationDate (DateTime, not null)` → e.g. `"2025-09-11 10:00:00"`
  - `pickupDate (DateTime)` → e.g. `"2025-09-13 09:00:00"`
  - `dueDate (DateTime)` → e.g. `"2025-09-30 23:59:59"`
  - `returnDate (DateTime)` → e.g. `"2025-09-25 15:00:00"`
  - `status (enum: active, picked_up, returned, cancelled)` → e.g. `"active"`

  ------

  ## 6. **WaitingList**

  - `waitId (int, PK)`
  - `user (FK → User)`
  - `book (FK → Book)`
  - `joinDate (DateTime, not null)` → e.g. `"2025-09-01 08:00:00"`
  - `notificationDate (DateTime)` → e.g. `"2025-09-05 09:00:00"`
  - `isNotified (boolean, default=false)` → e.g. `false`
  - `status (enum: waiting, notified, cancelled, fulfilled, default=waiting)` → e.g. `"waiting"`