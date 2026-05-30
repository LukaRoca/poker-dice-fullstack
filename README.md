# Poker Dice Full-Stack Platform 🎲

A robust, real-time Full-Stack web application for the classic Poker Dice game. This platform features a scalable Spring Boot backend, a responsive React frontend, and real-time event communication via Server-Sent Events (SSE).

## Live Demo
The application is currently deployed and running on AWS (EC2). You can test it live here:
**[Play Poker Dice Now](http://51.21.196.156)**

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen.svg)
![React](https://img.shields.io/badge/React-18-blue.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-Enabled-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)

---

## Overview

This platform allows users to create and join lobbies, play match rounds, and experience real-time updates through asynchronous communication. Built as a comprehensive full-stack solution, it focuses on clean architecture, secure authentication, and seamless user experience.

## Architecture

* **Backend:** Kotlin + Spring Boot API, PostgreSQL for persistence, and JWT-based authentication.
* **Frontend:** React + TypeScript (SPA), leveraging Context API and Hooks for state management.
* **Real-time Communication:** Server-Sent Events (SSE) for instant match updates and lobby notifications.
* **Deployment:** Containerized via Docker & Docker Compose, running on AWS (EC2).

## Key Features

* **Lobby Management:** Real-time lobby creation, player joining/leaving, and matchmaking.
* **Game Engine:** State machine handling dice rolls, re-rolls, and round scoring logic.
* **Security:** JWT-based Bearer Authentication with secure endpoints.
* **Responsive UI:** Modern interface optimized for lobby management and match gameplay.
* **Cloud Optimized:** Fully automated deployment flow.

## Tech Stack

### Backend
* **Language:** Kotlin
* **Framework:** Spring Boot 3
* **Persistence:** PostgreSQL (Jdbi)
* **Auth:** Spring Security (JWT)

### Frontend
* **Library:** React (TypeScript)
* **Styling:** CSS3
* **State Management:** React Context API
* **Communication:** Fetch API & Server-Sent Events (SSE)

## Quick Start

### Prerequisites
* [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
* [Node.js](https://nodejs.org/) & [npm](https://www.npmjs.com/)

### 1. Backend
Navigate to the backend directory and start the services:
```bash
cd jvm
docker-compose up -d
```

### 2. Frontend
Navigate to the frontend directory, install dependencies, and start the app:
```bash
cd js
npm install
npm run dev
```
The application will be running at http://localhost:5173
=======
# DAW project

Please edit this document with an introduction to your project, including links to all the existing documentation.

In addition, please add a [`.mailmap`](https://git-scm.com/docs/gitmailmap) file, mapping the emails used in the commits to your ISEL email and student number
Example:

```
12345 <a12345@alunos.isel.pt> <mygithubuser@whatever.com>
```

