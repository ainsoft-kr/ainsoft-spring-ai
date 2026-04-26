# Codex Profiles

Use these profiles with the repository MCP rules in `AGENTS.md`.

This repository uses `.codex` as `CODEX_HOME`. If your shell does not already set it, run commands as:

```sh
CODEX_HOME="$PWD/.codex" codex --profile low-token
```

## low-token

Default profile for routine edits, code reading, and small fixes.

```sh
codex --profile low-token
```

## api-smoke

Use for quick API checks, curl-based verification, and simple status questions.

```sh
codex --profile api-smoke
```

## deep-code

Use for broad refactors, dependency upgrades, migration work, or debugging that needs deeper reasoning.

```sh
codex --profile deep-code
```

## Token Discipline

- Let `AGENTS.md` decide MCP order.
- Prefer Symdex, OpenAPI MCP, and Context7 before opening large files or searching the web.
- Keep build and test output scoped to failures.
