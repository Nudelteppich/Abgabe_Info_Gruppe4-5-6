const express = require('express'); // Express für den Server
const bodyParser = require('body-parser'); // Middleware für JSON-Daten
const fetch = (...args) => import('node-fetch').then(({ default: fetch }) => fetch(...args)); // Dynamischer Import für node-fetch
const puppeteer = require('puppeteer'); // Für Headless-Browser-Interaktion
const path = require('path'); // Für Dateipfade

const app = express(); // Express-App initialisieren
const port = 3000; // Server läuft auf Port 3000

// Middleware
app.use(bodyParser.json()); // JSON-Daten verarbeiten

// Route für HTML-Seite
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

const performLoginAndGetToken = async () => {
    console.log("Starte automatisierte Anmeldung bei Rapla...");

    // Puppeteer Browser starten
    const browser = await puppeteer.launch(); // Startet einen Headless-Browser
    const page = await browser.newPage(); // Öffnet eine neue Seite

    // Zu Rapla-Login-Seite navigieren
    await page.goto('http://localhost:8051/rapla/login');

    // Eingabefelder ausfüllen
    await page.type('input[name=username]', 'lucas.kling'); // Benutzername eintragen
    await page.type('input[name=password]', 'Test123'); // Passwort leer lassen

    // Login-Button klicken
    await page.click('button[type=submit]');

    // Warte auf Weiterleitung (z. B. URL mit Token)
    await page.waitForNavigation();

    // Extrahieren des Tokens aus der Weiterleitungs-URL
    const newUrl = page.url();
    console.log("Weitergeleitete URL:", newUrl);

    // Alles nach dem `#` extrahieren (Fragment in der URL)
    const fragment = newUrl.split('#')[1];
    if (!fragment) {
        throw new Error('Login fehlgeschlagen: Kein Fragment mit Token gefunden.');
    }

    // Token und valid_until aus URL extrahieren
    const urlParams = new URLSearchParams(fragment);

    // + vor Decodierung zu %2B konvertieren (nur wenn nötig)
    const rawToken = fragment.match(/raplaLoginToken=([^&]*)/)?.[1] || '';
    const token = decodeURIComponent(rawToken.replace(/\+/g, '%2B'));

    const validUntil = urlParams.get('valid_until');

    // Puppeteer Browser schließen
    await browser.close();

    if (!token) {
        throw new Error('Login fehlgeschlagen: Token konnte nicht extrahiert werden.');
    }

    console.log('Erfolgreich eingeloggt! Token:', token, 'Valid until:', validUntil);
    return { token, validUntil };
};

// POST-Route, um JSON zu empfangen und an Rapla weiterzuleiten
app.post('/save', async (req, res) => {
    const jsonData = req.body;

    try {
        // Schritt 1: Login und Token abrufen
        const { token, validUntil } = await performLoginAndGetToken();

        // Schritt 2: Daten an Rapla senden
        console.log("Sende Daten an Rapla...");
        const raplaResponse = await fetch('http://localhost:8051/rapla/events', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`, // Token aus Login einfügen
            },
            body: JSON.stringify(jsonData),
        });

        // Antwort prüfen
        const contentType = raplaResponse.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const raplaResult = await raplaResponse.json();
            console.log("Rapla-Antwort (JSON):", raplaResult);
            res.send(`Daten erfolgreich in Rapla gespeichert: ${JSON.stringify(raplaResult)}`);
        } else {
            const responseText = await raplaResponse.text();
            console.error("Rapla-Fehlerantwort (kein JSON):", responseText);
            res.status(500).send(`Fehler vom Rapla-Server: ${responseText}`);
        }
    } catch (error) {
        console.error('Fehler beim Weiterleiten an den Rapla-Server:', error.message);
        res.status(500).send(`Fehler beim Login oder Weiterleitung: ${error.message}`);
    }
});

// Server starten
app.listen(port, () => {
    console.log(`Server läuft auf http://localhost:${port}`);
});