#!/bin/bash
# Xcode Cloud: Inyecta variables de entorno en Info.plist antes del build.
# Definí PATOVA_API_BASE_URL y PATOVA_API_KEY en las variables de entorno
# del workflow de Xcode Cloud (App Store Connect > Xcode Cloud > Workflow > Environment Variables).

set -e

PLIST="${CI_PRIMARY_REPOSITORY_PATH}/Patova-Info.plist"

echo "🔐 Patova CI: Verificando variables de entorno..."

inject_value() {
    local key="$1"
    local value="$2"
    if /usr/libexec/PlistBuddy -c "Print :${key}" "$PLIST" &>/dev/null; then
        /usr/libexec/PlistBuddy -c "Set :${key} ${value}" "$PLIST"
    else
        /usr/libexec/PlistBuddy -c "Add :${key} string ${value}" "$PLIST"
    fi
    echo "   ✓ ${key} inyectada"
}

if [ -n "$PATOVA_API_BASE_URL" ]; then
    inject_value "PATOVA_API_BASE_URL" "$PATOVA_API_BASE_URL"
else
    echo "   ⚠️ PATOVA_API_BASE_URL no definida, usando default del plist"
fi

if [ -n "$PATOVA_API_KEY" ]; then
    inject_value "PATOVA_API_KEY" "$PATOVA_API_KEY"
else
    echo "   ⚠️ PATOVA_API_KEY no definida, usando default del plist"
fi

echo "✅ Patova CI: Configuración lista."
