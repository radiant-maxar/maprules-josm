// License: GPL. For details, see LICENSE file.
package maprules;

import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Plugin to use MapRules presets and validation rules in JOSM
 * @author Max Grossman
 *
 */
public class MapRulesPlugin extends Plugin {
    public MapRulesPlugin(PluginInformation info) {
        super(info);
        init();
    }
    private void init() {
    	new RemoteControl().addRequestHandler(MapRulesHandler.command, MapRulesHandler.class);
    }
}