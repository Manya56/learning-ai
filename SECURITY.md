# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| latest (main branch) | ✅ |

## Reporting a Vulnerability

**Please do NOT open a public GitHub issue for security vulnerabilities.**

If you discover a security issue — especially anything related to:
- JWT token handling or authentication bypass
- SQL injection or database exposure
- API key exposure
- Rate limit bypass
- User data leakage

Please report it privately by emailing: **nimishnatani26@gmail.com**

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (optional)

We will acknowledge your report within 48 hours and aim to release a fix within 7 days for critical issues.

## Security Features in This Project

- JWT access + refresh tokens (refresh tokens stored as SHA-256 hashes)
- BCrypt password hashing (strength 12)
- Input sanitization against prompt injection
- Rate limiting per user per endpoint (Bucket4j)
- CORS configured for known origins only
- Stateless session (no server-side session state)
- Environment variables for all secrets (never hardcoded)