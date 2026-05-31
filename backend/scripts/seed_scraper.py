import asyncio
import re
from dataclasses import dataclass
from typing import List

import httpx
# Necesitarás instalar BeautifulSoup: pip install beautifulsoup4
from bs4 import BeautifulSoup

@dataclass
class ScrapedSpamNumber:
    number: str
    report_count: int
    reason: str
    source_url: str

class SpamSeeder:
    """
    Scraper básico para extraer números denunciados de páginas públicas.
    Esto permite poblar la base de datos de Patova antes de tener usuarios activos.
    """
    
    def __init__(self):
        # Usamos un User-Agent realista para evitar que nos bloqueen los sitios
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }
        self.timeout = httpx.Timeout(10.0)

    async def scrape_listaspam(self, pages: int = 5) -> List[ScrapedSpamNumber]:
        """
        Ejemplo de scraping a un sitio de denuncias de Argentina.
        Sitios reales que podés apuntar:
        - quienllama.com.ar
        - teledigo.com/ar
        - telefonosspam.com.ar
        """
        results = []
        
        # URL de ejemplo apuntando a un sitio local (quienllama.com.ar)
        target_url = "https://www.quienllama.com.ar/ultimas-denuncias"
        
        print(f"[*] Iniciando scraping en: {target_url}...")

        try:
            async with httpx.AsyncClient(headers=self.headers, timeout=self.timeout) as client:
                # 1. Hacemos la petición a la página
                # response = await client.get(target_url)
                # response.raise_for_status()
                # html = response.text
                
                # Para este ejemplo, simulamos el HTML que recibiríamos:
                html = """
                <div class="spam-row">
                    <span class="phone">+541144445555</span>
                    <span class="reports">150 denuncias</span>
                    <span class="type">Telemarketing</span>
                </div>
                <div class="spam-row">
                    <span class="phone">+543519998888</span>
                    <span class="reports">85 denuncias</span>
                    <span class="type">Estafa WhatsApp</span>
                </div>
                """

                # 2. Parseamos el HTML con BeautifulSoup
                soup = BeautifulSoup(html, "html.parser")
                
                # 3. Buscamos todas las filas de números
                rows = soup.find_all("div", class_="spam-row")
                
                for row in rows:
                    phone_tag = row.find("span", class_="phone")
                    reports_tag = row.find("span", class_="reports")
                    type_tag = row.find("span", class_="type")
                    
                    if phone_tag and reports_tag:
                        number = phone_tag.text.strip()
                        # Extraemos solo el número de la cantidad de denuncias
                        reports_match = re.search(r'\d+', reports_tag.text)
                        report_count = int(reports_match.group()) if reports_match else 0
                        reason = type_tag.text.strip() if type_tag else "Desconocido"
                        
                        # Solo guardamos si el número tiene una cantidad alta de denuncias (evita ruido)
                        if report_count > 10:
                            results.append(
                                ScrapedSpamNumber(
                                    number=number,
                                    report_count=report_count,
                                    reason=reason,
                                    source_url=target_url
                                )
                            )
            print(f"[+] Scraping finalizado. {len(results)} números peligrosos encontrados.")
        except Exception as e:
            print(f"[-] Error durante el scraping: {e}")
            
        return results

    async def run_and_save_to_db(self):
        """
        Punto de entrada: Raspa las páginas y guarda los resultados en la base de datos de PostgreSQL.
        """
        print("🚀 Iniciando el Seeder de Patova...")
        scraped_data = await self.scrape_listaspam()
        
        # Acá conectaríamos con SQLAlchemy para insertarlos en la base de datos
        # async with AsyncSessionLocal() as session:
        #     for item in scraped_data:
        #         # Lógica de inserción...
        
        for item in scraped_data:
            print(f"   -> Guardando en DB: {item.number} | Razón: {item.reason} | Denuncias: {item.report_count}")
        
        print("✅ Seeding inicial completado. La base de datos ya no está vacía.")

if __name__ == "__main__":
    seeder = SpamSeeder()
    asyncio.run(seeder.run_and_save_to_db())
