package subtick.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

import java.util.HashSet;
import java.util.Set;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import subtick.IQueues;
import subtick.Settings;
import subtick.queues.TickingQueue;

public class QueueCommand
{
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
  {
    dispatcher.register(
      literal("queueStep")
      .then(argument("queue", word()).suggests((c, b) -> suggest(TickingQueue.commandKeys, b))
        .then(argument("count", integer(1)).suggests((c, b) -> suggest(new HashSet<String>()
        // HACKY FIX because brigadier doesn't like 2 arguments both suggesting stuff
        {{
          Set<String> modes = TickingQueue.byCommandKey(getString(c, "queue")).getModes();
          if(modes.isEmpty())
            add("1");
          else
            addAll(modes);
         }}.toArray(new String[0]), b))
          .then(argument("range", integer(-1, 46340)).suggests((c, b) -> suggest(new String[]{"-1", "32"}, b))
            .then(literal("force")
              .executes((c) -> step(c.getSource(), getString(c, "queue"), "", getInteger(c, "count"), getInteger(c, "range"), true))
            )
            .executes((c) -> step(c.getSource(), getString(c, "queue"), "", getInteger(c, "count"), getInteger(c, "range"), false))
          )
          .then(literal("force")
            .executes((c) -> step(c.getSource(), getString(c, "queue"), "", getInteger(c, "count"), Settings.subtickDefaultRange, true))
          )
          .executes((c) -> step(c.getSource(), getString(c, "queue"), "", getInteger(c, "count"), Settings.subtickDefaultRange, false))
        )
        .then(argument("mode", word())
          .then(argument("count", integer(1)).suggests((c, b) -> suggest(new String[]{"1"}, b))
            .then(argument("range", integer(-1, 46340)).suggests((c, b) -> suggest(new String[]{"-1", "32"}, b))
              .then(literal("force")
                .executes((c) -> step(c.getSource(), getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), getInteger(c, "range"), true))
              )
              .executes((c) -> step(c.getSource(), getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), getInteger(c, "range"), false))
            )
            .then(literal("force")
              .executes((c) -> step(c.getSource(), getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), Settings.subtickDefaultRange, true))
            )
            .executes((c) -> step(c.getSource(), getString(c, "queue"), getString(c, "mode"), getInteger(c, "count"), Settings.subtickDefaultRange, false))
          )
          .then(literal("force")
            .executes((c) -> step(c.getSource(), getString(c, "queue"), getString(c, "mode"), 1, Settings.subtickDefaultRange, true))
          )
          .executes((c) -> step(c.getSource(), getString(c, "queue"), getString(c, "mode"), 1, Settings.subtickDefaultRange, false))
        )
        .then(literal("force")
          .executes((c) -> step(c.getSource(), getString(c, "queue"), "", 1, Settings.subtickDefaultRange, true))
        )
        .executes((c) -> step(c.getSource(), getString(c, "queue"), "", 1, Settings.subtickDefaultRange, false))
      )
    );
  }

  private static int step(CommandSourceStack c, String commandKey, String modeKey, int count, int range, boolean force) throws CommandSyntaxException
  {
    IQueues.get(c).schedule(c, TickingQueue.byCommandKey(commandKey), modeKey, count,
    //#if MC >= 11904
    //$$ BlockPos.containing(c.getPosition()),
    //#else
    new BlockPos(c.getPosition()),
    //#endif
    range, force);
    return Command.SINGLE_SUCCESS;
  }
}
