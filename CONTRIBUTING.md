# Contributing to LearnAI

Thank you for your interest in contributing! 🎉

## Getting Started

1. Fork the repo and clone it locally
2. Follow the [Quick Start](README.md#-quick-start) guide to set up your dev environment
3. Pick an open issue or propose a new feature via a Discussion

## How to Contribute

### Reporting Bugs
- Check existing issues first
- Use the Bug Report issue template
- Include steps to reproduce, expected vs actual behavior, and your environment

### Suggesting Features
- Open a Discussion first for major features
- For small improvements, open an issue directly

### Pull Requests

**Branch naming:**
- `feature/short-description` — new features
- `fix/short-description` — bug fixes
- `docs/short-description` — documentation only
- `refactor/short-description` — code refactoring

**Commit message format (Conventional Commits):**
```
feat: add voice input support
fix: correct SM-2 interval calculation
docs: update API endpoint table
refactor: simplify LearningProfileService
test: add quiz service unit tests
```

**PR checklist:**
- [ ] Code compiles with `./gradlew build`
- [ ] No hardcoded secrets or API keys
- [ ] `.env.example` updated if new env vars added
- [ ] `application.yaml` uses `${ENV_VAR}` placeholders
- [ ] Existing endpoints are not broken

## Code Style

- Java: follow existing patterns (Lombok, Spring conventions)
- Python: PEP 8, type hints where practical
- No unused imports
- Log important operations with appropriate level (INFO/WARN/ERROR)

## Areas We Need Help With

- Unit and integration tests
- Frontend (Kotlin Jetpack Compose / React)
- New topic data seeding (Finance, Music, etc.)
- Translations / i18n
- Documentation improvements

## Questions?

Open a GitHub Discussion — we're happy to help.