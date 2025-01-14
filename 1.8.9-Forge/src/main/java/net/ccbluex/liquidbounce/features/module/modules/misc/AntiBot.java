/*
 * AimWhere Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc;

import me.kiras.aimwhere.utils.timer.TimerUtil;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S38PacketPlayerListItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.network.play.server.S38PacketPlayerListItem.Action.ADD_PLAYER;

@ModuleInfo(
        name = "AntiBot",
        description = "Prevents KillAura from attacking AntiCheat bots.",
        category = ModuleCategory.WORLD
)
public class AntiBot extends Module {
    private static final ListValue modeValue = new ListValue("Mode", new String[]{"Custom","AntiCheat","Hypixel","Mineplex","Syuu"},"Custom");
    private final BoolValue tabValue = new BoolValue("Tab", true);
    private final ListValue tabModeValue = new ListValue("TabMode", new String[] {"Equals", "Contains"}, "Contains");
    private final BoolValue entityIDValue = new BoolValue("EntityID", true);
    private final BoolValue colorValue = new BoolValue("Color", false);
    private final BoolValue livingTimeValue = new BoolValue("LivingTime", false);
    private final IntegerValue livingTimeTicksValue = new IntegerValue("LivingTimeTicks", 40, 1, 200);
    private final BoolValue groundValue = new BoolValue("Ground", true);
    private final BoolValue airValue = new BoolValue("Air", false);
    private final BoolValue invalidGroundValue = new BoolValue("InvalidGround", true);
    private final BoolValue swingValue = new BoolValue("Swing", false);
    private final BoolValue healthValue = new BoolValue("Health", false);
    private final BoolValue derpValue = new BoolValue("Derp", true);
    private final BoolValue wasInvisibleValue = new BoolValue("WasInvisible", false);
    private final BoolValue armorValue = new BoolValue("Armor", false);
    private final BoolValue pingValue = new BoolValue("Ping", false);
    private final BoolValue needHitValue = new BoolValue("NeedHit", false);
    private final BoolValue duplicateInWorldValue = new BoolValue("DuplicateInWorld", false);
    private final BoolValue duplicateInTabValue = new BoolValue("DuplicateInTab", false);
    private final List<Integer> ground = new ArrayList<>();
    private final List<Integer> air = new ArrayList<>();
    private final Map<Integer, Integer> invalidGround = new HashMap<>();
    private final List<Integer> swing = new ArrayList<>();
    private final List<Integer> invisible = new ArrayList<>();
    private final List<Integer> hitted = new ArrayList<>();
    private final List<EntityPlayer> removedBots = new ArrayList<>();
    private final TimerUtil removeTimer = new TimerUtil();
    @Override
    public void onDisable() {
        clearAll();
        super.onDisable();
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if(mc.thePlayer == null || mc.theWorld == null)
            return;

        final Packet<?> packet = event.getPacket();
        S38PacketPlayerListItem playerListItem;
        if (modeValue.get().equals("AntiCheat")) {
            if (packet instanceof S38PacketPlayerListItem) {
                if ((playerListItem = (S38PacketPlayerListItem) packet).getAction() == ADD_PLAYER) {
                    for(Entity i : mc.theWorld.loadedEntityList) {
                        if(!(i instanceof EntityLivingBase))
                            continue;
                        EntityLivingBase entityLivingBase = (EntityLivingBase) i;
                        for(S38PacketPlayerListItem.AddPlayerData j : playerListItem.getEntries()) {
                            if(!(entityLivingBase instanceof EntityPlayerSP) && j.getProfile().getName().equalsIgnoreCase(entityLivingBase.getName()) && (j.getProfile().getId() != entityLivingBase.getUniqueID() || j.getProfile().getId() != entityLivingBase.getPersistentID())) {
                                ClientUtils.displayChatMessage(("\u00a77[\u00a79AntiBot\u00a77] \u00a79Removed a bot: \u00a74" + j.getProfile().getName()));
                                event.cancelEvent();
                            }
                        }
                    }
                }
            }
        }
        if(packet instanceof S14PacketEntity) {
            final S14PacketEntity packetEntity = (S14PacketEntity) event.getPacket();
            final Entity entity = packetEntity.getEntity(mc.theWorld);

            if(entity instanceof EntityPlayer) {
                if(packetEntity.getOnGround() && !ground.contains(entity.getEntityId()))
                    ground.add(entity.getEntityId());

                if(!packetEntity.getOnGround() && !air.contains(entity.getEntityId()))
                    air.add(entity.getEntityId());

                if(packetEntity.getOnGround()) {
                    if(entity.prevPosY != entity.posY)
                        invalidGround.put(entity.getEntityId(), invalidGround.getOrDefault(entity.getEntityId(), 0) + 1);
                }else{
                    final int currentVL = invalidGround.getOrDefault(entity.getEntityId(), 0) / 2;

                    if (currentVL <= 0)
                        invalidGround.remove(entity.getEntityId());
                    else
                        invalidGround.put(entity.getEntityId(), currentVL);
                }

                if(entity.isInvisible() && !invisible.contains(entity.getEntityId()))
                    invisible.add(entity.getEntityId());
            }
        }

        if(packet instanceof S0BPacketAnimation) {
            final S0BPacketAnimation packetAnimation = (S0BPacketAnimation) event.getPacket();
            final Entity entity = mc.theWorld.getEntityByID(packetAnimation.getEntityID());

            if(entity instanceof EntityLivingBase && packetAnimation.getAnimationType() == 0 && !swing.contains(entity.getEntityId()))
                swing.add(entity.getEntityId());
        }
    }

    @EventTarget
    public void onAttack(final AttackEvent e) {
        final Entity entity = e.getTargetEntity();
        if(entity instanceof EntityLivingBase && !hitted.contains(entity.getEntityId()))
            hitted.add(entity.getEntityId());
    }

    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        if(removeTimer.hasReached(3000)) {
            if(removedBots.size() != 0)
                ClientUtils.displayChatMessage(("\u00a77[\u00a79BotKiller\u00a77] \u00a79Removed " + removedBots.size() + " Bots"));
            removedBots.clear();
            removeTimer.reset();
        }
        switch(modeValue.get()) {
            case "Mineplex": {
                for (EntityPlayer e : mc.theWorld.playerEntities) {
                    if (e == null) continue;
                    if (e.ticksExisted >= 2 || !(e.getHealth() < 20.0f) || !(e.getHealth() > 0.0f) || e == mc.thePlayer)
                        continue;
                    removedBots.add(e);
                    mc.theWorld.removeEntity(e);
                }
                break;
            }
            case "Hypixel": {
                for (EntityPlayer i : mc.theWorld.playerEntities) {
                    if (i == null || i == mc.thePlayer)
                        continue;
                    if (i.getName().contains("\u00A7") || (i.hasCustomName() && i.getCustomNameTag().contains(i.getName()))) {
                        removedBots.add(i);
                        mc.theWorld.removeEntity(i);
                    }
                }
                break;
            }
            case "Syuu": {
                for (EntityPlayer i : mc.theWorld.playerEntities) {
                    ItemStack[] stacks = i.inventory.armorInventory;
                    Item item1 = stacks[0].getItem();
                    Item item2 = stacks[1].getItem();
                    Item item3 = stacks[2].getItem();
                    Item item4 = stacks[3].getItem();
                    if (i.getHealth() == 3838 && (isFood(item1) || isFood(item2) || isFood(item3) || isFood(item4))) {
                        ClientUtils.displayChatMessage(("\u00a77[\u00a79BotKiller\u00a77] \u00a79Removed a bot: \u00a74" + i.getName() + " \u00a79Coords \u00a7bX:\u00a7f" + Math.round(i.posX) + "\u00a7bY:\u00a7f" + Math.round(i.posY) + "\u00a7bZ:\u00a7f" + Math.round(i.posZ)));
                        mc.theWorld.removeEntity(i);
                        removedBots.add(i);
                    }
                }
                break;
            }
        }
    }

    private boolean isFood(Item item) {
        return item instanceof ItemFood;
    }

    @EventTarget
    public void onWorld(final WorldEvent event) {
        clearAll();
    }

    private void clearAll() {
        removedBots.clear();
        hitted.clear();
        swing.clear();
        ground.clear();
        invalidGround.clear();
        invisible.clear();
    }

    public static boolean isBot(final EntityLivingBase entity) {
        if(!modeValue.get().equals("Custom"))
            return false;
        if (!(entity instanceof EntityPlayer))
            return false;

        final AntiBot antiBot = LiquidBounce.moduleManager.getModule(AntiBot.class);

        if (!antiBot.getState())
            return false;

        if (antiBot.colorValue.get() && !entity.getDisplayName().getFormattedText()
                .replace("§r", "").contains("§"))
            return true;

        if (antiBot.livingTimeValue.get() && entity.ticksExisted < antiBot.livingTimeTicksValue.get())
            return true;

        if (antiBot.groundValue.get() && !antiBot.ground.contains(entity.getEntityId()))
            return true;

        if (antiBot.airValue.get() && !antiBot.air.contains(entity.getEntityId()))
            return true;

        if(antiBot.swingValue.get() && !antiBot.swing.contains(entity.getEntityId()))
            return true;

        if(antiBot.healthValue.get() && entity.getHealth() > 20F)
            return true;

        if(antiBot.entityIDValue.get() && (entity.getEntityId() >= 1000000000 || entity.getEntityId() <= -1))
            return true;

        if(antiBot.derpValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true;

        if(antiBot.wasInvisibleValue.get() && antiBot.invisible.contains(entity.getEntityId()))
            return true;

        if(antiBot.armorValue.get()) {
            final EntityPlayer player = (EntityPlayer) entity;

            if (player.inventory.armorInventory[0] == null && player.inventory.armorInventory[1] == null &&
                    player.inventory.armorInventory[2] == null && player.inventory.armorInventory[3] == null)
                return true;
        }

        if(antiBot.pingValue.get()) {
            EntityPlayer player = (EntityPlayer) entity;

            if(mc.getNetHandler().getPlayerInfo(player.getUniqueID()).getResponseTime() == 0)
                return true;
        }

        if(antiBot.needHitValue.get() && !antiBot.hitted.contains(entity.getEntityId()))
            return true;

        if(antiBot.invalidGroundValue.get() && antiBot.invalidGround.getOrDefault(entity.getEntityId(), 0) >= 10)
            return true;

        if(antiBot.tabValue.get()) {
            final boolean equals = antiBot.tabModeValue.get().equalsIgnoreCase("Equals");
            final String targetName = ColorUtils.stripColor(entity.getDisplayName().getFormattedText());

            if (targetName != null) {
                for (final NetworkPlayerInfo networkPlayerInfo : mc.getNetHandler().getPlayerInfoMap()) {
                    final String networkName = ColorUtils.stripColor(EntityUtils.getName(networkPlayerInfo));

                    if (networkName == null)
                        continue;

                    if (equals ? targetName.equals(networkName) : targetName.contains(networkName))
                        return false;
                }

                return true;
            }
        }

        if(antiBot.duplicateInWorldValue.get()) {
            if (mc.theWorld.loadedEntityList.stream()
                    .filter(currEntity -> currEntity instanceof EntityPlayer && ((EntityPlayer) currEntity)
                            .getDisplayNameString().equals(((EntityPlayer) currEntity).getDisplayNameString()))
                    .count() > 1)
                return true;
        }

        if(antiBot.duplicateInTabValue.get()) {
            if (mc.getNetHandler().getPlayerInfoMap().stream()
                    .filter(networkPlayer -> entity.getName().equals(ColorUtils.stripColor(EntityUtils.getName(networkPlayer))))
                    .count() > 1)
                return true;
        }

        return entity.getName().isEmpty() || entity.getName().equals(mc.thePlayer.getName());
    }

}
