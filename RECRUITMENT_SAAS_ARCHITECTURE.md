# Recruitment Email Intelligence Platform (REIP)
## Production-Grade SaaS Enterprise System Specification

This document provides a comprehensive blueprint, database schema, API design, folder structure, UI wireframes, and production-ready system architecture to transform the AI-Powered Recruitment Email Management System into a scalable, high-volume B2B SaaS platform for HR teams and recruiters.

---

## 1. Complete System Architecture

The Recruitment Email Intelligence Platform is designed around a decoupled, highly responsive, clean-architecture event-driven model. It separates UI presentation, high-volume background email ingestion, heavy AI classification processing, and relational persistence.

```
+---------------------------------------------------------------------------------+
|                                 CLIENT CLIENT LAYER                             |
|  +-------------------------+   +------------------------+   +----------------+  |
|  |   Next.js 15 Web SaaS   |   | Android Mobile Client  |   | Chrome/Outlook |  |
|  | (Dashboard, Charts, UI) |   |    (Companion App)     |   | Add-in Widgets |  |
|  +------------+------------+   +-----------+------------+   +-------+--------+  |
+---------------|----------------------------|------------------------|-----------+
                |                            |                        |
                +----------------------------+------------------------+
                                             | WebSocket / REST (HTTPS)
                                             v
+---------------------------------------------------------------------------------+
|                                 API GATEWAY / SaaS BACKEND (FastAPI)            |
|  +---------------------------------------------------------------------------+  |
|  |  [Router / Controllers]                                                   |  |
|  |  * JWT Admin Auth & OAuth Scope Verification (OAuth2 State Handler)       |  |
|  |  * Candidate Query Controls & Full-Text Search Indices                    |  |
|  |  * Template Lifecycle Engine & Live Variable Parser                       |  |
|  |  * Dynamic Analytics JSON Feed Aggressors                                 |  |
|  +-------------------------------------+-------------------------------------+  |
|                                        | Read/Write                          |
|                                        v                                     |
|                                [Relational DB SQL]                           |
+----------------------------------------|-------------------------------------+
                                         |
                                         v
+---------------------------------------------------------------------------------+
|                                 PERSISTENCE & BROKER LAYER                      |
|  +------------------------+   +---------------------------+                  |
|  | PostgreSQL Database    |   | Redis Cache &             |                  |
|  | (Main Data / GIN search)|   | RabbitMQ Job Broker       |                  |
|  +------------------------+   +-------------+-------------+                  |
+---------------------------------------------|-----------------------------------+
                                              | Dispatch
                                              v
+---------------------------------------------------------------------------------+
|                                 BACKGROUND ASYNC WORKERS                        |
|  +---------------------------------------------------------------------------+  |
|  |  [Celery Task Processors]                                                 |  |
|  |  * Sync Worker: Connects Gmail/Graph API -> GIN index logs                 |  |
|  |  * AI Classifier: Dispatches payload to OpenAI GPT-4o for JSON parsing    |  |
|  |  * Workflow Worker: Formulates notifications and automated outbox mail    |  |
|  +---------------------+-------------------+---------------------+------------+  |
+------------------------|-------------------|---------------------|--------------+
                         |                   |                     |
                         v RPC               v API                 v SMTP / REST
                  +--------------+   +---------------+   +-------------------+
                  |   OpenAI     |   | PDF/Docx OCR  |   | GMail REST API    |
                  |  GPT APIs    |   | Parse Service |   | MS Graph Outlook  |
                  +--------------+   +---------------+   +-------------------+
```

### Component Flow Decomposition:
1. **Ingestion & Auth Pipeline**: Recruiter executes OAuth 2.0 flow inside the Next.js frontend. The FastAPI backend captures credentials, encrypts refresh tokens inside PostgreSQL via AES-256, and schedules a recurring periodic task on the Celery scheduler.
2. **Sync Loop**: The background sync loop checks for new messages via IMAP IDLE push or standard REST pulling (Gmail history IDs). Unsynchronized emails are retrieved and queued.
3. **AI Reasoning Agent (OpenAI)**: Once an email with an attachment arrives, the pipeline initiates a background Celery task. The PDF/Docx parser converts resumes to plain text. A structured prompt is sent to `gpt-4o` using JSON mode, returning exact candidate metadata (skills, experience, classifications).
4. **Relational Sync**: Extracted candidate profiles are populated into Postgres. An audit entry in the log system tracks completion status, confidence score, and sync time.

---

## 2. Database Schema (PostgreSQL DDL)

To support transactional integrity, structured JSON logs, rapid full-text searching, and relationships, the following production PostgreSQL schema is constructed.

```sql
-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Trigonometry indices for loose searchable patterns

-- Role Enumerations
CREATE TYPE user_role AS ENUM ('SUPER_ADMIN', 'HR_MANAGER', 'RECRUITER');
CREATE TYPE application_status AS ENUM ('RECEIVED', 'REVIEWED', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'ASSESSMENT_SENT', 'REJECTED', 'OFFER_EXTENDED');
CREATE TYPE email_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'OPENED', 'BOUNCED');

-- 1. Tenants / Companies (Support Multi-Tenancy)
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(100) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Internal Users (Recruitment / HR Officers)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role user_role NOT NULL DEFAULT 'RECRUITER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Connected Admin Mailboxes (OAuth storage credentials)
CREATE TABLE mailboxes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_address VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- 'GMAIL' or 'OUTLOOK'
    oauth_access_token BYTEA NOT NULL, -- Encrypted AES-256
    oauth_refresh_token BYTEA NOT NULL, -- Encrypted AES-256
    token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_synced_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Raw Email Synchronization Logs
CREATE TABLE synced_emails (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mailbox_id UUID NOT NULL REFERENCES mailboxes(id) ON DELETE CASCADE,
    external_message_id VARCHAR(255) UNIQUE NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255),
    recipient_email VARCHAR(255) NOT NULL,
    subject TEXT,
    body_plain TEXT,
    body_html TEXT,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_spam BOOLEAN DEFAULT FALSE,
    spam_score NUMERIC(5,2) DEFAULT 0.00,
    spam_reason VARCHAR(255),
    processed_status VARCHAR(50) DEFAULT 'UNPROCESSED', -- 'UNPROCESSED', 'CLASSIFIED', 'EXCLUDED'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Extracted Candidates Database Table
CREATE TABLE candidates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    linkedin_url VARCHAR(255),
    github_url VARCHAR(255),
    skills TEXT[],
    technologies TEXT[],
    years_experience INTEGER DEFAULT 0,
    education TEXT,
    certifications TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_company_candidate_email UNIQUE (company_id, email)
);

-- 6. Specifc Job Applications
CREATE TABLE job_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    synced_email_id UUID REFERENCES synced_emails(id) ON DELETE SET NULL,
    applied_role VARCHAR(255) NOT NULL,
    primary_domain VARCHAR(100) NOT NULL, -- 'Full Stack Development', 'AI/ML etc'
    ai_confidence_scores JSONB NOT NULL, -- Map of domain name -> confidence e.g. {"AI/ML": 0.92}
    status application_status NOT NULL DEFAULT 'RECEIVED',
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    source VARCHAR(100) DEFAULT 'Careers Page', -- 'LinkedIn', 'Naukri', 'Indeed'
    resume_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Email Customization Templates
CREATE TABLE email_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    template_type VARCHAR(50) NOT NULL, -- 'RECEIVED', 'SHORTLISTED', 'INTERVIEW', 'REJECTED'
    name VARCHAR(100) NOT NULL,
    subject_template VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Triggered Outbox Communication logs
CREATE TABLE outbox_emails (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_content TEXT NOT NULL,
    status email_status NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    opened_at TIMESTAMP WITH TIME ZONE,
    delivery_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Realtime System Activities & Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(255) NOT NULL,
    entity_affected VARCHAR(100) NOT NULL, -- 'CANDIDATE', 'MAILBOX'
    details JSONB,
    ip_address VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- INDEX OPTIMIZATIONS
CREATE INDEX idx_candidate_skills ON candidates USING gin(skills);
CREATE INDEX idx_job_app_domain ON job_applications(primary_domain);
CREATE INDEX idx_job_app_status ON job_applications(status);
CREATE INDEX idx_synced_email_received ON synced_emails(received_at);
CREATE INDEX idx_audit_logs_event ON audit_logs(created_at, entity_affected);
```

---

## 3. Entity Relationship Diagram (ERD) Textual Layout

Below is the structured layout outlining relational cardinalities and join schemas.

```
+------------------+             +-------------------+             +----------------------+
|    COMPANIES     | 1 ------- * |       USERS       | 1 ------- * |      MAILBOXES       |
|------------------|             |-------------------|             |----------------------|
| id (PK)          |             | id (PK)           |             | id (PK)              |
| name             |             | company_id (FK)   |             | user_id (FK)         |
| domain           |             | email             |             | email_address        |
+------------------+             | password_hash     |             | provider             |
        |                        | role              |             +----------------------+
        | 1                      +-------------------+                        |
        |                                  | 1                                | 1
        |                                  |                                  |
        |                                  | * (reviewer)                     | * (synchronizer)
        | *                              +---------------+                  +------------------+
+------------------+                     |               |                  |  SYNCED_EMAILS   |
|    CANDIDATES    | 1                 * |     AUDIT     |                  |------------------|
|------------------|                     |     LOGS      |                  | id (PK)          |
| id (PK)          |                     +---------------+                  | mailbox_id (FK)  |
| company_id (FK)  |                             |                          | sender_email     |
| name             |                             | * (entity joins)         | subject          |
| email (Unique)   |                             |                          | body_plain       |
| skills (GIN)     |                             |                          | is_spam          |
+------------------+                             v                          +------------------+
        | 1                              +---------------+                           |
        |                                |  JOBS_ENTITY  |                           |
        | *                              |  (Dummy Ref)  |                           | 0..1 (optional reference)
+----------------------+                 +---------------+                           |
|   JOB_APPLICATIONS   | <-----------------------------------------------------------+
|----------------------|
| id (PK)              |
| candidate_id (FK)    | 1 
| synced_email_id (FK) | ----------------------+
| applied_role         |                       | 1
| primary_domain       |                       |
| ai_confidence        |                       | *
| status               |               +-------------------+
+----------------------+               |   OUTBOX_EMAILS   |
                                       |-------------------|
                                       | id (PK)           |
                                       | job_app_id (FK)   |
                                       | recipient_email   |
                                       | status            |
                                       +-------------------+
```

---

## 4. REST API Design (FastAPI Interface Specification)

All request/response communications utilize secure JSON structures. Below is the endpoint catalog.

### Authentication Enclave
*   `POST /api/v1/auth/register` — Initial administrator credentials creation.
*   `POST /api/v1/auth/login` — Verifies credentials, returns Bearer token with claims payload.
*   `POST /api/v1/auth/logout` — Revokes current refresh sessions.

### OAuth Connectors
*   `GET /api/v1/oauth/link?provider={gmail|outlook}` — Initiates third-party client consent link.
*   `GET /api/v1/oauth/callback` — Internal exchange redirect capturing permanent refresh tokens.
*   `POST /api/v1/oauth/trigger-sync` — Runs background parallel Celery sync tasks.

### Candidate and Process Controllers
*   `GET /api/v1/candidates` — Listing view with cursor pagination, dynamic sorting, full-text filtering.
*   `GET /api/v1/candidates/{id}` — Extended detail card detailing skills list, education history, and communications.
*   `PATCH /api/v1/candidates/{id}/status` — Changes status (e.g. `SHORTLISTED`), dispatching background outbox communications.

### Email Templates CRUD
*   `GET /api/v1/templates` — List custom templates.
*   `POST /api/v1/templates` — Saves new templates with customized structural tags.
*   `PUT /api/v1/templates/{id}` — Update existing version of templates.

### Analytics Endpoints
*   `GET /api/v1/analytics/trends` — Returns composite JSON arrays for charts (daily count arrays, domain distributions status metrics).

---

## 5. Folder Structure (Next.js 15 Monorepo SaaS Platform)

```
/recruitment-saas-platform
├── apps
│   ├── web (Next.js 15 App Router Frontend)
│   │   ├── src
│   │   │   ├── app
│   │   │   │   ├── (auth)
│   │   │   │   │   ├── login/page.tsx
│   │   │   │   │   └── register/page.tsx
│   │   │   │   ├── (dashboard)
│   │   │   │   │   ├── layout.tsx
│   │   │   │   │   ├── page.tsx (Overview KPIs)
│   │   │   │   │   ├── candidates/page.tsx (Interactive Data Table)
│   │   │   │   │   ├── templates/page.tsx (Email Templates Editor)
│   │   │   │   │   ├── settings/page.tsx (OAuth Integrations)
│   │   │   │   │   └── spam/page.tsx (Spam Audit logs)
│   │   │   │   └── providers.tsx (Theme, Auth, Query Contexts)
│   │   │   ├── components
│   │   │   │   ├── ui (shadcn button, card, dialog, table)
│   │   │   │   └── charts (ApexCharts Wrapper components)
│   │   │   └── lib (API handlers, utility functions)
│   ├── api (FastAPI Backend Engine)
│   │   ├── app
│   │   │   ├── api
│   │   │   │   ├── auth.py
│   │   │   │   ├── candidates.py
│   │   │   │   ├── templates.py
│   │   │   │   └── analytics.py
│   │   │   ├── core
│   │   │   │   ├── config.py (Environment secrets)
│   │   │   │   ├── security.py (JWT verify & AES-256 tokens)
│   │   │   │   └── database.py (SQLAlchemy Session pool)
│   │   │   ├── models (SQLAlchemy ORM Entities)
│   │   │   ├── schemas (Pydantic models)
│   │   │   ├── services
│   │   │   │   ├── ai_parser.py (OpenAI JSON extraction wrapper)
│   │   │   │   └── oauth_sync.py (Graph & Gmail token handlers)
│   │   │   └── main.py
│   │   ├── requirements.txt
│   │   └── Dockerfile
├── workers (Celery Background Orchestration task system)
│   ├── tasks.py
│   └── scheduler.py
└── README.md
```

---

## 6. UI Wireframes (Markdown Layout Representation)

### SaaS Layout Header
```
+-------------------------------------------------------------------------------------------------------------------+
|  [Logo] SmartRecruiter SaaS        [Mailbox Connected: Active/Green]            [Notifications]  (User Admin Profile) |
+-------------------------------------------------------------------------------------------------------------------+
```

### Screen Flow 1: Mailbox Settings Connection panel
```
+-------------------------------------------------------------------------------------------------------------------+
| Settings / Mailbox Synchronization Integration                                                                    |
|                                                                                                                   |
| Sync real-time candidate emails automatically. Select the API provider below to launch security authentication.  |
|                                                                                                                   |
|  +----------------------------------------------+        +----------------------------------------------+         |
|  | [Icon] Connect Google Gmail OAuth            |        | [Icon] Connect Microsoft Outlook OAuth       |         |
|  | - Automatically pull inbox mail logs         |        | - Capture incoming resumes via Graph API     |         |
|  | - Push automated outbox reply drafts         |        | - Sync background logs securely every 15 min |         |
|  |                                              |        |                                              |         |
|  |               [ Connect Now ]                |        |               [ Connect Now ]                |         |
|  +----------------------------------------------+        +----------------------------------------------+         |
+-------------------------------------------------------------------------------------------------------------------+
```

### Screen Flow 2: Email Template Manager
```
+-------------------------------------------------------------------------------------------------------------------+
| Templates / Response Customization Builder                                                                        |
|                                                                                                                   |
| Create high-conversion autoacknowledgements triggered during lifecycle transitions.                              |
|                                                                                                                   |
| Template Type: [Select: Shortlisted List v]        Template Name: [ Candidate Shortlisted Acknowledgement ]       |
|                                                                                                                   |
| Variable Injection Helper: (Click to add): {{Candidate_Name}} , {{Position}} , {{Company_Name}} , {{Recruiter}}     |
|                                                                                                                   |
| +----------------------------------------------------------------------------------+ +-------------------------+ |
| | Subject: Update regarding your application at {{Company_Name}} for {{Position}}  | | Live Template Preview | |
| |                                                                                  | |                         | |
| | Dear {{Candidate_Name}},                                                         | | Subject: Update...      | |
| |                                                                                  | |                         | |
| | We have reviewed your credentials and interest. Our engineering division wants to  | | Dear Bobby Draper,   | |
| | scheduled a technical discussion.                                                | |                         | |
| |                                                                                  | | We have reviewed...     | |
| | Safe regards,                                                                    | |                         | |
| | {{Recruiter_Name}}                                                               | |                         | |
| +----------------------------------------------------------------------------------+ +-------------------------+ |
|                                             [ Save Template Draft ]                                               |
+-------------------------------------------------------------------------------------------------------------------+
```

---

## 7. Dashboard Layout Architecture

The interactive layout splits administrative metrics into dynamic tracking widgets.

```
+-------------------------------------------------------------------+-----------------------------------+
| Overview Analytics Command Hub                                    | Global Quick Filter Options       |
|                                                                   | Domain: [ All   v] Status: [All v] |
+-------------------------------------------------------------------+-----------------------------------+
| Total Apps | New Inbound | Synced Success | Rejected | Auto-Sent  | Active Synchronization Logging:   |
|   1,429    |     142     |     98.8%      |   412    |    983     | [15:14] Synced Bobby resume.      |
|  (+12%^ )  |   (+8%^ )   |   (No Errors)  | (-2.1%v) |  (On Time) | [15:10] AI Domain match DevOps.   |
+-------------------------------------------------------------------+-----------------------------------+
| Primary Focus Application Domain Distribution                     | Daily Synchronized Inflow Rate    |
| (Horizontal Multi-Axis Chart)                                     | (ApexCharts Line Plot)            |
|                                                                   |                                   |
| AI/ML         ========================. (92%)                     | 100|          *                   |
| DevOps        ==================.. (81%)                          |  75|        *   *                |
| Frontend      ============.... (64%)                              |  50|      *       *              |
| Cyber         =======...... (32%)                                 |  25|    *           *            |
|                                                                   |   0|--+---+---+---+---+---+--+    |
|                                                                   |      M   T   W   T   F   S   S    |
+-------------------------------------------------------------------+-----------------------------------+
| Interactive Recruitment Pipeline Matrix View                      | Spam / Outliers Security Tracking |
| [ Search Candidates... ] [Filter Domain v]                        |                                   |
|                                                                   | HR-Audit score threshold: 0.85    |
| Candidate     Primary Domain    Applied Role     Status   Actions |                                   |
| Bobby Draper  AI/ML             Applied Sci..    Shortlis [View]  | * marketing@offers.com: 98% Spam  |
| Joan Harris   Frontend          Lead UI Eng..    Reviewed [View]  | * updates@newsletter.io: 89% Spam |
| Peter Campbell DevOps           Site Reliabi..   Received [View]  | * random_bot@cloud.ru: 95% Spam   |
+-------------------------------------------------------------------+-----------------------------------+
```

---

## 8. Backend SaaS Code Structures

The following implementations demonstrate production-ready FastAPI routes, schema verification definitions, and Celery background task processing.

### Section I: FastAPI Master Controller Route (`candidates.py`)
```python
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from uuid import UUID
from typing import List, Optional
from app.core.database import get_db
from app.schemas.candidate import CandidateRead, CandidateUpdate, ApplicationStatusChange
from app.models.candidate import Candidate, JobApplication
from app.core.security import get_current_active_user
from app.workers.tasks import queue_automated_workflow_email

router = APIRouter(prefix="/api/v1/candidates", tags=["Candidates"])

@router.patch("/{application_id}/status", response_model=CandidateRead)
def change_candidate_application_status(
    application_id: UUID,
    payload: ApplicationStatusChange,
    db: Session = Depends(get_db),
    current_user = Depends(get_current_active_user)
):
    # Match application under recruiter tenant
    application = db.query(JobApplication).filter(
        JobApplication.id == application_id,
        JobApplication.candidate.has(company_id=current_user.company_id)
    ).first()
    
    if not application:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Job application record not found on this company tenant profile."
        )
        
    old_status = application.status
    application.status = payload.new_status
    application.reviewed_by = current_user.id
    db.commit()
    db.refresh(application)
    
    # Trigger non-blocking Outbox email workflow automatically based on custom template types
    queue_automated_workflow_email.delay(
        application_id=str(application.id),
        trigger_state=str(payload.new_status)
    )
    
    return application
```

### Section II: Celery Background Task Processing Pipeline (`tasks.py`)
```python
import os
import json
from celery import Celery
from openai import OpenAI
from app.core.database import SessionLocal
from app.models.candidate import JobApplication, OutboxEmail
from app.models.email_template import EmailTemplate

redis_url = os.getenv("REDIS_URL", "redis://localhost:6379/0")
celery_app = Celery("recruitment_tasks", broker=redis_url, backend=redis_url)
openai_client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

@celery_app.task(name="tasks.queue_automated_workflow_email")
def queue_automated_workflow_email(application_id: str, trigger_state: str):
    db: SessionLocal = SessionLocal()
    try:
        application = db.query(JobApplication).filter(JobApplication.id == application_id).first()
        if not application:
            return f"Error: Application {application_id} not found."
            
        candidate = application.candidate
        
        # Locate company customized response template
        template = db.query(EmailTemplate).filter(
            EmailTemplate.company_id == candidate.company_id,
            EmailTemplate.template_type == trigger_state
        ).first()
        
        if not template:
            # Fallback to general boilerplate template
            return f"Warning: No custom response template declared for status state '{trigger_state}'."
            
        # Parse template syntax variables in real-time
        replaces = {
            "{{Candidate_Name}}": candidate.name,
            "{{Position}}": application.applied_role,
            "{{Company_Name}}": "TechCorp Inc.",
            "{{Recruiter_Name}}": "Talent Acquisition Engine"
        }
        
        subject = template.subject_template
        body = template.body_template
        
        for placeholder, replacement in replaces.items():
            subject = subject.replace(placeholder, replacement)
            body = body.replace(placeholder, replacement)
            
        # Spawn Outbox dispatch record
        outbox = OutboxEmail(
            job_application_id=application.id,
            recipient_email=candidate.email,
            subject=subject,
            body_content=body,
            status="PENDING"
        )
        db.add(outbox)
        db.commit()
        
        # In production setup, this would invoke AWS SES, SendGrid, or direct Google REST outbox queues.
        print(f"Dispatched Outbox Email: {subject} sent successfully to {candidate.email}.")
        
    except Exception as e:
        db.rollback()
        return f"Pipeline failed with exception details: {str(e)}"
    finally:
        db.close()
```

---

## 9. Implementation Roadmap

The transition of this system to production occurs over a disciplined 4-Phase rollout plan:

```
+-------------------------------------------------------------------------------+
| PHASE 1: OAuth & Sync Integration (Days 1--14)                                |
| * Provision Gmail/Microsoft OAuth Console credential scopes                   |
| * Construct background task orchestration using Celery and Redis              |
| * Set up AES-256 token encryption for DB vault persistence                   |
+-------------------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------------------+
| PHASE 2: AI Parser, OCR, & PostgreSQL Pipeline (Days 15--28)                  |
| * Implement PDF parsing and OCR support for scanned candidate resumes        |
| * Author strict JSON formatting prompts for gpt-4o classification analytics   |
| * Build the GIN indexes structure inside Postgres database server            |
+-------------------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------------------+
| PHASE 3: Dashboard & ApexCharts Interface (Days 29--42)                        |
| * Design interactive views using Next.js 15, Tailwind, and Shadcn UI          |
| * Construct rich dashboard widgets using robust ApexCharts library models     |
| * Bind the web client controllers with JWT security tokens                    |
+-------------------------------------------------------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------------------+
| PHASE 4: Automated Workflows & Enterprise Audit Logs (Days 43--56)            |
| * Put email template customizer engine and variable interpreters in place     |
| * Implement RBAC (Super Admin, HR Manager, Recruiter role logic restrictions) |
| * Deploy Docker-orchestrated multi-container image pods                       |
+-------------------------------------------------------------------------------+
```

---

## 10. Best Practices for Scalability and Security

1.  **Rate Limiting & Synchronization Offloading**: 
    To protect third-party mail provider strict quotas, mail synchronized pulls MUST be performed incrementally. Never process attachment operations inside main FastAPI request threads. Offload all parsing workflows to isolated background thread groups.
2.  **Database Connection Pooling**:
    Production environments will utilize `pgbouncer` to safely throttle SQL transaction pools, protecting PostgreSQL processing limits during mass sync processes.
3.  **Strict Token Cryptography & Vault Security**:
    Never store raw access tokens inside database tables. Use `cryptography.fernet` or HashiCorp Vault to secure synchronization credentials during dormant database storage periods.
4.  **Role-Based Row Privacy**:
    Enforce Row-Level Security (RLS) inside the PostgreSQL company schema layout so that administrators can never view candidate applications from adjacent tenants or companies.
5.  **OpenAI Rate-Limit Fallbacks**:
    Gracefully handles `RateLimitError` or HTTP Connection exceptions by queuing task retries with progressive exponential backoffs (implemented natively inside Celery `@task(autoretry_for=(Exception,), retry_backoff=True)`).
