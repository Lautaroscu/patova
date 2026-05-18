# syntax=docker/dockerfile:1

FROM python:3.12-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

RUN python -m venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH"

COPY pyproject.toml .
COPY src/ src/
RUN pip install --no-cache-dir -e ".[dev]"

FROM python:3.12-slim AS runtime

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PATH="/app/.venv/bin:$PATH"

WORKDIR /app

RUN groupadd --system numguard && useradd --system --gid numguard numguard

COPY --from=builder /app/.venv /app/.venv
COPY alembic.ini .
COPY alembic/ alembic/
COPY src/ src/
COPY .env.example .env.example

RUN chown -R numguard:numguard /app

USER numguard

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

CMD ["uvicorn", "numguard.main:create_app", "--factory", "--host", "0.0.0.0", "--port", "8000", "--log-level", "info"]
