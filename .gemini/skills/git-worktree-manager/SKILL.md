---
name: git-worktree-manager
description: Manages the lifecycle of Git worktrees for parallel development tasks. Handles creation, safe removal, and branch management to keep the main working directory clean.
---

# Git Worktree Manager Skill

This skill enables the agent to safely create, manage, and destroy Git worktrees. This allows for parallel development (e.g., fixing a bug while in the middle of a feature) without disrupting the main working directory.

## 1. Directory Strategy
To prevent path resolution issues and nesting conflicts, all worktrees MUST be created as **siblings** to the current project root.

*   **Current Root**: `../project-name`
*   **Worktree Root**: `../project-name-<feature>`

**Example:**
If working in `P:/dev/my-app`, a worktree for a "auth-fix" should be at `P:/dev/my-app-auth-fix`.

## 2. Worktree Protocols

### <PROTOCOL:WORKTREE_CREATE>
**Trigger**: When the user needs to switch contexts (e.g., "fix this bug" while changes are unstaged) or explicitly requests a clean environment.

1.  **Analyze Request**: Determine the target branch name and the purpose.
2.  **Determine Path**: Construct the sibling path `../<current_folder_name>-<short-description>`.
3.  **Execute Creation**:
    *   *New Branch*: `git worktree add ../<path> -b <branch-name>`
    *   *Existing Branch*: `git worktree add ../<path> <branch-name>`
4.  **Confirm**: Report the location of the new worktree and remind the user that dependencies (like `node_modules`) may need to be re-installed there.

### <PROTOCOL:WORKTREE_REMOVE>
**Trigger**: When the task in the worktree is complete and changes are committed.

1.  **Verify State**: Ensure changes in the worktree are committed or stashable.
2.  **Execute Removal**:
    *   Attempt standard removal: `git worktree remove <path>`
    *   *Handle Untracked Files*: If the command fails because of untracked files (e.g., `node_modules`, `target/`), and the work is definitely saved/committed, use force: `git worktree remove --force <path>`.
3.  **Prune**: Run `git worktree prune` to clean up references.

## 3. Operational Guidelines

*   **Dependency Awareness**: A new worktree is a fresh checkout. It does **not** share `node_modules`, `target`, or `venv` folders. You MUST check for their existence and run install commands (`npm install`, `./mvnw compile`) before running tests in the new worktree.
*   **Path Context**: When operating in a worktree, always use absolute paths or relative paths carefully calculated from the worktree root.
*   **Shell Context**: When running shell commands, explicitly `cd` into the worktree directory: `cd ../<worktree-path>; <command>`.
