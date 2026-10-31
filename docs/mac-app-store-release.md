# Yarn Box auf dem Mac abschließen

Version **1.0**, Build **1**, Bundle-ID **de.appfabrik.garnbox**.
Die Übergabe enthält Quellen und vorbereitete Eingaben für Apple Silicon und
Intel. Sie enthält noch keine nativ gebaute, ausgeführte oder signierte Mac-App.
Automatisierte Tests wurden auf Nutzerwunsch nicht ausgeführt.

## Voraussetzungen

Mac mit Xcode und Command Line Tools, Python 3.12 oder neuer und Eclipse Temurin
21.0.13+9 für die Architektur des ausführenden Macs. Passende JDK-Archive liegen
unter `MacInputs/dependencies`. `JAVA_HOME` auf dessen `Contents/Home` setzen.
Eine systemweite Java-Installation ist nicht nötig. Entwicklerzugang und passende
Zertifikate werden erst für die lokale Signierung benötigt.

Der Universal-Launcher lädt Java im selben Prozess. Je ein jlink-Runtime-Image,
JavaFX und SQLite sind für ARM64 und Intel enthalten. Der native Fotodialog nutzt
NSOpenPanel mit dem gespeicherten Ordner als Navigationshinweis. Das System
berechtigt die jeweilige Dateiauswahl. Die Fotokopie wird in SQLite gespeichert.
Es werden keine dauerhaften Leserechte für die Originalbilder benötigt.

## Eingaben kontrollieren und bauen

Den gesamten Übergabeordner auf den Mac kopieren und im Terminal in `Source`
wechseln. Die Eingaben sind an den Quellstand gebunden. Änderungen erfordern
neue Eingaben und neue manuelle Abnahme.

```bash
python3 scripts/macos_release.py check --inputs ../MacInputs
python3 scripts/macos_release.py build --inputs ../MacInputs --output ../MacBuild-1.0-1
python3 scripts/macos_release.py inspect --app "../MacBuild-1.0-1/Yarn Box.app"
```

`build` kompiliert Launcher und Fotodialog auf dem Mac. Der Helfer erstellt neue
Ordner und überschreibt keine frühere Lieferung. Der Store-Launcher benötigt
eine aktive Sandbox. Vor dem Start deshalb mit einem Entwicklungsprofil signieren.

## Entwicklungssignatur

`packaging/macos/signing.example.json` lokal nach `development.local.json`
kopieren und ausfüllen. Private Dateien nicht weitergeben oder committen.

| Feld | Inhalt |
| --- | --- |
| teamId | Eigene zehnstellige Apple-Team-ID |
| appStoreConnectBundleId | de.appfabrik.garnbox |
| applicationIdentity | Vollständiger Name des Apple-Development-Zertifikats |
| installerIdentity | Für Entwicklung leer |
| provisioningProfile | Absoluter Pfad zum gültigen Mac-Entwicklungsprofil dieser App |

```bash
python3 scripts/macos_release.py sign --app "../MacBuild-1.0-1/Yarn Box.app" --config packaging/macos/development.local.json --mode development
open "../MacBuild-1.0-1/Yarn Box.app"
```

## Manuelle Abnahme durch den Nutzer

Einen separaten macOS-Testbenutzer mit erfundenen Daten verwenden. Die App
speichert im Container unter `Library/Application Support/GarnBox` die Datei
`garnbox.db` sowie Sprache und Fotoordner. Eine vorhandene Sammlung außerhalb
des Containers wird nicht automatisch verschoben, importiert oder gelöscht.
Der bewusste Transfer erfolgt über JSON. Der Store-Build nimmt kein `--data-dir` an.

| Ablauf | Erwartung |
| --- | --- |
| Start ohne Internet und globales Java | App mit eingebetteter Laufzeit, erster Start auf Englisch |
| Language auf Deutsch, App neu starten | Deutsche Oberfläche und erhaltene Sammlung |
| Garne anlegen, bearbeiten, suchen und löschen | Angaben und Notizen bleiben korrekt |
| Farbwähler öffnen, Farbe speichern, erneut bearbeiten | Gleiches Farbfeld und gleicher Farbwert |
| Foto aus anderem Ordner auswählen | JPEG/PNG erscheint als optimierte Kopie, Original unverändert |
| App neu starten, Fotoauswahl öffnen | Letzter Ordner wird im nativen Dialog vorgeschlagen |
| Gemerkten Ordner entfernen oder Laufwerk trennen | Dateiauswahl bleibt bedienbar, anderer Ordner wählbar |
| Fotoauswahl oder Bearbeitung abbrechen | Bisherige Einträge und Bilder unverändert |
| Exportordner und Dateiname wählen | Vollständige JSON-Datei mit Farben und Fotos |
| Exportdialoge abbrechen oder vorhandenen Namen verwenden | Bestehende Dateien erhalten, kein unvollständiger Export |
| JSON importieren, dann denselben Export nochmals importieren | Vorhandene Garne erhalten, identische Kopien übersprungen |
| Beschädigter Import oder ungültiges Foto | Verständliche Fehlermeldung, keine Teiländerung |
| Kleine Fenster, Info und Sprache | Bedienbare Dialoge, vollständige Datenschutzerklärung |

Diese Abnahme in beiden Architekturen durchführen. Auf Apple Silicon ist der
Intel-Pfad nach Installation von Rosetta so erreichbar. Die App vorher schließen:

```bash
arch -x86_64 "../MacBuild-1.0-1/Yarn Box.app/Contents/MacOS/YarnBox"
```

Rosetta-Ausführung ausdrücklich als solche dokumentieren. Sie ersetzt keine
Abnahme auf echter Intel-Hardware. Sandbox, JIT und Dateidialoge sind unter Linux
nicht überprüfbar. Bei einem Fehler die Ursache anhand der macOS-Konsole klären.
Der Helfer deaktiviert keine Library Validation und verlangt keine pauschalen
Dateisystem- oder Netzwerkrechte.

## Endgültige Mac-Screenshots

Deutsch einstellen und neu starten. Über Importieren die beiliegende Datei
`assets/store_demo/yarn-box-de-DE.json` auswählen. Sie enthält fünf erfundene
Garne mit Farbfeldern und eigenen Garnillustrationen. Persönliche Daten nicht verwenden.

Vier Ansichten aufnehmen: Sammlung mit ausgewähltem Foto, Bearbeitung mit
Farbwähler, Suche nach „Strickjacke“, JSON-Export mit Dateiname. Die Bilder als
PNG ohne Alpha in 2560 × 1600 unter `Screenshots-Final-Mac/de-DE` ablegen.
Bildtexte stehen unter `Store/de-DE/captions.txt`. Die Linux-Aufnahmen sind
Entwürfe und kein Nachweis für den Mac-Build. Ein Vorschauvideo ist nicht vorgesehen.

## Store-Signierung und Paket

Nach erfolgreicher manueller Abnahme `signing.example.json` nach `store.local.json`
kopieren. App-Identität und Team müssen zu dieser App passen. Für die App ein
Apple-Distribution- oder „3rd Party Mac Developer Application“-Zertifikat mit
Mac-App-Store-Profil verwenden. Für das Paket ist „3rd Party Mac Developer
Installer“ beziehungsweise „Mac Installer Distribution“ erforderlich.
Developer ID ist für diesen Store-Weg nicht vorgesehen.

```bash
python3 scripts/macos_release.py sign --app "../MacBuild-1.0-1/Yarn Box.app" --config packaging/macos/store.local.json --mode store
python3 scripts/macos_release.py inspect --app "../MacBuild-1.0-1/Yarn Box.app" --signed
```

`acceptance.example.json` nach `acceptance.local.json` kopieren. Nur tatsächlich
bestandene manuelle Prüfungen auf `true` setzen. Unter `x86_64Method` `native`
oder `rosetta` angeben. Den `sourceDigest` aus `MacInputs/inputs.json` übernehmen
und die verwendeten Mac-/macOS-Versionen im Notizfeld dokumentieren. Der
Pakethelfer verlangt dieses Protokoll für den aktuellen Quellstand.

```bash
python3 scripts/macos_release.py package --app "../MacBuild-1.0-1/Yarn Box.app" --config packaging/macos/store.local.json --acceptance packaging/macos/acceptance.local.json --output "../Yarn-Box-1.0-build-1-universal.pkg"
```

Das Paket anschließend mit Transporter validieren und auf ausdrücklichen Auftrag
hochladen. Nach Apples Verarbeitung den Build in App Store Connect auswählen.
Kein Helfer dieser Übergabe führt einen Upload durch.

## Vor der Einreichung

`JETZT-IN-APP-STORE-CONNECT.md` abarbeiten. Öffentliche Datenschutzseite und
versionsbezogene Laufzeitquellen bereitstellen. Erforderliche Privacy-Manifeste,
API-Gründe und Verschlüsselungsangaben anhand der tatsächlich gebauten App samt
Runtime prüfen. Offline-Betrieb allein beantwortet den Verschlüsselungsfragebogen
nicht. Händlerstatus und Vertragszustimmungen bleiben beim Kontoinhaber.

## Vorbereitung erneut erzeugen

Die folgenden Befehle führen keine automatisierten Tests aus. Erforderlich sind
JDK, Python, Xvfb und die Maven-Abhängigkeiten. `YARNBOX_XVFB` kann auf ein lokal
vorhandenes Xvfb zeigen. Die Bildaufnahme verwendet einen separaten Bildschirm
und eine temporäre, erfundene Sammlung.

```bash
./mvnw --batch-mode --no-transfer-progress spotless:apply
./mvnw --batch-mode --no-transfer-progress dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/gui-classpath.txt
python3 scripts/macos_release.py static-check
python3 scripts/capture_mac_drafts.py --output target/mac-drafts-neu
python3 scripts/macos_release.py prepare --output build/mac-inputs-neu
```

Nach erfolgreicher statischer Prüfung Änderungen lokal committen. Dann den neuen
Übergabeordner erzeugen. Bei belegtem Ziel einen neuen Namen verwenden.

```bash
python3 scripts/prepare_mac_handoff.py --inputs build/mac-inputs-neu --captures target/mac-drafts-neu --output build/releases/mac-app-store/Yarn-Box-1.0-build-1-neu
```

## Referenzen

[Apple: Sandbox und Dateizugriffe](https://developer.apple.com/documentation/security/accessing-files-from-the-macos-app-sandbox)

[Apple: NSOpenPanel](https://developer.apple.com/documentation/appkit/nsopenpanel)

[Apple: Screenshots](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)
