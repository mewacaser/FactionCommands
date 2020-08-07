package com.skyline.factioncommands.common.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.mojang.brigadier.CommandDispatcher;
import com.skyline.factioncommands.common.data.FactionSavedData;
import com.skyline.factioncommands.common.data.FactionSavedData.FactionData;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class FactionCommand {
	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("faction")
			.then(Commands.literal("list")
				.executes((cc) -> {
					CommandSource source = cc.getSource();
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
	
						source.sendSuccess(new TranslationTextComponent("commands.faction.list",
								new StringTextComponent(faction.name), 
								new StringTextComponent(Integer.toString(online)),
								new StringTextComponent(Integer.toString(active)),
								new StringTextComponent(Integer.toString(total))), false);
					});
					
					return 1;
				})
				.then(Commands.argument("faction", FactionArgument.active()).executes((cc) -> {
					CommandSource source = cc.getSource();
					FactionSavedData data = FactionSavedData.get(source.getServer());
					
					String factionInput  = FactionArgument.getFaction(cc, "faction");
					String factionID = FactionData.formatID(factionInput);
					
					if(data.factions.containsKey(factionID)) {
						FactionData faction = data.factions.get(factionID);
						int online = 0, active = 0, total = faction.players.size();
						
						List<String> onlinePlayers = new ArrayList<String>();
						List<String> activePlayers = new ArrayList<String>();
						List<String> inactivePlayers = new ArrayList<String>();
						
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
						source.sendSuccess(new TranslationTextComponent("commands.faction.list",
								new StringTextComponent(faction.name), 
								new StringTextComponent(Integer.toString(online)),
								new StringTextComponent(Integer.toString(active)),
								new StringTextComponent(Integer.toString(total))), false);
						
						source.sendSuccess(new TranslationTextComponent("commands.faction.queryteamlist",
								new StringTextComponent(String.join(", ", onlinePlayers)),
								new StringTextComponent(String.join(", ", activePlayers)),
								new StringTextComponent(String.join(", ", inactivePlayers))), false);
					} else {
						source.sendSuccess(new TranslationTextComponent("argument.faction.invalid",
								new StringTextComponent(factionID)), false);
					}

					return 1;
				})))
			.then(Commands.literal("query")
				.then(Commands.argument("player", EntityArgument.players()).executes((cc) -> {
					CommandSource source = cc.getSource();
					Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(cc, "player");
					FactionSavedData data = FactionSavedData.get(source.getServer());
					
					String sourceID = source.getTextName();
					
					for(ServerPlayerEntity target : targets) {
						String targetID = target.getGameProfile().getName();
	
						for(Entry<String, FactionData> fEntry : data.factions.entrySet()) {
							FactionData faction = fEntry.getValue();
							
							if(faction.players.containsKey(targetID)) {
								FactionSavedData.FactionPlayerData playerData = faction.players.get(targetID);
								if (playerData.active) {
									source.sendSuccess(new TranslationTextComponent("commands.faction.query",
											target.getName(), 
											new StringTextComponent(faction.name)), false);
									if (faction.players.containsKey(sourceID) && faction.players.get(sourceID).active) {
										source.sendSuccess(
												new TranslationTextComponent("commands.faction.queryteam", 
														target.getName(),
												new StringTextComponent(new BlockPos(target.getPosition(1)).toString())), false);
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
				CommandSource source = cc.getSource();
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
						faction.players.put(playerID, new FactionSavedData.FactionPlayerData(true, faction.origin, new CompoundNBT()));
					}
					//reload old faction
					FactionSavedData.FactionPlayerData playerData = faction.players.get(playerID);
					playerData.active = true;
					source.getPlayerOrException().teleportTo(source.getServer().getLevel(playerData.position.dim), playerData.position.pos.x, playerData.position.pos.y, playerData.position.pos.z, 0, 0);
					source.getPlayerOrException().readAdditionalSaveData(playerData.playerData);
					
					data.setDirty();
					
					source.sendSuccess(new TranslationTextComponent("commands.faction.switch", new StringTextComponent(faction.name)), true);
				} else {
					source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(factionInput)), true);
				}
	
				return 1;
			})))
			.then(Commands.literal("register").then(Commands.argument("faction", FactionArgument.creation()).executes((cc) -> {
				CommandSource source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(factionName)), true);
				} else {
					data.factions.put(factionID, new FactionSavedData.FactionData(factionName, new FactionSavedData.DimLocation(source.getPosition(), source.getLevel().dimension())));
					source.sendSuccess(new TranslationTextComponent("commands.faction.register",
							new StringTextComponent(factionName),
						new StringTextComponent(new BlockPos(source.getPosition()).toString())), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("relocate").then(Commands.argument("faction", FactionArgument.active()).executes((cc) -> {
				CommandSource source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					FactionData faction = data.factions.get(factionID);
					faction.origin = new FactionSavedData.DimLocation(source.getPosition(), source.getLevel().dimension());
					source.sendSuccess(new TranslationTextComponent("commands.faction.register.move",
							new StringTextComponent(faction.name),
						new StringTextComponent(new BlockPos(source.getPosition()).toString())), true);
				} else {
					source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("rename").then(Commands.argument("faction", FactionArgument.active()).then(Commands.argument("replacement", FactionArgument.creation()).executes((cc) -> {
				CommandSource source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				String replacementName = FactionArgument.getFaction(cc, "replacement");
				String replacementID = FactionData.formatID(replacementName);
				
				if(data.factions.containsKey(factionID)) {
					if(data.factions.containsKey(replacementID)) {
						source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(replacementName)), true);
					} else {
						FactionData faction = data.factions.remove(factionID);
						faction.name = replacementName;
						data.factions.put(replacementID, faction);
						source.sendSuccess(new TranslationTextComponent("commands.faction.register.rename",
								new StringTextComponent(factionName),
								new StringTextComponent(replacementName)), true);
					}
				} else {
					source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			}))))
			.then(Commands.literal("hide").requires((cc) -> {
				return cc.hasPermission(2);
			}).then(Commands.argument("faction", FactionArgument.active()).executes((cc) -> {
				CommandSource source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					FactionData faction = data.factions.get(factionID);
					faction.hidden = true;
					source.sendSuccess(new TranslationTextComponent("commands.faction.hide", new StringTextComponent(faction.name)), true);
				} else {
					source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("reveal").requires((cc) -> {
				return cc.hasPermission(2);
			}).then(Commands.argument("faction", FactionArgument.hidden()).executes((cc) -> {
				CommandSource source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					FactionData faction = data.factions.get(factionID);
					faction.hidden = false;
					source.sendSuccess(new TranslationTextComponent("commands.faction.unhide", new StringTextComponent(faction.name)), true);
				} else {
					source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
			.then(Commands.literal("clear").requires((cc) -> {
				return cc.hasPermission(2);
			}).executes((cc) -> {
				CommandSource source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());
				
				data.factions.clear();
				source.sendSuccess(new TranslationTextComponent("commands.faction.clear", new StringTextComponent("everything")), true);
				
				data.setDirty();
				
				return 1;
			}).then(Commands.argument("faction", FactionArgument.all()).executes((cc) -> {
				CommandSource source = cc.getSource();
				FactionSavedData data = FactionSavedData.get(source.getServer());

				String factionName = FactionArgument.getFaction(cc, "faction");
				String factionID = FactionData.formatID(factionName);
				
				if(data.factions.containsKey(factionID)) {
					data.factions.remove(factionID);
					source.sendSuccess(new TranslationTextComponent("commands.faction.clear", new StringTextComponent(factionName)), true);
				} else {
					source.sendSuccess(new TranslationTextComponent("argument.faction.invalid", new StringTextComponent(factionName)), true);
				}
				data.setDirty();

				return 1;
			})))
		);
	}
}