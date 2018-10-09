package maprules;

import java.awt.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MenuScroller;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSeparator;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * Task that makes makes MapRules presets and validation rules usable in JOSM
 * @author Max Grossman
 *
 */
public class MapRulesTask implements Runnable {

	private String epoch;
	private String id;
	private String url;
	private Collection<String> currentIds = new ArrayList<>();
	private static MapRulesTask instance;

	public MapRulesTask() {};

	static MapRulesTask getInstance() {
		if (instance == null) {
			instance = new MapRulesTask();
		}
		return instance;
	}


	public void init (String id) {
		if (this.id != null && !id.equals(this.id)) removeStalePresets();
		this.id = id;
		this.url = MapRulesConstants.url + "/config/" + id;
	}

	private void removeStalePresets() {
		try {
			LinkedList<String> t = new LinkedList<>(MainApplication.getToolbar().getToolString());
			currentIds.forEach(id -> {
				if (t.contains(id)) {
					t.remove(id);
					Config.getPref().putList("toolbar", t);
					MainApplication.getToolbar().refreshToolbarControl();
				};
			});
			currentIds.clear();
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	/**
	 * Adds time parameter to ensure caching does make updates to rules/presets mute
	 * @author Max Grossman
	 * @param url maprules url to append time stamp to
	 * @return url with time parameter
	 */
	private String addEpoch(String url) {
		return url + "?time=" + String.valueOf(System.currentTimeMillis());
	}

	/**
	 * GETs a MapCSS file from MapRules services, then adds its rules to tag checker validation
	 * @author Max Grossman
	 */
	public void setupMapCSS() {
		try {
			String mapcssURL = addEpoch(this.url + "/rules/JOSM");
			SourceEntry mapcss = new SourceEntry(SourceType.TAGCHECKER_RULE, mapcssURL, false, null, id, (id + " Tag Checks"), true);
			// perhaps there's a more explicit add/remove seq like with presets...
			OsmValidator.getTest(MapCSSTagChecker.class).initialize();
			OsmValidator.getTest(MapCSSTagChecker.class).addMapCSS(mapcss.url);
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	/**
	 * GETs JOSM Preset XML from MapRules services, then loads tagging preset and ui elements
	 * @author Max Grossman
	 *
	 */
	public void setupPresets() {
		try {
			String presetsURL = addEpoch(this.url + "/presets/JOSM");
	        SourceEntry presetEntry = new SourceEntry(SourceType.TAGGING_PRESET, presetsURL, false, null, id, (id + " Presets"), true);
	        Collection<TaggingPreset> maprulesPresets = TaggingPresetReader.readAll(presetEntry.url, false);
	        TaggingPresets.readFromPreferences();
	        TaggingPresets.addTaggingPresets(maprulesPresets);
	        addPresetMenus(maprulesPresets);
            maprulesPresets.stream().filter(p -> p instanceof TaggingPresetMenu).forEach(p -> {
            	currentIds.add(p.getToolbarString());
            	MainApplication.getToolbar().addCustomButton(p.getToolbarString(), -1, false);
           		MainApplication.getToolbar().refreshToolbarControl();
           	});
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	/**
	 * {@link org.openstreetmap.josm.gui.tagging.presets.TaggingPresets#initialize()}
	 * Builds toolbar buttons to access mapping presets
	 * @author Max Grossman
	 * @param presets collection of tagging preset objects
	 */
	private void addPresetMenus(Collection<TaggingPreset> presets) {
		Map<TaggingPresetMenu, JMenu> submenus = new HashMap<>();
        for (final TaggingPreset p : presets) {
            JMenu m = p.group != null ? submenus.get(p.group) : MainApplication.getMenu().presetsMenu;
            if (m == null && p.group != null) {
                Logging.error("No tagging preset submenu for " + p.group);
            } else if (m == null) {
                Logging.error("No tagging preset menu. Tagging preset " + p + " won't be available there");
            } else if (p instanceof TaggingPresetSeparator) {
                m.add(new JSeparator());
            } else if (p instanceof TaggingPresetMenu) {
                JMenu submenu = new JMenu(p);
                submenu.setText(p.getLocaleName());
                ((TaggingPresetMenu) p).menu = submenu;
                submenus.put((TaggingPresetMenu) p, submenu);
                m.add(submenu);
            } else {
                JMenuItem mi = new JMenuItem(p);
                mi.setText(p.getLocaleName());
                m.add(mi);
            }
        }
        for (JMenu submenu : submenus.values()) {
            if (submenu.getItemCount() >= Config.getPref().getInt("taggingpreset.min-elements-for-scroller", 15)) {
                MenuScroller.setScrollerFor(submenu);
            }
        }
	}

	@Override
	public void run() {
		setupMapCSS();
		setupPresets();
	}

}
