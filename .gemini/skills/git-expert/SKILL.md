---
name: git-expert
description: Expert Git workflow manager enforcing Conventional Commits. Handles staging, drafting semantic messages, and pushing changes with strict adherence to scope and formatting rules.
---

# Git Expert Skill

This skill allows the agent to manage Git operations with professional rigor, ensuring the project history is semantic, clean, and automated-release ready.

## 1. Scope Detection Logic
Analyze the `git status` output to determine the primary scope. If multiple areas are touched, try to split commits or use `root` if it's a global change.

| Directory path                               | Scope Tag  |
| :------------------------------------------- | :--------- |
| `gateway/**`                                 | `gateway`  |
| `api/specification/**`                       | `api-spec` |
| `backend/**`                                 | `backend`  |
| `frontend/**`                                | `frontend` |
| `website/**`                                 | `docs`     |
| `.github/**`                                 | `ci`       |
| `docker-compose.yml`, `Dockerfile`, `k8s/**` | `ops`      |
| `pom.xml`, `package.json` (root)             | `build`    |

## 2. Commit Message Template (The Law)

You MUST structure every commit message as follows:

```text
<type>(<scope>): <subject>

<body>

<footer>
```

### A. Header (Max 50 chars)
*   **Type**:
    *   `feat`: New feature (Minor version)
    *   `fix`: Bug fix (Patch version)
    *   `docs`: Documentation only
    *   `style`: Formatting, missing semi-colons, etc.
    *   `refactor`: Code change that neither fixes a bug nor adds a feature
    *   `perf`: Code change that improves performance
    *   `test`: Adding missing tests or correcting existing tests
    *   `build`: Changes that affect the build system or external dependencies
    *   `ci`: Changes to our CI configuration files and scripts
    *   `chore`: Other changes that don't modify src or test files
    *   `revert`: Reverts a previous commit
*   **Scope**: See Section 1.
*   **Subject**: Imperative mood ("Add" not "Added"). No period. Lowercase first letter.

### B. Body (The "Why" and "How")
*   **Required for**: `feat`, `fix`, `refactor`, `perf`.
*   **Format**: Wrap at 72 characters.
*   **Content**:
    *   Explain the *motivation* for the change.
    *   Contrast with previous behavior.
    *   "The previous sorting algorithm caused O(n^2) latency... This change switches to MergeSort..."

### C. Footer (References & Breaking)
*   **Ticket Linking**: `Closes #123`, `Relates-to #456`.
*   **Breaking Changes**:
    *   MUST start with `BREAKING CHANGE:`.
    *   Followed by a summary of the breaking change.
    *   Followed by a migration instruction.

## 3. The Commit Workflow (<PROTOCOL:COMMIT>)

When asked to "commit" or "save changes", you must follow this sequence:

1.  **Status Check**: Run `git status` to see what is changed.
2.  **Diff Analysis**: Run `git diff` (or `git diff --staged`) to understand the *content* of the change.
3.  **Drafting**: Construct the conventional commit message based on the analysis.
    *   *Self-Correction*: If the body or footer information (like ticket numbers) is missing, ASK the user before finalizing the message.
4.  **Execution**:
    *   `git add <files>`
    *   `git commit -m "header" -m "body" -m "footer"`
5.  **Verification**: Run `git log -1` to show the result.
