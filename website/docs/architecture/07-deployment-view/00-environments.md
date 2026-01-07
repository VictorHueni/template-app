---
title: Environments Overview
sidebar_position: 1
---

# Environments Overview

The system is designed to run in multiple environments. This section documents the deployment topology for each target environment.

## Available Environments

| Environment           | Description                                       | Status    | Link                                       |
| :-------------------- | :------------------------------------------------ | :-------- | :----------------------------------------- |
| **Local Development** | Docker Compose based setup for rapid development. | ✅ Active  | [View Deployment](./local-docker/overview) |
| **Staging**           | Cloud-based pre-production environment.           | 🚧 Pending | -                                          |
| **Production**        | Live environment serving end-users.               | 🚧 Pending | -                                          |

## Infrastructure Code

The single source of truth for the local infrastructure is the `docker-compose.yml` file located in the project root.

```bash
# To start the local environment
docker-compose up -d
```
