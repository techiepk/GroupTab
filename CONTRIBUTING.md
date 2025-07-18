# Contributing to PennyWise AI

Thank you for your interest in contributing to PennyWise AI! We welcome contributions from the community.

## How to Contribute

### 🐛 Reporting Bugs

1. Check if the bug has already been reported in [Issues](https://github.com/sarim2000/pennywiseai-tracker/issues)
2. If not, create a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Device info (Android version, device model)
   - Screenshots if applicable

### 💡 Suggesting Features

1. Check existing [feature requests](https://github.com/sarim2000/pennywiseai-tracker/issues?q=is%3Aissue+label%3Aenhancement)
2. Open a new issue with the `enhancement` label
3. Describe the feature and its benefits
4. Include mockups/examples if possible

### 🔧 Code Contributions

#### Setup

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/pennywiseai-tracker.git
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/sarim2000/pennywiseai-tracker.git
   ```

#### Development Process

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes following our coding standards:
   - Follow Kotlin coding conventions
   - Use meaningful variable and function names
   - Add comments for complex logic
   - Write unit tests for new features

3. Test your changes:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

4. Check code style:
   ```bash
   ./gradlew ktlintCheck
   ./gradlew ktlintFormat  # Auto-fix issues
   ```

5. Commit with clear messages:
   ```bash
   git commit -m "feat: add transaction export feature"
   ```
   
   Use conventional commits:
   - `feat:` New feature
   - `fix:` Bug fix
   - `docs:` Documentation changes
   - `style:` Code style changes
   - `refactor:` Code refactoring
   - `test:` Test additions/changes
   - `chore:` Build process/auxiliary changes

#### Submitting Pull Requests

1. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Create a Pull Request with:
   - Clear title and description
   - Reference any related issues
   - Screenshots for UI changes
   - Test results

3. Wait for review and address feedback

## Code Style Guidelines

### Kotlin
- Use `camelCase` for functions and variables
- Use `PascalCase` for classes and interfaces
- Use `UPPER_SNAKE_CASE` for constants
- Prefer immutability (`val` over `var`)
- Use meaningful names over comments

### XML Layouts
- Use `snake_case` for IDs
- Extract strings to `strings.xml`
- Extract dimensions to `dimens.xml`
- Follow Material Design guidelines

### Architecture
- Follow MVVM pattern
- Keep Activities/Fragments lean
- Business logic in ViewModels
- Data operations in Repositories
- Use coroutines for async operations

## Testing

- Write unit tests for ViewModels and business logic
- Use mock objects for dependencies
- Test edge cases and error scenarios
- Aim for >70% code coverage

## Documentation

- Update README.md for new features
- Add KDoc comments for public APIs
- Include usage examples
- Update screenshots if UI changes

## Review Process

1. All PRs require at least one review
2. CI checks must pass
3. No merge conflicts
4. Code coverage shouldn't decrease
5. Follow up on review comments promptly

## Community

- Be respectful and constructive
- Help others in issues and discussions
- Share your use cases and feedback
- Spread the word about PennyWise AI!

## Questions?

Feel free to:
- Open an issue for clarification
- Start a discussion
- Contact maintainers

Thank you for contributing! 🙏