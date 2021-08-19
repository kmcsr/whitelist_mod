
package com.github.zyxgad.whitelist_mod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import static net.minecraft.server.command.CommandManager.literal;

import com.github.zyxgad.whitelist_mod.WhiteListMod;
import com.github.zyxgad.whitelist_mod.storage.WhiteListStorage;

public final class WhlistCommand{
	private static final String HELP_MSG = new StringBuilder().
		append("`/whlist help` to get help message\n").
		append("`/whlist enable` [<true/false>] to query/set this mod enabled\n").
		append("`/whlist list` to show the white list\n").
		append("`/whlist add <name>` to add player to the white list\n").
		append("`/whlist remove <name>` to remove player from the white list\n").
		append("`/whlist query <name>` to query player is in the white list\n").
		append("`/whlist save` to save config file\n").
		append("`/whlist reload` to reload config file\n").
		toString();

	private static boolean isOp(ServerCommandSource context){
		return context.hasPermissionLevel(2);
	}

	private static boolean isHelper(ServerCommandSource context){
		return isOp(context) || (
			(context.getEntity() instanceof ServerPlayerEntity) &&
			WhiteListStorage.INSTANCE.isHelper(((ServerPlayerEntity)(context.getEntity())).getGameProfile().getName()));
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
		dispatcher.register(literal("whlist").
			executes((context)->{
				context.getSource().sendFeedback(WhiteListMod.createText(HELP_MSG), false);
				return Command.SINGLE_SUCCESS;
			}).
			then(literal("help").
				executes((context)->{
					context.getSource().sendFeedback(WhiteListMod.createText(HELP_MSG), false);
					return Command.SINGLE_SUCCESS;
			})).
			then(literal("enable").requires(WhlistCommand::isOp).
				then(RequiredArgumentBuilder.<ServerCommandSource, Boolean>argument("value", BoolArgumentType.bool()).executes((context)->{
					final boolean value = context.getArgument("value", Boolean.class).booleanValue();
					WhiteListMod.INSTANCE.setWhlEnable(value);
					context.getSource().sendFeedback(WhiteListMod.createText("Whitelist set to " + 
						(value ?"enable" :"disable") + " now"), true);
					return Command.SINGLE_SUCCESS;
				})).
				executes((context)->{
					context.getSource().sendFeedback(WhiteListMod.createText("Whitelist is " + 
						(WhiteListMod.INSTANCE.getWhlEnable() ?"enable" :"disable") + " now"), false);
					return Command.SINGLE_SUCCESS;
				})).
			then(literal("list").requires(WhlistCommand::isHelper).executes((context)->{
				final StringBuilder remsg = new StringBuilder();
				remsg.append('[');
				final String[] users = WhiteListMod.INSTANCE.getWhlUsers();
				for(String name: users){
					remsg.append(name);
					remsg.append(", ");
				}
				if(users.length > 0){
					remsg.delete(remsg.length() - 2, remsg.length());
				}
				remsg.append(']');
				context.getSource().sendFeedback(WhiteListMod.createText(remsg.toString()), false);
				return Command.SINGLE_SUCCESS;
			})).
			then(literal("add").requires(WhlistCommand::isHelper).then(
				RequiredArgumentBuilder.<ServerCommandSource, String>argument("name", StringArgumentType.word()).executes((context)->{
				final String name = context.getArgument("name", String.class);
				WhiteListMod.INSTANCE.addWhlUser(name);
				context.getSource().sendFeedback(WhiteListMod.createText("added \"" + name + "\" to white list"), true);
				return Command.SINGLE_SUCCESS;
			}))).
			then(literal("remove").requires(WhlistCommand::isHelper).then(
				RequiredArgumentBuilder.<ServerCommandSource, String>argument("name", StringArgumentType.word()).executes((context)->{
				final String name = context.getArgument("name", String.class);
				if(WhiteListMod.INSTANCE.removeWhlUser(name)){
					context.getSource().sendFeedback(WhiteListMod.createText("removed \"" + name + "\" from white list"), true);
				}else{
					context.getSource().sendFeedback(WhiteListMod.createText("Error: \"" + name + "\" not in white list"), false);
				}
				return Command.SINGLE_SUCCESS;
			}))).
			then(literal("query").requires(WhlistCommand::isHelper).then(
				RequiredArgumentBuilder.<ServerCommandSource, String>argument("name", StringArgumentType.word()).executes((context)->{
				final String name = context.getArgument("name", String.class);
				context.getSource().sendFeedback(WhiteListMod.createText(
					name + " " + (WhiteListMod.INSTANCE.queryWhlUser(name) ?"is" :"isn't") + " in white list"), false);
				return Command.SINGLE_SUCCESS;
			}))).
			then(literal("reload").requires(WhlistCommand::isOp).executes((context)->{
				WhiteListMod.INSTANCE.onReload();
				context.getSource().sendFeedback(WhiteListMod.createText("Reload whitelist SUCCESS"), true);
				return Command.SINGLE_SUCCESS;
			})).
			then(literal("save").requires(WhlistCommand::isOp).executes((context)->{
				WhiteListMod.INSTANCE.onSave();
				context.getSource().sendFeedback(WhiteListMod.createText("Save whitelist SUCCESS"), true);
				return Command.SINGLE_SUCCESS;
			})));
	}
}
