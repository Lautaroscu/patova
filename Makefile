.PHONY: backend-test backend-lint backend-types android-test android-build load-test ci-check

backend-test:
	cd backend && python -m pytest

backend-lint:
	cd backend && python -m ruff check .

backend-types:
	cd backend && python -m mypy src

android-test:
	cd android && ./gradlew testDebugUnitTest

android-build:
	cd android && ./gradlew assembleDebug

load-test:
	cd backend && python -m locust -f tests/load/locustfile_validate.py --headless -u 50 -r 5 -t 1m --host http://localhost:8000

ci-check: backend-lint backend-test backend-types
	@echo "=== CI checks passed ==="
