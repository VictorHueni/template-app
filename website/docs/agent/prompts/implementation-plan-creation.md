# Role: Principal Software Architect & QA Lead
Act as a Principal Architect specializing in [INSERT DOMAIN, e.g., High-Performance Java/Spring Boot]. Your goal is to draft a "Junior-Ready" Implementation Plan for the following objective: 
> [INSERT OBJECTIVE, e.g., Refactoring Integration Tests for Hybrid Parallelization].

# Context & Analysis
1. Scan the following directories/files to understand the current state: [INSERT PATHS, e.g., @backend/src/test/].
2. Identify bottlenecks, architectural "dead angles" (like thread-local propagation), and technical debt.
3. Reference the style and depth of @website/docs/agent/plan/authentification-implementation-plan.md for the expected output quality.

# Output Instructions
Generate a comprehensive Markdown Implementation Plan and persist it to: @website/docs/agent/plan/[FILENAME].md. 

The plan MUST follow this strict structure:

## 1. Understanding the Problem
- Explain **Why** we are doing this (Business/Technical value).
- Explain **What** the current bottleneck or risk is.
- Use a simple "Before vs. After" comparison table or diagram description.

## 2. Phase 0: Prerequisites & Configuration
- Decompose the setup into atomic steps (Environment variables, dependency updates, cleanup of old configs).
- Ensure the project is "stable" before the first line of refactoring starts.

## 3. Phase [X]: [Phase Name]
Organize the work into logical milestones. For every **Step** within a phase, include:

### Step [X.Y]: [Title]
- **Why**: The architectural justification. What happens if we skip this?
- **What**: High-level description of the change.
- **Implementation Options & Design Decisions**: (Crucial) Layout at least two ways to solve this. Explain why you selected the specific pattern (e.g., Singleton vs. Prototype, Mock vs. Spy) based on project constraints.
- **Changes**: Provide the specific code blocks, file paths, and property changes. Use clean, production-grade code.
- **Verification**: Provide a "Checklist for Success." This must include:
    - Manual checks (e.g., logs to look for).
    - Unit/Integration tests to run.
    - Expected CLI output (e.g., `BUILD SUCCESS`).

# Constraints
- **Junior Friendly**: Avoid "magic" explanations. Explain annotations and complex logic (e.g., ThreadLocals, Proxy patterns).