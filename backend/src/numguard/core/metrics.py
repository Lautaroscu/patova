from prometheus_client import Counter, Gauge, Histogram, Info, make_asgi_app

validate_requests_total = Counter(
    "numguard_validate_requests_total",
    "Total validation requests",
    labelnames=["verdict"],
)

validate_latency_seconds = Histogram(
    "numguard_validate_latency_seconds",
    "Validation request latency in seconds",
    buckets=[0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5],
)

validate_cache_hits_total = Counter(
    "numguard_validate_cache_hits_total",
    "Total cache hits on validate endpoint",
)

reports_total = Counter(
    "numguard_reports_total",
    "Total report submissions",
)

rate_limit_hits_total = Counter(
    "numguard_rate_limit_hits_total",
    "Total rate-limited requests blocked",
    labelnames=["endpoint"],
)

http_requests_total = Counter(
    "numguard_http_requests_total",
    "Total HTTP requests",
    labelnames=["method", "endpoint", "status_code"],
)

http_request_duration_seconds = Histogram(
    "numguard_http_request_duration_seconds",
    "HTTP request latency in seconds",
    labelnames=["method", "endpoint"],
    buckets=[0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0],
)

http_requests_in_flight = Gauge(
    "numguard_http_requests_in_flight",
    "HTTP requests currently being processed",
    labelnames=["method"],
)

app_info = Info(
    "numguard_app",
    "NumGuard application info",
)

metrics_app = make_asgi_app()
