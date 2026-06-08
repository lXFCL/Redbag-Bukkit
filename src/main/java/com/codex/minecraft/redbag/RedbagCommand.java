package com.codex.minecraft.redbag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class RedbagCommand implements CommandExecutor, TabCompleter {
    private final RedbagPlugin plugin;
    private final RedbagDropGui dropGui;
    private RedbagService service;

    RedbagCommand(RedbagPlugin plugin, RedbagService service, RedbagDropGui dropGui) {
        this.plugin = plugin;
        this.service = service;
        this.dropGui = dropGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            return reload(sender);
        }
        if (!sender.hasPermission("redbag.use")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args[0].equalsIgnoreCase("send")) {
            return send(sender, args);
        }
        if (args[0].equalsIgnoreCase("code")) {
            return code(sender, args);
        }
        if (args[0].equalsIgnoreCase("grab") || args[0].equalsIgnoreCase("claim")) {
            return grab(sender, args);
        }
        if (args[0].equalsIgnoreCase("list")) {
            return list(sender);
        }
        if (args[0].equalsIgnoreCase("info")) {
            return info(sender, args);
        }
        sendHelp(sender);
        return true;
    }

    private boolean send(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("invalid-usage"));
            return true;
        }
        Double total = parseDouble(args[1]);
        Integer count = parseInt(args[2]);
        if (total == null || count == null) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return true;
        }
        String message = args.length > 3 ? join(args, 3) : "恭喜发财";
        dropGui.open((Player) sender, total, count, message);
        sender.sendMessage(plugin.msg("choose-drop-item"));
        return true;
    }

    private boolean code(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(plugin.msg("invalid-code-usage"));
            return true;
        }
        Double total = parseDouble(args[1]);
        Integer count = parseInt(args[2]);
        if (total == null || count == null) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return true;
        }
        String passphrase = args[3];
        String message = args.length > 4 ? join(args, 4) : "口令红包";
        RedbagService.CreateResult result = service.create((Player) sender, total, count, message, passphrase);
        if (!result.isSuccess()) {
            sender.sendMessage(plugin.msg(result.getMessageKey()).replace("{total}", Money.format(total)));
            return true;
        }
        Redbag redbag = result.getRedbag();
        sender.sendMessage(plugin.msg("created-code")
                .replace("{id}", String.valueOf(redbag.getId()))
                .replace("{total}", Money.format(redbag.getTotal()))
                .replace("{count}", String.valueOf(redbag.getCount()))
                .replace("{passphrase}", redbag.getPassphrase()));
        service.broadcastCreated(redbag);
        return true;
    }

    private boolean grab(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&e/redbag grab <id>"));
            return true;
        }
        Integer id = parseInt(args[1]);
        if (id == null) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return true;
        }
        String passphraseAnswer = args.length > 2 ? join(args, 2) : "";
        RedbagService.ClaimResult result = service.claim((Player) sender, id, passphraseAnswer);
        if (!result.isSuccess()) {
            sender.sendMessage(plugin.msg(result.getMessageKey()));
            return true;
        }
        sender.sendMessage(plugin.msg("grabbed")
                .replace("{id}", String.valueOf(result.getRedbag().getId()))
                .replace("{amount}", Money.format(result.getClaim().getAmount())));
        return true;
    }

    private boolean list(CommandSender sender) {
        List<Redbag> open = service.getOpenRedbags();
        if (open.isEmpty()) {
            sender.sendMessage(plugin.msg("list-empty"));
            return true;
        }
        sender.sendMessage(plugin.color("&c[红包]&r &e当前可领取红包:"));
        for (Redbag redbag : open) {
            sender.sendMessage(plugin.color("&7#" + redbag.getId() + " &f" + redbag.getOwnerName() + " &a" + Money.format(redbag.getRemaining()) + "&7/" + Money.format(redbag.getTotal()) + " &e" + redbag.getRemainingCount() + "份 &f" + redbag.getMessage()));
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.color("&e/redbag info <id>"));
            return true;
        }
        Integer id = parseInt(args[1]);
        if (id == null) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return true;
        }
        Redbag redbag = service.get(id);
        if (redbag == null) {
            sender.sendMessage(plugin.msg("not-found"));
            return true;
        }
        sender.sendMessage(plugin.color("&c[红包]&r &e#" + redbag.getId() + " &f" + redbag.getOwnerName() + " &7" + redbag.getMessage()));
        sender.sendMessage(plugin.color("&7总额: &e" + Money.format(redbag.getTotal()) + " &7剩余: &e" + Money.format(redbag.getRemaining()) + " &7份数: &e" + redbag.getClaims().size() + "/" + redbag.getCount()));
        if (redbag.hasPassphrase()) {
            sender.sendMessage(plugin.color("&7类型: &d口令红包 &7领取时需要输入口令。"));
        } else if (redbag.requiresDropItem()) {
            sender.sendMessage(plugin.color("&7类型: &a物品红包 &7手持右键: &e" + redbag.getClaimItemMaterial()));
        }
        for (Claim claim : redbag.getClaims().values()) {
            sender.sendMessage(plugin.color("&7- &f" + claim.getPlayerName() + " &e" + Money.format(claim.getAmount())));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("redbag.reload")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        plugin.reloadPlugin();
        this.service = plugin.getRedbagService();
        sender.sendMessage(plugin.msg("reloaded"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.color("&c[红包]&r &e/redbag send <总金额> <份数> [祝福语] &7打开物品选择界面"));
        sender.sendMessage(plugin.color("&c[红包]&r &e/redbag code <总金额> <份数> <口令> [祝福语]"));
        sender.sendMessage(plugin.color("&c[红包]&r &e/redbag grab <id> [口令]"));
        sender.sendMessage(plugin.color("&c[红包]&r &e/redbag list"));
        sender.sendMessage(plugin.color("&c[红包]&r &e/redbag info <id>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<String>(Arrays.asList("send", "code", "grab", "list", "info"));
            if (sender.hasPermission("redbag.reload")) {
                options.add("reload");
            }
            return filter(options, args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }

    private Double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
