#!/bin/sh
# Surface the real IDEDistribution export error (exit 70 is generic)
echo "===== Dumping IDEDistribution export logs ====="
find "$CI_DERIVED_DATA_PATH/../tmp" -name "IDEDistribution.standard.log" 2>/dev/null | while read -r f; do
  echo "----- $f -----"
  cat "$f"
done

# fallback: buscar en todo /Volumes/workspace/tmp
find /Volumes/workspace/tmp -name "IDEDistribution*.log" 2>/dev/null | while read -r f; do
  echo "----- $f -----"
  cat "$f"
done

echo "===== End export logs ====="
