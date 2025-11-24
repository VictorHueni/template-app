
## High Level Architecture 
```mermaid
flowchart TB
  %% Style / groups
  classDef title fill:#111,color:#fff,stroke:#444,stroke-width:1px;
  classDef aws fill:#0b1e2d,color:#d9e8ff,stroke:#1f3a5b,stroke-width:1px;
  classDef svc fill:#112a4c,color:#e7f1ff,stroke:#224f87,stroke-width:1px;
  classDef data fill:#1c3b24,color:#e5ffe9,stroke:#2e6b3c,stroke-width:1px;
  classDef edge fill:#262626,color:#f5f5f5,stroke:#444,stroke-width:1px;

  %% External
  user[User Browser]:::edge
  dns[Route 53 DNS<br/>app.example.com]:::edge
  gha[GitHub Actions<br/>OIDC]:::edge

  %% Certificates
  acm_cf[ACM Certificate<br/>us-east-1<br/>for CloudFront]:::edge
  acm_alb[ACM Certificate<br/>eu-central-2<br/>for ALB 443]:::edge

  %% ECR (image source)
  subgraph ecrgrp[ECR u-central-2]
    ecr[ECR Repositories]:::aws
  end

  %% Edge: WAF + CloudFront with two origins
  subgraph edgegrp[Edge Layer • Single Domain]
    waf[WAF Web ACL]:::edge
    cf[CloudFront CDN<br/>Behaviors]:::edge
    note1[/"Behaviors: - /* → S3 origin (SPA) AND /api/* → ALB origin (API) - Cache: disabled for /api"/]
    s3[S3 • React Static Site OAC]:::edge
  end

  %% VPC
  subgraph vpc[VPC eu-central-2 • Zurich]
    subgraph pub[Public Subnets]
      alb[Application Load Balancer<br/>HTTPS 443]:::svc
    end
    subgraph priv[Private Subnets]
      ecs[ECS Fargate Service<br/>ghost-template-api]:::svc
      logs[CloudWatch Logs]:::edge
      sm[Secrets Manager]:::edge
      rds[(RDS PostgreSQL<br/>ghostdb)]:::data
    end
  end

  %% DNS & edge routing
  user -->|HTTPS| dns --> waf --> cf
  cf -->|/*| s3
  cf -->|/api/*| alb
  alb -->|Targets| ecs

  %% Certificates wiring
  acm_cf --> cf
  acm_alb --> alb

  %% Runtime wiring
  ecs -->|JDBC 5432| rds
  ecs --> logs
  ecs --> sm

  %% Image supply chain
  gha -- OIDC assume --> iam[(IAM Roles)]:::edge
  gha -->|build & push| ecr
  ecs -. pull image .-> ecr
```


| Component                | Responsibility                                                    |
| ------------------------ | ----------------------------------------------------------------- |
| **GitHub Actions**       | Runs CI/CD, builds and deploys code.                              |
| **OIDC + IAM Roles**     | Securely authorize GitHub to assume AWS roles.                    |
| **Amazon ECR**           | Stores versioned Docker images.                                   |
| **Amazon ECS (Fargate)** | Runs backend containers with zero server management.              |
| **RDS PostgreSQL**       | Persistent managed database.                                      |
| **S3 + CloudFront**      | Host and distribute the React frontend globally.                  |
| **CloudWatch**           | Logs, metrics, and alarms for monitoring and rollback visibility. |


# Backend Clean Architecture

```mermaid
classDiagram
    class GreetingController {
      +GET /api/greetings
      +GET /api/greetings/id
      -GreetingService greetingService
    }

    class GreetingService {
      -GreetingRepository greetingRepository
      -Clock clock
      +GreetingDto greet(name)
      +GreetingDto getById(id)
    }

    class GreetingRepository {
      <<domain port>>
      +Greeting save(Greeting)
      +Optional~Greeting~ findById(Long)
    }

    class GreetingRepositoryAdapter {
      <<infrastructure adapter>>
      -GreetingRepositoryJpa jpa
      +Greeting save(Greeting)
      +Optional~Greeting~ findById(Long)
    }

    class GreetingRepositoryJpa {
      <<Spring Data JPA>>
      +save(GreetingEntity)
      +findById(Long)
    }

    class GreetingEntity {
      <<JPA entity>>
      Long id
      String name
      String message
      Instant createdAt
    }

    class GreetingConfiguration {
      <<@Configuration>>
      +Clock clock()
      +GreetingService greetingService(...)
      +GreetingRepository greetingRepository(...)
    }

    class Clock {
      <<java.time.Clock>>
    }

    class PostgreSQL {
      <<DB>>
    }

    GreetingController --> GreetingService
    GreetingService --> GreetingRepository
    GreetingRepositoryAdapter ..|> GreetingRepository
    GreetingRepositoryAdapter --> GreetingRepositoryJpa
    GreetingRepositoryJpa --> GreetingEntity
    GreetingRepositoryJpa --> PostgreSQL
    GreetingService --> Clock
    GreetingConfiguration --> GreetingService
    GreetingConfiguration --> GreetingRepositoryAdapter
    GreetingConfiguration --> Clock
```

```mermaid
sequenceDiagram
    participant C as Client (Browser/Frontend)
    participant GC as GreetingController
    participant GS as GreetingService
    participant GR as GreetingRepository (port)
    participant GRA as GreetingRepositoryAdapter
    participant JPA as GreetingRepositoryJpa
    participant DB as PostgreSQL DB

    C->>GC: HTTP GET /api/greetings?name=Alice
    GC->>GS: greet("Alice")
    GS->>GS: build domain Greeting (uses Clock)
    GS->>GR: save(greeting)
    GR->>GRA: (interface call)
    GRA->>JPA: save(GreetingEntity)
    JPA->>DB: INSERT INTO greetings (...)
    DB-->>JPA: persisted row
    JPA-->>GRA: GreetingEntity
    GRA-->>GS: Greeting (domain)
    GS-->>GC: GreetingDto
    GC-->>C: 200 OK + JSON body
```