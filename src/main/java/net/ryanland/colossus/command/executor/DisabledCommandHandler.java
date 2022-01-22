package net.ryanland.colossus.command.executor;

import net.dv8tion.jda.api.entities.SelfUser;
import net.ryanland.colossus.Colossus;
import net.ryanland.colossus.command.Command;
import net.ryanland.colossus.command.CommandException;
import net.ryanland.colossus.sys.file.database.Table;
import net.ryanland.colossus.sys.file.serializer.Serializer;

import java.util.ArrayList;
import java.util.List;

public class DisabledCommandHandler {

    private static final DisabledCommandHandler INSTANCE = new DisabledCommandHandler();

    public static DisabledCommandHandler getInstance() {
        return INSTANCE;
    }

    public List<Command> getDisabledCommands() {
        return Colossus.getDisabledCommandsSerializer().deserialize(Colossus.getGlobalTable().get("_dc", new ArrayList<>()));
    }

    public boolean isDisabled(Command command) {
        return getDisabledCommands().contains(command);
    }

    public Serializer<?, List<Command>> getSerializer() {
        return Colossus.getDisabledCommandsSerializer();
    }

    public void enable(Command command) throws CommandException {
        Table<SelfUser> table = Colossus.getGlobalTable();
        List<Command> disabled = getDisabledCommands();
        if (!disabled.contains(command)) {
            throw new CommandException("This command is already enabled.");
        }
        disabled.remove(command);
        table.put("_dc", getSerializer().serialize(disabled));
        Colossus.getDatabaseDriver().updateTable(Colossus.getSelfUser(), table);
    }

    public void disable(Command command) throws CommandException {
        Table<SelfUser> table = Colossus.getGlobalTable();
        List<Command> disabled = getDisabledCommands();
        if (disabled.contains(command))
            throw new CommandException("This command is already disabled.");
        disabled.add(command);
        table.put("_dc", getSerializer().serialize(disabled));
        Colossus.getDatabaseDriver().updateTable(Colossus.getSelfUser(), table);
    }

}
