# Bob observability (Datadog)

This is how logging and tracing work for Bob today: what the app emits, how it reaches Datadog, how traces attach, and what we still need to change.

Sports360 (Grafana/OTEL) was the behavior reference. Bob uses **Datadog**, not OpenTelemetry/Grafana.

---

## What we wanted vs what you see now

**Intended (Spring Boot default / “pretty” console),** like:

```text
2026-09-19 21:28:22.055 "[PaymentScheduler] Run complete. Total: 0 | Paid: 0 | Still Pending: 0 | Already Processed (skipped): 0 | Errors: 0"
```

**What we ship today** is one **JSON object per line** (Logstash encoder), including framework noise:

```json
{"@timestamp":"2026-09-19T21:30:21.88980965+06:00","@version":"1","message":"HV000001: Hibernate Validator 9.1.3.Final","logger_name":"org.hibernate.validator.internal.util.Version","thread_name":"background-preinit","level":"INFO","level_value":20000,"service":"bob","env":"dev","version":"unknown"}
{"@timestamp":"2026-09-19T21:30:21.912340903+06:00","@version":"1","message":"Starting BobApplication using Java 25.0.3 with PID 106031 ...","logger_name":"com.bob.api.BobApplication","level":"INFO","service":"bob","env":"dev","version":"unknown"}
```

Datadog still shows a readable **list row** from the JSON `message` field. Local `bootRun` and `docker logs` show raw JSON, not the default Spring pattern.

**Follow-up (not done):** switch or dual-encode so humans get Spring-style lines (or Datadog’s Java log pipeline on pattern logs) without losing `dd.trace_id` / `dd.span_id` correlation. That adjustment comes later.

---

## End-to-end path

```text
Bob (Spring)  --SLF4J-->  Logback JSON stdout
        |
        +-- dd-java-agent.jar (in the image)  --traces-->  dd-agent:8126  -->  Datadog APM
Docker stdout  -->  Datadog Agent (container logs)  -->  Datadog Logs
```

1. Code calls `log.info(...)`. HTTP requests also put method, route template, sanitized User-Agent, and anonymized IP into MDC.
2. [`logback-spring.xml`](../src/main/resources/logback-spring.xml) writes that as JSON on **stdout**. The `dd-java-agent` injects `dd.trace_id` and `dd.span_id` into MDC when a trace is active (`DD_LOGS_INJECTION=true`).
3. The **Datadog Agent container** (`dd-agent`) tails Docker logs and ships them to Datadog Logs (`source:java`, `service:bob`).
4. The same JVM agent auto-instruments Spring, JDBC/Hibernate, and outbound HTTP. Spans go to **`http://dd-agent:8126`**, not to the EC2 host IP.

Logs can appear in Datadog even when traces fail. Traces need Bob and `dd-agent` on the **same Docker network**, with `DD_AGENT_HOST=dd-agent`.

---

## App-side setup

| Piece | Role |
|--------|------|
| `net.logstash.logback:logstash-logback-encoder` | JSON console |
| [`logback-spring.xml`](../src/main/resources/logback-spring.xml) | Console appender; `service` / `env` / `version` from `DD_*`; MDC keys for Datadog + HTTP |
| `ClientRequestContextFilter` | MDC: `http.request.method`, anonymized IP, User-Agent |
| `ClientRouteContextInterceptor` | MDC: `http.route` (Spring pattern, not raw IDs) |
| Event lines | `PREFIX action=key=value` on Facebook / message create / conversation mutations (`FB_WEBHOOK`, `FB_GRAPH`, `MESSAGE`, `CONVERSATION`) |
| [`Dockerfile`](../Dockerfile) | Pins `dd-java-agent` **1.66.0**, starts with `-javaagent:/app/dd-java-agent.jar` |
| `spring.application.name` | `bob` |

Root log level is **INFO**. Hibernate SQL is not dumped into logs; SQL shows up as APM spans.

`/opt/bob/.env` (app does **not** need `DD_API_KEY`):

```text
DD_SERVICE=bob
DD_ENV=dev
DD_LOGS_INJECTION=true
DD_AGENT_HOST=dd-agent
DD_TRACE_ENABLED=true
```

Deploy sets `DD_VERSION` to the git sha, `DD_TRACE_SAMPLE_RATE=1` (dev), and turns off Dynamic Instrumentation so `/debugger/v1/diagnostics` is not hammered when the Agent is down.

---

## Agent and deploy (EC2)

Datadog Agent runs as Docker **`dd-agent`** (`registry.datadoghq.com/agent:7`), not systemd. APM listens **inside that container** on `8126`. Sending traces to `host.docker.internal:8126` fails (`ConnectException`, `agent_version=null`).

**Agent container** needs:

- `DD_APM_ENABLED=true`
- `DD_APM_NON_LOCAL_TRAFFIC=true` (other containers may call 8126)
- Docker socket (or equivalent) so it can collect Bob’s stdout

**Network:** user-defined network `datadog`. GitHub deploy ([`.github/workflows/deploy-dev.yml`](../.github/workflows/deploy-dev.yml)):

1. `docker network create datadog` (ok if it exists)
2. `docker network connect datadog dd-agent` if `dd-agent` exists
3. `docker run --name bob --network datadog ... --label com.datadoghq.ad.logs='[{"source":"java","service":"bob"}]'`

One-time on the host (if not already done):

```bash
docker network create datadog 2>/dev/null || true
docker network connect datadog dd-agent
```

`DD_AGENT_HOST` in `/opt/bob/.env` must be **`dd-agent`** (the container name).

The Bob JRE image has **no `wget`**. Do not `docker exec bob wget ...`. Check APM on the Agent:

```bash
docker exec dd-agent curl -s http://127.0.0.1:8126/info
```

Or: Datadog **APM → Services → bob**.

---

## How to use it in Datadog

- **Logs:** `service:bob`. Search `FB_WEBHOOK`, `MESSAGE`, `CONVERSATION`, or a `message` substring.
- **APM:** service `bob`. Spans look like Sports360: repository methods, SQL, outbound HTTP (e.g. Facebook Graph).
- Open a request log that has **`dd.trace_id`** and jump to the matching trace.

Startup / validator lines (`Hibernate Validator`, `Starting BobApplication`) are normal INFO JSON until we change the encoder.

---

## Later: pretty console logs

We still want human-readable lines like the PaymentScheduler example (default Spring Boot console), especially for local `bootRun` and a cleaner Datadog list body.

That is a **later change**: keep trace–log correlation (`dd.trace_id` / `dd.span_id`), then either:

- Spring `logging.pattern.console` (or a pattern encoder) plus Datadog’s Java log pipeline, or
- JSON in production only and pattern locally.

Until then, treat JSON `message` as the log text and ignore that stdout is not the pretty Spring format.
