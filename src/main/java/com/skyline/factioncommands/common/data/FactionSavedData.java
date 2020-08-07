package com.skyline.factioncommands.common.data;

import java.util.HashMap;
import java.util.Map;

import com.skyline.factioncommands.FactionCommands;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

public class FactionSavedData extends WorldSavedData {
	private static final String DATA_NAME = FactionCommands.MODID + "_FactionData";
	public static final int version = 1;

	public Map<String, FactionData> factions = new HashMap<String, FactionData>();

	public static class DimLocation {
		public static final String OVERWORLD = "overworld";
		public static final String NETHER = "nether";
		public static final String END = "end";

		public Vector3d pos;
		public RegistryKey<World> dim;

		public DimLocation(Vector3d pos, RegistryKey<World> dim) {
			this.pos = pos;
			this.dim = dim;
		}

		public static DimLocation read(CompoundNBT nbt) {
			RegistryKey<World> dim = World.OVERWORLD;
			Vector3d pos = Vector3d.ZERO;
			if (nbt.contains("dim")) {
				String dimKey = nbt.getString("dim");
				dim = dimKey == OVERWORLD ? World.OVERWORLD : dimKey == NETHER ? World.NETHER : dimKey == END ? World.END : World.OVERWORLD;
			}
			if (nbt.contains("pos_x") && nbt.contains("pos_y") && nbt.contains("pos_z")) {
				pos = new Vector3d(nbt.getDouble("pos_x"), nbt.getDouble("pos_y"), nbt.getDouble("pos_z"));
			}
			return new DimLocation(pos, dim);
		}

		public CompoundNBT write() {
			CompoundNBT nbt = new CompoundNBT();
			nbt.putDouble("pos_x", pos.x);
			nbt.putDouble("pos_y", pos.y);
			nbt.putDouble("pos_z", pos.z);
			nbt.putString("dim", dim == World.OVERWORLD ? OVERWORLD : dim == World.NETHER ? NETHER : dim == World.END ? END : "confusion");
			return nbt;
		}
	}

	public static class FactionData {
		public String name;
		public boolean hidden = false;
		public DimLocation origin;
		public Map<String, FactionPlayerData> players = new HashMap<String, FactionPlayerData>();

		public FactionData(String name, DimLocation origin) {
			this.name = name;
			this.origin = origin;
		}

		public static FactionData read(CompoundNBT nbt) {
			String name = nbt.contains("name") ? nbt.getString("name") : "default_faction_name";
			DimLocation origin = nbt.contains("origin") ? DimLocation.read(nbt.getCompound("origin")) : new DimLocation(Vector3d.ZERO, World.OVERWORLD);
			FactionData fd = new FactionData(name, origin);
			if (nbt.contains("hidden")) {
				fd.hidden = nbt.getBoolean("hidden");
			}
			if (nbt.contains("players")) {
				ListNBT playerData = nbt.getList("players", 10);
				playerData.forEach(item -> {
					CompoundNBT loadPlayer = (CompoundNBT) item;
					fd.players.put(loadPlayer.getString("id"), FactionPlayerData.read(loadPlayer.getCompound("player_data")));
				});
			}
			return fd;
		}

		public CompoundNBT write() {
			CompoundNBT nbt = new CompoundNBT();
			nbt.putString("name", name);
			nbt.putBoolean("hidden", hidden);
			nbt.put("origin", origin.write());
			ListNBT playerData = new ListNBT();
			players.forEach((id, data) -> {
				CompoundNBT savePlayer = new CompoundNBT();
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
		public CompoundNBT playerData;

		public FactionPlayerData(boolean active, DimLocation position, CompoundNBT playerData) {
			this.active = active;
			this.position = position;
			this.playerData = playerData;
		}

		public static FactionPlayerData read(CompoundNBT nbt) {
			boolean active = nbt.contains("active") ? nbt.getBoolean("active") : true;
			DimLocation position = nbt.contains("position") ? DimLocation.read(nbt.getCompound("position"))
				: new DimLocation(Vector3d.ZERO, World.OVERWORLD);
			CompoundNBT playerData = nbt.contains("player_data") ? nbt.getCompound("player_data") : new CompoundNBT();
			return new FactionPlayerData(active, position, playerData);
		}

		public CompoundNBT write() {
			CompoundNBT nbt = new CompoundNBT();
			nbt.putBoolean("active", active);
			nbt.put("position", position.write());
			nbt.put("player_data", playerData);
			return nbt;
		}
	}

	public FactionSavedData() {
		super(DATA_NAME);
	}

	@Override
	public void load(CompoundNBT nbt) {
		if (nbt.contains(DATA_NAME)) {
			CompoundNBT saveData = nbt.getCompound(DATA_NAME);
			if (saveData.contains("version")) {
				int dVersion = saveData.getInt("version");
				if(dVersion == 0) {
					read_v0(saveData);
				} else if(dVersion == 1) {
					read_v1(saveData);
				}
			}
		}
	}
	
	private void read_v0(CompoundNBT saveData) {
		ListNBT factionData = saveData.getList("factions", 10);
		ListNBT playerData = saveData.getList("players", 10);
		
		factionData.forEach(item -> {
			CompoundNBT loadFaction = (CompoundNBT) item;
			String fName = loadFaction.getString("id");
			CompoundNBT fData = loadFaction.getCompound("faction_data");
			FactionData faction = new FactionData(fName, fData.contains("origin") ? DimLocation.read(fData.getCompound("origin")) : new DimLocation(Vector3d.ZERO, World.OVERWORLD));
			factions.put(FactionData.formatID(fName), faction);
		});
		
		playerData.forEach(item -> {
			CompoundNBT loadPlayer = (CompoundNBT) item;
			String factionKey = loadPlayer.getString("id");
			String[] fKeySplit = factionKey.split(":");
			factions.get(FactionData.formatID(fKeySplit[0])).players.put(fKeySplit[1], FactionPlayerData.read(loadPlayer.getCompound("player_data")));
		});
	}
	
	private void read_v1(CompoundNBT saveData) {
		ListNBT factionData = saveData.getList("factions", 10);
		
		factionData.forEach(item -> {
			CompoundNBT loadFaction = (CompoundNBT) item;
			factions.put(loadFaction.getString("id"), FactionData.read(loadFaction.getCompound("faction_data")));
		});
	}

	@Override
	public CompoundNBT save(CompoundNBT compound) {
		CompoundNBT saveData = new CompoundNBT();
		saveData.putInt("version", version);

		ListNBT factionData = new ListNBT();

		factions.forEach((id, data) -> {
			CompoundNBT saveFaction = new CompoundNBT();
			saveFaction.putString("id", id);
			saveFaction.put("faction_data", data.write());
			factionData.add(saveFaction);
		});

		saveData.put("factions", factionData);

		compound.put(DATA_NAME, saveData);
		return compound;
	}

	public static FactionSavedData get(MinecraftServer server) {
		DimensionSavedDataManager storage = server.getLevel(World.OVERWORLD).getDataStorage();

		FactionSavedData instance = storage.computeIfAbsent(() -> new FactionSavedData(), DATA_NAME);

		return instance;
	}
}
