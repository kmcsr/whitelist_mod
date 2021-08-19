
package com.github.zyxgad.whitelist_mod;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.LiteralText;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.github.zyxgad.whitelist_mod.command.WhlistCommand;
import com.github.zyxgad.whitelist_mod.storage.WhiteListStorage;

public class WhiteListMod implements ModInitializer {
	public static WhiteListMod INSTANCE = null;
	public static final Logger LOGGER = LogManager.getLogger("WhiteList");

	private MinecraftServer server = null;
	private boolean isstarted = false;
	private File folder;

	public WhiteListMod(){
		this.folder = new File("whitelist");
		if(!this.folder.exists()){
			this.folder.mkdirs();
		}
		INSTANCE = this;
	}

	public MinecraftServer getServer(){
		return this.server;
	}

	public File getDataFolder(){
		return this.folder;
	}

	@Override
	public void onInitialize(){
		LOGGER.info("WhiteList is onInitialize");
		ServerLifecycleEvents.SERVER_STARTING.register(this::onStarting);
		ServerLifecycleEvents.SERVER_STARTED.register(this::onStarted);
		CommandRegistrationCallback.EVENT.register(this::onRegisterCommands);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onStopping);
	}

	public void onStarting(MinecraftServer server){
		this.isstarted = false;
		this.server = server;
		this.onReload();
	}

	public void onStarted(MinecraftServer server){
		this.isstarted = true;
	}

	public void onRegisterCommands(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated){
		WhlistCommand.register(dispatcher);
	}

	public void onReload(){
		WhiteListStorage.INSTANCE.reload();
		if(this.isstarted){
			this.checkUsers();
		}
	}

	public void onSave(){
		WhiteListStorage.INSTANCE.save();
	}

	public void onStopping(MinecraftServer server){
		this.onSave();
		this.server = null;
	}

	public void checkUsers(){
		this.server.getPlayerManager().getPlayerList().forEach((player)->{
			if(player != null){
				if(!WhiteListStorage.INSTANCE.checkUser(player.getGameProfile())){
					player.networkHandler.disconnect(createText("You are not in the white list."));
				}
			}
		});
	}

	public static Text createText(final String text){
		return new LiteralText(text);
	}

	public void addWhlUser(String name){
		WhiteListStorage.INSTANCE.addUser(name);
	}

	public boolean queryWhlUser(String name){
		return WhiteListStorage.INSTANCE.queryUser(name);
	}

	public boolean removeWhlUser(String name){
		if(WhiteListStorage.INSTANCE.removeUser(name)){
			final ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(name);
			if(player != null){
				if(!WhiteListStorage.INSTANCE.checkUser(player.getGameProfile())){
					player.networkHandler.disconnect(createText("You have just been removed from white list."));
				}
			}
			return true;
		}
		return false;
	}

	public String[] getWhlUsers(){
		return WhiteListStorage.INSTANCE.getUsers();
	}

	public void setWhlEnable(final boolean value){
		WhiteListStorage.INSTANCE.setEnable(value);
		if(value){
			this.checkUsers();
		}
	}

	public boolean getWhlEnable(){
		return WhiteListStorage.INSTANCE.getEnable();
	}
}
