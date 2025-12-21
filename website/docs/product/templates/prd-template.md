---
id: prd-feature-name
title: '[FEATURE-ID] Feature Name'
sidebar_label: Feature Name
status: Draft
owner: '@product-owner'
---

# Product Requirement Document: [Feature Name]

| Metadata           | Details                                              |
| :----------------- | :--------------------------------------------------- |
| **Epic Ticket**    | [LINK-123](https://jira.example.com/browse/LINK-123) |
| **Status**         | 🚧 Draft / ✅ Approved                                 |
| **Target Release** | v1.x.x                                               |
| **Reviewers**      | @tech-lead, @security-lead                           |

## 1. Problem Alignment
### 1.1 The "Why"
*Describe the user pain point or business opportunity. Why is this important now?*

### 1.2 Success Metrics (KPIs)
*How will we know this feature is successful?*
* [ ] Reduce API latency by 100ms
* [ ] Increase user conversion by 5%

## 2. User Experience
### 2.1 User Personas
* **Admin:** Wants to configure...
* **End User:** Wants to view...

### 2.2 User Stories
| ID         | As a... | I want to...      | So that...              | Priority |
| :--------- | :------ | :---------------- | :---------------------- | :------- |
| **USR-01** | User    | login with Google | I don't need a password | P0       |
| **USR-02** | Admin   | revoke access     | I can secure the app    | P1       |

## 3. Functional Requirements
*Detailed behavior of the system. This feeds into the API Spec.*

### 3.1 API Requirements
* **GET /resource:** Must support filtering by date.
* **Error Handling:** Must return 409 if resource exists.

### 3.2 UI/Frontend Requirements
* Dashboard must auto-refresh every 30s.
* [Link to Figma/Wireframes]

## 4. Non-Functional Requirements (NFRs)
* **Security:** Data must be encrypted at rest.
* **Performance:** API response < 200ms at 95th percentile.
* **Scale:** Support 10k concurrent users.

## 5. Traceability & Out of Scope
* **Out of Scope:** We will NOT support mobile for v1.
* **Dependencies:** Requires `Service-X` v2.0 update.
