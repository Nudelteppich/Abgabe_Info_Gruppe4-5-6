const express = require('express'); // Import des Express-Framework für den Server
const bodyParser = require('body-parser'); // Middleware für JSON-Daten (Schnittstelle server.js und JSON Daten)
const fetch = (...args) => import('node-fetch').then(({ default: fetch }) => fetch(...args)); // Dynamischer Import für node-fetch
const puppeteer = require('puppeteer'); // Für Headless-Browser-Interaktion
const path = require('path'); // Für Dateipfade

const app = express(); // Express-App initialisieren
const port = 3000; // Server läuft auf Port 3000

// Middleware
app.use(bodyParser.json()); // JSON-Daten verarbeiten

// Route für HTML-Seite
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html')); //Abruf und Start der HTML Datei 
});

// Funktion für automatisiertes Login und Tokenabruf
const performLoginAndGetToken = async () => {
    console.log("Starte automatisierte Anmeldung bei Rapla...");

    const browser = await puppeteer.launch(); // Startet einen Headless-Browser
    const page = await browser.newPage(); // Öffnet eine neue Seite

    // Zu Rapla-Login-Seite navigieren
    await page.goto('http://localhost:8051/rapla/login');
    await page.type('input[name=username]', 'admin'); // Benutzername eintragen
    await page.type('input[name=password]', ''); // Passwort leer lassen
    await page.click('button[type=submit]'); // Login-Button klicken
    await page.waitForNavigation(); // Warte auf Weiterleitung

    const newUrl = page.url();
    const fragment = newUrl.split('#')[1];
    if (!fragment) { //Wenn .split nichts aus der gegebenen URL findet --> Fehler
        throw new Error('Login fehlgeschlagen: Kein Fragment mit Token gefunden.'); //Wirft Fehler (wird unten gefangen)
    }

    const rawToken = fragment.match(/raplaLoginToken=([^&]*)/)?.[1] || '';  //Extrahiert den Token 
    const token = decodeURIComponent(rawToken.replace(/\+/g, '%2B')); // Dekodiert Token
    const validUntil = new URLSearchParams(fragment).get('valid_until'); //Extrahiert Ablaufdatum

    await browser.close(); //Schließt Headlessbrowser
    if (!token) { //Wenn Extrahierung und Dekodierung fehlschlägt 
        throw new Error('Login fehlgeschlagen: Token konnte nicht extrahiert werden.');//Wirft Fehler (wird unten gefangen)
    }
    //Wenn Extrahierung und Dekodierung NICHT fehlschlägt
    console.log('Erfolgreich eingeloggt! Token:', token, 'Valid until:', validUntil);
    return { token, validUntil };
};


// POST-Route, um JSON von der HTML zu empfangen und an Rapla weiterzuleiten
app.post('/save', async (req, res) => {
    const jsonData = req.body;

    try {
        // Schritt 1: Login und Token abrufen
        const { token, validUntil } = await performLoginAndGetToken(); //Mit Token der davor von Headlessbrowser geholt wurde


        // Schritt 2: Daten an Rapla senden
        console.log("Sende Daten an Rapla...");
        const raplaResponse = await fetch('http://localhost:8051/rapla/events', { //Endpoint der Rapla API um Events anzulegen
            method: 'POST', // Methode um Daten an Rapla zu senden ohne diese in der URL zu "leaken"
            headers: {
                'Content-Type': 'application/json', //JSON Daten von HTML abrufen 
                Authorization: `Bearer ${token}`, // Token aus Login einfügen
            },
            body: JSON.stringify(jsonData), //POST-Methode --> JSON Daten werden über Body übetragen 
        });

        //Rapla sendet (falls Server ordnungsgemäß läuft und der Endpoint passt) eine Antwort bezüglich der gestellten Anfrage
        //Mit der Antwort kann auch Debugging durchgeführt werden da Rapla auch die Fehlermeldung mit schickt 
        // Antwort prüfen
        const contentType = raplaResponse.headers.get('content-type'); //Holt sich Rapla antwort  zur Content überprüfung 
        if (contentType && contentType.includes('application/json')) { //Checkt Content-Typ der Antwort (basically schaut ob er JSON als antwort bekommt)
            const raplaResult = await raplaResponse.json(); //Wartet dann auf vollständige Antwort in Form von "await xxx.js" (--> weil weis ja jetzt erst dass JSON ist)
            console.log("Rapla-Antwort (JSON):", raplaResult); //Sendet Rapla antwort als JSON
            res.send(`Daten erfolgreich in Rapla gespeichert: ${JSON.stringify(raplaResult)}`); //Druckt Rapla antwort im Temrinal sowie String dass Prozedere erfolgreich war 
        } else {
            const responseText = await raplaResponse.text(); //Falls keine JSON Datei empfangen wurde --> Fehler Meldung
            console.error("Rapla-Fehlerantwort (kein JSON):", responseText); //Sendet Fehlermeldung aka. Rapla Antwort 
            res.status(500).send(`Fehler vom Rapla-Server: ${responseText}`); //Druckt Fehlermeldung im Temrinal 
        }
    } catch (error) { //Fängt die Fehelr von OBben 
        console.error('Fehler beim Weiterleiten an den Rapla-Server:', error.message); //Ausgabe in der Browser Console 
        res.status(500).send(`Fehler beim Login oder Weiterleitung: ${error.message}`); //Ausgabe am Server Terminal 
    }
});

// Server starten
app.listen(port, () => { //Startet Server auf Port von obene (3000)
    console.log(`Server läuft auf http://localhost:${port}`);
});
