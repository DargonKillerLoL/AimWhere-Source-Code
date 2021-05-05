package me.kiras.aimwhere.customskinloader.tweaker;

import me.kiras.aimwhere.customskinloader.Logger;
import me.kiras.aimwhere.customskinloader.utils.MinecraftUtil;
import java.io.File;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class ModSystemTweaker implements ITweaker {

   public static Logger logger = new Logger();


   public void acceptOptions(List args, File gameDir, File assetsDir, String profile) {
      MinecraftUtil.minecraftDataFolder = gameDir;
      File tweakerLogFile = new File(MinecraftUtil.getMinecraftDataDir(), "CustomSkinLoader/Tweaker.log");
      logger = new Logger(tweakerLogFile);
      logger.info("Using ModSystemTweaker");
      logger.info("ModSystemTweaker: acceptOptions");
   }

   public void injectIntoClassLoader(LaunchClassLoader classLoader) {
      logger.info("ModSystemTweaker: injectIntoClassLoader");
      classLoader.registerTransformer("customskinloader.tweaker.ClassTransformer");
      logger.info("ClassTransformer Registered");
   }

   public String getLaunchTarget() {
      logger.info("ModSystemTweaker: getLaunchTarget");
      return "net.minecraft.client.main.Main";
   }

   public String[] getLaunchArguments() {
      logger.info("ModSystemTweaker: getLaunchArguments");
      return new String[0];
   }
}
