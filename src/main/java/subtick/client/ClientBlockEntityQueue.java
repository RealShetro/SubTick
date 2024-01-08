package subtick.client;

import java.util.HashSet;
import java.util.Iterator;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import subtick.QueueElement;

public class ClientBlockEntityQueue
{
  private static boolean stepping;
  private static final HashSet<TickingBlockEntity> ticked_block_entities = new HashSet<>();
  private static final HashSet<BlockPos> poses = new HashSet<>();

  private static void start(ClientLevel level)
  {
    if(!level.pendingBlockEntityTickers.isEmpty())
    {
      level.blockEntityTickers.addAll(level.pendingBlockEntityTickers);
      level.pendingBlockEntityTickers.clear();
    }
  }

  public static void addPos(QueueElement element)
  {
    poses.add(element.blockPos());
  }

  public static void step(ClientLevel level)
  {
    if(poses.isEmpty())
      return;

    if(!stepping)
    {
      start(level);
      stepping = true;
      level.tickingBlockEntities = true;
    }

    Iterator<TickingBlockEntity> iterator = level.blockEntityTickers.iterator();
    while(iterator.hasNext())
    {
      TickingBlockEntity be = iterator.next();
      if(be.getPos() == null)
        continue;

      if(poses.contains(be.getPos()))
      {
        if(be.isRemoved())
          iterator.remove();
        else
        {
          be.tick();
          ticked_block_entities.add(be);
        }
        break;
      }
    }
    poses.clear();
  }

  public static boolean end(ClientLevel level)
  {
    if(!stepping)
      return false;
    stepping = false;

    Iterator<TickingBlockEntity> iterator = level.blockEntityTickers.iterator();
    while(iterator.hasNext())
    {
      TickingBlockEntity be = iterator.next();
      if(!ticked_block_entities.contains(be))
      {
        if(be.isRemoved())
          iterator.remove();
        else
          be.tick();
      }
    }

    level.tickingBlockEntities = false;
    ticked_block_entities.clear();
    return true;
  }
}
