package com.skyline.factioncommands;

import com.skyline.factioncommands.common.commands.FactionArgument;
import com.skyline.factioncommands.common.commands.FactionCommand;
import javax.annotation.Nonnull;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod(FactionCommands.MODID)
public class FactionCommands {
	public static final String MODID = "factioncommands";

	public FactionCommands() {
	}

	@Mod.EventBusSubscriber(bus = Bus.FORGE)
	public static class ForgeRegistryEvents {
		@SubscribeEvent
		public static void registerCommands(@Nonnull final RegisterCommandsEvent event) {
			FactionCommand.register(event.getDispatcher());
		}
	}

	@Mod.EventBusSubscriber(bus = Bus.MOD)
	public static class ModSetupEvents {
		@SubscribeEvent
		public static void registerArgumentTypes(FMLCommonSetupEvent event) {
			ArgumentTypes.register("factioncommands:faction", FactionArgument.class, new FactionArgument.Serializer());
		}

		@SubscribeEvent
		public static void gatherData(GatherDataEvent event) {
			DataGenerator gen = event.getGenerator();

			if (event.includeClient()) {
				gen.addProvider(new Lang(gen));
			}
		}
	}
}
