📱 App-Beschreibung: MeshMoment
"Verbinde dich, wenn nichts mehr geht."
MeshMoment ist eine dezentrale Kommunikations-App, die speziell für Situationen entwickelt wurde, in denen kein Internet, kein Mobilfunknetz und kein WLAN-Router verfügbar sind. Ob beim Wandern in abgelegenen Gebieten, auf überfüllten Festivals oder in Notfallsituationen – MeshMoment nutzt die Wi-Fi Aware Technologie (NAN), um Geräte direkt miteinander zu vernetzen.
Hauptmerkmale:
•
Off-Grid Networking: Erstelle lokale "Räume" direkt von Gerät zu Gerät ohne Access Point.
•
Echtzeit-Radar: Finde andere Nutzer in deiner unmittelbaren Umgebung (bis zu 100m) visuell auf einem Radar.
•
Sicherer Chat: Ende-zu-Ende verschlüsselte Nachrichtenübertragung innerhalb deines lokalen Mesh-Netzwerks.
•
Morse-Modus: Kommuniziere klassisch per Morsezeichen – ideal für diskrete oder weitreichende Signale.
•
Völlige Privatsphäre: Keine Server, keine Cloud, keine Datenspeicherung außerhalb deiner Geräte.
📖 Bedienungsanleitung
1. Voraussetzungen
Damit MeshMoment optimal funktioniert, müssen folgende Bedingungen erfüllt sein:
•
Hardware: Beide Geräte benötigen Android 8.0 (API 26) oder höher und Hardware-Unterstützung für Wi-Fi Aware.
•
Berechtigungen: Die App benötigt Zugriff auf den Standort (erforderlich für Wi-Fi Scans) und WLAN-Berechtigungen.
•
Status: WLAN muss auf beiden Geräten aktiviert sein (eine Verbindung zu einem Router ist NICHT notwendig).
2. Einen Raum erstellen (Host)
Wenn du eine Gruppe starten möchtest:
1.
Öffne die App und navigiere zum Tab "Verbinden".
2.
Tippe auf "Raum erstellen".
3.
Gib deinem Raum einen Namen (z. B. "Wandergruppe Alpha") und optional ein Passwort ein.
4.
Dein Gerät sendet nun ein Signal aus, das für andere in der Nähe sichtbar ist.
3. Einem Raum beitreten (Client)
Um dich mit einem bestehenden Mesh zu verbinden:
1.
Aktiviere das "Radar" in der App.
2.
Verfügbare Räume werden als Punkte auf dem Radar oder in einer Liste angezeigt.
3.
Wähle den gewünschten Raum aus und tippe auf "Beitreten".
4.
Nach erfolgreicher Kopplung wird die Verbindung automatisch verschlüsselt hergestellt.
4. Chatten & Features
•
Chat: Sobald du verbunden bist, kannst du Textnachrichten senden. Ein grüner Indikator zeigt an, dass die Mesh-Verbindung aktiv ist.
•
Radar: Das Radar aktualisiert sich periodisch. Die Entfernung wird basierend auf der Signalstärke geschätzt.
•
Morse-Feature: Wechsel in den Morse-Tab, um Lichtsignale oder Vibrationen als Morsecode an die Gruppe zu senden.
5. Verbindung trennen
•
Tippe auf das "X" oder "Verlassen" im aktiven Raum.
•
Die App schließt die Wi-Fi Aware Session automatisch, um Akku zu sparen.
⚠️ Wichtige Hinweise zum Akku:
Wi-Fi Aware ist energieeffizienter als ein dauerhafter Hotspot, verbraucht aber dennoch mehr Strom als der Standby-Modus. Es wird empfohlen, die Entdeckung (Discovery) zu deaktivieren, wenn keine Kommunikation benötigt wird.
Entwickler-Status (intern):
•
Connectivity-Kern: Stabilisiert (Fix: Assignment Type Mismatch behoben).
•
Sicherheit: E2EE-Layer in :core:security integriert.
•
UI: Radar-Feature nutzt Standortdaten zur Visualisierung lokaler Peers.
