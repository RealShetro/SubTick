package subtick;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;

import org.apache.commons.lang3.tuple.Triple;

import subtick.network.ServerNetworkHandler;
import subtick.queues.BlockEventQueue;
import subtick.queues.TickingQueue;
import subtick.util.Translations;

public class Queues implements IQueues
{
  public static final DynamicCommandExceptionType INVALID_QUEUE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid queue '" + key + "'"));

  private final TickHandler tickHandler;

  public Queues(TickHandler tickHandler)
  {
    this.tickHandler = tickHandler;
  }

  private TickingQueue queue;
  private TickingQueue prev_queue;
  private int count;
  private BlockPos pos;
  private int range;
  private CommandSourceStack actor;
  private ServerLevel level;

  public boolean scheduled;
  private boolean stepping;
  private boolean should_end;

  public void printDebugInfo(CommandSourceStack c)
  {
    Messenger.m(c, "w queue: " + queue);
    Messenger.m(c, "w prev_queue: " + prev_queue);
    Messenger.m(c, "w count: " + count);
    Messenger.m(c, "w pos: " + pos);
    Messenger.m(c, "w range: " + range);
  }

  private void step(TickingQueue newQueue, CommandSourceStack c, int newCount, BlockPos newPos, int newRange) throws CommandSyntaxException
  {
    queue = newQueue;
    actor = c;
    count = newCount;
    pos = newPos;
    range = newRange;
    scheduled = true;
  }

  @Override
  public void schedule(CommandSourceStack c, TickingQueue newQueue, String modeKey, int count, BlockPos pos, int range, boolean force) throws CommandSyntaxException
  {
    level = c.getLevel();
    newQueue.setMode(modeKey);
    TickPhase phase = new TickPhase(level, newQueue.getPhase());

    if(force ? tickHandler.canStep(0, phase) : tickHandler.canStep(c, 0, phase))
    {
      step(newQueue, c, count, pos, range);
      tickHandler.step(c, 0, phase);
    }
    else if(force && tickHandler.canStep(c, 1, phase))
    {
      step(newQueue, c, count, pos, range);
      tickHandler.step(c, 1, phase);
    }
  }

  @Override
  public void scheduleEnd()
  {
    should_end = true;
  }

  @Override
  public void execute()
  {
    if(!scheduled) return;

    if(!stepping)
    {
      queue.start(level);
      stepping = true;
    }

    // Protects program state when stepping into an update suppressor
    try
    {
      Triple<Integer, Integer, Boolean> triple = queue.step(count, pos, range);
      queue.sendQueueStep(actor, triple.getLeft());
      sendFeedback(triple.getMiddle(), triple.getRight());
    }
    catch(Exception e)
    {
      Translations.m(actor, "queueCommand.err.crash", queue);
    }

    prev_queue = queue;
    scheduled = false;
  }

  @Override
  public void end()
  {
    if(!should_end)
      return;

    should_end = false;
    if(!stepping)
      return;

    prev_queue.step(1, BlockPos.ZERO, -2);
    prev_queue.end();
    prev_queue.exhausted = false;
    tickHandler.advancePhase(level);
    // this clears block event highlights
    ServerNetworkHandler.sendTickStep(level, 0, tickHandler.targetPhase());
    stepping = false;
  }

  @Override
  public void onScheduleBlockEvent(ServerLevel level, BlockEventData be)
  {
    if(stepping && queue instanceof BlockEventQueue beq)
      beq.updateQueue(level, be);
  }

  private void sendFeedback(int steps, boolean exhausted)
  {
    if(steps == 0)
      Translations.m(actor, "queueCommand.err.exhausted", queue);
    else if(steps == 1)
      if(exhausted)
        Translations.m(actor, "queueCommand.success.single.exhausted", queue, steps);
      else
        Translations.m(actor, "queueCommand.success.single", queue, steps);
    else
      if(exhausted)
        Translations.m(actor, "queueCommand.success.multiple.exhausted", queue, steps);
      else
        Translations.m(actor, "queueCommand.success.multiple", queue, steps);
  }
}
