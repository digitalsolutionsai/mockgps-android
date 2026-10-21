# Ficha de Play Store — Español (es-ES)

## Nombre de la app (≤ 30)
MockGPS – Ubicación simulada

## Descripción breve (≤ 80)
Simula GPS, rutas y joystick para probar apps con ubicación. Sin anuncios.

## Descripción completa (≤ 4000)
MockGPS es una herramienta de ubicación simulada para desarrolladores Android y testers de QA. Elige un punto, reproduce una ruta o muévete con un joystick, y todas las apps del dispositivo verán esa ubicación: el GPS, la red y el proveedor fused de Google Play services informan la misma posición, así que las apps no vuelven a tu ubicación real.

PUNTO
• Mantén presionado el mapa, busca una dirección, pega coordenadas o un enlace de Google Maps
• Comparte un lugar desde Google Maps directamente a MockGPS
• Guarda favoritos y salta a ellos con un toque

RUTAS
• Añade puntos intermedios y sigue carreteras reales (a pie, bici, moto, auto) o líneas rectas
• Importa tracks GPX
• Ajusta la velocidad, pausa y reanuda, y elige entre detenerte, repetir o ir y volver al llegar al final

JOYSTICK
• Joystick flotante sobre cualquier app, con velocidades desde caminar hasta conducir

PENSADA PARA DESARROLLADORES
• Control total por adb: inicia, mueve, reproduce rutas, cambia la velocidad y detén desde tu PC o tus scripts de CI
• Ajusta la precisión, la altitud, la frecuencia de actualización y la fluctuación natural
• Elige qué proveedores simular: GPS, red, fused
• Configuración guiada de las Opciones de desarrollador, además de una configuración con adb en una sola línea

PRIVADA POR DISEÑO
• Sin anuncios, sin analíticas, sin cuenta, sin servidores propios
• Los favoritos y los ajustes se quedan en tu dispositivo
• Mapas de OpenFreeMap y OpenStreetMap; rutas con OSRM (puedes usar tu propio servidor)

CONVIENE SABER
• Requiere activar las Opciones de desarrollador y seleccionar MockGPS como "app de ubicación simulada".
• Android marca las ubicaciones simuladas como mock (Location.isMock), así que las apps que lo comprueban lo detectarán. MockGPS está pensada para probar tus propias apps; no la uses para incumplir los términos de otros servicios.

Datos del mapa © OpenStreetMap contributors. Preguntas o comentarios: loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
