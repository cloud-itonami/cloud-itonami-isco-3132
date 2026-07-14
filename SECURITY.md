# Security Policy

## Reporting Security Issues

**Do not** open a public GitHub issue to report security vulnerabilities.

Instead, please email the maintainer privately with:
- A description of the vulnerability.
- Steps to reproduce.
- The affected versions.
- Any proposed fix or mitigation.

We will acknowledge your report within 48 hours and work with you to resolve the issue.

## Security considerations for this project

This actor is designed to operate under human oversight:
- All proposals require either automatic approval (high confidence, allowed ops) or **explicit human approval** (escalated proposals).
- The Governor enforces hard invariants (registered plant, `:propose` effect only, no generator/turbine/grid-sync control).
- The audit ledger is append-only and provides traceability.
- Secrets (plant credentials, API keys) should be managed outside this system via standard secrets management (Kubernetes secrets, 1Password, etc.).

Deployers should:
- Run the system in a restricted network environment.
- Ensure human approvers are authenticated before granting approval.
- Monitor and audit the append-only ledger regularly.
- Use TLS for any external communication.
- Rotate credentials regularly.
