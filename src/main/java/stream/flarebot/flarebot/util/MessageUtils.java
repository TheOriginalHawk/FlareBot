package stream.flarebot.flarebot.util;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.commands.Command;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageUtils {

    private static FlareBot flareBot = FlareBot.getInstance();

    private static final Pattern INVITE_REGEX = Pattern
            .compile("(?:https?://)?discord(?:app\\.com/invite|\\.gg)/(\\S+?)");
    private static final Pattern LINK_REGEX = Pattern
            .compile("((http(s)?://)(www\\.)?)[a-zA-Z0-9-]+\\.[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+)?/?(.+)?");
    private static final Pattern YOUTUBE_LINK_REGEX = Pattern
            .compile("(http(s)?://)?(www\\.)?youtu(be\\.com)?(\\.be)?/(watch\\?v=)?[a-zA-Z0-9-_]+");

    private static final Pattern ESCAPE_MARKDOWN = Pattern.compile("[`~*_\\\\]");
    private static final Pattern SPACE = Pattern.compile(" ");

    private static final String ZERO_WIDTH_SPACE = "\u200B";

    public static void sendPM(User user, String message) {
        try {
            user.openPrivateChannel().complete()
                    .sendMessage(message.substring(0, Math.min(message.length(), 1999))).queue();
        } catch (ErrorResponseException ignored) {
        }
    }

    public static void sendPM(TextChannel channel, User user, String message) {
        try {
            user.openPrivateChannel().complete()
                    .sendMessage(message.substring(0, Math.min(message.length(), 1999))).queue();
        } catch (ErrorResponseException e) {
            channel.sendMessage(message).queue();
        }
    }

    public static void sendPM(TextChannel channel, User user, EmbedBuilder message) {
        try {
            user.openPrivateChannel().complete().sendMessage(message.build()).complete();
        } catch (ErrorResponseException e) {
            channel.sendMessage(message.build()).queue();
        }
    }

    public static void sendException(String s, Throwable e, TextChannel channel) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String trace = sw.toString();
        pw.close();
        sendErrorMessage(getEmbed().setDescription(s + "\n**Stack trace**: " + paste(trace)), channel);
    }

    public static void sendFatalException(String s, Throwable e, TextChannel channel) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String trace = sw.toString();
        pw.close();
        channel.sendMessage(new MessageBuilder().append(
                flareBot.getOfficialGuild().getRoleById(Constants.DEVELOPER_ID).getAsMention())
                .setEmbed(getEmbed().setColor(Color.red).setDescription(s + "\n**Stack trace**: " + paste(trace))
                        .build()).build()).queue();
    }

    public static String paste(String trace) {
        if (flareBot.getPasteKey() == null || flareBot.getPasteKey().isEmpty()) {
            FlareBot.LOGGER.warn("Paste server key is missing! Pastes will not work!");
            return null;
        }
        try {
            Response response = WebUtils.post(new Request.Builder().url("https://paste.flarebot.stream/documents")
                    .addHeader("Authorization", flareBot.getPasteKey()).post(RequestBody
                            .create(WebUtils.APPLICATION_JSON, trace)));
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            ResponseBody body = response.body();
            if (body != null) {
                String key = new JSONObject(body.string()).getString("key");
                return "https://paste.flarebot.stream/" + key;
            } else {
                FlareBot.LOGGER.error("Local instance of hastebin is down");
                return null;
            }
        } catch (IOException | JSONException e) {
            FlareBot.LOGGER.error("Could not make POST request to paste!", e);
            return null;
        }
    }

    public static void editMessage(Message message, String content) {
        message.editMessage(content).queue();
    }

    public static EmbedBuilder getEmbed() {
        return new EmbedBuilder()
                .setAuthor("FlareBot", "https://github.com/FlareBot/FlareBot", flareBot.getSelfUser()
                        .getEffectiveAvatarUrl());
    }

    public static String getTag(User user) {
        return user.getName() + '#' + user.getDiscriminator();
    }

    public static String getUserAndId(User user) {
        return getTag(user) + " (" + user.getId() + ")";
    }

    public static EmbedBuilder getEmbed(User user) {
        return getEmbed().setFooter("Requested by @" + getTag(user), user.getEffectiveAvatarUrl());
    }

    public static String getAvatar(User user) {
        return user.getEffectiveAvatarUrl();
    }

    public static String getDefaultAvatar(User user) {
        return user.getDefaultAvatarUrl();
    }

    public static void sendFatalErrorMessage(String s, TextChannel channel) {
        channel.sendMessage(new MessageBuilder().append(
                flareBot.getOfficialGuild().getRoleById(Constants.DEVELOPER_ID).getAsMention())
                .setEmbed(getEmbed().setColor(Color.red).setDescription(s).build()).build()).queue();
    }

    private static void sendMessage(MessageEmbed embed, TextChannel channel) {
        if (channel == null) return;
        channel.sendMessage(embed).queue();
    }

    public static void sendMessage(MessageType type, String message, TextChannel channel) {
        sendMessage(type, message, channel, null);
    }

    public static void sendMessage(MessageType type, String message, TextChannel channel, User sender) {
        sendMessage(type, message, channel, sender, 0);
    }

    public static void sendMessage(MessageType type, String message, TextChannel channel, long autoDeleteDelay) {
        sendMessage(type, message, channel, null, autoDeleteDelay);
    }

    public static void sendMessage(MessageType type, String message, TextChannel channel, User sender, long autoDeleteDelay) {
        sendMessage(type, (sender != null ? getEmbed(sender) : getEmbed()).setColor(type.getColor())
                .setTimestamp(OffsetDateTime.now(Clock.systemUTC()))
                .setDescription(GeneralUtils.formatCommandPrefix(channel, message)), channel, autoDeleteDelay);
    }

    // Root of sendMessage(Type, Builder, channel)
    public static void sendMessage(MessageType type, EmbedBuilder builder, TextChannel channel) {
        sendMessage(type, builder, channel, 0);
    }

    // Final root of sendMessage
    public static void sendMessage(MessageType type, EmbedBuilder builder, TextChannel channel, long autoDeleteDelay) {
        if (builder.build().getColor() == null)
            builder.setColor(type.getColor());
        if(type == MessageType.ERROR)
            builder.setDescription(builder.build().getDescription() + "\n\nIf you need more support join our " +
                    "[Support Server](" + FlareBot.INVITE_URL + ")! Our staff can support on any issue you may have! "
                    + FlareBot.getInstance().getEmoteById(386550693294768129L).getAsMention());
        if (autoDeleteDelay > 0)
            sendAutoDeletedMessage(builder.build(), autoDeleteDelay, channel);
        else
            sendMessage(builder.build(), channel);
    }

    public static void sendMessage(String message, TextChannel channel) {
        sendMessage(MessageType.NEUTRAL, message, channel);
    }

    public static void sendMessage(String message, TextChannel channel, User sender) {
        sendMessage(MessageType.NEUTRAL, message, channel, sender);
    }

    public static void sendErrorMessage(String message, TextChannel channel) {
        sendMessage(MessageType.ERROR, message, channel);
    }

    public static void sendErrorMessage(String message, TextChannel channel, User sender) {
        sendMessage(MessageType.ERROR, message, channel, sender);
    }

    public static void sendErrorMessage(EmbedBuilder builder, TextChannel channel) {
        sendMessage(MessageType.ERROR, builder, channel);
    }

    public static void sendWarningMessage(String message, TextChannel channel) {
        sendMessage(MessageType.WARNING, message, channel);
    }

    public static void sendWarningMessage(String message, TextChannel channel, User sender) {
        sendMessage(MessageType.WARNING, message, channel, sender);
    }

    public static void sendSuccessMessage(String message, TextChannel channel) {
        sendMessage(MessageType.SUCCESS, message, channel);
    }

    public static void sendSuccessMessage(String message, TextChannel channel, User sender) {
        sendMessage(MessageType.SUCCESS, message, channel, sender);
    }

    public static void sendInfoMessage(String message, TextChannel channel) {
        sendMessage(MessageType.INFO, message, channel);
    }

    public static void sendInfoMessage(String message, TextChannel channel, User sender) {
        sendMessage(MessageType.INFO, message, channel, sender);
    }

    public static void sendModMessage(String message, TextChannel channel) {
        sendMessage(MessageType.MODERATION, message, channel);
    }

    public static void sendModMessage(String message, TextChannel channel, User sender) {
        sendMessage(MessageType.MODERATION, message, channel, sender);
    }

    public static void editMessage(EmbedBuilder embed, Message message) {
        editMessage(message.getContentRaw(), embed, message);
    }

    public static void editMessage(String s, EmbedBuilder embed, Message message) {
        if (message != null)
            message.editMessage(new MessageBuilder().setContent((s == null ? ZERO_WIDTH_SPACE : s)).setEmbed(embed.build()).build()).queue();
    }

    public static boolean hasInvite(Message message) {
        return INVITE_REGEX.matcher(message.getContentRaw()).find();
    }

    public static boolean hasInvite(String message) {
        return INVITE_REGEX.matcher(message).find();
    }

    public static boolean hasLink(Message message) {
        return LINK_REGEX.matcher(message.getContentRaw()).find();
    }

    public static boolean hasLink(String message) {
        return LINK_REGEX.matcher(message).find();
    }

    public static boolean hasYouTubeLink(Message message) {
        return YOUTUBE_LINK_REGEX.matcher(message.getContentRaw()).find();
    }

    public static void autoDeleteMessage(Message message, long delay) {
        message.delete().queueAfter(delay, TimeUnit.MILLISECONDS);
    }

    public static void sendAutoDeletedMessage(Message message, long delay, MessageChannel channel) {
        channel.sendMessage(message).queue(msg -> autoDeleteMessage(msg, delay));
    }

    public static void sendAutoDeletedMessage(MessageEmbed messageEmbed, long delay, MessageChannel channel) {
        channel.sendMessage(messageEmbed).queue(msg -> autoDeleteMessage(msg, delay));
    }

    public static void sendUsage(Command command, TextChannel channel, User user, String[] args) {
        String title = capitalize(command.getCommand()) + " Usage";
        List<String> usages = UsageParser.matchUsage(command, args);

        String usage = GeneralUtils.formatCommandPrefix(channel, usages.stream().collect(Collectors.joining("\n")));
        EmbedBuilder b = getEmbed(user).setTitle(title, null).setDescription(usage).setColor(Color.RED);
        if (command.getExtraInfo() != null) {
            b.addField("Extra Info", command.getExtraInfo(), false);
        }
        if (command.getPermission() != null) {
            b.addField("Permission", command.getPermission() + "\n" +
                    "**Default Permission: **" + command.isDefaultPermission() + "\n" +
                    "**Beta Command: **" + command.isBetaTesterCommand(), false);
        }
        channel.sendMessage(b.build()).queue();

    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String makeAsciiTable(java.util.List<String> headers, java.util.List<java.util.List<String>> table, String footer) {
        return makeAsciiTable(headers, table, footer, "");
    }

    public static String makeAsciiTable(java.util.List<String> headers, java.util.List<java.util.List<String>> table, String footer, String lang) {
        StringBuilder sb = new StringBuilder();
        int padding = 1;
        int[] widths = new int[headers.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = 0;
        }
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).length() > widths[i]) {
                widths[i] = headers.get(i).length();
            }
        }
        for (java.util.List<String> row : table) {
            for (int i = 0; i < row.size(); i++) {
                String cell = row.get(i);
                if (cell.length() > widths[i]) {
                    widths[i] = cell.length();
                }
            }
        }
        sb.append("```").append(lang).append("\n");
        StringBuilder formatLine = new StringBuilder("|");
        for (int width : widths) {
            formatLine.append(" %-").append(width).append("s |");
        }
        formatLine.append("\n");
        sb.append(appendSeparatorLine("+", "+", "+", padding, widths));
        sb.append(String.format(formatLine.toString(), headers.toArray()));
        sb.append(appendSeparatorLine("+", "+", "+", padding, widths));
        for (java.util.List<String> row : table) {
            sb.append(String.format(formatLine.toString(), row.toArray()));
        }
        if (footer != null) {
            sb.append(appendSeparatorLine("+", "+", "+", padding, widths));
            sb.append(getFooter(footer, padding, widths));
        }
        sb.append(appendSeparatorLine("+", "+", "+", padding, widths));
        sb.append("```");
        return sb.toString();
    }

    private static String appendSeparatorLine(String left, String middle, String right, int padding, int... sizes) {
        boolean first = true;
        StringBuilder ret = new StringBuilder();
        for (int size : sizes) {
            if (first) {
                first = false;
                ret.append(left).append(StringUtils.repeat("-", size + padding * 2));
            } else {
                ret.append(middle).append(StringUtils.repeat("-", size + padding * 2));
            }
        }
        return ret.append(right).append("\n").toString();
    }

    public static String getFooter(String footer, int padding, int... sizes) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        int total = 0;
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            total += size + (i == sizes.length - 1 ? 0 : 1) + padding * 2;
        }
        sb.append(footer);
        sb.append(StringUtils.repeat(" ", total - footer.length()));
        sb.append("|\n");
        return sb.toString();
    }

    public static String getMessage(String[] args, int min) {
        return Arrays.stream(args).skip(min).collect(Collectors.joining(" ")).trim();
    }

    public static String getMessage(String[] args, int min, int max) {
        StringBuilder message = new StringBuilder();
        for (int index = min; index < max; index++) {
            message.append(args[index]).append(" ");
        }
        return message.toString().trim();
    }

    public static String escapeMarkdown(String s) {
        return ESCAPE_MARKDOWN.matcher(s).replaceAll("\\\\$0");
    }

    public static String getNextArgument(String message, String from) {
        if (!message.contains(from)) return null;

        String[] args = SPACE.split(message);
        if (args.length == 0) return message;
        for (int i = 0; i < args.length; i++) {
            if (args.length <= (i + 1)) return null;
            if (args[i].equals(from))
                if (args[i + 1] != null && !args[i + 1].isEmpty())
                    return args[i + 1];
                else
                    return null;
        }
        return null;
    }
}
