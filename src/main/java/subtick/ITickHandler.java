package subtick;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;

/**
 * Stores the state of the server. Has public methods for scheduling actions.
 */
public interface ITickHandler
{
  public static ITickHandler get(CommandContext<CommandSourceStack> c)
  {
    return ((ITickHandleable)c.getSource().getServer()).tickHandler();
  }

  public static ITickHandler get(CommandSourceStack c)
  {
    return ((ITickHandleable)c.getServer()).tickHandler();
  }

  /*
   * Placed in a {@link com.llamalad7.mixinextras.injector.WrapWithCondition} before each tick phase. Returns whether that phase should execute.
   */
  public boolean shouldTick(ServerLevel level, int tickPhase);

  /*
   * Called by the /tick when command.
   */
  public int when(CommandSourceStack c);

  /*
   * Called by the /tick freeze on command.
   */
  public int freeze(CommandSourceStack c, int phase);

  /*
   * Called by the /tick freeze off command.
   */
  public int unfreeze(CommandSourceStack c);

  /*
   * Called by the /tick freeze command.
   */
  public int toggleFreeze(CommandSourceStack c, int phase);

  /*
   * Called by the /tick step command.
   */
  public int step(CommandSourceStack c, int count, int phase);

  /*
   * Called by the /phaseStep command. Steps [count] phases.
   */
  public int phaseStep(CommandSourceStack c, int count);

  /*
   * Called by the /phaseStep command. Steps to the next given phase.
   */
  public int stepToPhase(CommandSourceStack c, int phase, boolean force);

  /*
   * Prints and returns whether the tick handler can currently tick step.
   */
  public boolean canStep(CommandSourceStack c, int count, TickPhase phase);

  /*
   * Returns whether the tick handler can currently tick step.
   */
  public boolean canStep(int count, TickPhase phase);

  /*
   * Returns the IQueues for this tick handler.
   */
  public IQueues queues();

  /*
   * Returns whether the handler is frozen. This is used in the ServerLevel to know if it should record scheduled block events for queue stepping highlights.
   */
  public boolean frozen();

  /*
   * Returns the current phase.
   */
  public TickPhase currentPhase();

  /*
   * Returns the target phase for tick stepping. This is used in queue stepping to clear client highlights.
   */
  public TickPhase targetPhase();
}
