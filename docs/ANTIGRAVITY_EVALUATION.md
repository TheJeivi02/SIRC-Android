# SIRC — Evaluación de herramientas de agente: OpenCode vs Google Antigravity

> Investigación (16-ago-2026) + **decisión de herramientas** del LOOP
> ENGINEERING — Backend Supabase. Distingue `[DOC]` = documentación oficial
> Google, `[INF]` = inferencia razonada, `[COM]` = evidencia de comunidad,
> `[N/V]` = no verificado oficialmente. Fuentes: antigravity.google, docs del
> producto, developer.android.com/tools/agents, github.com/android/skills.

## 1. Estado actual de Google Antigravity (16-ago-2026)

- **Antigravity 2.0** (lanzado en I/O, 19-may-2026): app de escritorio "agent-first"
  sin centrarse en el IDE; **Antigravity IDE** (fork de VS Code) sigue existiendo;
  **Antigravity CLI** (`agy`, Go) reemplaza a Gemini CLI (retirado 18-jun-2026);
  SDK Python. Disponible en Windows/macOS/Linux. Estado: **public preview**, no GA
  `[DOC]`.
- **Funcionalidades**: Projects/workspaces, modo Local o New Worktree Mode
  (worktrees git aislados por conversación), agente + subagentes dinámicos
  (research/browser/self; profundidad máx. 10), browser agent sandboxed,
  **Android CLI 1.0 + Android skills** (`github.com/android/skills`) + Android
  Knowledge Base integradas como plugins "Build with Google" `[DOC]`, soporte
  **MCP** (stdio/remote, OAuth, permisos prompt-based), **artifacts** auditables
  (planes, capturas, diffs), sandbox de terminal por SO, modelo en la nube de
  Google (default Gemini 3.5 Flash).
- **Limitaciones `[DOC]/[COM]`**: sin BYOK/BYO-endpoint (modelo solo vía Google);
  cuotas opacas y cambiantes; public preview; calidad de código profunda reportada
  menor que Claude Code/Codex (p. ej. SWE-bench 80.6 vs 88/88 `[COM]`); sin
  representación persistente del código en repos grandes `[COM]`; sandbox de
  Windows con bugs reportados `[COM]`; UI de la app 2.0 principalmente en inglés.
- Precios/tier (referencia de comunidad): Free ~20 req/día, AI Pro $20/mes,
  AI Ultra $100/mes, Ultra Max $200/mes `[COM]`.

## 2. Comparativa por área

| Área | OpenCode actual (SIRC) | Antigravity | Mejor opción |
|---|---|---|---|
| Android / Kotlin | Herramientas genéricas + skills superpowers; OpenCode CLI | Android skills oficiales + Android CLI 1.0 (integra Android Studio, navigation, AGP9/Compose) `[DOC]` | **Mixto**: OpenCode para implementación; skills Android de Antigravity como referencia |
| Gradle (builds) | Ok: `gradlew` desde CLI + superpowers | Capacidad no documentada oficialmente para repos Gradle grandes `[N/V]`; reportes comunitarios de degradación `[COM]` | **OpenCode** |
| Git | Git CLI/gh; control directo; orquestador | Worktrees nativos por conversación; git/gh vía agente o MCP GitHub; sin PRs nativos `[DOC]` | **Mixto** (worktrees útiles pero autoridad de git en OpenCode) |
| GitHub | `gh` integrado; commits/PRs controlados | Integración vía agente/MCP; no nativa | **OpenCode** |
| Agentes/subagentes | Subagentes (Task tool); orquestación manual | Subagentes dinámicos automáticos (research/browser/self) `[DOC]` | **OpenCode** (control humano) |
| Investigación web | WebFetch/WebSearch | Browser agent sandboxed (automatización web real) | **Antigravity** para navegación/automatización |
| UI testing | No hay UI testing automatizado integrado hoy (componentes + unit tests) | No verificado para Compose UI testing `[N/V]` | **OpenCode** / manual |
| Documentación | OpenCode edita docs del repo con trazabilidad | ídem, pero con artefactos/planes `[DOC]` | **OpenCode** (ya es el flujo) |
| Skills | Superpowers (skills) ya en uso | Android skills oficiales de Google + MCP para subir skills `[DOC]` | **Mixto** |
| MCP | OpenCode con MCP según configuración | MCP completo (stdio/remote/OAuth) `[DOC]` | Neutral |
| Control del workspace | Directo (workdir) | Worktrees/projects; permisos por proyecto `[DOC]` | **Mixto** (worktrees útiles, pero SIRC trabaja en single worktree) |
| Trazabilidad | Gradual, commits atómicos documentados | Artifacts/planes auditables | **OpenCode** (ya consolidado) |
| Loop Engineering | Ciclo LOOP ya integrado y funcional en este repo | No se usó-loop; sin evidencia de encaje | **OpenCode** |
| Control humano | Permisos por herramienta, pausas en pasos críticos | Permisos por defecto de comandos/ediciones (con bugs reportados) `[COM]` | **OpenCode** |
| Riesgo de cambios simultáneos | Regla R17 prohíbe doble-agente en mismo branch | Worktrees aislados mitigarían, pero se prohíbe igual | **OpenCode** + regla R17 |
| Costo | Modelo actual (free) | Free limitado (20 req/día) `[COM]` | **OpenCode** como principal |

## 3. Decisión de herramientas

**OPCIÓN C — OpenCode como agente principal + Antigravity complementario.**

- **OpenCode**: agente PRINCIPAL (implementa código, mantiene arquitectura,
  roadmap, WPs, integración, release, git/GitHub). Autoridad total (regla R17).
- **Antigravity**: **complemento exploratorio** para (a) investigación web con
  **browser agent**, (b) usar las **Android skills/CLI** de Google como fuente de
  referencia de mejores prácticas Android/Compose, y (c) prototipado aislado en
  worktrees si algún día se necesita. **No sustituye** a OpenCode porque: no hay
  evidencia oficial de que maneje repos Gradle grandes mejor que el flujo actual;
  su calidad de código profunda es inferior (comunidad); es public preview; sin
  BYOK; y el Loop Engineering/control humano ya está resuelto en OpenCode.

> Regla adjunta: **prohibido doble-agente simultáneo en el mismo workspace/branch**
> (regla R17). Si se prueba Antigravity con tareas específicas, será **en un
> worktree aislado** con commit separado y revisión humana.

## 4. Decisiones registradas

- Decisión de herramientas: **OpenCode principal + Antigravity complementario**
  (registrado como D14.4 en `.ai/DECISIONS.md`).
- No se requiere más infraestructura; no se instala nada todavía. Prueba
  opcional de Antigravity en tareas de investigación/documentación futuras.