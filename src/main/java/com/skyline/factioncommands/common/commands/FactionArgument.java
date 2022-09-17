package com.skyline.factioncommands.common.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.skyline.factioncommands.common.data.FactionSavedData;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

public class FactionArgument implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("factionA", "\"faction B\"", "Other Faction");
	public static final SimpleCommandExceptionType GENERIC = new SimpleCommandExceptionType(
			new TranslatableComponent("argument.faction.invalid"));
	private FactionType type;

	protected FactionArgument(FactionType type) {
		this.type = type;
	}

	public static FactionArgument creation() {
		return new FactionArgument(FactionType.NONE);
	}

	public static FactionArgument active() {
		return new FactionArgument(FactionType.ACTIVE);
	}

	public static FactionArgument hidden() {
		return new FactionArgument(FactionType.HIDDEN);
	}

	public static FactionArgument all() {
		return new FactionArgument(FactionType.ALL);
	}

	public static String getFaction(final CommandContext<?> context, final String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader sr) throws CommandSyntaxException {
		return sr.readString();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> cc, SuggestionsBuilder sb) {
		if (!FactionType.NONE.equals(type)) {
			if (cc.getSource() instanceof CommandSourceStack source) {
				FactionSavedData data = FactionSavedData.get(source.getServer());
				return SharedSuggestionProvider.suggest(data.factions.entrySet().stream()
						.filter((entry) -> FactionType.ALL.equals(type)
								|| FactionType.HIDDEN.equals(type) && entry.getValue().hidden
								|| FactionType.ACTIVE.equals(type) && !entry.getValue().hidden)
						.map((entry) -> StringArgumentType.escapeIfRequired(entry.getKey())), sb);
			} else if (cc.getSource() instanceof SharedSuggestionProvider ssp) {
				return ssp.customSuggestion((CommandContext<SharedSuggestionProvider>) cc, sb);
			}
		}

		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public enum FactionType {
		NONE, ACTIVE, HIDDEN, ALL;

		public static FactionType fromOrdinal(byte i) {
			return values()[i];
		}
	}

	public static class Serializer implements ArgumentSerializer<FactionArgument> {
		@Override
		public void serializeToNetwork(FactionArgument arg, FriendlyByteBuf buffer) {
			buffer.writeByte(arg.type.ordinal());
		}

		@Override
		public @NotNull FactionArgument deserializeFromNetwork(FriendlyByteBuf buffer) {
			return new FactionArgument(FactionType.fromOrdinal(buffer.readByte()));
		}

		@Override
		public void serializeToJson(FactionArgument arg, JsonObject obj) {
			obj.addProperty("type", arg.type.name());
		}
	}
}