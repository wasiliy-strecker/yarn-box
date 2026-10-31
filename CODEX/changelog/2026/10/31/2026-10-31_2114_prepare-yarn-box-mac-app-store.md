# Yarn Box für den Mac App Store vorbereiten

Nur Yarn Box geändert. Bookshelf und die drei weiteren Foto-Apps unverändert.
Öffentlicher Name Yarn Box, Bundle-ID de.appfabrik.garnbox, Version 1.0,
Build 1. Eigenständige Mac-Helfer und Konfiguration für ARM64 und Intel aus
der ersten App übernommen und an Fotos, Farbwahl und JSON-Sicherung angepasst.

Store-Build verwendet den vom Cocoa-Launcher bestimmten App-Container.
JSON-Export fordert Ordner und Dateiname an. Nativer JNI-Fotodialog verwendet
den gemerkten Ordner als Navigationshinweis und berechtigt jede neue Auswahl.
Normale Datenpfade, Fotos, Sprachwahl und JSON-Schema erhalten.

Deutsche Feldtexte, synthetischer Importbestand mit Farbfeldern und eigenen
Garnillustrationen, vier echte Linux-Bildentwürfe in 2560 × 1600 RGB,
Datenschutzexport und konkrete Mac-/Store-Anleitungen vorbereitet.
Signierungsbeispiele enthalten keine privaten Werte. Originale gepinnte
Mac-Abhängigkeiten und Runtime-Quellen mit Prüfsummen in der Übergabe.

Verifikation: Maven Compiler-Lint, Enforcer, Spotless und SpotBugs bestanden.
Python-Syntax, Shell-Syntax, Feldlängen und RGB-Bildmaße kontrolliert.
Auf ausdrücklichen Nutzerwunsch keine automatisierten Tests ausgeführt.
Bildaufnahme dient ausschließlich der Asset-Erstellung. Manuelle Mac-Abnahme
für Fotos, Ordner nach Neustart, Farbwahl, Import/Export und beide Architekturen
dokumentiert. Native Kompilierung und Ausführung auf diesem Linux-System
nicht erfolgt. Keine Signierung und kein Store-Upload.

Übergabe: build/releases/mac-app-store/Yarn-Box-1.0-build-1-2026-10-31/.
Quellen, Eingaben und Bilder über Quellhash gebunden. Alle gelieferten Dateien
mit SHA-256 inventarisiert. Öffentliche Datenschutzadresse, Veröffentlichung
der Runtime-Quellen, finale Mac-Aufnahmen, Mac-Abnahme und signiertes Paket
ausdrücklich offen. Kein Push und keine persönlichen Sammlungen verwendet.
