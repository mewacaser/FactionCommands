package com.skyline.factioncommands.common.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

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

import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;

public class FactionArgument implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("factionA", "\"faction B\"", "Other Faction");
	public static final SimpleCommandExceptionType GENERIC = new SimpleCommandExceptionType(
			new TranslationTextComponent("argument.faction.invalid"));
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
			if (cc.getSource() instanceof CommandSource) {
				CommandSource source = (CommandSource) cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());
				return ISuggestionProvider.suggest(data.factions.entrySet().stream()
						.filter((entry) -> FactionType.ALL.equals(type)
								|| FactionType.HIDDEN.equals(type) && entry.getValue().hidden
								|| FactionType.ACTIVE.equals(type) && !entry.getValue().hidden)
						.map((entry) -> StringArgumentType.escapeIfRequired(entry.getKey())), sb);
			} else if (cc.getSource() instanceof ISuggestionProvider) {
				ISuggestionProvider isuggestionprovider = (ISuggestionProvider) cc.getSource();
				return isuggestionprovider.customSuggestion((CommandContext<ISuggestionProvider>) cc, sb);
			}
		}

		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public enum FactionType {
		NONE((byte) 0), ACTIVE((byte) 1), HIDDEN((byte) 2), ALL((byte) 3);

		public byte value;

		FactionType(byte i) {
			this.value = i;
		}
	}

	public static class Serializer implements IArgumentSerializer<FactionArgument> {
		@Override
		public void serializeToNetwork(FactionArgument arg, PacketBuffer buffer) {
			buffer.writeByte(arg.type.value);
		}

		@Override
		public FactionArgument deserializeFromNetwork(PacketBuffer buffer) {
			byte b0 = buffer.readByte();
			return new FactionArgument(b0 == 0 ? FactionType.NONE
					: b0 == 1 ? FactionType.ACTIVE : b0 == 2 ? FactionType.HIDDEN : FactionType.ALL);
		}

		@Override
		public void serializeToJson(FactionArgument arg, JsonObject obj) {
			obj.addProperty("type", arg.type.name());
		}
	}
}