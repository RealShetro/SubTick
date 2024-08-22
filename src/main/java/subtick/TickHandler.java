package subtick;

import com.mojang.brigadier.Command;

import carpet.utils.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import subtick.network.ServerNetworkHandler;
import subtick.util.Translations;

public class TickHandler implements ITickHandler
{
  private static enum State
  {
    UNFROZEN,
    FROZEN,

    UNFREEZING,
    FREEZING,

    STEPPING
  }

  private final Queues queues = new Queues(this);

  private State state = State.UNFROZEN;

  private CommandSourceStack actor;
  private int remainingTicks;

  private TickPhase targetPhase = new TickPhase(0, 0);
  private TickPhase currentPhase = new TickPhase(0, 0);

  @Override
  public boolean frozen(){return state == State.FROZEN;}

  @Override
  public TickPhase currentPhase(){return currentPhase;}

  @Override
  public TickPhase targetPhase(){return targetPhase;}

  @Override
  public Queues queues(){return queues;}

  //public static boolean freezing(){return state == State.FREEZING;}

  public int printDebugInfo(CommandSourceStack c)
  {
    Messenger.m(c, "w remainingTicks: " + remainingTicks);
    Messenger.m(c, "w targetPhase: " + targetPhase);
    Messenger.m(c, "w currentPhase: " + currentPhase);
    Messenger.m(c, "w state: " + state.name());
    Messenger.m(c, "w Queue:");
    queues.printDebugInfo(c);
    return 1;
  }

  public void reset()
  {
    actor = null;
    remainingTicks = 0;
    targetPhase = new TickPhase(0, 0);
    currentPhase = new TickPhase(0, 0);
    state = State.UNFROZEN;
  }

  @Override
  public int when(CommandSourceStack c)
  {
    Translations.m(c, switch(state)
    {
      case FROZEN -> "tickCommand.when.frozen";
      case UNFROZEN -> "tickCommand.when.unfrozen";
      case FREEZING -> "tickCommand.when.freezing";
      case UNFREEZING -> "tickCommand.when.unfreezing";
      case STEPPING -> "tickCommand.when.stepping";
    }, currentPhase);
    return Command.SINGLE_SUCCESS;
  }

  @Override
  public boolean shouldTick(ServerLevel level, int tickPhase)
  {
    TickPhase phase = new TickPhase(level, tickPhase);

    return switch(state)
    {
      // Unfrozen cases --------------
      case UNFROZEN -> {
        currentPhase = phase;
        yield true;
      }

      case FREEZING -> {
        if(!phase.equals(targetPhase))
          yield true;

        state = State.FROZEN;
        currentPhase = phase;
        yield false;
      }

      // Frozen cases ----------------
      case FROZEN -> {
        queues.end();
        yield false;
      }

      case UNFREEZING -> {
        queues.end();
        if(!phase.equals(currentPhase))
          yield false;

        state = State.UNFROZEN;
        yield true;
      }

      case STEPPING -> {
        queues.end();
        if(!phase.equals(currentPhase))
          yield false;

        if(remainingTicks == 0 && phase.dim() == targetPhase.dim())
        {
          if(phase.phase() == targetPhase.phase())
          {
            //stepping = false;
            state = State.FROZEN;
            queues.execute();
            yield false;
          }

          // This block will only execute if the step has to end at a phase that doesn't currently exist
          if(phase.phase() > targetPhase.phase())
          {
            // Go 2 phases back; From entityManagment to entity
            currentPhase = new TickPhase(phase.dim(), TickPhase.ENTITY);
            Translations.m(actor, "tickCommand.step.err.unloaded", phase);
            yield false;
          }
        }

        if(phase.isLast())
          remainingTicks --;

        advancePhase(level);
        yield true;
      }
    };
  }

  public void advancePhase(ServerLevel level)
  {
    currentPhase = currentPhase.next(level);
  }

  @Override
  public int freeze(CommandSourceStack c, int phase)
  {
    if(state != State.UNFROZEN)
    {
      Translations.m(c, "tickCommand.freeze.err");
      return 0;
    }

    state = State.FREEZING;
    TickPhase tickPhase = new TickPhase(c.getLevel(), phase);
    targetPhase = tickPhase;
    ServerNetworkHandler.sendFrozen(c.getLevel(), tickPhase);
    Translations.m(c, "tickCommand.freeze.success", tickPhase);
    return Command.SINGLE_SUCCESS;
  }

  @Override
  public int unfreeze(CommandSourceStack c)
  {
    switch(state)
    {
      case FROZEN -> {
        state = State.UNFREEZING;
        queues.scheduleEnd();
        ServerNetworkHandler.sendUnfrozen(c.getLevel());
        Translations.m(c, "tickCommand.unfreeze.success");
        return Command.SINGLE_SUCCESS;
      }
      case FREEZING -> {
        state = State.UNFROZEN;
        ServerNetworkHandler.sendUnfrozen(c.getLevel());
        Translations.m(c, "tickCommand.unfreeze.success");
        return Command.SINGLE_SUCCESS;
      }
      case STEPPING -> {
        state = State.UNFREEZING;
        ServerNetworkHandler.sendUnfrozen(c.getLevel());
        Translations.m(c, "tickCommand.unfreeze.success");
        return Command.SINGLE_SUCCESS;
      }
      default -> {
        Translations.m(c, "tickCommand.unfreeze.err");
        return 0;
      }
    }
  }

  @Override
  public int toggleFreeze(CommandSourceStack c, int phase)
  {
    // unfrozen -> freeze
    // unfreezing -> error
    // frozen, freezing, stepping -> unfreeze
    switch(state)
    {
      case UNFROZEN -> {
        state = State.FREEZING;
        TickPhase tickPhase = new TickPhase(c.getLevel(), phase);
        targetPhase = tickPhase;
        ServerNetworkHandler.sendFrozen(c.getLevel(), tickPhase);
        Translations.m(c, "tickCommand.freeze.success", tickPhase);
        return Command.SINGLE_SUCCESS;
      }
      case UNFREEZING -> {
        Translations.m(c, "tickCommand.freeze.err.unfreezing");
        return 0;
      }
      case FROZEN -> {
        state = State.UNFREEZING;
        queues.scheduleEnd();
        ServerNetworkHandler.sendUnfrozen(c.getLevel());
        Translations.m(c, "tickCommand.unfreeze.success");
        return Command.SINGLE_SUCCESS;
      }
      case FREEZING -> {
        state = State.UNFROZEN;
        ServerNetworkHandler.sendUnfrozen(c.getLevel());
        Translations.m(c, "tickCommand.unfreeze.success");
        return Command.SINGLE_SUCCESS;
      }
      case STEPPING -> {
        state = State.UNFREEZING;
        ServerNetworkHandler.sendUnfrozen(c.getLevel());
        Translations.m(c, "tickCommand.unfreeze.success");
        return Command.SINGLE_SUCCESS;
      }
    }
    return 0;
  }

  @Override
  public int step(CommandSourceStack c, int ticks, int phase)
  {
    TickPhase tickPhase = new TickPhase(c.getLevel(), phase);
    if(!canStep(c, ticks, tickPhase)) return 0;

    if(ticks == 1)
      Translations.m(c, "tickCommand.step.success.single", tickPhase, 1);
    else
      Translations.m(c, "tickCommand.step.success.multiple", tickPhase, ticks);

    actor = c;
    state = State.STEPPING;
    remainingTicks = ticks;
    targetPhase = tickPhase;
    if(ticks != 0 || !tickPhase.equals(currentPhase))
    {
      queues.scheduleEnd();
      ServerNetworkHandler.sendTickStep(c.getLevel(), ticks, tickPhase);
    }
    return Command.SINGLE_SUCCESS;
  }

  @Override
  public int phaseStep(CommandSourceStack c, int count)
  {
    int currentPhaseInt = currentPhase.phase();
    int phase = currentPhaseInt + count;
    int ticks = phase/TickPhase.totalPhases;
    return step(c, phase < currentPhaseInt ? ticks + 1 : ticks, phase % TickPhase.totalPhases);
  }

  @Override
  public int stepToPhase(CommandSourceStack c, int phase, boolean force)
  {
    return phase < currentPhase.phase() && force ?
      step(c, 1, phase) :
      step(c, 0, phase);
  }

  public int step(CommandSourceStack c, int ticks, TickPhase phase)
  {
    state = State.STEPPING;
    remainingTicks = ticks;
    targetPhase = phase;
    if(ticks != 0 || !phase.equals(currentPhase))
    {
      queues.scheduleEnd();
      ServerNetworkHandler.sendTickStep(c.getLevel(), ticks, phase);
    }
    return Command.SINGLE_SUCCESS;
  }


  @Override
  public boolean canStep(CommandSourceStack c, int count, TickPhase phase)
  {
    if(state == State.STEPPING)
    {
      Translations.m(c, "tickCommand.step.err.stepping");
      return false;
    }

    if(state != State.FROZEN)
    {
      Translations.m(c, "tickCommand.step.err.notfrozen");
      return false;
    }

    if(count == 0 && phase.isPriorTo(currentPhase))
    {
      Translations.m(c, "tickCommand.step.err.backwards");
      return false;
    }

    if(queues.scheduled)
    {
      Translations.m(c, "tickCommand.step.err.qstepping");
      return false;
    }

    return true;
  }

  @Override
  public boolean canStep(int count, TickPhase phase)
  {
    if(state != State.FROZEN)
      return false;

    if(count == 0 && phase.isPriorTo(currentPhase))
      return false;

    if(queues.scheduled)
      return false;

    return true;
  }
}
