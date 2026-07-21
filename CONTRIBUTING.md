# 🤝 Contributing to HealthX Android

First off, thank you for considering contributing to **HealthX Android**.

HealthX is a modern healthcare platform focused on delivering a secure, reliable, AI-powered, and offline-first experience. Every contribution—whether it's fixing bugs, improving performance, enhancing the UI, or adding new features—helps make the project better for everyone.

---

# 📚 Table of Contents

- [Code of Conduct](#-code-of-conduct)
- [Development Philosophy](#-development-philosophy)
- [Getting Started](#-getting-started)
- [Development Workflow](#-development-workflow)
- [Project Structure](#-project-structure)
- [Coding Guidelines](#-coding-guidelines)
- [Commit Message Guidelines](#-commit-message-guidelines)
- [Pull Request Guidelines](#-pull-request-guidelines)
- [Issue Guidelines](#-issue-guidelines)
- [Reporting Bugs](#-reporting-bugs)
- [Feature Requests](#-feature-requests)
- [Security Policy](#-security-policy)

---

# ❤️ Code of Conduct

Please be respectful and professional.

We expect all contributors to:

- Be respectful toward everyone.
- Welcome constructive feedback.
- Keep discussions professional.
- Focus on improving the project.
- Help create an inclusive community.

Harassment, discrimination, or abusive behavior will not be tolerated.

---

# 🏗 Development Philosophy

HealthX follows a few core principles:

- Offline First
- Privacy by Default
- Security First
- Modular Architecture
- Clean Code
- Reusable Components
- Modern Android Development
- Consistent User Experience

When contributing, try to follow these principles.

---

# 🚀 Getting Started

## 1. Fork the Repository

Fork the repository to your own GitHub account.

---

## 2. Clone Your Fork

```bash
git clone https://github.com/<your-username>/HealthX.git
```

---

## 3. Open in Android Studio

Open the project using the latest stable version of Android Studio.

Allow Gradle to sync completely before making changes.

---

## 4. Configure Required Services

Before running the project:

- Add `google-services.json`
- Configure the backend API URL
- Add your Razorpay test key if required

---

## 5. Create a New Branch

Always create a feature branch.

Example:

```bash
git checkout -b feature/improve-reminders
```

or

```bash
git checkout -b fix/login-crash
```

Avoid committing directly to the `main` branch.

---

# 🔄 Development Workflow

1. Create a feature branch.
2. Make your changes.
3. Test the application.
4. Ensure the project builds successfully.
5. Commit your changes.
6. Push your branch.
7. Open a Pull Request.

---

# 📁 Project Structure

```text
HealthX/

├── app/
├── ui/
├── viewmodels/
├── repositories/
├── network/
├── database/
├── models/
├── notifications/
├── workers/
├── alarms/
├── utils/
└── res/
```

Try to keep new code inside the appropriate module.

---

# 🧹 Coding Guidelines

## Kotlin

- Use Kotlin for all new code.
- Prefer immutable objects (`val`) whenever possible.
- Use meaningful variable names.
- Avoid unnecessary comments.
- Write self-documenting code.

---

## Architecture

Follow the existing architecture.

- MVVM
- Repository Pattern
- Separation of Concerns
- Single Responsibility Principle

Avoid introducing tightly coupled code.

---

## UI

For UI contributions:

- Use Jetpack Compose.
- Follow Material Design 3.
- Keep spacing consistent.
- Support both light and dark themes.
- Avoid hardcoded colors and dimensions.
- Reuse existing components whenever possible.

---

## Performance

Please avoid:

- Blocking the main thread.
- Unnecessary recompositions.
- Large memory allocations.
- Duplicate network requests.

Prefer asynchronous operations using Kotlin Coroutines.

---

## Security

Never commit:

- API keys
- JWT tokens
- Firebase credentials
- Passwords
- Personal data
- Private medical information

Sensitive configuration should remain outside the repository.

---

# 📝 Commit Message Guidelines

Use clear, descriptive commit messages.

Examples:

```text
feat: add hydration reminder support

fix: resolve notification scheduling issue

refactor: simplify nutrition repository

docs: update installation guide

ui: redesign subscription screen

perf: optimize dashboard rendering
```

Avoid messages like:

```text
update

changes

fixed stuff

misc
```

---

# 🔀 Pull Request Guidelines

Before opening a Pull Request:

- Ensure the project builds successfully.
- Test your changes.
- Keep changes focused on a single feature or fix.
- Update documentation if necessary.
- Remove unused code.
- Resolve merge conflicts.

Your Pull Request should include:

- Summary
- Motivation
- Screenshots (for UI changes)
- Testing performed
- Related issue (if applicable)

---

# 🐞 Issue Guidelines

Before opening a new issue:

- Search existing issues first.
- Verify the issue still exists.
- Provide enough information to reproduce it.

Include:

- Android version
- Device model
- Steps to reproduce
- Expected behavior
- Actual behavior
- Screenshots (if applicable)
- Logs (if available)

---

# 🐛 Reporting Bugs

A good bug report should include:

- Description
- Reproduction steps
- Device information
- Android version
- Application version
- Screenshots
- Crash logs
- Additional context

The more information provided, the easier it is to investigate.

---

# 💡 Feature Requests

Feature requests are welcome.

Please include:

- Problem statement
- Proposed solution
- Benefits
- Possible alternatives
- Additional context or mockups

Large feature requests may require discussion before implementation.

---

# 📖 Documentation

Documentation improvements are always appreciated.

Examples include:

- README improvements
- API documentation
- Architecture documentation
- Code comments
- Screenshots
- Installation guides
- Tutorials

---

# 🧪 Testing

Before submitting a Pull Request, ensure that:

- The project builds successfully.
- Existing functionality still works.
- New functionality has been tested.
- No obvious regressions are introduced.

---

# 🔒 Security Policy

If you discover a security vulnerability, please **do not create a public GitHub issue**.

Instead, contact the project maintainer privately with:

- Vulnerability description
- Steps to reproduce
- Potential impact
- Suggested mitigation (if available)

We appreciate responsible disclosure and will investigate all valid reports.

---

# 🙌 Thank You

Thank you for taking the time to contribute to HealthX Android.

Every contribution—whether it's code, documentation, testing, design improvements, bug reports, or feature suggestions—helps make HealthX a better healthcare platform for everyone.

Happy Coding! 🚀