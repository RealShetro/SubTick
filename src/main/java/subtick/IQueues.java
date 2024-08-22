package subtick;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import subtick.queues.TickingQueue;

/*
 * Has logic for queue stepping.
 */
public interface IQueues
{
  public static IQueues get(CommandContext<CommandSourceStack> c)
  {
    return ((ITickHandleable)c.getSource().getServer()).tickHandler().queues();
  }

  public static IQueues get(CommandSourceStack c)
  {
    return ((ITickHandleable)c.getServer()).tickHandler().queues();
  }

  /*
   * Called by the /queueStep command
   */
  public void schedule(CommandSourceStack c, TickingQueue newQueue, String modeKey, int count, BlockPos pos, int range, boolean force) throws CommandSyntaxException;

  /*
   * Marks the queue as dirty and needing to be ended. Called by the {@link subtick.ITickHandler} when it schedules an unfreeze or step (only if the step goes at least 1 phase forwards).
   */
  public void scheduleEnd();

  /*
   * Called by the {@link subtick.ITickHandler} at the end of tick stepping, to ensure the queue is executed at the correct phase.
   */
  public void execute();

  /*
   * Called by the {@link subtick.ITickHandler} before unfreezing or stepping. Ends the queue only if an end is scheduled by scheduleEnd.
   */
  public void end();

  /*
   * Called by a mixin every time a block event is scheduled. This keeps the queue's block event queue up to date. Block events are the only queue where new elements can be added in the middle of stepping.
   */
  public void onScheduleBlockEvent(ServerLevel level, BlockEventData be);
}
