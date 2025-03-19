package org.rapla.plugin.exampleeditmenu;

import org.jetbrains.annotations.PropertyKey;
import org.rapla.components.i18n.AbstractBundle;
import org.rapla.components.i18n.BundleManager;
import org.rapla.components.i18n.I18nBundle;
import org.rapla.inject.Extension;

import javax.inject.Inject;

@Extension(provides = I18nBundle.class, id = ExampleEditMenuResources.BUNDLENAME)
public class ExampleEditMenuResources extends AbstractBundle
{
    
    public static final String BUNDLENAME = ExampleEditMenu.PLUGIN_ID + ".ExampleEditMenuResources";

    @Inject
    public ExampleEditMenuResources( BundleManager bundleManager)
    {
        super(BUNDLENAME, bundleManager);
    }

    public String getString(@PropertyKey(resourceBundle = BUNDLENAME) String key)
    {
        return super.getString(key);
    }

    public String format(@PropertyKey(resourceBundle = BUNDLENAME) String key, Object... obj)
    {
        return super.format(key, obj);
    }

}


/*-----------------------------------------------------------------------------*
| No Copyright© 2025 Lucas Kling, Luis Kölbel, Lukas Steiner, Elias Reißmüller,|
| Tim Peteler                                                                |
| This code was created and developed by the above-mentioned individuals.      |
| In certain sections, various AI models were used.                            |
| The code can be modified and used according to the terms of the unattached   | 
| and non-existent license. The code is not subject to copyright protection.   |
| Note: This plugin code is not associated with Rapla and was developed        |  
| independently.                                                               |                                                            |
*-----------------------------------------------------------------------------*/
