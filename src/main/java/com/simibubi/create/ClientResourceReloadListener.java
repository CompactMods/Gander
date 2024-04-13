package com.simibubi.create;

import com.simibubi.create.CreateClient;
import com.simibubi.create.utility.LangNumberFormat;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ClientResourceReloadListener implements ResourceManagerReloadListener {

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		CreateClient.invalidateRenderers();
		LangNumberFormat.numberFormat.update();
	}
}
