package com.skyline.factioncommands.common.data;

import com.skyline.factioncommands.FactionCommands;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class FactionSavedData extends SavedData {
	private static final String DATA_NAME = FactionCommands.MODID + "_FactionData";
	public static final int version = 1;

	public Map<String, FactionData> factions = new HashMap<>();

	public static class DimLocation {
		public static final String OVERWORLD = "overworld";
		public static final String NETHER = "nether";
		public static final String END = "end";

		public Vec3 pos;
		public ResourceKey<Level> dim;

		public DimLocation(Vec3 pos, ResourceKey<Level> dim) {
			this.pos = pos;
			this.dim = dim;
		}

		public static DimLocation read(CompoundTag nbt) {
			ResourceKey<Level> dim = Level.OVERWORLD;
			Vec3 pos = Vec3.ZERO;
			if (nbt.contains("dim")) {
				String dimKey = nbt.getString("dim");
				dim = dimKey.equals(OVERWORLD) ? Level.OVERWORLD : dimKey.equals(NETHER) ? Level.NETHER : dimKey.equals(END) ? Level.END : Level.OVERWORLD;
			}
			if (nbt.contains("pos_x") && nbt.contains("pos_y") && nbt.contains("pos_z")) {
				pos = new Vec3(nbt.getDouble("pos_x"), nbt.getDouble("pos_y"), nbt.getDouble("pos_z"));
			}
			return new DimLocation(pos, dim);
		}

		public CompoundTag write() {
			CompoundTag nbt = new CompoundTag();
			nbt.putDouble("pos_x", pos.x);
			nbt.putDouble("pos_y", pos.y);
			nbt.putDouble("pos_z", pos.z);
			nbt.putString("dim", dim == Level.OVERWORLD ? OVERWORLD : dim == Level.NETHER ? NETHER : dim == Level.END ? END : "confusion");
			return nbt;
		}
	}

	public static class FactionData {
		public String name;
		public boolean hidden = false;
		public DimLocation origin;
		public Map<String, FactionPlayerData> players = new HashMap<>();

		public FactionData(String name, DimLocation origin) {
			this.name = name;
			this.origin = origin;
		}

		public static FactionData read(CompoundTag nbt) {
			String name = nbt.contains("name") ? nbt.getString("name") : "default_faction_name";
			DimLocation origin = nbt.contains("origin") ? DimLocation.read(nbt.getCompound("origin")) : new DimLocation(Vec3.ZERO, Level.OVERWORLD);
			FactionData fd = new FactionData(name, origin);
			if (nbt.contains("hidden")) {
				fd.hidden = nbt.getBoolean("hidden");
			}
			if (nbt.contains("players")) {
				ListTag playerData = nbt.getList("players", 10);
				playerData.forEach(item -> {
					CompoundTag loadPlayer = (CompoundTag) item;
					fd.players.put(loadPlayer.getString("id"), FactionPlayerData.read(loadPlayer.getCompound("player_data")));
				});
			}
			return fd;
		}

		public CompoundTag write() {
			CompoundTag nbt = new CompoundTag();
			nbt.putString("name", name);
			nbt.putBoolean("hidden", hidden);
			nbt.put("origin", origin.write());
			ListTag playerData = new ListTag();
			players.forEach((id, data) -> {
				CompoundTag savePlayer = new CompoundTag();
				savePlayer.putString("id", id);
				savePlayer.put("player_data", data.write());
				playerData.add(savePlayer);
			});
			nbt.put("players", playerData);
			return nbt;
		}
		
		public static String formatID(String name) {
			return name.toLowerCase().trim();
		}
	}

	public static class FactionPlayerData {
		public boolean active;
		public DimLocation position;
		public CompoundTag playerData;

		public FactionPlayerData(boolean active, DimLocation position, CompoundTag playerData) {
			this.active = active;
			this.position = position;
			this.playerData = playerData;
		}

		public static FactionPlayerData read(CompoundTag nbt) {
			boolean active = !nbt.contains("active") || nbt.getBoolean("active");
			DimLocation position = nbt.contains("position") ? DimLocation.read(nbt.getCompound("position"))
				: new DimLocation(Vec3.ZERO, Level.OVERWORLD);
			CompoundTag playerData = nbt.contains("player_data") ? nbt.getCompound("player_data") : new CompoundTag();
			return new FactionPlayerData(active, position, playerData);
		}

		public CompoundTag write() {
			CompoundTag nbt = new CompoundTag();
			nbt.putBoolean("active", active);
			nbt.put("position", position.write());
			nbt.put("player_data", playerData);
			return nbt;
		}
	}

	public static FactionSavedData load(CompoundTag nbt) {
		if (nbt.contains(DATA_NAME)) {
			CompoundTag saveData = nbt.getCompound(DATA_NAME);
			if (saveData.contains("version")) {
				int dVersion = saveData.getInt("version");
				return switch (dVersion) {
					case 0 -> new FactionSavedData().read_v0(saveData);
					case 1 -> new FactionSavedData().read_v1(saveData);
					default -> throw new IllegalStateException("Unexpected value: " + dVersion);
				};
			}
		}
		return null;
	}
	
	private FactionSavedData read_v0(CompoundTag saveData) {
		ListTag factionData = saveData.getList("factions", 10);
		ListTag playerData = saveData.getList("players", 10);
		
		factionData.forEach(item -> {
			CompoundTag loadFaction = (CompoundTag) item;
			String fName = loadFaction.getString("id");
			CompoundTag fData = loadFaction.getCompound("faction_data");
			FactionData faction = new FactionData(fName, fData.contains("origin") ? DimLocation.read(fData.getCompound("origin")) : new DimLocation(Vec3.ZERO, Level.OVERWORLD));
			factions.put(FactionData.formatID(fName), faction);
		});
		
		playerData.forEach(item -> {
			CompoundTag loadPlayer = (CompoundTag) item;
			String factionKey = loadPlayer.getString("id");
			String[] fKeySplit = factionKey.split(":");
			factions.get(FactionData.formatID(fKeySplit[0])).players.put(fKeySplit[1], FactionPlayerData.read(loadPlayer.getCompound("player_data")));
		});

		return this;
	}
	
	private FactionSavedData read_v1(CompoundTag saveData) {
		ListTag factionData = saveData.getList("factions", 10);
		
		factionData.forEach(item -> {
			CompoundTag loadFaction = (CompoundTag) item;
			factions.put(loadFaction.getString("id"), FactionData.read(loadFaction.getCompound("faction_data")));
		});

		return this;
	}

	@Override
	public @NotNull CompoundTag save(CompoundTag compound) {
		CompoundTag saveData = new CompoundTag();
		saveData.putInt("version", version);

		ListTag factionData = new ListTag();

		factions.forEach((id, data) -> {
			CompoundTag saveFaction = new CompoundTag();
			saveFaction.putString("id", id);
			saveFaction.put("faction_data", data.write());
			factionData.add(saveFaction);
		});

		saveData.put("factions", factionData);

		compound.put(DATA_NAME, saveData);
		return compound;
	}

	public static FactionSavedData get(MinecraftServer server) {
		DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();

		return storage.computeIfAbsent(FactionSavedData::load, FactionSavedData::new, DATA_NAME);
	}
}
