# 🧠 LearnAI — Adaptive Learning Platform

> An AI-powered personalized learning system that adapts to every learner — from DSA to Dance, Stock Market to Spring Boot.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)
![Python](https://img.shields.io/badge/Python-3.11-yellow?style=flat-square&logo=python)
![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)

---

## 📖 What is LearnAI?

LearnAI is a **next-generation adaptive learning backend** that goes far beyond simple Q&A. It builds a **Learning DNA profile** for every user and continuously adapts difficulty, explanation style, and content — just like a personal tutor.

**Learn anything:** DSA, Finance, Music, Dance, Spring Boot, Machine Learning — any topic.

---

## ✨ Core Features

| Feature | Description |
|---|---|
| 🧬 **Learning DNA** | Tracks accuracy, speed, hint usage, learning style per user |
| 🗺️ **Adaptive Roadmap** | AI-generated personalized learning path for any goal |
| 🤖 **AI Mentor (Aria)** | Persistent memory, personality, DNA-aware responses |
| 📝 **Quiz Engine** | 5-question adaptive quizzes with progressive hints |
| 💻 **Practice Engine** | Coding/Math/Written problem generation + AI evaluation |
| 🔄 **Spaced Repetition** | SM-2 algorithm + Ebbinghaus forgetting curve |
| 🌐 **Cross-lingual RAG** | Ask in Hindi, get answers in Hindi — RAG pipeline underneath |
| 📊 **Analytics Dashboard** | Heatmap, weak concepts, difficulty history, weekly trends |
| 🏆 **XP + Leaderboard** | Gamification with weekly/all-time Redis leaderboards |
| 📧 **Email Notifications** | Revision reminders, streak alerts, motivational emails |
| 🕷️ **Web Scraper** | Auto-scrapes trusted sources to build topic knowledge base |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Mobile / Web Client                   │
│              (Kotlin Jetpack Compose / React)            │
└──────────────────────────┬──────────────────────────────┘
                           │ REST API
┌──────────────────────────▼──────────────────────────────┐
│              Spring Boot Backend (Java 21)               │
│  Auth │ Quiz │ Mentor │ RAG │ Roadmap │ Analytics │ XP   │
└────┬──────────────────────────────────────┬─────────────┘
     │                                      │
┌────▼────────┐  ┌──────────────┐  ┌───────▼──────────────┐
│ PostgreSQL  │  │    Redis     │  │   Python Services     │
│ + pgvector  │  │ (Cache + LB) │  │  Embedding | Scraper  │
└─────────────┘  └──────────────┘  └──────────────────────┘
                           │
              ┌────────────▼────────────┐
              │    Groq API (LLM)       │
              │  llama-3.3-70b-versatile│
              └─────────────────────────┘
```

---

## 🛠️ Tech Stack

**Backend**
- Java 21 + Spring Boot 3.5
- Spring Security + JWT (access + refresh tokens)
- Spring Data JPA + PostgreSQL 16
- pgvector for semantic search
- Redis 7 (caching + leaderboards + rate limiting)
- Bucket4j (rate limiting)

**AI / ML**
- Groq API with key pool (round-robin load balancing)
- RAG pipeline (scrape → chunk → embed → retrieve → generate)
- sentence-transformers/all-MiniLM-L6-v2 (384-dim embeddings)
- SM-2 spaced repetition algorithm

**Python Microservices**
- FastAPI embedding service (sentence-transformers)
- FastAPI scraper service (Playwright + BeautifulSoup)

**Infrastructure**
- Docker + Docker Compose
- GitHub Actions CI/CD

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Docker + Docker Compose
- Python 3.11+ (for microservices)
- Groq API key (free at [console.groq.com](https://console.groq.com))

### 1. Clone the repository

```bash
git clone https://github.com/NimishNatani/learning-ai.git
cd learnai-backend
```

### 2. Set up environment variables

```bash
cp .env.example .env
# Edit .env with your actual values
nano .env
```

### 3. Start infrastructure (PostgreSQL + Redis)

```bash
cd backend
docker-compose up postgres redis -d
```

### 4. Start Python microservices

**Embedding Service:**
```bash
cd embeddings
pip install -r requirements.txt
python main.py
# Runs on http://localhost:8002
```

**Scraper Service:**
```bash
cd scraper-service
pip install -r requirements.txt
playwright install chromium
python main.py
# Runs on http://localhost:8001
```

### 5. Run the Spring Boot backend

```bash
cd backend
./gradlew bootRun
# Runs on http://localhost:8080
```

### 6. Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## 📁 Project Structure

```
learnai-backend/
├── backend/                          # Spring Boot application
│   ├── src/main/java/com/learningai/
│   │   ├── config/                   # Security, Redis, Rate limit, CORS
│   │   ├── controller/               # REST controllers (16 endpoints)
│   │   ├── dto/                      # Request/Response DTOs
│   │   ├── entity/                   # JPA entities
│   │   ├── repository/               # Spring Data repositories
│   │   ├── scheduler/                # Cron jobs
│   │   ├── service/                  # Business logic
│   │   │   └── scraper/              # RAG pipeline services
│   │   └── util/                     # Constants, InputSanitizer
│   ├── build.gradle
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── embeddings/                       # Python embedding microservice
│   ├── main.py                       # FastAPI + sentence-transformers
│   └── requirements.txt
│
├── scraper-service/                  # Python scraper microservice
│   ├── main.py                       # FastAPI + Playwright
│   └── requirements.txt
│
├── .env.example                      # Template for environment variables
├── .gitignore
└── README.md
```

---

## 🔌 API Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login + get JWT |
| POST | `/api/onboarding/quiz` | Get knowledge assessment quiz |
| POST | `/api/onboarding/complete` | Complete onboarding → create Learning DNA |
| GET | `/api/roadmap` | Get personalized roadmap |
| GET | `/api/roadmap/daily-plan` | Today's study plan |
| POST | `/api/learn/explain` | RAG-powered explanation |
| POST | `/api/quiz/start` | Start adaptive quiz session |
| POST | `/api/quiz/{id}/answer` | Submit quiz answer |
| POST | `/api/practice/generate` | Generate practice problem |
| POST | `/api/practice/evaluate` | AI-evaluate submission |
| POST | `/api/mentor/chat` | Chat with AI mentor Aria |
| GET | `/api/revision/due` | Get due revision cards |
| POST | `/api/revision/complete` | Submit SM-2 review |
| GET | `/api/analytics/overview` | Learning analytics dashboard |
| GET | `/api/leaderboard/weekly` | Weekly XP leaderboard |

Full API docs available at `/swagger-ui.html` when running locally.

---

## ⚙️ Configuration

All configuration is done via environment variables. Copy `.env.example` to `.env` and fill in your values.

**Required variables:**
- `DB_URL`, `DB_USER`, `DB_PASSWORD` — PostgreSQL connection
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` — Redis connection
- `JWT_SECRET` — minimum 64 characters
- `GROQ_API_KEY1` — at least one Groq key required
- `MAIL_USERNAME`, `MAIL_PASSWORD` — for email notifications

---

## 🧬 How Learning DNA Works

Every user has a `LearningProfile` that tracks:

- **Difficulty** (EASY/MEDIUM/HARD) — auto-adjusted based on rolling accuracy over last 10 questions
- **Learning Style** (VISUAL/READING/PRACTICE) — inferred from behavior (hint usage, time per question, coding vs MCQ ratio)
- **Concept scores** — per-concept EMA score (0.0–1.0) for weak/strong concept tracking
- **Spaced repetition state** — SM-2 parameters per concept

The engine adjusts difficulty only after 20+ total attempts with 10-question sliding window — preventing premature changes from a single lucky quiz.

---

## 🤝 Contributing

We welcome contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to branch: `git push origin feature/your-feature-name`
5. Open a Pull Request

---

## 🔒 Security

Found a vulnerability? Please read [SECURITY.md](SECURITY.md) and **do not** open a public issue.

---

## 📄 License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

## 🙏 Acknowledgements

- [Groq](https://groq.com) for blazing fast LLM inference
- [pgvector](https://github.com/pgvector/pgvector) for vector similarity search
- [sentence-transformers](https://www.sbert.net) for embeddings
- [Bucket4j](https://bucket4j.com) for rate limiting
- SM-2 algorithm by Piotr Woźniak

---

<p align="center">Built with ❤️ for learners everywhere</p>