package com.oitsjustjose.persistent_bits;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.oitsjustjose.persistent_bits.block.BlockChunkLoader;
import com.oitsjustjose.persistent_bits.chunkloading.ChunkLoadingCallback;
import com.oitsjustjose.persistent_bits.chunkloading.ChunkLoadingDatabase;
import com.oitsjustjose.persistent_bits.chunkloading.DimCoordinate;
import com.oitsjustjose.persistent_bits.proxy.CommonProxy;
import com.oitsjustjose.persistent_bits.tileentity.TileChunkLoader;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod(modid = Lib.MODID, name = Lib.NAME, version = Lib.VERSION, acceptedMinecraftVersions = "1.9.4", dependencies = "after:rftoolsdim")
public class PersistentBits
{
	@Instance(Lib.MODID)
	public static PersistentBits INSTANCE;

	@SidedProxy(clientSide = Lib.CLIENT_PROXY, serverSide = Lib.COMMON_PROXY, modId = Lib.MODID)
	public static CommonProxy proxy;

	public static Logger LOGGER = LogManager.getLogger(Lib.MODID);
	public static Config config;
	public static Block chunkLoader;
	public static ChunkLoadingDatabase database;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		config = new Config(event.getSuggestedConfigurationFile());
		chunkLoader = new BlockChunkLoader();
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(chunkLoader, 1), new Object[] { " E ", "DOD", "OXO", 'E', Items.ENDER_PEARL, 'D', "gemDiamond", 'O', Blocks.OBSIDIAN, 'X', Blocks.ENCHANTING_TABLE }));
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		ForgeChunkManager.setForcedChunkLoadingCallback(INSTANCE, new ChunkLoadingCallback());
	}

	@EventHandler
	public void postInit(FMLInitializationEvent event)
	{
		// Handles model registration via forge
		// Normally I use a Lib for automation for this,
		// but PersistentBits has only one block
		proxy.register(Item.getItemFromBlock(chunkLoader));
	}

	// Handles all of the self-loading features
	// Moved to "ServerStarted" to improve compat
	@EventHandler
	public void serverStarted(FMLServerStartedEvent event)
	{
		database = new ChunkLoadingDatabase();

		WorldServer world;
		database.deserialize();

		for (DimCoordinate coord : database.getCoordinates())
		{
			world = DimensionManager.getWorld(coord.getDimensionID());
			if (world != null && !world.isRemote)
			{
				TileChunkLoader chunkLoader = (TileChunkLoader) world.getTileEntity(coord.getPos());
				if (chunkLoader != null)
				{
					chunkLoader.setWorldObj(world);
					chunkLoader.validate();
					if (config.enableNotification)
						LOGGER.info("The Chunk Loader at " + coord + " has been automatically loaded!");
				}
			}
		}
	}
}