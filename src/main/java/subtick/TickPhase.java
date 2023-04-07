package subtick;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

public enum TickPhase
{
  UNKNOWN          (null              , null),
  WORLD_BORDER     ("worldBorder"     , "World Border"),
  WEATHER          ("weather"         , "Weather"),
  TIME             ("time"            , "Time"),
  BLOCK_TICK       ("blockTick"       , "Block Tick"  , "Block Ticks"),
  FLUID_TICK       ("fluidTick"       , "Fluid Tick"  , "Block Ticks"),
  RAID             ("raid"            , "Raid"),
  CHUNK            ("chunk"           , "Chunk"),
  BLOCK_EVENT      ("blockEvent"      , "Block Event" , "Block Events"),
  ENTITY           ("entity"          , "Entity"      , "Entities"),
  BLOCK_ENTITY     ("blockEntity"     , "Block Entity", "Block Entities"),
  ENTITY_MANAGEMENT("entityManagement", "Entity Management");

  private static final TickPhase[] BY_ID;
  private static final Map<String, TickPhase> BY_COMMAND_KEY;

  public static final DynamicCommandExceptionType INVALID_TICK_PHASE_EXCEPTION = new DynamicCommandExceptionType(key -> new LiteralMessage("Invalid tick phase '" + key + "'"));

  static 
  {
    TickPhase[] values = SubTick.getTickPhaseOrder();
    BY_ID = new TickPhase[values.length];
    BY_COMMAND_KEY = new HashMap<>();

    int id = 0;

    for (TickPhase phase : values)
    {
      if (phase == null || phase == UNKNOWN)
        throw new IllegalStateException("invalid tick phase " + phase + " provided in tick phase order!");
      if (phase.id != -1)
        throw new IllegalStateException("tick phase " + phase + " appears multiple times in tick phase order!");
      phase.id = id++;
      BY_ID[phase.id] = phase;
      BY_COMMAND_KEY.put(phase.commandKey, phase);
    }
  }

  private final String commandKey;
  private final String nameSingle;
  private final String nameMultiple;

  private int id = -1;

  private TickPhase(String commandKey, String name)
  {
    this(commandKey, name, name);
  }

  private TickPhase(String commandKey, String nameSingle, String nameMultiple)
  {
    this.commandKey = commandKey;
    this.nameSingle = nameSingle;
    this.nameMultiple = nameMultiple;
  }

  @Override
  public String toString() {
    return commandKey == null ? "UNKNOWN" : commandKey + "[" + id + "]";
  }

  public int getId()
  {
    return id;
  }

  public String getCommandKey()
  {
    return commandKey;
  }

  public String getName()
  {
    return getName(1);
  }

  public String getName(int count)
  {
    return count == 1 ? nameSingle : nameMultiple;
  }

  public boolean exists()
  {
    return id >= 0;
  }

  public boolean isFirst()
  {
    return id == 0;
  }

  public boolean isPriorTo(TickPhase other)
  {
    return id < other.id;
  }

  public boolean isPosteriorTo(TickPhase other)
  {
    return id > other.id;
  }

  public TickPhase next()
  {
    return next(1);
  }

  public TickPhase next(int count)
  {
    return BY_ID[(id + count) % BY_ID.length];
  }

  public static TickPhase byId(int id)
  {
    return id >= 0 && id < BY_ID.length ? BY_ID[id] : UNKNOWN;
  }

  public static TickPhase byCommandKey(String key) throws CommandSyntaxException
  {
    TickPhase phase = BY_COMMAND_KEY.get(key);
    if (phase == null || !phase.exists())
      throw INVALID_TICK_PHASE_EXCEPTION.create(key);
    return phase;
  }

  public static Collection<String> getCommandKeys()
  {
    return BY_COMMAND_KEY.keySet();
  }
}