package net.ryanland.colossus.command.arguments.types;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.ryanland.colossus.command.arguments.parsing.exceptions.ArgumentException;
import net.ryanland.colossus.command.arguments.parsing.exceptions.MalformedArgumentException;
import net.ryanland.colossus.events.MessageCommandEvent;
import net.ryanland.colossus.events.SlashEvent;

public abstract class NumberArgument<T> extends SingleArgument<T> {

    @Override
    public T resolveSlashCommandArgument(OptionMapping arg, SlashEvent event) throws ArgumentException {
        try {
            return resolveSlashCommandArgument(event, arg);
        } catch (NumberFormatException e) {
            throw new MalformedArgumentException("Invalid number provided.");
        }
    }

    @Override
    public T resolveMessageCommandArgument(String arg, MessageCommandEvent event) throws ArgumentException {
        try {
            return resolveMessageCommandArgument(event, arg);
        } catch (NumberFormatException e) {
            throw new MalformedArgumentException("Invalid number provided.");
        }
    }

    public abstract T resolveSlashCommandArgument(SlashEvent event, OptionMapping arg) throws ArgumentException;

    public abstract T resolveMessageCommandArgument(MessageCommandEvent event, String arg) throws ArgumentException;
}
