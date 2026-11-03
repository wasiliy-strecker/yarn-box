# Jetzt in App Store Connect eintragen

Produkt: **Yarn Box**. Plattform: **macOS**.
Erste Veröffentlichung: **Deutsch**, Version **1.0**, Apple-Buildnummer **1**.

Deutsch bezeichnet hier die Store- und Release-Texte. Die App startet ohne
gespeicherte Sprachwahl auf Englisch. Vorhandene UI-Sprachen bleiben auswählbar.
Der lokale Ordnername und die technische Kennung bleiben unverändert.

## Aktuelle Versionsseite

Die Dateien unter `Store/de-DE` in der Übergabe enthalten jeweils ausschließlich
den Feldinhalt. Im Repository liegen sie unter `store-listings/mac-app-store/de-DE`.

| Feld | Datei oder Einstellung |
| --- | --- |
| Werbetext | `promotional-text.txt` |
| Beschreibung | `description.txt` |
| Schlüsselwörter | `keywords.txt` |
| Support-URL | `support-url.txt` |
| Marketing-URL | `marketing-url.txt` |
| Copyright | `copyright.txt` |
| Version | `1.0` |
| Anmeldung erforderlich | Ausschalten |
| Kontaktinformationen für die Prüfung | Eigene erreichbare Telefonnummer und E-Mail sowie Vor- und Nachname |
| Anmerkungen zur Prüfung | `review-notes.txt` |
| Game Center | Ausgeschaltet |
| Versionsveröffentlichung | Automatisch nach Apples Freigabe, wie im vorhandenen Eintrag gewählt |

Jetzt **Sichern** wählen. Den Build nach der Mac-Abnahme und dem Upload ergänzen.
Die Prüfhinweise beschreiben den vorgesehenen finalen Mac-Build. Sie sind
kein Nachweis, dass dieser bereits ausgeführt wurde.

Stand 3. November 2026: Die neue Inhaltslieferung verwendet die englische
GitHub-Datenschutzerklärung. Die Beschreibung unterscheidet die offline
nutzbare Garnverwaltung vom Browseraufruf. Frühere Lieferungen bleiben
unverändert. Nach dem Eintragen der Datenschutz-URL und dem Beantworten
der Datenerfassung die Datenschutzangaben im Portal veröffentlichen.

## App-Informationen

Name: `name.txt`. Untertitel: `subtitle.txt`. Primärsprache Deutsch.
Primäre Kategorie: Produktivität. Keine zweite Kategorie erforderlich.
Die bestehende Registrierung im Portal prüfen. Der bisherige Paketierungsweg
verwendet noch `de.appfabrik.garnbox`. Für neue Registrierungen gilt die zentrale
Vorgabe englischer Kennungen, entsprechend `de.appfabrik.yarnbox`. Vor dem
Mac-Store-Build die tatsächlich registrierte Kennung und die Paketierung
abgleichen. Dieser Inhaltsauftrag ändert keine Kennungen oder Datenpfade und
legt keinen neuen App-Eintrag an.

Die Antworten zur Altersfreigabe ergeben sich aus einer privaten lokalen
Garnsammlung. Kein öffentliches Teilen von Nutzerinhalten, Chat, Werbung,
Browser, Glücksspiel, Käufe oder medizinische Beratung vorhanden. Persönliche
Notizen sind keine öffentliche Inhaltsplattform. Den aktuellen Apple-Fragebogen
entsprechend den konkreten Fragen beantworten. Die Altersstufe berechnet Apple.

## App-Datenschutz

Für die lokale Anwendung ist weiterhin „Es werden keine Daten erfasst“ vorgesehen.
Die App besitzt keine Konten, Werbung, Telemetrie oder eigene Cloud-Synchronisation.
Der ausdrücklich angeforderte Browseraufruf überträgt keine Sammlung.

Datenschutz-URL für alle Store-Sprachen:

```text
https://github.com/wasiliy-strecker/yarn-box/blob/main/PRIVACY.md
```

Die vollständige englische Erklärung steht in `PRIVACY.md`. Neue Übergaben
enthalten eine unveränderte Kopie und `privacy-url.txt` unter `Privacy`.
Vor der Einreichung die URL ohne Anmeldung öffnen und den App-Namen prüfen.
Die optionale URL für Datenschutzoptionen bleibt leer.

Händlerstatus, Inhaltsrechte und Vertragszustimmungen sind persönliche rechtliche
Erklärungen. Diese Vorbereitung trifft oder ändert sie nicht automatisch.

## Preise und Verfügbarkeit

Geplanter Preis kostenlos beziehungsweise 0,00, wie bei der ersten App. Keine In-App-Käufe und kein Abonnement.
Deutschland für die deutsche Erstveröffentlichung einschließen. Weitere
Vertriebsländer nach der eigenen gewünschten Verfügbarkeit wählen. Der bereits
aktive Vertrag für kostenlose Apps bleibt die Grundlage.

## Bilder und Build

`Screenshot-Drafts-Linux` enthält vier echte Aufnahmen der Linux-App mit
synthetischen Daten und dem aktuellen Namen. Sie dienen der Vorbereitung.
Sie dürfen nicht als fertige Mac-Aufnahmen oder als Mac-Abnahme ausgegeben werden.
Vor Einreichung mit der tatsächlich laufenden Mac-App vergleichen und
abweichende Ansichten auf dem Mac erneut aufnehmen. Die vorhandenen Texte,
Illustrationen und Ansichten bleiben nutzbar. `Screenshots-Final-Mac/de-DE`
bleibt bis zu diesem Vergleich leer. Bild 4 zeigt den echten Java-Dateinamendialog
des vorgesehenen Store-Exports. Es belegt keinen abgeschlossenen Export auf macOS.

Mac-Screenshots benötigen 16:10 und eine von Apple unterstützte Größe.
Vorbereitet ist **2560 × 1600**. Alle vier Bilder müssen dieselben Maße und keine Transparenz haben.
Ein Video ist für diese Einreichung nicht erforderlich.

Nach nativer Abnahme die fertige App signieren, als `.pkg` verpacken und mit
Transporter hochladen. Erst nach Apples Verarbeitung den Build in dieser Version
auswählen. Die Anleitung `docs/mac-app-store-release.md` im Repository
beziehungsweise `AUF-DEM-MAC.md` in der bisherigen technischen Übergabe
führt durch diese Schritte.

## Inhaltslieferung ohne App-Build

Die aktuelle Inhaltslieferung enthält Feldtexte, vier Aufnahmen, eine
synthetische Garnsammlung und die kanonische Datenschutzerklärung. Sie enthält
keine neuen Mac-Bauinputs oder Laufzeitarchive. Der vorhandene technische
Paketierungsweg bleibt separat erhalten.

`capture_mac_drafts.py` verwendet die geprüfte `target/garnbox-1.0.jar`,
passende `target/classes`, die Abhängigkeiten aus dem vorhandenen App-Image
und `target/javafx-modules`. Nur der separate Aufnahmehelfer wird kompiliert.
Eine veraltete App-JAR im App-Image wird weder verwendet noch ersetzt.
Die Aufnahme läuft mit eigenen temporären Daten und eigenem Xvfb. Keine
Maven-Aufrufe, App-Builds oder automatisierten Tests für diese Inhaltslieferung.

```bash
python3 scripts/capture_mac_drafts.py --output target/mac-store-content-drafts-neu
python3 scripts/prepare_mac_handoff.py --content-only --captures target/mac-store-content-drafts-neu --output build/releases/mac-store-content/Yarn-Box-1.0-de-DE-neu
```

Bei Bedarf `YARNBOX_XVFB` auf das vorhandene Xvfb setzen. Vier Aufnahmen vor
Übergabe visuell prüfen, Quellen committen und danach den zweiten Befehl
ausführen. Jeder Ausgabeordner muss neu sein. Keine ZIP-Datei.

## Vor „Zur Prüfung hinzufügen“

Endgültige Mac-Bilder, ausgewählter Build, öffentliche Datenschutzseite,
Datenschutzantworten, Altersfreigabe, kostenloser Preis und Prüferkontakt müssen
vollständig sein. Berechtigungsangaben mit dem tatsächlich signierten Bundle
abgleichen. Die Vorbereitung verwendet keine temporären Sandbox-Ausnahmen.
Der Verschlüsselungsfragebogen muss sich auf den endgültigen Build einschließlich
der eingebetteten Laufzeit beziehen. Nicht allein aus „Offline-App“ ableiten,
dass keinerlei Kryptografie enthalten ist.

## Quellen

[Apple: Screenshot-Spezifikationen](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)

[Apple: Datenschutzangaben](https://developer.apple.com/app-store/app-privacy-details/)

[Apple: Builds hochladen](https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds/)
