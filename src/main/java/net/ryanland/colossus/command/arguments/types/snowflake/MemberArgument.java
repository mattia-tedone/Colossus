package net.ryanland.colossus.command.arguments.types.snowflake;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.ryanland.colossus.command.arguments.ArgumentOptionData;
import net.ryanland.colossus.command.arguments.parsing.exceptions.ArgumentException;
import net.ryanland.colossus.events.MessageCommandEvent;
import net.ryanland.colossus.events.SlashCommandEvent;

public class MemberArgument extends SnowflakeArgument<Member> {

    @Override
    public ArgumentOptionData getArgumentOptionData() {
        return new ArgumentOptionData(OptionType.USER);
    }

    @Override
    public Member resolveSlashCommandArgument(OptionMapping arg, SlashCommandEvent event) throws ArgumentException {
        Member member = arg.getAsMember();
        if (member == null)
            throw new ArgumentException("The provided user must be a member of this server.");
        return member;
    }

    @Override
    public Member resolveMessageCommandArgument(MessageCommandEvent event, String id) throws ArgumentException {
        return event.getGuild().getMemberById(id);
    }

}
