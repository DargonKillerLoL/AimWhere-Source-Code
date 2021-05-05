/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world;

import me.kiras.aimwhere.utils.render.GLUtils;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.*;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.block.PlaceInfo;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TickTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.FloatValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBush;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks beneath your feet.", category = ModuleCategory.WORLD, keyBind = Keyboard.KEY_I)
public class Scaffold extends Module {

    /**
     * OPTIONS
     */

    // Mode
    public final ListValue modeValue = new ListValue("Mode", new String[]{"Normal", "Rewinside", "Expand"}, "Normal");

    // Delay
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 0, 0, 1000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = minDelayValue.get();

            if (i > newValue)
                set(i);
        }
    };

    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 0, 0, 1000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int i = maxDelayValue.get();

            if (i < newValue)
                set(i);
        }
    };
    private final BoolValue placeableDelay = new BoolValue("PlaceableDelay", false);

    // AutoBlock
    private final ListValue autoBlockValue = new ListValue("AutoBlock", new String[]{"Off", "Spoof", "Switch"}, "Spoof");

    // Basic stuff
    public final BoolValue sprintValue = new BoolValue("Sprint", true);
    private final BoolValue swingValue = new BoolValue("Swing", true);
    private final BoolValue searchValue = new BoolValue("Search", true);
    private final BoolValue downValue = new BoolValue("Down", true);
    private final ListValue placeModeValue = new ListValue("PlaceTiming", new String[]{"Pre", "Post"}, "Post");

    // Eagle
    private final ListValue eagleValue = new ListValue("Eagle", new String[]{"Normal", "EdgeDistance", "Silent", "Off"}, "Off");
    private final IntegerValue blocksToEagleValue = new IntegerValue("BlocksToEagle", 0, 0, 10);
    private final FloatValue edgeDistanceValue = new FloatValue("EagleEdgeDistance", 0.2F, 0F, 0.5F);

    // Expand
    private final IntegerValue expandLengthValue = new IntegerValue("ExpandLength", 5, 1, 6);

    // RotationStrafe
    private final BoolValue rotationStrafeValue = new BoolValue("RotationStrafe", false);

    // Rotations
    private final ListValue rotationModeValue = new ListValue("RotationMode", new String[]{"Normal", "Static", "StaticPitch", "StaticYaw", "Off"}, "Normal");
    private final BoolValue silentRotation = new BoolValue("SilentRotation", true);
    private final BoolValue keepRotationValue = new BoolValue("KeepRotation", false);
    private final IntegerValue keepLengthValue = new IntegerValue("KeepRotationLength", 0, 0, 20);
    private final FloatValue staticPitchValue = new FloatValue("StaticPitchOffset", 86F, 70F, 90F);
    private final FloatValue staticYawOffsetValue = new FloatValue("StaticYawOffset", 0F, 0F, 90F);

    // Tower
    private final ListValue towerModeValue = new ListValue("TowerMode", new String[] {
            "None", "Jump", "Motion", "ConstantMotion", "PlusMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4"
    }, "None");
    private final BoolValue stopWhenBlockAbove = new BoolValue("StopWhenBlockAbove", false);

    // Other
    private final FloatValue xzRangeValue = new FloatValue("XZ-Range", 0.8F, 0.1F, 1.0F);
    private final FloatValue yRangeValue = new FloatValue("Y-Range", 0.8F, 0.1F, 1.0F);

    // SearchAccuracy
    private final IntegerValue searchAccuracyValue = new IntegerValue("SearchAccuracy", 8, 1, 24) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            if (getMaximum() < newValue) {
                set(getMaximum());
            } else if (getMinimum() > newValue) {
                set(getMinimum());
            }
        }
    };

    // Turn Speed
    private final FloatValue maxTurnSpeedValue = new FloatValue("MaxTurnSpeed", 180, 1, 180) {
        @Override
        protected void onChanged(final Float oldValue, final Float newValue) {
            float v = minTurnSpeedValue.get();
            if (v > newValue) set(v);
            if (getMaximum() < newValue) {
                set(getMaximum());
            } else if (getMinimum() > newValue) {
                set(getMinimum());
            }
        }
    };
    private final FloatValue minTurnSpeedValue = new FloatValue("MinTurnSpeed", 180, 1, 180) {
        @Override
        protected void onChanged(final Float oldValue, final Float newValue) {
            float v = maxTurnSpeedValue.get();
            if (v < newValue) set(v);
            if (getMaximum() < newValue) {
                set(getMaximum());
            } else if (getMinimum() > newValue) {
                set(getMinimum());
            }
        }
    };

    // Zitter
    private final BoolValue zitterValue = new BoolValue("Zitter", false);
    private final ListValue zitterModeValue = new ListValue("ZitterMode", new String[]{"Teleport", "Smooth"}, "Teleport");
    private final FloatValue zitterSpeed = new FloatValue("ZitterSpeed", 0.13F, 0.1F, 0.3F);
    private final FloatValue zitterStrength = new FloatValue("ZitterStrength", 0.072F, 0.05F, 0.2F);

    // Game
    private final FloatValue timerValue = new FloatValue("Timer", 1F, 0.1F, 10F);
    private final FloatValue speedModifierValue = new FloatValue("SpeedModifier", 1F, 0, 2F);
    private final BoolValue slowValue = new BoolValue("Slow", false) {
        @Override
        protected void onChanged(final Boolean oldValue, final Boolean newValue) {
            if (newValue)
                sprintValue.set(false);
        }
    };
    private final FloatValue slowSpeed = new FloatValue("SlowSpeed", 0.6F, 0.2F, 0.8F);

    // Safety
    private final BoolValue sameYValue = new BoolValue("SameY", false);
    private final BoolValue safeWalkValue = new BoolValue("SafeWalk", true);
    private final BoolValue airSafeValue = new BoolValue("AirSafe", false);

    //Render
    private final IntegerValue r = new IntegerValue("MarkRed",255,0,255);
    private final IntegerValue g = new IntegerValue("MarkGreen",255,0,255);
    private final IntegerValue b = new IntegerValue("MarkBlue",255,0,255);
    private final IntegerValue a = new IntegerValue("MarkAlpha",255,0,255);
    private final BoolValue outline = new BoolValue("MarkOutline",false);

    // Jump mode
    private final FloatValue jumpMotionValue = new FloatValue("TowerJumpMotion", 0.42F, 0.3681289F, 0.79F);
    private final IntegerValue jumpDelayValue = new IntegerValue("TowerJumpDelay", 0, 0, 20);

    // PlusMotion
    private final FloatValue plusMotionValue = new FloatValue("TowerPlusMotion", 0.1F, 0.01F, 0.2F);
    private final FloatValue plusMaxMotionValue = new FloatValue("TowerPlusMaxMotion", 0.8F, 0.1F, 2F);

    // ConstantMotion
    private final FloatValue constantMotionValue = new FloatValue("TowerConstantMotion", 0.42F, 0.1F, 1F);
    private final FloatValue constantMotionJumpGroundValue = new FloatValue("TowerConstantMotionJumpGround", 0.79F, 0.76F, 1F);

    // Teleport
    private final FloatValue teleportHeightValue = new FloatValue("TowerTeleportHeight", 1.15F, 0.1F, 5F);
    private final IntegerValue teleportDelayValue = new IntegerValue("TowerTeleportDelay", 0, 0, 20);
    private final BoolValue teleportGroundValue = new BoolValue("TowerTeleportGround", true);
    private final BoolValue teleportNoMotionValue = new BoolValue("TowerTeleportNoMotion", false);

    // Visuals
    private final BoolValue counterDisplayValue = new BoolValue("Counter", true);
    private final BoolValue markValue = new BoolValue("Mark", false);

    /**
     * MODULE
     */

    // Target block
    private PlaceInfo targetPlace;

    // Launch position
    private int launchY;

    // Rotation lock
    private Rotation lockRotation;
    private Rotation limitedRotation;
    private boolean facesBlock = false;

    // Auto block slot
    private int slot;

    // Zitter Smooth
    private boolean zitterDirection;

    // Delay
    private final MSTimer delayTimer = new MSTimer();
    private final MSTimer zitterTimer = new MSTimer();
    private final TickTimer towerTimer = new TickTimer();
    private long delay;

    // Eagle
    private int placedBlocksWithoutEagle = 0;
    private boolean eagleSneaking;

    // Down
    private boolean shouldGoDown = false;

    private double jumpGround = 0;
    /**
     * Enable module
     */
    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        launchY = (int) mc.thePlayer.posY;
    }

    /**
     * Update event
     *
     * @param event
     */
    @EventTarget
    public void onUpdate(final UpdateEvent event) {
        mc.timer.timerSpeed=timerValue.get();


        shouldGoDown = downValue.get() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && getBlocksAmount() > 1;
        if (shouldGoDown)
            mc.gameSettings.keyBindSneak.pressed=false;

        if (slowValue.get()) {
            mc.thePlayer.motionX=mc.thePlayer.motionX * slowSpeed.get();
            mc.thePlayer.motionZ=mc.thePlayer.motionZ * slowSpeed.get();
        }

        if (sprintValue.get()) {
            if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSprint)) {
                mc.gameSettings.keyBindSprint.pressed=false;
            }
            if (GameSettings.isKeyDown(mc.gameSettings.keyBindSprint)) {
                mc.gameSettings.keyBindSprint.pressed=true;
            }
            if (mc.gameSettings.keyBindSprint.isKeyDown()) {
                mc.thePlayer.setSprinting(true);
            }
            if (!mc.gameSettings.keyBindSprint.isKeyDown()) {
                mc.thePlayer.setSprinting(false);
            }
        }

        if (mc.thePlayer.onGround) {
            final String mode = modeValue.get();

            // Rewinside scaffold mode
            if (mode.equalsIgnoreCase("Rewinside")) {
                MovementUtils.strafe(0.2F);
                mc.thePlayer.motionY=0D;
            }

            // Smooth Zitter
            if (zitterValue.get() && zitterModeValue.get().equalsIgnoreCase("smooth")) {
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight))
                    mc.gameSettings.keyBindRight.pressed=false;

                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
                    mc.gameSettings.keyBindLeft.pressed=false;

                if (zitterTimer.hasTimePassed(100)) {
                    zitterDirection = !zitterDirection;
                    zitterTimer.reset();
                }

                if (zitterDirection) {
                    mc.gameSettings.keyBindRight.pressed=true;
                    mc.gameSettings.keyBindLeft.pressed=false;
                } else {
                    mc.gameSettings.keyBindRight.pressed=false;
                    mc.gameSettings.keyBindLeft.pressed=true;
                }
            }

            // Eagle
            if (!eagleValue.get().equalsIgnoreCase("Off") && !shouldGoDown) {
                double dif = 0.5D;
                if (eagleValue.get().equalsIgnoreCase("EdgeDistance") && !shouldGoDown) {
                    for (int i = 0; i < 4; i++) {
                        switch (i) {
                            case 0: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX - 1, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posX - blockPos.getX();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                            case 1: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + 1, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posX - blockPos.getX();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                            case 2: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ - 1);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posZ - blockPos.getZ();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                            case 3: {
                                final BlockPos blockPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D), mc.thePlayer.posZ + 1);
                                final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

                                if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                                    double calcDif = mc.thePlayer.posZ - blockPos.getZ();
                                    calcDif -= 0.5D;

                                    if (calcDif < 0)
                                        calcDif *= -1;
                                    calcDif -= 0.5;

                                    if (calcDif < dif)
                                        dif = calcDif;
                                }

                            }
                        }
                    }
                }

                if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                    final boolean shouldEagle = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX,
                            mc.thePlayer.posY - 1D, mc.thePlayer.posZ)).getBlock().equals(Blocks.air) || (dif < edgeDistanceValue.get() && eagleValue.get().equalsIgnoreCase("EdgeDistance"));

                    if (eagleValue.get().equalsIgnoreCase("Silent") && !shouldGoDown) {
                        if (eagleSneaking != shouldEagle) {
                            mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, shouldEagle?
                                    C0BPacketEntityAction.Action.START_SNEAKING : C0BPacketEntityAction.Action.STOP_SNEAKING));
                        }

                        eagleSneaking = shouldEagle;
                    } else
                        mc.gameSettings.keyBindSneak.pressed=shouldEagle;

                    placedBlocksWithoutEagle = 0;
                } else
                    placedBlocksWithoutEagle++;
            }

            // Zitter
            if (zitterValue.get() && zitterModeValue.get().equalsIgnoreCase("teleport")) {
                MovementUtils.strafe(zitterSpeed.get());


                final double yaw = Math.toRadians(mc.thePlayer.rotationYaw + (zitterDirection ? 90D : -90D));
                mc.thePlayer.motionX=mc.thePlayer.motionX - Math.sin(yaw) * zitterStrength.get();
                mc.thePlayer.motionZ=mc.thePlayer.motionZ + Math.cos(yaw) * zitterStrength.get();
                zitterDirection = !zitterDirection;
            }
        }
    }

    @EventTarget
    public void onPacket(final PacketEvent event) {
        if (mc.thePlayer == null)
            return;

        // AutoBlock
        if (event.getPacket() instanceof C09PacketHeldItemChange) {
            final C09PacketHeldItemChange packetHeldItemChange=(C09PacketHeldItemChange)event.getPacket();
            slot = packetHeldItemChange.getSlotId();
        }
    }

    @EventTarget
    private void onStrafe(StrafeEvent event) {

        if (!rotationStrafeValue.get())
            return;
        RotationUtils.serverRotation.applyStrafeToPlayer(event);
        event.cancelEvent();
    }

    @EventTarget
    public void onMotion(final MotionEvent event) {
        final EventState eventState = event.getEventState();

        // Tower
        if (!(towerModeValue.get().equalsIgnoreCase("None")
                || (!mc.gameSettings.keyBindJump.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown() || mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()))
                && (!stopWhenBlockAbove.get() || BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX,
                mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir)){
            move();
        }

        // Lock Rotation
        if (!rotationModeValue.get().equalsIgnoreCase("Off") && keepRotationValue.get() && lockRotation != null)
            setRotation(lockRotation);


        if ((facesBlock || rotationModeValue.get().equalsIgnoreCase("Off")) && placeModeValue.get().equalsIgnoreCase(eventState.getStateName()))
            place();

        // Update and search for new block
        if (eventState == EventState.PRE)
            update();

        // Reset placeable delay
        if (targetPlace == null && placeableDelay.get())
            delayTimer.reset();
    }

    private void fakeJump() {
        mc.thePlayer.isAirBorne = true;
        mc.thePlayer.triggerAchievement(StatList.jumpStat);
    }

    private void move() {
        switch (towerModeValue.get().toLowerCase()) {
            case "jump":
                if (mc.thePlayer.onGround && towerTimer.hasTimePassed(jumpDelayValue.get())) {
                    fakeJump();
                    mc.thePlayer.motionY = jumpMotionValue.get();
                    towerTimer.reset();
                }
                break;
            case "motion":
                if (mc.thePlayer.onGround) {
                    fakeJump();
                    mc.thePlayer.motionY = 0.42D;
                } else if (mc.thePlayer.motionY < 0.1D) mc.thePlayer.motionY = -0.3D;
                break;
            case "motiontp":
                if (mc.thePlayer.onGround) {
                    fakeJump();
                    mc.thePlayer.motionY = 0.42D;
                } else if (mc.thePlayer.motionY < 0.23D)
                    mc.thePlayer.setPosition(mc.thePlayer.posX, (int) mc.thePlayer.posY, mc.thePlayer.posZ);
                break;
            case "packet":
                if (mc.thePlayer.onGround && towerTimer.hasTimePassed(2)) {
                    fakeJump();
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                            mc.thePlayer.posY + 0.42D, mc.thePlayer.posZ, false));
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,
                            mc.thePlayer.posY + 0.753D, mc.thePlayer.posZ, false));
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 1D, mc.thePlayer.posZ);
                    towerTimer.reset();
                }
                break;
            case "teleport":
                if (teleportNoMotionValue.get())
                    mc.thePlayer.motionY = 0;

                if ((mc.thePlayer.onGround || !teleportGroundValue.get()) && towerTimer.hasTimePassed(teleportDelayValue.get())) {
                    fakeJump();
                    mc.thePlayer.setPositionAndUpdate(mc.thePlayer.posX, mc.thePlayer.posY + teleportHeightValue.get(), mc.thePlayer.posZ);
                    towerTimer.reset();
                }
                break;
            case "plusmotion":
                mc.thePlayer.motionY += plusMotionValue.get();
                if(mc.thePlayer.motionY>=plusMaxMotionValue.get()){
                    mc.thePlayer.motionY=plusMaxMotionValue.get();
                }
                break;
            case "constantmotion":
                if (mc.thePlayer.onGround) {
                    fakeJump();
                    jumpGround = mc.thePlayer.posY;
                    mc.thePlayer.motionY = constantMotionValue.get();
                }

                if (mc.thePlayer.posY > jumpGround + constantMotionJumpGroundValue.get()) {
                    fakeJump();
                    mc.thePlayer.setPosition(mc.thePlayer.posX, (int) mc.thePlayer.posY, mc.thePlayer.posZ);
                    mc.thePlayer.motionY = constantMotionValue.get();
                    jumpGround = mc.thePlayer.posY;
                }
                break;
            case "aac3.3.9":
                if (mc.thePlayer.onGround) {
                    fakeJump();
                    mc.thePlayer.motionY = 0.4001;
                }
                mc.timer.timerSpeed = 1F;

                if (mc.thePlayer.motionY < 0) {
                    mc.thePlayer.motionY -= 0.00000945;
                    mc.timer.timerSpeed = 1.6F;
                }
                break;
            case "aac3.6.4":
                if (mc.thePlayer.ticksExisted % 4 == 1) {
                    mc.thePlayer.motionY = 0.4195464;
                    mc.thePlayer.setPosition(mc.thePlayer.posX - 0.035, mc.thePlayer.posY, mc.thePlayer.posZ);
                } else if (mc.thePlayer.ticksExisted % 4 == 0) {
                    mc.thePlayer.motionY = -0.5;
                    mc.thePlayer.setPosition(mc.thePlayer.posX + 0.035, mc.thePlayer.posY, mc.thePlayer.posZ);
                }
                break;
        }
    }

    private void update() {
        final boolean isHeldItemBlock = mc.thePlayer.getHeldItem() != null && (mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock);
        if (!autoBlockValue.get().equalsIgnoreCase("Off") ? InventoryUtils.findAutoBlockBlock() == -1 && !isHeldItemBlock : !isHeldItemBlock)
            return;

        findBlock(modeValue.get().equalsIgnoreCase("expand"));
    }

    private void setRotation(Rotation rotation, int keepRotation) {
        if (silentRotation.get()) {
            RotationUtils.setTargetRotation(rotation, keepRotation);
        } else {
            mc.thePlayer.rotationYaw=rotation.getYaw();
            mc.thePlayer.rotationPitch=rotation.getPitch();
        }
    }

    private void setRotation(Rotation rotation) {
        setRotation(rotation, 0);
    }

    /**
     * Search for new target block
     */
    private void findBlock(final boolean expand) {
        final BlockPos blockPosition = shouldGoDown ? (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ?
                new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6D, mc.thePlayer.posZ)
                : new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.6, mc.thePlayer.posZ).down()) :
                (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? new BlockPos(mc.thePlayer)
                        : new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ).down());

        if (!expand && (!BlockUtils.isReplaceable(blockPosition) || search(blockPosition, !shouldGoDown)))
            return;

        if (expand) {
            for (int i = 0; i < expandLengthValue.get(); i++) {
                if (search(blockPosition.add(
                        mc.thePlayer.getHorizontalFacing().equals(EnumFacing.WEST) ? -i : mc.thePlayer.getHorizontalFacing().equals(EnumFacing.EAST) ? i : 0,
                        0,
                        mc.thePlayer.getHorizontalFacing().equals(EnumFacing.NORTH) ? -i : mc.thePlayer.getHorizontalFacing().equals(EnumFacing.SOUTH) ? i : 0
                ), false))

                    return;
            }
        } else if (searchValue.get()) {
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown))
                        return;
        }
    }

    /**
     * Place target block
     */
    private void place() {
        if (targetPlace == null) {
            if (placeableDelay.get())
                delayTimer.reset();
            return;
        }

        if (!delayTimer.hasTimePassed(delay) || (sameYValue.get() && launchY - 1 != (int) targetPlace.getVec3().yCoord))
            return;

        int blockSlot = -1;
        ItemStack itemStack = mc.thePlayer.getHeldItem();

        if (itemStack == null || !(itemStack.getItem() instanceof ItemBlock) ||
                (((ItemBlock)itemStack.getItem()).getBlock() instanceof BlockBush) || mc.thePlayer.getHeldItem().stackSize <= 0) {
            if (autoBlockValue.get().equalsIgnoreCase("Off"))
                return;

            blockSlot = InventoryUtils.findAutoBlockBlock();

            if (blockSlot == -1)
                return;


            if (autoBlockValue.get().equalsIgnoreCase("Spoof")) {
                if (blockSlot - 36 != slot)
                    mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot - 36));
            } else {
                mc.thePlayer.inventory.changeCurrentItem(blockSlot - 36);
                mc.playerController.updateController();
            }
            itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).getStack();
        }


        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, targetPlace.getBlockPos(), targetPlace.getEnumFacing(), targetPlace.getVec3())) {
            delayTimer.reset();
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (mc.thePlayer.onGround) {
                final float modifier = speedModifierValue.get();

                mc.thePlayer.motionX=mc.thePlayer.motionX * modifier;
                mc.thePlayer.motionZ=mc.thePlayer.motionZ * modifier;
            }

            if (swingValue.get())
                mc.thePlayer.swingItem();
            else
                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
        }

        /*
        if (!stayAutoBlock.get() && blockSlot >= 0)
            mc.getNetHandler().addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer.getInventory().getCurrentItem()));
         */

        // Reset
        this.targetPlace = null;
    }

    /**
     * Disable scaffold module
     */
    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed=false;

            if (eagleSneaking)
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight))
            mc.gameSettings.keyBindRight.pressed=false;

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
            mc.gameSettings.keyBindLeft.pressed=false;

        lockRotation = null;
        limitedRotation = null;
        facesBlock = false;
        mc.timer.timerSpeed=1F;
        shouldGoDown = false;

        if (slot != mc.thePlayer.inventory.currentItem)
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }

    /**
     * Entity movement event
     *
     * @param event
     */
    @EventTarget
    public void onMove(final MoveEvent event) {
        if (!safeWalkValue.get() || shouldGoDown)
            return;

        if (airSafeValue.get() || mc.thePlayer.onGround)
            event.setSafeWalk(true);
    }

    /**
     * Scaffold visuals
     *
     * @param event
     */
    @EventTarget
    public void onRender2D(final Render2DEvent event) {
        if (counterDisplayValue.get()) {
            int blockSlot = InventoryUtils.findAutoBlockBlock();
            if (blockSlot == -1)
                return;
            ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(blockSlot).getStack();
            ScaledResolution res = new ScaledResolution(mc);
            GL11.glPushMatrix();
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            if (mc.theWorld != null)
                GLUtils.enableGUIStandardItemLighting();
            GlStateManager.pushMatrix();
            GlStateManager.disableAlpha();
            GlStateManager.clear(256);
            mc.getRenderItem().zLevel = -150.0f;
            mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, res.getScaledWidth() / 2 - 20, res.getScaledHeight() / 2 + 2);
            Fonts.font35.drawStringWithShadow(getBlocksAmount() + " Blocks", res.getScaledWidth() / 2, res.getScaledHeight() / 2 + 7, Color.WHITE.getRGB());
            mc.getRenderItem().zLevel = 0.0f;
            GlStateManager.disableBlend();
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.scale(2.0f, 2.0f, 2.0f);
            GlStateManager.enableAlpha();
            GlStateManager.popMatrix();
            GL11.glPopMatrix();
        }
    }

    /**
     * Scaffold visuals
     *
     * @param event
     */
    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if (!markValue.get())
            return;

        for (int i = 0; i < (modeValue.get().equalsIgnoreCase("Expand") ? expandLengthValue.get() + 1 : 2); i++) {
            final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + (mc.thePlayer.getHorizontalFacing() == EnumFacing.WEST ? -i : mc.thePlayer.getHorizontalFacing() == EnumFacing.EAST ? i : 0), mc.thePlayer.posY - (mc.thePlayer.posY == (int) mc.thePlayer.posY + 0.5D ? 0D : 1.0D) - (shouldGoDown ? 1D : 0), mc.thePlayer.posZ + (mc.thePlayer.getHorizontalFacing() == EnumFacing.NORTH ? -i : mc.thePlayer.getHorizontalFacing() == EnumFacing.SOUTH ? i : 0));
            final PlaceInfo placeInfo = PlaceInfo.get(blockPos);

            if (BlockUtils.isReplaceable(blockPos) && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, new Color(r.get(), g.get(), b.get(), a.get()), outline.get());
                break;
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */
    private boolean search(final BlockPos blockPosition, final boolean checks) {
        if (!BlockUtils.isReplaceable(blockPosition))
            return false;
        // StaticModes
        final boolean staticMode = rotationModeValue.get().equalsIgnoreCase("Static");
        final boolean staticPitchMode = staticMode || rotationModeValue.get().equalsIgnoreCase("StaticPitch");
        final boolean staticYawMode = staticMode || rotationModeValue.get().equalsIgnoreCase("StaticYaw");
        final float staticPitch = staticPitchValue.get();
        final float staticYawOffset = staticYawOffsetValue.get();

        // SearchRanges
        final double xzRV = xzRangeValue.get();
        final double xzSSV = calcStepSize(xzRV);
        final double yRV = yRangeValue.get();
        final double ySSV = calcStepSize(yRV);

        double xSearchFace = 0;
        double ySearchFace = 0;
        double zSearchFace = 0;


        final Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.getEntityBoundingBox().minY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        PlaceRotation placeRotation = null;

        for (final EnumFacing side : EnumFacing.values()) {
            final BlockPos neighbor = blockPosition.offset(side);

            if (!BlockUtils.canBeClicked(neighbor))
                continue;

            final Vec3 dirVec = new Vec3(side.getDirectionVec());

            for (double xSearch = 0.5D - (xzRV / 2); xSearch <= 0.5D + (xzRV / 2); xSearch += xzSSV) {
                for (double ySearch = 0.5D - (yRV / 2); ySearch <= 0.5D + (yRV / 2); ySearch += ySSV) {
                    for (double zSearch = 0.5D - (xzRV / 2); zSearch <= 0.5D + (xzRV / 2); zSearch += xzSSV) {
                        final Vec3 posVec = new Vec3(blockPosition).addVector(xSearch, ySearch, zSearch);
                        final double distanceSqPosVec = eyesPos.squareDistanceTo(posVec);
                        final Vec3 hitVec = posVec.add(new Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5));

                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18D || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null))
                            continue;

                        // face block
                        for (int i = 0; i < (staticYawMode ? 2 : 1); i++) {
                            final double diffX = staticYawMode && i == 0 ? 0 : hitVec.xCoord - eyesPos.xCoord;
                            final double diffY = hitVec.yCoord - eyesPos.yCoord;
                            final double diffZ = staticYawMode && i == 1 ? 0 : hitVec.zCoord - eyesPos.zCoord;

                            final double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

                            final float pitch = staticPitchMode ? staticPitch : MathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)));
                            final Rotation rotation = new Rotation(
                                    MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F +
                                            (staticYawMode ? staticYawOffset : 0)), pitch);

                            final Vec3 rotationVector = RotationUtils.getVectorForRotation(rotation);
                            final Vec3 vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4);
                            final MovingObjectPosition obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true);

                            if (obj.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !obj.getBlockPos().equals(neighbor))
                                continue;

                            if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation.getRotation())) {
                                placeRotation = new PlaceRotation(new PlaceInfo(neighbor, side.getOpposite(), hitVec), rotation);
                            }
                            xSearchFace = xSearch;
                            ySearchFace = ySearch;
                            zSearchFace = zSearch;
                        }
                    }
                }
            }
        }

        if (placeRotation == null) return false;

        if (!rotationModeValue.get().equalsIgnoreCase("Off")) {
            if (minTurnSpeedValue.get() < 180) {
                limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, placeRotation.getRotation(), (float) (Math.random() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()));
                setRotation(limitedRotation, keepLengthValue.get());
                lockRotation = limitedRotation;

                facesBlock = false;
                for (final EnumFacing side : EnumFacing.values()) {
                    final BlockPos neighbor = blockPosition.offset(side);

                    if (!BlockUtils.canBeClicked(neighbor))
                        continue;

                    final Vec3 dirVec = new Vec3(side.getDirectionVec());

                    final Vec3 posVec = new Vec3(blockPosition).addVector(xSearchFace, ySearchFace, zSearchFace);
                    final double distanceSqPosVec = eyesPos.squareDistanceTo(posVec);
                    final Vec3 hitVec = posVec.add(new Vec3(dirVec.xCoord * 0.5, dirVec.yCoord * 0.5, dirVec.zCoord * 0.5));

                    if (checks && (eyesPos.squareDistanceTo(hitVec) > 18D || distanceSqPosVec > eyesPos.squareDistanceTo(posVec.add(dirVec)) || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null))
                        continue;

                    final Vec3 rotationVector = RotationUtils.getVectorForRotation(limitedRotation);
                    final Vec3 vector = eyesPos.addVector(rotationVector.xCoord * 4, rotationVector.yCoord * 4, rotationVector.zCoord * 4);
                    final MovingObjectPosition obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true);

                    if (!(obj.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && obj.getBlockPos().equals(neighbor)))
                        continue;
                    facesBlock = true;
                    break;
                }
            } else {
                setRotation(placeRotation.getRotation(), keepLengthValue.get());
                lockRotation = placeRotation.getRotation();
                facesBlock = true;
            }
        }
        targetPlace = placeRotation.getPlaceInfo();
        return true;
    }

    private double calcStepSize(double range) {
        double accuracy = searchAccuracyValue.get();
        accuracy += accuracy % 2; // If it is set to uneven it changes it to even. Fixes a bug
        if (range / accuracy < 0.01D)
            return 0.01D;
        return range / accuracy;
    }

    public int getBlocksAmount() {
        int amount = 0;

        for (int i = 36; i < 45; i++) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if (itemStack != null && itemStack.getItem() instanceof ItemBlock
                    && !InventoryUtils.BLOCK_BLACKLIST.contains(((ItemBlock) itemStack.getItem()).getBlock()))
                amount += itemStack.stackSize;
        }

        return amount;
    }

    @Override
    public String getTag() {
        return modeValue.get();
    }
}