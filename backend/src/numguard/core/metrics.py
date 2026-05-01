from prometheus_client import Counter, Histogram, make_asgi_app

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

metrics_app = make_asgi_app()
