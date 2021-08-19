
package com.github.zyxgad.whitelist_mod.storage;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import com.github.zyxgad.whitelist_mod.WhiteListMod;

public class WhiteListStorage{
	public static final WhiteListStorage INSTANCE = new WhiteListStorage();
	private WhiteListStorage(){}

	static class Item{
		UUID uuid;
		String name;
		Item(UUID uuid, String name){
			this.uuid = uuid;
			this.name = name;
		}

		Item(String name){
			this(null, name);
		}

		Item(){
			this(null, null);
		}

		void toJson(final JsonWriter writer) throws IOException{
			writer.beginObject();
			writer.name("uuid");
			if(this.uuid != null){
				writer.value(this.uuid.toString());
			}else{
				writer.nullValue();
			}
			writer.name("name");
			writer.value(this.name);
			writer.endObject();
		}

		static Item fromJson(final JsonReader reader) throws IOException{
			final Item item = new Item();
			reader.beginObject();
			while(reader.hasNext()){
				final String key = reader.nextName();
				if(key.equals("uuid")){
					if(reader.peek() == JsonToken.NULL){
						reader.nextNull();
						item.uuid = null;
					}else{
						item.uuid = UUID.fromString(reader.nextString());
					}
				}else if(key.equals("name")){
					item.name = reader.nextString();
				}else{
					reader.skipValue();
				}
			}
			reader.endObject();
			return item;
		}

		public int hashCode(){
			return this.name.toUpperCase().hashCode();
		}

		public boolean equals(Object other){
			return other == this || (other instanceof Item && this.name.equalsIgnoreCase(((Item)other).name));
		}
	}

	private boolean enable = true;
	private Set<Item> storage = new HashSet<>();
	private Set<String> helpers = new HashSet<>();

	public void setEnable(boolean enable){
		this.enable = enable;
	}

	public boolean getEnable(){
		return this.enable;
	}

	public void addUser(String name){
		final ServerPlayerEntity player = WhiteListMod.INSTANCE.getServer().getPlayerManager().getPlayer(name);
		if(player != null){
			name = player.getGameProfile().getName();
			this.storage.add(new Item(player.getGameProfile().getId(), name));
		}else{
			this.storage.add(new Item(name));
		}
	}

	public boolean checkUser(final GameProfile profile){
		final UUID userid = profile.getId();
		final String name = profile.getName();
		final Iterator<Item> iter = this.storage.iterator();
		Item i;
		while(iter.hasNext()){
			i = iter.next();
			if(i.uuid == null){
				if(name.equalsIgnoreCase(i.name)){
					i.uuid = userid;
					i.name = name;
					iter.remove();
					this.storage.add(i);
					return true;
				}
			}else if(userid.equals(i.uuid)){
				if(!name.equals(i.name)){
					i.name = name;
					iter.remove();
					this.storage.add(i);
				}
				return true;
			}
		}
		return !this.enable;
	}

	public boolean queryUser(final String name){
		final Iterator<Item> iter = this.storage.iterator();
		Item i;
		while(iter.hasNext()){
			i = iter.next();
			if(name.equalsIgnoreCase(i.name)){
				return true;
			}
		}
		return false;
	}

	public boolean removeUser(final String name){
		final Iterator<Item> iter = this.storage.iterator();
		Item i;
		while(iter.hasNext()){
			i = iter.next();
			if(name.equalsIgnoreCase(i.name)){
				iter.remove();
				return true;
			}
		}
		return false;
	}

	public boolean isHelper(final String name){
		return this.helpers.contains(name);
	}

	public String[] getUsers(){
		final List<String> users = new ArrayList<>(this.storage.size());
		for(Item i: this.storage){
			users.add(i.name);
		}
		return users.toArray(new String[0]);
	}

	public void reload(){
		this.enable = true;
		this.storage.clear();
		this.helpers.clear();
		final File file = new File(WhiteListMod.INSTANCE.getDataFolder(), "whitelist.json");
		if(!file.exists()){
			return;
		}
		try(
			FileReader freader = new FileReader(file);
			JsonReader jreader = new JsonReader(freader);
		){
			jreader.beginObject();
			while(jreader.hasNext()){
				final String key = jreader.nextName();
				if(key.equals("enable")){
					this.enable = jreader.nextBoolean();
				}else if(key.equals("whitelist")){
					jreader.beginArray();
					while(jreader.hasNext()){
						this.storage.add(Item.fromJson(jreader));
					}
					jreader.endArray();
				}else if(key.equals("helpers")){
					jreader.beginArray();
					while(jreader.hasNext()){
						this.helpers.add(jreader.nextString());
					}
					jreader.endArray();
				}else{
					jreader.skipValue();
				}
			}
			jreader.endObject();
		}catch(IOException e){
			WhiteListMod.LOGGER.error("load bot config file error:\n", e);
		}
	}

	public void save(){
		final File file = new File(WhiteListMod.INSTANCE.getDataFolder(), "whitelist.json");
		if(!file.exists()){
			try{
				file.createNewFile();
			}catch(IOException e){
				WhiteListMod.LOGGER.error("create bot config file error:\n", e);
				return;
			}
		}
		try(
			FileWriter fwriter = new FileWriter(file);
			JsonWriter jwriter = new JsonWriter(fwriter);
		){
			jwriter.setIndent("  ");
			jwriter.beginObject();
			jwriter.name("enable");
			jwriter.value(this.enable);
			jwriter.name("whitelist");
			jwriter.beginArray();
			for(Item i: this.storage){
				i.toJson(jwriter);
			}
			jwriter.endArray();
			jwriter.name("helper");
			jwriter.beginArray();
			for(String i: this.helpers){
				jwriter.value(i);
			}
			jwriter.endArray();
			jwriter.endObject();
		}catch(IOException e){
			WhiteListMod.LOGGER.error("save bot config file error:\n", e);
		}
	}
}