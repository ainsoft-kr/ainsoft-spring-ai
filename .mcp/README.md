# MCP Setup

Project-local MCP servers are configured in `.codex/config.toml`.

Active servers:

- `context7`: library documentation lookup.
- `symdex`: symbol index and context packs.
- `filesystem`: scoped to this repository only.
- `memory`: persistent project decisions in `.mcp/memory.json`.
- `git`: scoped to this repository.
- `openapi_spring_ai`: dynamic OpenAPI tools from `.mcp/spring-ai-openapi.json`.
- `playwright`: headless browser automation for Swagger UI or future UI tests.

GitHub MCP is not enabled because no GitHub token is configured in this environment.
To enable it, set `GITHUB_PERSONAL_ACCESS_TOKEN` in your shell and add the template in
`github-mcp.example.toml` to `.codex/config.toml`.

After changing MCP config, restart Codex so the MCP server list is reloaded.
