# Business Model

## Positioning

Power plant operators run one of the most complex and risk-critical industrial workflows. Operational tracking — production readings, maintenance scheduling, anomalous-condition flagging, shift handovers — is routinely done in spreadsheets, paper logs, or proprietary closed SaaS platforms that lock in the operator and expose the utility to vendor risk.

This open-source blueprint decouples operations coordination from vendor lock-in by providing a reference implementation of an actor-based, governance-gated robotics system that a plant operator can deploy, modify, and own.

## Value proposition

**For a plant operator:**
- Keep your own operational history and maintenance records (not rented from a SaaS vendor).
- Audit-logged, tamper-evident operational decisions.
- Routine logging and maintenance scheduling can be automated without loss of human oversight (escalation to operator for anything anomalous).
- Integrated with your own identity and credential systems (no vendor API keys required).
- Forkable and modifiable to your specific plant's design and regulatory requirements.

**For a technology partner:**
- Deploy this as a reference implementation for your customers.
- Customize the Advisor (swap mock-advisor for your domain-specific LLM).
- Customize the Governor rules to reflect your plant's safety standards and regulatory constraints.
- Operate it on your own cloud or on-premises infrastructure.

## Economic model

This is **open-source software, AGPL-3.0-or-later**. There is no per-seat licensing or per-operation fee. A plant operator or technology partner can:
1. Deploy this reference implementation (code provided).
2. Hire or retain engineers to operate it.
3. Customize the Advisor or Governor as needed for the plant's regulatory environment.
4. Participate in the community (issues, PRs, documentation improvements).

## Regulatory & Safety considerations

**This actor is NOT a generator/turbine/grid-control system.** It does not:
- Start or stop generators.
- Control turbine loads.
- Manage grid synchronization.
- Dispatch power to the grid.

It supports **back-office operations coordination only** — logging production readings, scheduling maintenance, flagging anomalous conditions to human operators for review. All generator/turbine/grid operations remain under licensed operator control.

This scoping is intentional and enforced at the Governor level. It keeps the system's attack surface small and makes it easy to certify that the actor cannot inadvertently cause a grid incident.

## Deployment architecture

### Minimal single-plant deployment
```
┌─────────────────────┐
│   Plant Operator    │
│   (human approval)  │
└──────────┬──────────┘
           │ (approval)
        ┌──▼──────────────────┐
        │   PlantOpsActor     │
        │  (langgraph graph)  │
        └──┬───────────────────┘
           │
    ┌──────┴────────┬──────────────┐
    │               │              │
┌───▼────┐  ┌──────▼─────┐  ┌─────▼──┐
│ Advisor │  │  Governor  │  │ Store  │
│ (mock)  │  │  (policy)  │  │ (file) │
└─────────┘  └────────────┘  └────────┘
    │               │              │
    └───────────────┴──────────────┘
           (in-process)
```

### Scaled multi-plant deployment
```
┌─────────────────────────────────────────────┐
│   Operator Console (web, shared frontend)   │
│   (authentication, human approval UI)       │
└──────────┬──────────────────────────────────┘
           │
    ┌──────┴────────┬────────────┐
    │               │            │
┌───▼────────┐  ┌──▼────────┐ ┌─┴────────┐
│  Plant A   │  │  Plant B  │ │ Plant C  │
│  actor pod │  │ actor pod │ │actor pod │
└──────┬─────┘  └─────┬─────┘ └──┬──────┘
       │              │          │
   ┌───┴──────────────┴──────────┘
   │
┌──▼──────────────────────┐
│  Shared audit ledger    │
│  (durable backing)      │
└─────────────────────────┘
```

### Integration points
- **Telemetry ingestion**: SCADA systems, PI Historian, local IoT sensors → Plant Operator's infrastructure.
- **Advisor**: Can be swapped out for an LLM (langchain) or domain-specific model.
- **Identity**: Operator approvals linked to authenticated user identities (OAuth, OIDC, LDAP).
- **Audit ledger**: Can be durable (Postgres, DynamoDB) or ephemeral (for testing).
- **Downstream actions**: Approved maintenance scheduling → CMMS (Computerized Maintenance Management System), or email, or Slack notifications.

## Maturity & Roadmap

**Phase 1 (current):** Reference implementation with `mock-advisor`, in-memory store, and manual testing.

**Phase 2 (future):** LLM advisor integration (langchain/claude), durable audit ledger (Postgres), web-based operator console for approval UI.

**Phase 3 (future, if adopted):** Multi-plant deployment, SCADA integration templates, CMMS integrations, regulatory compliance packs (NERC CIP, IEEE, etc.).
