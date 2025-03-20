package org.rapla.plugin.exampleeditmenu;

import org.rapla.RaplaResources;
import org.rapla.client.RaplaWidget;
import org.rapla.client.dialog.DialogUiFactoryInterface;
import org.rapla.client.swing.RaplaGUIComponent;
import org.rapla.client.swing.images.RaplaImages;
import org.rapla.components.calendar.DateRenderer;
import org.rapla.components.iolayer.IOInterface;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.dynamictype.internal.DynamicTypeImpl;
import org.rapla.facade.CalendarModel;
import org.rapla.facade.client.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaInitializationException;
import org.rapla.framework.RaplaLocale;
import org.rapla.logger.Logger;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class ExampleEditMenuDialog extends RaplaGUIComponent implements RaplaWidget {
    RaplaLocale locale;

    JPanel panel = new JPanel();

    //Eingabefelder für Vor- und Nachname 
    private JTextField vornameField = new JTextField(20);
    private JTextField nachnameField = new JTextField(20);
    //Button für Hinzufügebutton(addButton) von Namen in Liste und für alle Namen anlegen(globaladdButton)
    private JButton addButton;
    private JButton globalAddButton;


    // Panel für die Name-Liste mit Lösch-Buttons
    private JPanel namesListPanel;
    private List<NameEntry> nameEntries = new ArrayList<>();

    ExampleEditMenuResources editMenuI18n;
    private final CalendarModel model;
    private final RaplaImages raplaImages;

    @Inject
    public ExampleEditMenuDialog(ClientFacade facade,
                                 RaplaResources i18n,
                                 RaplaLocale raplaLocale,
                                 Logger logger,
                                 ExampleEditMenuResources editMenuI18n,
                                 CalendarModel model,
                                 DateRenderer dateRenderer,
                                 DialogUiFactoryInterface dialogUiFactory,
                                 IOInterface ioInterface,
                                 RaplaImages raplaImages) throws RaplaInitializationException {
        super(facade, i18n, raplaLocale, logger);
        this.editMenuI18n = editMenuI18n;
        this.model = model;
        this.raplaImages = raplaImages;
        locale = getRaplaLocale();

        try {
            Period[] periods = getFacade().getPeriods();
        } catch (RaplaException e1) {
            throw new RaplaInitializationException(e1);
        }

        // Initialisiere und konfiguriere das Panel
        initPanel();
    }

    private void initPanel() {
        panel.setLayout(new BorderLayout(10, 10));

        // Oberes Panel für Eingabefelder
        JPanel inputPanel = new JPanel();
        double[][] inputSizes = {
                {TableLayout.PREFERRED, 5, TableLayout.FILL, 5, TableLayout.PREFERRED},
                {TableLayout.PREFERRED, 5, TableLayout.PREFERRED}
        };
        inputPanel.setLayout(new TableLayout(inputSizes));

        // Füge Beschriftungen und Textfelder hinzu
        inputPanel.add(new JLabel(editMenuI18n.getString("first_name") + ":"), "0,0");
        inputPanel.add(vornameField, "2,0");
        inputPanel.add(new JLabel(editMenuI18n.getString("last_name") + ":"), "0,2");
        inputPanel.add(nachnameField, "2,2");
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));

        // Hinzufügen-Button
        addButton = new JButton(editMenuI18n.getString("add"));
        addButton.setPreferredSize(new Dimension(100, 35));
        addButton.setMaximumSize(new Dimension(100, 35));
        addButton.setMinimumSize(new Dimension(100, 35));
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addName();
            }
        });
        inputPanel.add(addButton, "4,1");

        // Panel für die Liste der Namen
        namesListPanel = new JPanel();
        namesListPanel.setLayout(new BoxLayout(namesListPanel, BoxLayout.Y_AXIS));
        namesListPanel.setBorder(BorderFactory.createTitledBorder(editMenuI18n.getString("added_names")));

        // Erstelle einen globalen "Hinzufügen"-Button für die Liste
        globalAddButton = new JButton(editMenuI18n.getString("full_add"));
        globalAddButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Hier werden die eigentlichen Personen in Rapla erzeugt
                createRaplaPersons();
            }
        });

        // ScrollPane für die Namen-Liste
        JScrollPane scrollPane = new JScrollPane(namesListPanel);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        // Panel für den Button unterhalb der Liste erstellen
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(globalAddButton);

        // Container für Liste und Button
        JPanel listWithButtonPanel = new JPanel(new BorderLayout());
        listWithButtonPanel.add(scrollPane, BorderLayout.CENTER);
        listWithButtonPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Füge alles zum Hauptpanel hinzu
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(listWithButtonPanel, BorderLayout.CENTER);

        // Setze einen Rahmen für das Panel
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void addName() {
        String vorname = vornameField.getText().trim();
        String nachname = nachnameField.getText().trim();

        if (!vorname.isEmpty() && !nachname.isEmpty()) {
            NameEntry entry = new NameEntry(vorname, nachname);
            nameEntries.add(entry);
            updateNamesList();

            // Felder leeren
            vornameField.setText("");
            nachnameField.setText("");
            vornameField.requestFocus();
        } else {
            JOptionPane.showMessageDialog(panel,
                    editMenuI18n.getString("please_enter_name"),
                    editMenuI18n.getString("input_error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateNamesList() {
        namesListPanel.removeAll();

        for (NameEntry entry : nameEntries) {
            namesListPanel.add(createNameEntryPanel(entry));
        }

        namesListPanel.revalidate();
        namesListPanel.repaint();
    }

    private JPanel createNameEntryPanel(final NameEntry entry) {
        JPanel entryPanel = new JPanel(new BorderLayout(5, 0));
        entryPanel.setBorder(new EmptyBorder(3, 3, 3, 3));

        // Name-Label
        JLabel nameLabel = new JLabel(entry.getVorname() + " " + entry.getNachname());
        entryPanel.add(nameLabel, BorderLayout.CENTER);

        // Lösch-Button
        JButton deleteButton = new JButton("Entfernen");
        deleteButton.setBorderPainted(true);
        deleteButton.setContentAreaFilled(true);
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nameEntries.remove(entry);
                updateNamesList();
            }
        });
        entryPanel.add(deleteButton, BorderLayout.EAST);

        return entryPanel;
    }

    private void createRaplaPersons() {
        try {
            DynamicType lecturersType = null, neweventType = null;

            try {
                // Versuche den bestehenden DynamicType 'lecturers' zu holen
                lecturersType = getFacade().getDynamicType("lecturers");
                System.out.println("DynamicType 'lecturers' erfolgreich gefunden.");
                }
            
            
                catch (RaplaException ex) {
                // Falls der Typ nicht existiert, wird eine RaplaException geworfen
                System.out.println("'lecturers' nicht gefunden, versuche neuen Typ zu erstellen...");

                // Automatisch einen neuen Personentyp 'lecturers' anlegen
                DynamicTypeImpl dozentenType = (DynamicTypeImpl) getFacade().newDynamicType(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_PERSON);
                dozentenType.setKey("lecturers");
                dozentenType.getName().setName(getLocale().getLanguage(), "Dozenten");

                // Speichern des neu angelegten Typs
                System.out.println("Speichere den neuen DynamicType 'lecturers'...");
                getFacade().store(dozentenType);
                lecturersType = dozentenType;

                JOptionPane.showMessageDialog(panel,
                        "Personentyp 'lecturers' (Dozenten) wurde neu angelegt. Bitte 'Dozenten anlegen' wiederholen",
                        "Neuer Typ angelegt",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            
            
            try {
                // Versuche den bestehenden DynamicType 'verfügbar' zu holen
                neweventType = getFacade().getDynamicType("verfügbar");
                System.out.println("DynamicType 'Verfügbarkeit' erfolgreich gefunden.");    
                }
            
                catch (RaplaException ex) {
                // Falls der Typ nicht existiert, wird eine RaplaException geworfen
                System.out.println("'Verfügbarkeit' nicht gefunden, versuche neuen Typ zu erstellen...");

                // Automatisch einen neuen Eventtyp 'verfügbar' anlegen
                DynamicTypeImpl newavailableType = (DynamicTypeImpl) getFacade().newDynamicType(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);
                newavailableType.setKey("available_timeslot");
                newavailableType.getName().setName(getLocale().getLanguage(), "Verfügbarkeit");

                // Speichern des neu angelegten Typs
                System.out.println("Speichere den neuen DynamicType 'Verfügbarkeit'...");
                getFacade().store(newavailableType);
                neweventType = newavailableType;

                JOptionPane.showMessageDialog(panel,
                        "Eventtyp 'Verfügbarkeit' wurde neu angelegt",
                        "Neuer Typ angelegt",
                        JOptionPane.INFORMATION_MESSAGE);
                }
          

            List<Allocatable> newAllocatables = new ArrayList<>();
            for (NameEntry entry : nameEntries) {
                // Zuerst die Klassifikation aus dem Typ erstellen
                Classification classification = lecturersType.newClassification();

                // Allocatable über die Facade erstellen
                Allocatable person = getFacade().newAllocatable(classification, getUser());

                // Vor- / Nachname zuweisen
                classification.setValue("firstname", entry.getVorname());
                classification.setValue("surname", entry.getNachname());

                // Füge das neue Allocatable in die Liste der zu speichernden Personen
                newAllocatables.add(person);
                }

            // Falls keine Einträge vorhanden sind, abbrechen
            if (newAllocatables.isEmpty()) {
                JOptionPane.showMessageDialog(panel,
                        "Keine Namen in der Liste vorhanden!",
                        "Info",
                        JOptionPane.INFORMATION_MESSAGE);
               return;
               }

            // Speichere die neuen Personen in Rapla
            System.out.println("Speichere neue Personen in Rapla...");
            getFacade().storeObjects(newAllocatables.toArray(new Allocatable[newAllocatables.size()]));

            JOptionPane.showMessageDialog(panel,
                    "Es wurden " + newAllocatables.size() + " Personen erstellt und gespeichert.",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE);

            // Optional: Liste leeren und GUI aktualisieren
            nameEntries.clear();
            updateNamesList();
            
            showSummaryDialog(newAllocatables);
            }
        
        
            catch (RaplaException ex) {
            ex.printStackTrace(); // Stacktrace ausgeben
            JOptionPane.showMessageDialog(panel,
                    "Ordner Dozenten existiert nicht: " + ex.getMessage(),
                    "Personentyp nicht gefunden",
                    JOptionPane.ERROR_MESSAGE);
                   }
     }

    private void showSummaryDialog(List<Allocatable> newAllocatables) {
        StringBuilder message = new StringBuilder("Folgende Personen wurden hinzugefügt:\n\n");
        for (Allocatable person : newAllocatables) {
            message.append(person.getClassification().getValue("firstname"))
                   .append(" ")
                   .append(person.getClassification().getValue("surname"))
                   .append(" - ID: ")
                   .append(person.getId())
                   .append("\n");
        }

        // Erstellen und Anzeigen des Dialogs
        JOptionPane.showMessageDialog(panel,
                message.toString(),
                "Zusammenfassung der hinzugefügten Personen",
                JOptionPane.INFORMATION_MESSAGE);
    }
    @Override
    public JComponent getComponent() {
        return panel;
    }

    // Getter für die Liste der Namen
    public List<NameEntry> getNameEntries() {
        return new ArrayList<>(nameEntries);
    }
    
    public JButton getMainActionButton() {
        return globalAddButton; // Gibt den "Personen erstellen"-Button zurück
    }


    // Innere Klasse für einen Namen-Eintrag
    public static class NameEntry {
        private String vorname;
        private String nachname;

        public NameEntry(String vorname, String nachname) {
            this.vorname = vorname;
            this.nachname = nachname;
        }

        public String getVorname() {
            return vorname;
        }

        public String getNachname() {
            return nachname;
        }

        @Override
        public String toString() {
            return vorname + " " + nachname;
        }
    }
}

/*-----------------------------------------------------------------------------*
| No Copyright© 2025 Lucas Kling, Luis Kölbel, Lukas Steiner, Elias Reißmüller,|
| Tim Peteler                                                                  |
| This code was created and developed by the above-mentioned individuals.      |
| In certain sections, various AI models were used.                            |
| The code can be modified and used according to the terms of the unattached   | 
| and non-existent license. The code is not subject to copyright protection.   |
| Note: This plugin code is not associated with Rapla and was developed        |  
| independently.                                                               |
|                                                                              |
|                                                                              |                      
|			                         .-" `.                                    |
|			                         ;:":  ""--..                              | 
|			                .-+. ,gpd$L\:._      ""-._                         |
|			               /  //;$SS$$$$SS$$t--.      "-._                     |
|			             .'  `.//SS$P^"""TS$$S. "-.       "-,                  |
|			           .'    _ "-S^"      TS$$Sb   "-.       `.                |
|			         .'    .':S$Y      _.. SS$$Sb-'   "-.      ;               |
|			       .'    .'  SS$;,=-.  ._.`:S$$SS;       j     ;               |
|			     .'    .'   :SS$$.-'        SS$$SS\     /     /                |
|			   .'     /     SS$$S;    -     SS$$SS ;   /     /                 |
|			 .'      /   ._dSS$$SS   __ .  :SS$$$S\;  /     /                  |
|			/       /     :SS$$SS$b. `--'  $$SS$$S ) /     /                   |
|			\      :      ;SS$$SS$$SS.___.'$$SS$$Sb /     /                    |
|			 \      "-.   SS$$SS$$$SS      $$SS$$SS';    /                     |
|			  `.       "-dSS$$SS$$SS:;     :$$SSSP      /                      |
|			    `.              "^S^':     '^TSS'      /                       |
|			      "-.      `.     :/-.   _ .-"\\      /                        |
|			         "-.  -._\    //           \\  : :                         |
|			            "-.   \  //             \\ ; ;                         |
|			               $.  `./       ,      \\;:                           |
|			              dS$\    '-._    :  _.-"" \;                          |
|			             :S$^$t'      ""--:""       ;                          |
|			              TPS:$$ ;        ::        :                          | 
|			              :Sd$S$_:        ;-\       ;                          |
|			               :$SS$; `.____.'   `.___.j                           |
|			               $$SS$$                  ;                           |
|			                T$S$$;  ;      ;    ; :                            | 
|			                 `TS$$  :      :    : ;                            |
|			                   `T$         :     :                             |
|			                     ;         ;     ;                             |
|			                    /                ;                             |
|			                  .'                 :                             |  
|			                 /      :           ;:                             |
|			                /       ;     c     ::                             |
|			              : `.      ;           : ;                            |
|			             ;.   "-.   :           ; :                            |
|			            :  ".    "-.            .' ;                           |
|			            ;    -.     ""--..__..-"   :                           |    
|			           :       `.                _.-;                          |
|			          ;          `.           .-"   ;                          |
|			          ;            `.       .'      ;                          |
|			          ;              \     /        :                          |
|			          ;               \   /         :                          |
|			           ;               \ /          :                          |
|			           ;               :^:           :                         |
|			            ;               :;           ;                         | 
|			            :               :;           ;                         |
|			             ;              ;;          :                          |
|			              |             ;;          |                          |
|			              :            : ;          ;                          |
|			               ;           : ;         :                           |
|			               :           ; :         |                           |
|			                ;          ; :         ;                           |
|			                :          ; :        :                            |
|			                 ;         ; :        |                            |
|			                 :         ; :        ;                            |
|			                  ;        : :        :                            |
|			                  : ;   : :  ;  ;     |                            |
|			                   ;:   ; ;  :  :     :                            |
|			                   : \    ;   \       "-                           |
|			                   :      ;    \        \                          |
|			                   ;      :     \      .d$b                        |
|			                  db.___.d$b     \__.g$$$$$b                       |
|			                  $$$$$$$$$$     :$$$$$$$$$$b                      |
|			                  $$$$$$$$$$      T$$$$$$$$$$;                     | 
|			                  :$$$$$$$$$       T$$$$$$$$$$                     |
|			                   $$$$$$$$$        `T$$$$$$$$b                    |
|			                   $$$$$$$$;          T$$$$$$$$;                   |
|			                   :$$$$$$$            T$$$$$$$$                   |   
|			                   :$$$$$$$             T$$$$$$$;                  |
|			                    $$$$$$$              T$$$$$$$                  |
|			                    $$$$$$$               T$$$$$$;                 |
|			                    $$$$$$$                T$$$$$$                 |
|			                    :$$$$$;                 T$$$$$b                |
|			                    :$$$$$;                  T$$$S$b.              |
|			                    :$$$$S;                   SSS$$$$bp.           |
|			                    :$$$$S;                   :S$$$$$S$$;          |
|			                    $S$$SS;                    S$$$$$$SP           |
|			                   :$SSSSS;                    :$$$$$$S            |
|			                   $$$$$$$;                     $$$$$$$            |
|			                   $$$$$$$$                     :$$SS$$            |
|			                   $$$$$$$$                      SSS$$$            |
|			                   $$$$$$$$                      :$$$$;            |
|			                   :$$$$SS;                       `^^'             |
|			                    TSSSSP                                         |
|			                     `^^'                                          |
|		                                                                       |	 
 *----------------------------------------------------------------------------*/

