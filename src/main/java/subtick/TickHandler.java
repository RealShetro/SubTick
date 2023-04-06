package subtick;

import static subtick.SubTick.t;
import static subtick.SubTick.d;

import net.minecraft.server.level.ServerLevel;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import carpet.utils.Messenger;

import carpet.helpers.TickSpeed;
import subtick.mixins.carpet.ServerNetworkHandlerAccessor;
import carpet.CarpetSettings;
import carpet.network.CarpetClient;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public class TickHandler
{
  public final ServerLevel level;
  public final Queues queues = new Queues(this);
  public long time;

  // Freeze
  public boolean frozen = false;
  public boolean freezing = false;
  private boolean unfreezing = false;
  private TickPhase target_phase = TickPhase.byId(0);
  // Step
  private boolean stepping = false;
  private boolean in_first_stepped_phase = false;
  private int remaining_ticks = 0;
  public TickPhase current_phase = TickPhase.byId(0);

  public TickHandler(ServerLevel level)
  {
    this.level = level;
    this.time = level.getGameTime();
  }

  public void tickTime()
  {
    time += 1L;
  }

  public boolean shouldTick(TickPhase phase)
  {
    // Freezing
    if(freezing && phase == target_phase)
    {
      freezing = false;
      frozen = true;
      current_phase = phase;
      updateFrozenStateToConnectedPlayers();
      return false;
    }

    // Normal ticking
    if(!frozen)
    {
      current_phase = phase;
      return true;
    }
    // Everything below this is frozen logic

    // Unfreezing
    if(unfreezing && phase == current_phase)
    {
      unfreezing = false;
      frozen = false;
      updateFrozenStateToConnectedPlayers();
      return true;
    }

    if(!stepping || phase != current_phase) return false;
    // Continues only if stepping and in current_phase

    // Stepping
    if(in_first_stepped_phase)
      updateTickPlayerActiveTimeoutToConnectedPlayers();
    else if(phase.isFirst())
      --remaining_ticks;

    in_first_stepped_phase = false;
    if(remaining_ticks < 1 && phase == target_phase)
    {
      stepping = false;
      queues.executeScheduledSteps();
      return false;
    }
    advancePhase();
    return true;
  }

  public void advancePhase()
  {
    current_phase = current_phase.next();
  }

  public void step(int ticks, TickPhase phase)
  {
    stepping = true;
    in_first_stepped_phase = true;
    remaining_ticks = ticks;
    target_phase = phase;
    if(ticks != 0 || phase != current_phase)
      queues.finishQueueStep();
  }

  public void freeze(TickPhase phase)
  {
    freezing = true;
    target_phase = phase;
  }

  public void unfreeze()
  {
    if(freezing) freezing = false;
    else
    {
      unfreezing = true;
      stepping = false;
      queues.finishQueueStep();
    }
  }

  public boolean canStep(CommandContext<CommandSourceStack> c, int count, TickPhase phase)
  {
    if(!frozen)
    {
      Messenger.m(c.getSource(), d(level), t(" cannot step because it's not frozen"));
      return false;
    }

    if(stepping)
    {
      Messenger.m(c.getSource(), d(level), t(" cannot step because it's already tick stepping"));
      return false;
    }

    if(count == 0 && phase.isPriorTo(current_phase))
    {
      Messenger.m(c.getSource(), d(level), t(" cannot step to an earlier phase in the same tick"));
      return false;
    }

    if(queues.scheduled != TickPhase.UNKNOWN)
    {
      Messenger.m(c.getSource(), d(level), t(" cannot step because it's already queueStepping"));
      return false;
    }

    return true;
  }

  public static void sendNbt(ServerPlayer player, CompoundTag tag)
  {
      FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
      packetBuf.writeVarInt(CarpetClient.DATA);
      packetBuf.writeNbt(tag);

      player.connection.send(new ClientboundCustomPayloadPacket(CarpetClient.CARPET_CHANNEL, packetBuf));
  }

  private void updateFrozenStateToConnectedPlayers()
  {
    if(CarpetSettings.superSecretSetting) return;

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;

      CompoundTag tag = new CompoundTag();
      CompoundTag tickingState = new CompoundTag();
      tickingState.putBoolean("is_paused", frozen);
      tickingState.putBoolean("deepFreeze", frozen);
      tag.put("TickingState", tickingState);

      sendNbt(player, tag);
    }
  }

  public void updateFrozenStateToConnectedPlayer(ServerPlayer player)
  {
    if(CarpetSettings.superSecretSetting) return;

    if(ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().containsKey(player))
    {
      CompoundTag tag = new CompoundTag();
      CompoundTag tickingState = new CompoundTag();
      tickingState.putBoolean("is_paused", frozen);
      tickingState.putBoolean("deepFreeze", frozen);
      tag.put("TickingState", tickingState);

      sendNbt(player, tag);
    }
  }

  private void updateTickPlayerActiveTimeoutToConnectedPlayers()
  {
    if(CarpetSettings.superSecretSetting) return;

    for(ServerPlayer player : ServerNetworkHandlerAccessor.getRemoteCarpetPlayers().keySet())
    {
      if(player.level != level) continue;

      CompoundTag tag = new CompoundTag();
      tag.putInt("TickPlayerActiveTimeout", remaining_ticks + TickSpeed.PLAYER_GRACE);

      sendNbt(player, tag);
    }
  }
}
