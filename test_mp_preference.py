import urllib.request
import urllib.error
import json
import sys

def main():
    url = "http://localhost:8000/v1/payments/create-preference"
    headers = {
        "Content-Type": "application/json",
        "X-Patova-Key": "dev-dummy-key"
    }
    
    # Datos de prueba para crear la preferencia de suscripción mensual
    payload = {
        "plan_id": "premium_monthly",
        "email": "TESTUSER2124106455@testuser.com",
        "user_id": "usuario_test_123"
    }
    
    print("🚀 Iniciando petición de prueba a Mercado Pago en local...")
    print(f"📡 Endpoint: {url}")
    print(f"📦 Payload: {json.dumps(payload, indent=2)}")
    print("-" * 50)
    
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    
    try:
        with urllib.request.urlopen(req) as response:
            status_code = response.getcode()
            response_body = response.read().decode("utf-8")
            
            if status_code in (200, 201):
                res_json = json.loads(response_body)
                print("\n✅ ¡Preferencia de pago creada con éxito!")
                print(f"👤 User ID: {res_json.get('user_id')}")
                print(f"🔑 Preference ID: {res_json.get('preference_id')}")
                print("\n🔗 Clickeá en el siguiente enlace para abrir el Checkout de Prueba:")
                print(f"\033[94m{res_json.get('init_point')}\033[0m")
                print("\n💡 Recordá:")
                print("1. Abrí el enlace en una pestaña de incógnito de tu navegador.")
                print("2. Iniciá sesión con tu cuenta de Comprador de Prueba (de Mercado Pago Developers).")
                print("3. Pagá con tarjetas de prueba oficiales de Mercado Pago.")
            else:
                print(f"\n❌ Error del servidor (Código {status_code}):")
                print(response_body)
                
    except urllib.error.HTTPError as e:
        print(f"\n❌ Error HTTP {e.code}: {e.reason}")
        try:
            error_body = e.read().decode("utf-8")
            print(f"Detalle: {error_body}")
        except Exception:
            pass
    except urllib.error.URLError as e:
        print(f"\n❌ Error de conexión: No se pudo conectar al backend.")
        print(f"¿Ya levantaste los contenedores con 'docker compose up -d'?")
        print(f"Detalle del error: {e.reason}")
    except Exception as e:
        print(f"\n❌ Ocurrió un error inesperado: {str(e)}")

if __name__ == "__main__":
    main()
