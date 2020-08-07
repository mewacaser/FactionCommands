package com.skyline.factioncommands;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class Lang extends LanguageProvider {
	public Lang(DataGenerator gen) {
		super(gen, FactionCommands.MODID, "en_us");
	}

	@Override
	protected void addTranslations() {
		add("commands.faction.register", "Successfully registered %1$s at %2$s");
		add("commands.faction.register.move", "Successfully moved %1$s to %2$s");
		add("commands.faction.register.rename", "Successfully renamed %1$s to %2$s");
		add("commands.faction.list", "Faction: %1$s Online: %2$s | Active: %3$s | Total: %4$s");
		add("commands.faction.query", "%1$s is a part of %2$s");
		add("commands.faction.queryteam", "Your teammate %1$s is at %2$s");
		add("commands.faction.queryteamlist", "Online: [ %1$s ], Offline: [ %2$s ], Inactive: [ %3$s ]");
		add("commands.faction.switch", "Successfully switched factions to %s");
		add("commands.faction.hide", "Successfully removed %s");
		add("commands.faction.unhide", "Successfully revived %s");
		add("commands.faction.clear", "Permanently deleted %s");
		add("argument.faction.invalid", "Faction name: %s is invalid");
	}
}
