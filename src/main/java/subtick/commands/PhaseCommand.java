package subtick.commands;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.ITickHandler;
import subtick.TickPhase;

public class PhaseCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("phaseStep")
      .then(argument("count", integer(1))
        .executes((c) -> ITickHandler.get(c).phaseStep(c.getSource(), getInteger(c, "count")))
      )
      .then(argument("phase", word())
        .suggests((c, b) -> suggest(TickPhase.commandSuggestions, b))
        .then(literal("force")
          .executes((c) -> ITickHandler.get(c).stepToPhase(c.getSource(), TickPhase.byCommandKey(getString(c, "phase")), true))
        )
        .executes((c) -> ITickHandler.get(c).stepToPhase(c.getSource(), TickPhase.byCommandKey(getString(c, "phase")), false))
      )
      .executes((c) -> ITickHandler.get(c).phaseStep(c.getSource(), 1))
    );
  }
}
