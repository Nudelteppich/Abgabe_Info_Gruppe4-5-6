package org.rapla.plugin.exampleeditmenu;

import org.rapla.RaplaResources;
import org.rapla.client.PopupContext;
import org.rapla.client.dialog.DialogInterface;
import org.rapla.client.dialog.DialogUiFactoryInterface;
import org.rapla.client.extensionpoints.EditMenuExtension;
import org.rapla.client.internal.SaveUndo;
import org.rapla.client.swing.RaplaGUIComponent;
import org.rapla.client.swing.images.RaplaImages;
import org.rapla.client.swing.toolkit.RaplaMenuItem;
import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationStartComparator;
import org.rapla.facade.RaplaFacade;
import org.rapla.facade.client.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.inject.Extension;
import org.rapla.logger.Logger;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.ResolvedPromise;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.SwingUtilities;


@Extension(provides = EditMenuExtension.class,id=ExampleEditMenu.PLUGIN_ID)
public class ExampleEditMenu  extends RaplaGUIComponent implements EditMenuExtension, ActionListener
{
	public static final String PLUGIN_ID ="org.rapla.plugin.exampleeditmenu";
	
	RaplaMenuItem item;
	String id = "editMenuEntry";
	final String label ;

	private boolean enabled = true;
    private final ExampleEditMenuResources editMenuI18n;
    private final Provider<ExampleEditMenuDialog> copyDialogProvider;
    private final DialogUiFactoryInterface dialogUiFactory;
	@Inject
    public ExampleEditMenu(ClientFacade facade, RaplaResources i18n, RaplaLocale raplaLocale, Logger logger, ExampleEditMenuResources editMenuI18n, Provider<ExampleEditMenuDialog> copyDialogProvider,  DialogUiFactoryInterface dialogUiFactory)  {
        super(facade, i18n, raplaLocale, logger);
        this.editMenuI18n = editMenuI18n;
        this.copyDialogProvider = copyDialogProvider;
        this.dialogUiFactory = dialogUiFactory;

        label = editMenuI18n.getString(id) ;
		item = new RaplaMenuItem(id);
		
        item.setText( label );
        item.setIcon( RaplaImages.getIcon(i18n.getIcon("icon.copy") ));
        item.addActionListener(this);
    }


    @Override
	public String getId() {
		return id;
	}


	@Override
	public JMenuItem getComponent() {
		return item;
	}
    
     
	public void actionPerformed(ActionEvent evt) {
	    // Beim Klick auf den Menüeintrag wird der Dialog geöffnet.
	    
	    PopupContext popupContext = dialogUiFactory.createPopupContext(null);
	    final ExampleEditMenuDialog useCase = copyDialogProvider.get();
	    String[] buttons = new String[]{getString("cancel")};
	    final JComponent component = useCase.getComponent();
	    component.setSize(600, 500);
	    final DialogInterface dialog = dialogUiFactory.createContentDialog(popupContext, component, buttons);
	    dialog.setTitle(label);
	    dialog.getAction(0).setIcon(i18n.getIcon("icon.abort"));
	    
	    // Einen Button aus dem Dialog fokussieren
	    JButton customButton = useCase.getMainActionButton(); // Diese Methode wurde neu in Dialog implementieren
	    if (customButton != null) {
	        SwingUtilities.invokeLater(() -> {
	            customButton.requestFocusInWindow();
	        });
	    }
	    
	    dialog.start(true).thenCompose(index -> {
	        // Ergebnisse verarbeiten
	        return ResolvedPromise.VOID_PROMISE;
	    }).exceptionally(ex -> dialogUiFactory.showException(ex, popupContext));
	}
    
	@Override
	public boolean isEnabled()
	{
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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