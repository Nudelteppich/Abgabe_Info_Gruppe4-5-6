const express = require('express');
const bodyParser = require('body-parser');
const fetch = (...args) => import('node-fetch').then(({ default: fetch }) => fetch(...args));
const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');
const app = express();
const port = 3000;
const nodemailer = require('nodemailer');
const cors = require('cors');




// Middleware
app.use(bodyParser.json());
app.use(express.static('public')); // Statische Dateien aus dem 'public' Verzeichnis bereitstellen
app.use(cors());

const users = {};

const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: 'raplaserverpasswort@gmail.com', // Deine E-Mail-Adresse
        pass: 'mkrf bjqk lxfp soxr' // Dein E-Mail-Passwort oder App-Passwort
    }
});

// Route für die Dozenten-Anwesenheitsplanung
app.get('/dozenten_anwesenheitsplan', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'dozenten_anwesenheitsplan.html'));
});

// Route für die Semesterverwaltung
app.get('/semester', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'admin.html'));
});

// Standard-Route, die auf die Dozenten-Anwesenheitsplanung verweist
app.get('/', (req, res) => {
    res.redirect('/semester'); // Umleitung zur Dozenten-Anwesenheitsplanung
});

let dozentenLinks={};
const loadLinks=()=>{
    const filePath = path.join(__dirname, 'dozentenLinks.json');
    try {
        if (fs.existsSync(filePath)) {
            const data = fs.readFileSync(filePath, 'utf8');
            if (data) {
                dozentenLinks = JSON.parse(data);
            } else {
                dozentenLinks = {};
            }
        } else {
            dozentenLinks = {};
        }
    } catch (error) {
        console.error("Fehler beim Laden der Links:", error);
        dozentenLinks = {};
    }
};
// Funktion zum Speichern der Links in einer Datei
const saveLinks = () => {
    const filePath = path.join(__dirname, 'dozentenLinks.json');
    fs.writeFileSync(filePath, JSON.stringify(dozentenLinks, null, 2));
};

// Laden der Links beim Start des Servers
loadLinks();

// Funktion zur automatisierten Anmeldung bei Rapla, die die Anmeldedaten akzeptiert
const performLoginAndGetToken = async (username, password) => {
    console.log("Starte automatisierte Anmeldung bei Rapla...");
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    const username1 = "admin";
    const password1 ="";
    await page.goto('http://localhost:8051/rapla/login');
    await page.type('input[name=username]', username1); // Benutzername verwenden
    await page.type('input[name=password]', password1); // Passwort verwenden
    await page.click('button[type=submit]');
    await page.waitForNavigation();
    const newUrl = page.url();
    console.log("Weitergeleitete URL:", newUrl);
    const fragment = newUrl.split('#')[1];
    if (!fragment) {
        throw new Error('Login fehlgeschlagen: Kein Fragment mit Token gefunden.');
    }
    const urlParams = new URLSearchParams(fragment);
    const rawToken = fragment.match(/raplaLoginToken=([^&]*)/)?.[1] || '';
    const token = decodeURIComponent(rawToken.replace(/\+/g, '%2B'));
    const validUntil = urlParams.get('valid_until');
    await browser.close();
    if (!token) {
        throw new Error('Login fehlgeschlagen: Token konnte nicht extrahiert werden.');
    }
    console.log('Erfolgreich eingeloggt! Token:', token, 'Valid until:', validUntil);
    return { token, validUntil };
};

// POST-Route, um Anwesenheitsdaten zu empfangen und an Rapla weiterzuleiten
app.post('/save', async (req, res) => {
    const { dozent, password, preferences } = req.body; // Empfangene Daten extrahieren
    console.log("Empfangene Daten:", { dozent, password, preferences }); // Debugging-Log

    try {
        const { token, validUntil } = await performLoginAndGetToken(dozent, password);
        console.log("Sende Daten an Rapla...");

        let successCount = 0; // Zähler für erfolgreiche Sendungen
        const maxAttempts = 5; // Maximale Anzahl der Versuche
        const delay = 2000; // Verzögerung in Millisekunden

        for (const pref of preferences) {
            const jsonContent = {
                classification: {
                    type: "verfügbar",
                    data:{
                        name: [dozent]
                    }
                },
                appointments: [{
                    start: `${pref.date}T${pref.slot.split(' - ')[0]}:00Z`,
                    end: `${pref.date}T${pref.slot.split(' - ')[1]}:00Z`
                }],
                links: 
                    { resources:[password] }  // Hier wird links als Array von Objekten definiert
                
            };
            

            console.log("Sende Präferenz:", jsonContent); // Debugging-Log für jede Präferenz

            let success = false;
            let attempts = 0;

            while (!success && attempts < maxAttempts) {
                try {
                    const raplaResponse = await fetch('http://localhost:8051/rapla/events', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            Authorization: `Bearer ${token}`,
                        },
                        body: JSON.stringify(jsonContent), // Hier wird das formatierte Präferenzobjekt gesendet
                    });

                    const contentType = raplaResponse.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        const raplaResult = await raplaResponse.json();
                        console.log("Rapla-Antwort (JSON):", raplaResult);
                        success = true; // Erfolgreich gesendet
                        successCount++; // Erfolgreiche Sendung zählen
                    } else {
                        const responseText = await raplaResponse.text();
                        console.error("Rapla-Fehlerantwort (kein JSON):", responseText);
                        attempts++;
                        if (attempts < maxAttempts) {
                            console.log(`Versuch ${attempts} fehlgeschlagen, erneut versuchen...`);
                            await new Promise(resolve => setTimeout(resolve, delay)); // Verzögerung vor dem nächsten Versuch
                        } else {
                            console.error(`Fehler vom Rapla-Server: ${responseText}`);
                        }
                    }
                } catch (error) {
                    console.error("Fehler beim Senden an Rapla:", error.message);
                    attempts++;
                    if (attempts < maxAttempts) {
                        console.log(`Versuch ${attempts} fehlgeschlagen, erneut versuchen...`);
                        await new Promise(resolve => setTimeout(resolve, delay)); // Verzögerung vor dem nächsten Versuch
                    } else {
                        console.error(`Fehler beim Login oder Weiterleitung: ${error.message}`);
                    }
                }
            }
        }

        return res.send(`Daten erfolgreich in Rapla gespeichert: ${successCount} von ${preferences.length} gesendet.`); // Erfolgreiche Sendungen zurückgeben
    } catch (error) {
        console.error('Fehler beim Weiterleiten an den Rapla-Server:', error.message);
        return res.status(500).send(`Fehler beim Login oder Weiterleitung: ${error.message}`); // Verwenden Sie return
    }
});

app.post('/api/savedata', (req, res) => {
    const semesterDaten = req.body; // Empfangene Daten

    // Pfad zur Datei, in der die Daten gespeichert werden
    const filePath = path.join(__dirname, 'semesterDaten.json');
    const dirPath = path.dirname(filePath);

    // Verzeichnis erstellen, falls es nicht existiert
    if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
    }

    // Daten in die Datei schreiben
    fs.writeFile(filePath, JSON.stringify(semesterDaten, null, 2), (err) => {
        if (err) {
            console.error('Fehler beim Speichern der Daten:', err);
            return res.status(500).send('Fehler beim Speichern der Daten.');
        }
        console.log('Daten erfolgreich gespeichert:', semesterDaten);
        res.json({ message: 'Daten erfolgreich gespeichert.' });
    });
});



// Route zum Hinzufügen eines Dozenten mit Link
app.post('/api/addDozent', (req, res) => {
    const { dozent, password, validUntil } = req.body;
    const name = dozent +"!123@SIM:Möller+N.Blessing69&!55"+password;
    const uniqueKey = `${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
    const link = `dozenten_anwesenheitsplan.html?key=${encodeURIComponent(uniqueKey)}`;
    
    dozentenLinks[uniqueKey] = { link, validUntil, name};
    saveLinks();
    
    res.send({ link, validUntil, uniqueKey});
});


// Route zum Ungültigmachen des Links
app.post('/api/invalidateLink', (req, res) => {
    const {uniqueKey} = req.body;
    //const key = dozent + password;

    if (dozentenLinks[uniqueKey]) {
        delete dozentenLinks[uniqueKey]; // Link löschen
        saveLinks(); // Änderungen speichern
        res.send({ success: true, message: 'Link wurde ungültig gemacht.' });
    } else {
        res.send({ success: false, message: 'Dozent nicht gefunden oder Link bereits ungültig.' });
    }
});

// Route zum Anfordern eines Passworts
app.post('/semester/request-password', (req, res) => {
    const email = req.body.email;

    // Überprüfen, ob der Benutzer bereits existiert
    if (!users[email]) {
        const newPassword = Math.random().toString(36).slice(-8); // Generiere ein 8-stelliges Passwort
        users[email] = newPassword; // Speichere das Passwort für den Benutzer

        const mailOptions = {
            from: 'raplaserverpasswort@gmail.com',
            to: email,
            subject: 'Dein neues Passwort',
            text: `Hier ist dein neues Passwort: ${newPassword}.`
        };

        transporter.sendMail(mailOptions, (error, info) => {
            if (error) {
                console.error('Fehler beim Senden der E-Mail:', error); // Protokolliere den Fehler
                return res.status(500).send('Fehler beim Senden der E-Mail: ' + error.toString());
            }
            res.status(200).send('E-Mail gesendet: ' + info.response);
        });
        
    } else {
        res.status(400).send('Benutzer existiert bereits. Bitte logge dich ein.');
    }
});

app.post('/api/login', (req, res) => {
    const { email, password } = req.body; // Empfangene Anmeldedaten
    
    console.log("Login-Anfrage empfangen:", { email });
    
    // E-Mail-Format validieren (optional)
    if (!email.endsWith("@student.dhbw-heidenheim.de")) {
        return res.status(400).json({
            success: false,
            message: 'Bitte verwenden Sie eine gültige DHBW-Studentenadresse.'
        });
    }
    
    // Überprüfen der Anmeldedaten
    if (users[email] && users[email] === password) {
        console.log('Login erfolgreich für:', email);
        
        // Erfolgreiche Anmeldung
        return res.json({ 
            success: true, 
            message: 'Login erfolgreich.',
            user: { email }
        });
    } else {
        console.log('Login fehlgeschlagen für:', email);
        
        // Fehlgeschlagene Anmeldung
        return res.status(401).json({ 
            success: false, 
            message: 'Ungültige E-Mail oder Passwort.'
        });
    }
});



// Route zur Überprüfung der Gültigkeit des Links
app.get('/api/checkLink/:uniqueKey', (req, res) => {
    const { uniqueKey } = req.params;
    const entry = dozentenLinks[uniqueKey];

    if (entry) {
        const now = new Date();
        const validUntil = new Date(entry.validUntil);
        

        if (now < validUntil) {
            const name = entry.name;
            res.send({ valid: true, link: entry.link, name});
        } else {
            delete dozentenLinks[uniqueKey]; // Link löschen, wenn abgelaufen
            saveLinks();
            res.send({ valid: false, message: 'Link ist abgelaufen.' });
        }
    } else {
        res.send({ valid: false, message: 'Dozent nicht gefunden.' });
    }
});

// Route zum Abrufen der gespeicherten Semesterdaten
app.get('/api/getSemesterData', (req, res) => {
    const filePath = path.join(__dirname, 'semesterDaten.json');

    // Überprüfen, ob die Datei existiert
    if (fs.existsSync(filePath)) {
        const data = fs.readFileSync(filePath, 'utf8');
        const semesterDaten = JSON.parse(data);
        res.json(semesterDaten);
    } else {
        res.status(404).send('Keine Semesterdaten gefunden.');
    }
});

// Server starten
app.listen(port, () => {
    console.log(`Server läuft auf http://localhost:${port}`);
});