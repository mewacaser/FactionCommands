package com.skyline.factioncommands.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.skyline.factioncommands.common.data.FactionSavedData;
import com.skyline.factioncommands.common.data.FactionSavedData.FactionData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;

public class FactionCommand {
	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("faction")
			.then(Commands.literal("list")
				.executes((cc) -> {
					CommandSourceStack source = cc.getSource();
					FactionSavedData data = FactionSavedData.get(source.getServer());
					
					data.factions.forEach((fid, faction) -> {
						if(faction.hidden) {
							return;
						}
						int online = 0, active = 0, total = faction.players.size();
						
						for(Entry<String, FactionSavedData.FactionPlayerData> pEntry : faction.players.entrySet()) {
							String pid = pEntry.getKey();
							FactionSavedData.FactionPlayerData player = pEntry.getValue();
							if(player.active) {
								active++;
								if (source.getServer().getPlayerList().getPlayerByName(pid) != null) {
									online++;
								}
							}
						}

						source.sendSuccess(new TranslatableComponent("commands.faction.list",
								new TextComponent(faction.name),
								new TextComponent(Integer.toString(online)),
								new TextComponent(Integer.toString(active)),
								new TextComponent(Integer.toString(total))), false);
					});
					
					return 1;
				})
				.then(Commands.argument("faction", FactionArgument.active()).executes((cc) -> {
					CommandSourceStack source = cc.getSource();
					FactionSavedData data = FactionSavedData.get(source.getServer());
					
					String factionInput  = FactionArgument.getFaction(cc, "faction");
					String factionID = FactionData.formatID(factionInput);
					
					if(data.factions.containsKey(factionID)) {
						FactionData faction = data.factions.get(factionID);
						int online = 0, active = 0, total = faction.players.size();
						
						List<String> onlinePlayers = new ArrayList<>();
						List<String> activePlayers = new ArrayList<>();
						List<String> inactivePlayers = new ArrayList<>();
						
						for(Entry<String, FactionSavedData.FactionPlayerData> pEntry : faction.players.entrySet()) {
							String pid = pEntry.getKey();
							FactionSavedData.FactionPlayerData player = pEntry.getValue();
							if(player.active) {
								active++;
								if (source.getServer().getPlayerList().getPlayerByName(pid) != null) {
									online++;
									onlinePlayers.add(pid);
								} else {
									activePlayers.add(pid);
								}
							} else {
								inactivePlayers.add(pid);
							}
						}
						source.sendSuccess(new TranslatableComponent("commands.faction.list",
								new TextComponent(faction.name),
								new TextComponent(Integer.toString(online)),
								new TextComponent(Integer.toString(active)),
								new TextComponent(Integer.toString(total))), false);

						source.sendSuccess(new TranslatableComponent("commands.faction.queryteamlist",
								new TextComponent(String.join(", ", onlinePlayers)),
								new TextComponent(String.join(", ", activePlayers)),
								new TextComponent(String.join(", ", inactivePlayers))), false);
					} else {
						source.sendSuccess(new TranslatableComponent("argument.faction.invalid",
							new TextComponent(factionID)), false);
					}

					return 1;
				})))
			.then(Commands.literal("query")
				.then(Commands.argument("player", EntityArgument.players()).executes((cc) -> {
					CommandSourceStack source = cc.getSource();
					Collection<ServerPlayer> targets = EntityArgument.getPlayers(cc, "player");
					FactionSavedData data = FactionSavedData.get(source.getServer());
					
					String sourceID = source.getTextName();
					
					for(ServerPlayer target : targets) {
						String targetID = target.getGameProfile().getName();
	
						for(Entry<String, FactionData> fEntry : data.factions.entrySet()) {
							FactionData faction = fEntry.getValue();
							
							if(faction.players.containsKey(targetID)) {
								FactionSavedData.FactionPlayerData playerData = faction.players.get(targetID);
								if (playerData.active) {
									source.sendSuccess(new TranslatableComponent("commands.faction.query",
											target.getName(), 
											new TextComponent(faction.name)), false);
									if (faction.players.containsKey(sourceID) && faction.players.get(sourceID).active) {
										source.sendSuccess(
												new TranslatableComponent("commands.faction.queryteam",
														target.getName(),
												new TextComponent(new BlockPos(target.getPosition(1)).toString())), false);
									}
									break;
								}
							}
						}
					}

					return 1;
				}))
			)
			.then(Commands.literal("switch").then(Commands.argument("faction", FactionArgument.active()).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());
	
				String factionInput = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionInput);
				
				if(data.factions.containsKey(factionID)) {
					String playerID = source.getTextName();
					//save current faction stuff
					for(Entry<String, FactionData> fEntry : data.factions.entrySet()) {
						FactionData faction = fEntry.getValue();
						
						if(faction.players.containsKey(playerID)) {
							FactionSavedData.FactionPlayerData playerData = faction.players.get(playerID);
							if(playerData.active) {
								playerData.active = false;
								playerData.position = new FactionSavedData.DimLocation(source.getPosition(), source.getLevel().dimension());
								source.getPlayerOrException().addAdditionalSaveData(playerData.playerData);
								break;
							}	
						}
					}
					//load next faction stuff
					FactionData faction = data.factions.get(factionID);
					if(!faction.players.containsKey(playerID)) {
						//new faction for this player
						faction.players.put(playerID, new FactionSavedData.FactionPlayerData(true, faction.origin, new CompoundTag()));
					}
					//reload old faction
					FactionSavedData.FactionPlayerData playerData = faction.players.get(playerID);
					playerData.active = true;
					source.getPlayerOrException().teleportTo(source.getServer().getLevel(playerData.position.dim), playerData.position.pos.x, playerData.position.pos.y, playerData.position.pos.z, 0, 0);
					source.getPlayerOrException().readAdditionalSaveData(playerData.playerData);
					
					data.setDirty();
					
					source.sendSuccess(new TranslatableComponent("commands.faction.switch", new TextComponent(faction.name)), true);
				} else {
					source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(factionInput)), true);
				}
	
				return 1;
			})))
			.then(Commands.literal("register").then(Commands.argument("faction", FactionArgument.creation()).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(factionName)), true);
				} else {
					data.factions.put(factionID, new FactionSavedData.FactionData(factionName, new FactionSavedData.DimLocation(source.getPosition(), source.getLevel().dimension())));
					source.sendSuccess(new TranslatableComponent("commands.faction.register",
							new TextComponent(factionName),
						new TextComponent(new BlockPos(source.getPosition()).toString())), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("relocate").then(Commands.argument("faction", FactionArgument.active()).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					FactionData faction = data.factions.get(factionID);
					faction.origin = new FactionSavedData.DimLocation(source.getPosition(), source.getLevel().dimension());
					source.sendSuccess(new TranslatableComponent("commands.faction.register.move",
							new TextComponent(faction.name),
						new TextComponent(new BlockPos(source.getPosition()).toString())), true);
				} else {
					source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("rename").then(Commands.argument("faction", FactionArgument.active()).then(Commands.argument("replacement", FactionArgument.creation()).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				String replacementName = FactionArgument.getFaction(cc, "replacement");
				String replacementID = FactionData.formatID(replacementName);
				
				if(data.factions.containsKey(factionID)) {
					if(data.factions.containsKey(replacementID)) {
						source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(replacementName)), true);
					} else {
						FactionData faction = data.factions.remove(factionID);
						faction.name = replacementName;
						data.factions.put(replacementID, faction);
						source.sendSuccess(new TranslatableComponent("commands.faction.register.rename",
								new TextComponent(factionName),
								new TextComponent(replacementName)), true);
					}
				} else {
					source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			}))))
			.then(Commands.literal("hide").requires((cc) -> cc.hasPermission(2))
				.then(Commands.argument("faction", FactionArgument.active()).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					FactionData faction = data.factions.get(factionID);
					faction.hidden = true;
					source.sendSuccess(new TranslatableComponent("commands.faction.hide", new TextComponent(faction.name)), true);
				} else {
					source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("reveal").requires((cc) -> cc.hasPermission(2))
				.then(Commands.argument("faction", FactionArgument.hidden()).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					FactionData faction = data.factions.get(factionID);
					faction.hidden = false;
					source.sendSuccess(new TranslatableComponent("commands.faction.unhide", new TextComponent(faction.name)), true);
				} else {
					source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("clear").requires((cc) -> cc.hasPermission(2)).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());
				
				data.factions.clear();
				source.sendSuccess(new TranslatableComponent("commands.faction.clear", new TextComponent("everything")), true);
				
				data.setDirty();
				
				return 1;
			}).then(Commands.argument("faction", FactionArgument.all()).executes((cc) -> {
				CommandSourceStack source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					data.factions.remove(factionID);
					source.sendSuccess(new TranslatableComponent("commands.faction.clear", new TextComponent(factionName)), true);
				} else {
					source.sendSuccess(new TranslatableComponent("argument.faction.invalid", new TextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
		);
	}
}