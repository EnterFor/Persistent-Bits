package com.oitsjustjose.persistentbits.common.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class ChunkLoaderListStorage implements IStorage<IChunkLoaderList>
{
    @Override
    public INBT writeNBT(Capability<IChunkLoaderList> capability, IChunkLoaderList instance,
            net.minecraft.util.Direction side)
    {
        if (!(instance instanceof ChunkLoaderList))
        {
            return null;
        }

        ChunkLoaderList loaderList = (ChunkLoaderList) instance;
        CompoundNBT serialized = new CompoundNBT();

        loaderList.loadersPerChunk.entrySet().forEach((pair) -> {
            serialized.putInt(pair.getKey().toString(), pair.getValue());
        });

        return serialized;
    }

    @Override
    public void readNBT(Capability<IChunkLoaderList> capability, IChunkLoaderList instance,
            net.minecraft.util.Direction side, INBT nbt)
    {
        if (!(instance instanceof ChunkLoaderList) || !(nbt instanceof CompoundNBT))
        {
            return;
        }

        ChunkLoaderList loaderList = (ChunkLoaderList) instance;
        CompoundNBT serialized = (CompoundNBT) nbt;

        loaderList.currentlyLoading = true;

        try
        {
            for (String longString : serialized.keySet())
            {
                long asObj = Long.parseLong(longString);
                int numInChunk = serialized.getInt(longString);
                loaderList.loadersPerChunk.put(asObj, numInChunk);
            }
            if (loaderList.world != null)
            {
                loaderList.world.getServer().enqueue(new TickDelayedTask(1, () -> {
                    loaderList.loadersPerChunk.keySet().forEach((longObj) -> {
                        loaderList.forceLoad(BlockPos.fromLong(longObj));
                    });
                }));
            }
        }
        finally
        {
            loaderList.currentlyLoading = false;
        }
    }
}