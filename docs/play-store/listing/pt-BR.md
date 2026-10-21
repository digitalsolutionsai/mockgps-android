# Ficha da Play Store — Português (pt-BR)

## Nome do app (≤ 30)
MockGPS – Localização simulada

## Descrição curta (≤ 80)
Simule GPS, rotas e joystick para testar apps com localização. Sem anúncios.

## Descrição completa (≤ 4000)
O MockGPS é uma ferramenta de localização simulada para desenvolvedores Android e testadores de QA. Escolha um ponto, execute uma rota ou conduza com um joystick, e todos os apps do dispositivo enxergarão essa localização: GPS, rede e o provedor fused do Google Play services informam a mesma posição, então os apps não voltam para a sua localização real.

PONTO
• Mantenha o mapa pressionado, pesquise um endereço, cole coordenadas ou um link do Google Maps
• Compartilhe um lugar do Google Maps direto com o MockGPS
• Salve favoritos e vá até eles com um toque

ROTAS
• Adicione pontos de passagem e siga vias reais (a pé, bicicleta, moto, carro) ou linhas retas
• Importe trilhas GPX
• Defina a velocidade, pause e retome, e escolha parar, repetir ou ir e voltar ao chegar ao final

JOYSTICK
• Joystick flutuante sobre qualquer app, com velocidades de caminhada a direção

FEITO PARA DESENVOLVEDORES
• Controle total por adb: inicie, mova, execute rotas, mude a velocidade e pare pelo computador ou por scripts de CI
• Ajuste precisão, altitude, taxa de atualização e oscilação natural
• Escolha quais provedores simular: GPS, rede, fused
• Configuração guiada das Opções do desenvolvedor, além de uma configuração com adb em uma única linha

PRIVADO POR PROJETO
• Sem anúncios, sem análises, sem conta, sem servidores próprios
• Favoritos e configurações ficam no seu dispositivo
• Mapas do OpenFreeMap e do OpenStreetMap; rotas pelo OSRM (você pode usar seu próprio servidor)

BOM SABER
• Requer as Opções do desenvolvedor e selecionar o MockGPS como "app de local fictício".
• O Android marca localizações simuladas como mock (Location.isMock), então apps que verificam isso vão perceber. O MockGPS foi feito para testar seus próprios apps; não use para violar os termos de outros serviços.

Dados do mapa © OpenStreetMap contributors. Dúvidas ou sugestões: loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
