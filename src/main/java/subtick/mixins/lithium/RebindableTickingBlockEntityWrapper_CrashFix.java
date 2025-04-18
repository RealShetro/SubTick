package subtick.mixins.lithium;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/world/level/chunk/LevelChunk$RebindableTickingBlockEntityWrapper")
public class RebindableTickingBlockEntityWrapper_CrashFix
{
    @Shadow
    private TickingBlockEntity ticker;

    @Inject(method = "rebind", at = @At(value = "HEAD"), cancellable = true)
    private void sleepingBlockEntity_NullPosCrashFix(TickingBlockEntity tickingBlockEntity, CallbackInfo ci)
    {
        if (tickingBlockEntity.getType().equals("<lithium_sleeping>"))
        {
            BlockPos pos = this.ticker.getPos();
            String type = this.ticker.getType();

            this.ticker = new TickingBlockEntity() {
                @Override
                public void tick() {
                }

                @Override
                public boolean isRemoved() {
                    return false;
                }

                @Override
                public BlockPos getPos() {
                    return pos;
                }

                @Override
                public String getType() {
                    return type;
                }
            };
            ci.cancel();
        }
    }
}
