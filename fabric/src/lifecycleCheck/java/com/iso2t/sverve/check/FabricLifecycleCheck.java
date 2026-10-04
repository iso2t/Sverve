package com.iso2t.sverve.check;

import com.iso2t.sverve.test.LifecycleChecks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Test-only access to the production attachment registrations.
 */
public final class FabricLifecycleCheck implements ModInitializer, LifecycleChecks.Attachments {
	@Override
	public void onInitialize () {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> LifecycleChecks.run(server, this));
	}

	@Override
	public Object get (ServerPlayer player, String name) {
		return player.getAttachedOrCreate(type(name));
	}

	@Override
	public void set (ServerPlayer player, String name, Object value) {
		player.setAttached(type(name), value);
	}

	@SuppressWarnings("unchecked")
	private static AttachmentType<Object> type (String name) {
		// Fabric exposes lookup through its implementation registry in this version.
		return (AttachmentType<Object>) AttachmentRegistryImpl.get(Identifier.fromNamespaceAndPath("sverve", name));
	}
}
