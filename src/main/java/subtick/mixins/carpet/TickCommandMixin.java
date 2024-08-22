package subtick.mixins.carpet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import carpet.commands.TickCommand;
import net.minecraft.commands.CommandSourceStack;
import subtick.ITickHandler;
import subtick.Settings;
import subtick.TickPhase;

@Mixin(TickCommand.class)
public class TickCommandMixin
{
  @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/arguments/IntegerArgumentType;integer(II)Lcom/mojang/brigadier/arguments/IntegerArgumentType;", ordinal = 0), index = 0, remap = false)
  private static int integer(int in)
  {
    return 0;
  }

  @Inject(method = "freezeStatus", at = @At("HEAD"), cancellable = true, remap = false)
  private static void freezeStatus(CommandSourceStack c, CallbackInfoReturnable<Integer> cir)
  {
    cir.setReturnValue(ITickHandler.get(c).when(c));
  }

  @Inject(method = "setFreeze", at = @At("HEAD"), cancellable = true, remap = false)
  private static void setFreeze(CommandSourceStack c, boolean isDeep, boolean freeze, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
  {
    if(freeze)
      cir.setReturnValue(ITickHandler.get(c).freeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
    else
      cir.setReturnValue(ITickHandler.get(c).unfreeze(c));
  }

  @Inject(method = "toggleFreeze", at = @At("HEAD"), cancellable = true, remap = false)
  private static void toggleFreeze(CommandSourceStack c, boolean isDeep, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
  {
    cir.setReturnValue(ITickHandler.get(c).toggleFreeze(c, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
  }

  @Inject(method = "step", at = @At("HEAD"), cancellable = true, remap = false)
  private static void step(CommandSourceStack c, int ticks, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException
  {
    cir.setReturnValue(ITickHandler.get(c).step(c, ticks, TickPhase.byCommandKey(Settings.subtickDefaultPhase)));
  }
}
