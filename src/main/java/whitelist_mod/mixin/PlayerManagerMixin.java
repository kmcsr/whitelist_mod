
package com.github.zyxgad.whitelist_mod.mixin;

import java.net.SocketAddress;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.zyxgad.whitelist_mod.WhiteListMod;
import com.github.zyxgad.whitelist_mod.storage.WhiteListStorage;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Inject(at=@At("HEAD"), cancellable=true,
		method="checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;")
	private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> info){
		if(!WhiteListStorage.INSTANCE.checkUser(profile)){
			info.setReturnValue(WhiteListMod.createText("You are not in the white list."));
		}
	}
}
