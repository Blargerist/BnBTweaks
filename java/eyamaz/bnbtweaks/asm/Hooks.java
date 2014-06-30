package eyamaz.bnbtweaks.asm;

import eyamaz.bnbtweaks.ModBnBTweaks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class Hooks {

	public static Boolean onFullBucketUse(ItemStack itemstack, World world, EntityPlayer player, int x, int y, int z)
	{
		int block = 1;
		if (world.getBlockId(x, y, z) == 2957) 
		{
			block = world.getBlockId(x, y, z);
			ModBnBTweaks.Log.info("Block Id Is: " + block);
			ModBnBTweaks.Log.info("Returning Empty Bucket");
			return true;
		} else
			block = world.getBlockId(x, y, z);
			ModBnBTweaks.Log.info("Block Id Is: " + block);
			ModBnBTweaks.Log.info("Returning Full Bucket");
			return false;
	}
}
