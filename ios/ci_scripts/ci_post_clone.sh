#!/bin/bash
set -e

echo "Removiendo portal proxy personalizado..."
defaults delete com.apple.dt.Xcode DVTServiceConfiguration 2>/dev/null || true

echo "Forzando portal oficial de Apple..."
defaults write com.apple.dt.Xcode DVTProvisioningIsManaged -bool YES

echo "Verificando conectividad con Apple..."
curl -sI https://developerservices2.apple.com | head -n1 || echo "ADVERTENCIA: Sin acceso directo a Apple"

echo "CI Post-Clone: Configuracion lista."
