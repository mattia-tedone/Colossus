package net.ryanland.colossus.command.arguments.types.impl.number;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.ryanland.colossus.command.arguments.types.NumberArgument;
import net.ryanland.colossus.events.ContentCommandEvent;

public class LongArgument extends NumberArgument<Long> {

    @Override
    public Long parsed(OptionMapping argument, ContentCommandEvent event) {
        return argument.getAsLong();
    }
}
