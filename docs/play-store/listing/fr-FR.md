# Fiche Play Store — Français (fr-FR)

## Nom de l'application (≤ 30)
MockGPS – Position simulée

## Description courte (≤ 80)
Simulez GPS, itinéraires et joystick pour tester vos apps de position. Sans pub.

## Description complète (≤ 4000)
MockGPS est un outil de simulation de position pour les développeurs Android et les testeurs QA. Choisissez un point, lancez un itinéraire ou pilotez avec un joystick, et toutes les applications de l'appareil voient cette position : le GPS, le réseau et le fournisseur de localisation fused de Google Play services indiquent la même position, si bien que les applications ne reviennent pas à votre position réelle.

POINT
• Appuyez longuement sur la carte, cherchez une adresse, collez des coordonnées ou un lien Google Maps
• Partagez un lieu depuis Google Maps directement vers MockGPS
• Enregistrez des favoris et accédez-y en un seul geste

ITINÉRAIRES
• Ajoutez des étapes et suivez de vraies routes (à pied, vélo, moto, voiture) ou des lignes droites
• Importez des traces GPX
• Réglez la vitesse, mettez en pause et reprenez, puis choisissez l'arrêt, la répétition ou l'aller-retour à l'arrivée

JOYSTICK
• Joystick flottant par-dessus n'importe quelle application, de la vitesse de marche à celle de la conduite

CONÇU POUR LES DÉVELOPPEURS
• Contrôle complet par adb : démarrer, déplacer, lancer des itinéraires, changer la vitesse et arrêter depuis votre ordinateur ou vos scripts CI
• Réglez la précision, l'altitude, la fréquence de mise à jour et la fluctuation naturelle
• Choisissez les fournisseurs à simuler : GPS, réseau, fused
• Configuration guidée des options pour les développeurs, plus une configuration adb en une seule ligne

RESPECTUEUX DE VOTRE VIE PRIVÉE
• Aucune publicité, aucune analyse, aucun compte, aucun serveur à nous
• Les favoris et les paramètres restent sur votre appareil
• Cartes d'OpenFreeMap et d'OpenStreetMap ; itinéraires par OSRM (vous pouvez utiliser votre propre serveur)

BON À SAVOIR
• Nécessite les options pour les développeurs et la sélection de MockGPS comme « application de localisation fictive ».
• Android signale les positions simulées comme mock (Location.isMock) : les applications qui le vérifient s'en apercevront. MockGPS est conçu pour tester vos propres applications ; ne l'utilisez pas pour enfreindre les conditions d'autres services.

Données cartographiques © OpenStreetMap contributors. Questions ou commentaires : loivo@dsai.vn
GitHub: https://github.com/digitalsolutionsai/mockgps-android
