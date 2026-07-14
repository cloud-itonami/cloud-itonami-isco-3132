# Contributing to cloud-itonami-isco-3131

We welcome contributions! This document provides guidance on how to contribute.

## Before you start

- Read the [`README.md`](README.md) to understand the project's scope and design.
- Review the hard and escalation invariants in [`src/plant_ops/governor.cljc`](src/plant_ops/governor.cljc).
- Note: **This actor does NOT control generators, turbines, or grid synchronization** — if your proposal involves those, it's out of scope.

## Development

1. Clone the repository and create a feature branch.
2. Make changes (all source files are `.cljc`, targeting cljs/nbb/portable runtime).
3. Add tests in `test/plant_ops/` for any new functionality.
4. Run tests:
   ```bash
   clojure -M:test
   ```
5. Ensure all tests pass before opening a PR.

## Code style

- Follow the existing module structure (protocol definitions, implementations).
- Use descriptive function and variable names.
- Include docstrings for public functions.
- Keep modules focused (e.g., `advisor.cljc` for advice only, `governor.cljc` for policy).

## PR guidelines

- Keep PRs focused on a single concern (one feature, one bug fix).
- Include a clear description of why the change is needed.
- Reference any related issues.
- Ensure CI passes.

## Reporting bugs

Use the GitHub issues tracker. Include:
- Steps to reproduce.
- Expected behavior.
- Actual behavior.
- Environment (Clojure version, OS, etc.).

## Suggesting enhancements

Open an issue with the label `enhancement`. Describe the use case and proposed solution.
