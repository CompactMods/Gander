package com.simibubi.create.infrastructure.ponder;

import com.simibubi.create.Create;
import com.simibubi.create.ponder.PonderRegistrationHelper;

public class PonderIndex {

	static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(Create.ID);

	public static final boolean REGISTER_DEBUG_SCENES = true;

	public static void register() {
		if (REGISTER_DEBUG_SCENES)
			DebugScenes.registerAll();
	}

	public static boolean editingModeActive() {
		return false;
	}

}
