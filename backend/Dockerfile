# syntax=docker/dockerfile:1

FROM python:3.12-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1
ENV PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

RUN python -m venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH"

COPY pyproject.toml .
COPY src/ src/

RUN pip install --no-cache-dir .

FROM python:3.12-slim AS runtime

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PATH="/app/.venv/bin:$PATH"

WORKDIR /app

RUN groupadd --system --gid 1001 patova \
    && useradd --system --no-log-init --uid 1001 --gid patova --home /app patova

COPY --from=builder /app/.venv /app/.venv
COPY alembic.ini .
COPY alembic/ alembic/
COPY src/ src/
COPY .env.example .env.example

RUN chown -R patova:patova /app

USER patova

EXPOSE 8080 

HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
CMD python -c "import http.client, os; port = int(os.environ.get('PORT', '8080')); c = http.client.HTTPConnection('localhost', port); c.request('GET', '/health'); r = c.getresponse(); exit(0 if r.status == 200 else 1)" || exit 1

CMD ["sh", "-c", "uvicorn patova.main:create_app --factory --host 0.0.0.0 --port ${PORT:-8080} --log-level info"]
