package maprules;

import java.util.regex.Pattern;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Request Handler for downloading MapRules preset and validation rules
 * @link { org.openstreetmap.josm.io.remotecontrol.RemoteControl }
 * @author Max Grossman
 *
 */
public class MapRulesHandler extends RequestHandler {

	public static final String command = "load_maprules";
	private String id;
	// https://gist.github.com/johnelliott/cf77003f72f889abbc3f32785fa3df8d#file-uuidv4test-js-L7
    private Pattern uuidValidator = Pattern.compile("^[0-9A-F]{8}-[0-9A-F]{4}-4[0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}$", Pattern.CASE_INSENSITIVE);

    @Override
	public String[] getMandatoryParams() {
		return new String[]{"id"};
	}

	@Override
	public String getPermissionMessage() {
		return tr("Remote Control has been asked to load maprules presets and MapCSS rules");
	}

	@Override
	public PermissionPrefWithDefault getPermissionPref() {
		return PermissionPrefWithDefault.IMPORT_DATA;
	}

	@Override
	protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
		MapRulesTask.getInstance().init(id);
		MainApplication.worker.submit(MapRulesTask.getInstance());
	}

	@Override
	protected void validateRequest() throws RequestHandlerBadRequestException {
		this.id = args.get("id");
		try {
			if (!uuidValidator.matcher(id).find()) throw new Exception();
		} catch (Exception e) {
			throw new RequestHandlerBadRequestException("( " + this.id + " ) is not valid");
		}

	}

	@Override
	public String[] getUsageExamples() {
		return new String[]{
				"load_maprules?id=0a9d022d-c23f-44a2-8eda-5ea6a0e77b7e"
		};
	}

}
