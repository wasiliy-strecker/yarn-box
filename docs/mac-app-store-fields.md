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

Jetzt **Sichern** wählen. Bilder und Build folgen nach der Mac-Abnahme.
Die Prüfhinweise beschreiben den vorgesehenen finalen Mac-Build. Sie sind
kein Nachweis, dass dieser bereits ausgeführt wurde.

## App-Informationen

Name: `name.txt`. Untertitel: `subtitle.txt`. Primärsprache Deutsch.
Primäre Kategorie: Produktivität. Keine zweite Kategorie erforderlich.
Die Bundle-ID muss `de.appfabrik.garnbox` sein. Den bestehenden Eintrag
prüfen. Bei einer Abweichung anhalten und nicht eigenmächtig eine neue App oder
eine neue technische Kennung anlegen.

Die Antworten zur Altersfreigabe ergeben sich aus einer privaten lokalen
Garnsammlung. Kein öffentliches Teilen von Nutzerinhalten, Chat, Werbung,
Browser, Glücksspiel, Käufe oder medizinische Beratung vorhanden. Persönliche
Notizen sind keine öffentliche Inhaltsplattform. Den aktuellen Apple-Fragebogen
entsprechend den konkreten Fragen beantworten. Die Altersstufe berechnet Apple.

## App-Datenschutz

Für den vorgesehenen unveränderten Offline-Build werden keine Daten aus der
App an Entwickler oder Drittanbieter übertragen. Sammlung mit Fotos, Sprache und gemerkter Fotoordner liegen
lokal. Es gibt weder Tracking noch Analytics oder Werbe-SDKs. Dafür ist in Apples
Datenschutzfragebogen „Es werden keine Daten erfasst“ vorgesehen. Freiwillige
Support-E-Mails werden separat in der Datenschutzerklärung erläutert.

Die vollständige Erklärung liegt unter `Privacy/de-DE.txt` und `Privacy/index.html`.
Sie muss vor Einreichung öffentlich ohne Anmeldung erreichbar sein. Eine lokale
Datei oder ein noch nicht gepushter Repository-Pfad ist keine funktionierende
Datenschutz-URL. Eine allgemeine README ohne vollständige Erklärung reicht nicht.
Die aktuelle Übergabe führt die öffentliche Datenschutzadresse als **offen**.

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
`Screenshots-Final-Mac/de-DE` bleibt bis zu den nativen Aufnahmen leer.

Mac-Screenshots benötigen 16:10 und eine von Apple unterstützte Größe.
Vorbereitet ist **2560 × 1600**. Alle vier Bilder müssen dieselben Maße und keine Transparenz haben.
Ein Video ist für diese Einreichung nicht erforderlich.

Nach nativer Abnahme die fertige App signieren, als `.pkg` verpacken und mit
Transporter hochladen. Erst nach Apples Verarbeitung den Build in dieser Version
auswählen. Die Anleitung `AUF-DEM-MAC.md` führt durch diese Schritte.

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
