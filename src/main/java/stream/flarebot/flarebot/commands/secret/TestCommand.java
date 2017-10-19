package stream.flarebot.flarebot.commands.secret;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.objects.guilds.GuildWrapper;

import java.util.Base64;

public class TestCommand implements Command {

    @Override
    public void onCommand(User sender, GuildWrapper guild, TextChannel channel, Message message, String[] args, Member member) {
        if (!guild.getOptions().hasOption("commands.delete-command-message")) {
            guild.getOptions().setOption("commands.delete-command-message", true);
        }

        if(args.length == 1) {
            Boolean b = Boolean.valueOf(args[0]);
            guild.getOptions().setOption("commands.delete-command-message", b);
        }
        channel.sendMessage("Option `commands.delete-command-message` set to: "
                + guild.getOptions().getBoolean("commands.delete-command-message")).queue();
    }

    @Override
    public String getCommand() {
        return "test";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getUsage() {
        return "{%}test";
    }

    @Override
    public CommandType getType() {
        return CommandType.SECRET;
    }
}
