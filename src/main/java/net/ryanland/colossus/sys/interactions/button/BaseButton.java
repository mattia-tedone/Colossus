package net.ryanland.colossus.sys.interactions.button;

import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.ryanland.colossus.Colossus;
import net.ryanland.colossus.command.executor.functional_interface.CommandConsumer;
import net.ryanland.colossus.command.executor.functional_interface.CommandPredicate;
import net.ryanland.colossus.events.ClickButtonEvent;
import net.ryanland.colossus.sys.message.PresetBuilder;

import java.util.List;

/**
 * A Colossus Button. Also see the static methods within this record for helper constructors.
 * @param button The JDA Button object
 * @param onClick What to do when this button is clicked, with the click event given
 */
public record BaseButton(Button button, CommandConsumer<ClickButtonEvent> onClick) {

    /**
     * Create a button which only works if the provided predicate is true, and do something if false
     */
    public static BaseButton predicate(CommandPredicate<ClickButtonEvent> predicate,
                                       CommandConsumer<ClickButtonEvent> ifFalse, Button button,
                                       CommandConsumer<ClickButtonEvent> onClick) {
        return new BaseButton(button, event -> {
            if (!predicate.check(event)) ifFalse.consume(event);
            else if (onClick != null) onClick.consume(event);
        });
    }

    /**
     * Create a button which only one user can press
     */
    public static BaseButton user(Long userId, Button button,
                                  CommandConsumer<ClickButtonEvent> onClick) {
        return group(new Long[]{ userId }, button, onClick);
    }

    /**
     * Create a button which only a specific group of users can press
     */
    public static BaseButton group(Long[] userIds, Button button,
                                   CommandConsumer<ClickButtonEvent> onClick) {
        return predicate(event -> List.of(userIds).contains(event.getUser().getIdLong()),
            event -> event.reply(new PresetBuilder(Colossus.getErrorPresetType())
                .setTitle("Not Allowed")
                .setDescription("You're not allowed to use this button.")
            ), button, onClick);
    }
}
