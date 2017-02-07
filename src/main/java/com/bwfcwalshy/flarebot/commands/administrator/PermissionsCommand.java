package com.bwfcwalshy.flarebot.commands.administrator;

import com.bwfcwalshy.flarebot.FlareBot;
import com.bwfcwalshy.flarebot.MessageUtils;
import com.bwfcwalshy.flarebot.commands.Command;
import com.bwfcwalshy.flarebot.commands.CommandType;
import com.bwfcwalshy.flarebot.permissions.Group;
import com.bwfcwalshy.flarebot.permissions.User;
import com.bwfcwalshy.flarebot.util.Parser;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.IOException;
import java.util.stream.Collectors;

public class PermissionsCommand implements Command {

    @Override
    public void onCommand(net.dv8tion.jda.core.entities.User sender, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            MessageUtils.sendMessage(getDescription(), channel);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "givegroup":
                if (args.length < 3) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription(getDescription()), channel);
                    return;
                }
                Member user = Parser.mention(args[1], channel.getGuild());
                if (user == null) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("No such user!"), channel);
                    return;
                }
                if (!getPermissions(channel).hasGroup(args[2])) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("No such group!"), channel);
                    return;
                }
                Group group = getPermissions(channel).getGroup(args[2]);
                if (getPermissions(channel).getUser(user).addGroup(group))
                    MessageUtils.sendMessage("Success", channel);
                else
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("User already had that group!"), channel);
                break;
            case "revokegroup":
                if (args.length < 3) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription(getDescription()), channel);
                    return;
                }
                Member user2 = Parser.mention(args[1], channel.getGuild());
                if (user2 == null) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("No such user!"), channel);
                    return;
                }
                if (!getPermissions(channel).hasGroup(args[2])) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("No such group!"), channel);
                    return;
                }
                Group group2 = getPermissions(channel).getGroup(args[2]);
                if (getPermissions(channel).getUser(user2).removeGroup(group2))
                    MessageUtils.sendMessage("Success", channel);
                else
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("User never had that group!"), channel);
                break;
            case "groups":
                if(args.length == 1){
                    MessageUtils.sendMessage(MessageUtils.getEmbed(sender)
                            .setDescription("Groups: " + getPermissions(channel).getGroups().keySet().stream()
                                    .collect(Collectors.joining(", ", "`", "`"))), channel);
                    return;
                }
                Member iUser = Parser.mention(args[1], channel.getGuild());
                if (iUser == null) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("No such user!"), channel);
                    return;
                }
                User toList = getPermissions(channel).getUser(iUser);
                MessageUtils.sendMessage(MessageUtils.getEmbed(sender)
                        .addField("User", iUser.getAsMention(), true)
                        .setDescription("Groups: " + toList.getGroups().stream()
                                .collect(Collectors.joining(", ", "`", "`"))), channel);
                break;
            case "addpermission":
                if (args.length < 3) {
                    MessageUtils.sendMessage(getDescription(), channel);
                    return;
                }
                Group group3 = getPermissions(channel).getGroup(args[1]);
                if (getPermissions(channel).addPermission(group3.getName(), args[2]))
                    MessageUtils.sendMessage("Success", channel);
                else
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("Group already had that permission"), channel);
                break;
            case "removepermission":
                if (args.length < 3) {
                    MessageUtils.sendMessage(getDescription(), channel);
                    return;
                }
                Group group4 = getPermissions(channel).getGroup(args[1]);
                if (getPermissions(channel).removePermission(group4.getName(), args[2]))
                    MessageUtils.sendMessage("Success", channel);
                else
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("Group never had that permission"), channel);
                break;
            case "list":
                if (args.length < 2) {
                    MessageUtils.sendMessage(getDescription(), channel);
                    return;
                }
                if (!getPermissions(channel).hasGroup(args[1])) {
                    MessageUtils.sendErrorMessage(MessageUtils.getEmbed(sender).setDescription("No such group!"), channel);
                    return;
                }
                Group group5 = getPermissions(channel).getGroup(args[1]);
                StringBuilder perms = new StringBuilder("**Permissions for group ").append(group5.getName())
                        .append("**\n```fix\n");
                group5.getPermissions().forEach(perm -> perms.append(perm).append('\n'));
                perms.append("\n```");
                MessageUtils.sendMessage(perms, channel);
                break;
            case "save":
                if (getPermissions(channel).isCreator(sender))
                    try {
                        FlareBot.getInstance().getPermissions().save();
                    } catch (IOException e) {
                        MessageUtils.sendException("Could not save permissions!", e, channel);
                    }
                break;
            default:
                MessageUtils.sendMessage(getDescription(), channel);
                break;
        }
    }

    @Override
    public String getCommand() {
        return "permissions";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"perm", "perms"};
    }

    @Override
    public String getDescription() {
        return "`permissions givegroup` | `revokegroup` `<user> <group>` for user management.\n" +
                "Or `permissions list` | `addpermission` | `removepermission` `<group> <permission>` for group management.\n" +
                "`permissions groups <user>` lists groups of a user.\n" +
                "Better tutorial is found on the support server.";
    }

    @Override
    public CommandType getType() {
        return CommandType.ADMINISTRATIVE;
    }

    @Override
    public String getPermission() {
        return "flarebot.permissions";
    }
}
