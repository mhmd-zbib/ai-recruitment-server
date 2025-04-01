# Git Hooks for HireSync

This directory contains Git hooks to enforce code quality standards and ensure a consistent workflow
for the HireSync project.

## Available Hooks

| Hook | Description |
| --- | --- |
| `pre-commit` | Runs before commit to auto-format code and verify code quality |
| `commit-msg` | Validates commit messages follow the Conventional Commits format |
| `prepare-commit-msg` | Provides a template for conventional commit messages |
| `pre-push` | Verifies code quality and runs tests before pushing to remote |
| `post-checkout` | Provides helpful information after switching branches |
| `pre-rebase` | Prevents risky rebases on shared branches |

## Installation

### Automatic Installation

Run the following command to install hooks with all automation features:

```bash
# For Unix/Linux/macOS
bash .git-hooks/auto-install.sh

# For Windows
.\.git-hooks\auto-install.bat
```

This will:
- Install hooks immediately
- Configure Git to auto-update hooks when you pull changes
- Set up auto-installation for new clones
- Ensure hooks stay updated across the team

### Using the Run Script

You can also install hooks via the run script:

```bash
# For Unix/Linux/macOS
./run.sh githooks

# For Windows
./run.sh githooks --windows
```

## Line Ending Management

Different operating systems use different line endings, which can cause issues in a cross-platform team.
To ensure consistent line endings:

1. The repository includes a `.gitattributes` file that defines appropriate line endings for each file type
2. Use the provided line ending correction script if you encounter issues:

```bash
# For Unix/Linux/macOS
bash .git-hooks/correct-line-endings.sh

# For Windows
.\.git-hooks\correct-line-endings.bat
```

This script will:
- Apply the line ending rules defined in `.gitattributes`
- Make all shell scripts executable
- Show you which files had their line endings corrected

## Conventional Commits

This project follows the [Conventional Commits](https://www.conventionalcommits.org/) specification
for commit messages. Each commit message should have the following format:

```
<type>(<scope>): <subject>
```

For example:
- `feat(auth): add user authentication flow`
- `fix(api): resolve null pointer in user controller`
- `docs: update README with setup instructions`

### Commit Types

| Type | Description |
| --- | --- |
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Changes that do not affect the meaning of the code |
| `refactor` | A code change that neither fixes a bug nor adds a feature |
| `perf` | A code change that improves performance |
| `test` | Adding missing tests or correcting existing tests |
| `build` | Changes that affect the build system or external dependencies |
| `ci` | Changes to CI configuration files and scripts |
| `chore` | Other changes that don't modify src or test files |
| `revert` | Reverts a previous commit |

## Overriding Hooks

In some cases, you may need to bypass the hooks for a specific operation:

```bash
# Skip pre-commit hook
git commit --no-verify -m "your message"

# Skip pre-push hook
git push --no-verify
```

**Note:** Skipping hooks should be done only in exceptional circumstances.

### Pre-push Quick Mode

For faster pre-push checks:

```bash
# Enable quick mode for pre-push
git config hooks.pre-push.quick true

# Disable quick mode
git config hooks.pre-push.quick false
```

### Allowing Protected Branch Rebases

```bash
# Enable protected branch rebasing
git config hooks.allowProtectedRebase true

# Disable after rebasing (recommended)
git config hooks.allowProtectedRebase false
```

## Automatic Updates

Once you've run the auto-install script, hooks will automatically:

1. Update whenever you pull changes that modify the .git-hooks directory
2. Install automatically when someone clones the repository
3. Stay in sync across the team

This ensures everyone is using the same version of the hooks without manual intervention.

## Java Code Quality Tools

This project uses several Maven plugins to enforce code quality standards:

### Code Style and Formatting

- **Spotless with Google Java Format**: Automatically formats code according to Google Java Style guidelines
  - Configured in `pom.xml`
  - Run with: `mvn spotless:apply`
  - Checked during pre-commit

- **Checkstyle**: Enforces code style rules beyond formatting
  - Custom rules in: `config/checkstyle/checkstyle.xml`
  - Suppressions in: `config/checkstyle/suppressions.xml`
  - Run with: `mvn checkstyle:check`
  - Checked during pre-commit

### Static Analysis

- **PMD**: Detects common coding issues and bad practices
  - Run with: `mvn pmd:check`
  - Checked during pre-push

- **SpotBugs**: Finds potential bugs and security vulnerabilities
  - Exclusions in: `config/spotbugs-exclude.xml`
  - Run with: `mvn spotbugs:check`
  - Checked during pre-push

- **Maven Enforcer**: Ensures project dependency and environment rules
  - Configured in `pom.xml`
  - Enforces Maven version, Java version, and dependency convergence
  - Run with: `mvn enforcer:enforce`

### Security

- **OWASP Dependency Check**: Scans dependencies for known vulnerabilities
  - Run with: `mvn dependency-check:check`
  - Can be enabled for pre-push with: `git config hooks.pre-push.security true`

### Test Quality

- **JaCoCo**: Measures test coverage
  - Run with: `mvn jacoco:report`
  - Check coverage minimums with: `mvn jacoco:check`
  - Can be enabled for pre-push with: `git config hooks.pre-push.coverage true`

### Configuration

You can customize how these tools are used in the git hooks:

```bash
# Enable quick mode for faster pre-push checks
git config hooks.pre-push.quick true

# Skip static analysis in pre-push
git config hooks.pre-push.skipAnalysis true

# Enable security scans in pre-push (disabled by default)
git config hooks.pre-push.security true

# Enable coverage checks in pre-push (disabled by default)
git config hooks.pre-push.coverage true
```

## Troubleshooting

If you encounter issues with the hooks:

1. Make sure hooks are executable: `chmod +x .git/hooks/*`
2. Verify Git is configured to use the hooks: `git config core.hooksPath`
3. On Windows, ensure you have Git Bash installed
4. Run the line ending correction script: `bash .git-hooks/correct-line-endings.sh`
5. Try reinstalling the hooks with the auto-install script: `bash .git-hooks/auto-install.sh`

## Contributing

When adding or modifying hooks, please:

1. Follow shell scripting best practices
2. Include proper error handling
3. Add helpful messages for developers
4. Update this README with any new functionality

For any questions or issues, please contact the HireSync development team. 