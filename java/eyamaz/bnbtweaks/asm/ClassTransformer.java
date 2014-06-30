package eyamaz.bnbtweaks.asm;

import static org.objectweb.asm.Opcodes.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import eyamaz.bnbtweaks.ModBnBTweaks;

public class ClassTransformer implements IClassTransformer
{

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes)
	{
		if (name.equals("glassmaker.extratic.common.RecipeHandler"))
		{
			ModBnBTweaks.Log.info("Patching ExtraTiC's RecipeHandler...");

			ClassNode classNode = readClassFromBytes(bytes);
			MethodNode methodNode = findMethodNodeOfClass(classNode, "_addMeltingOreRecipe", "(Ljava/lang/String;Ljava/lang/String;II)V");
			if (methodNode != null)
			{
				fixExtraTiCMelting(methodNode);
			}

			methodNode = findMethodNodeOfClass(classNode, "_addMeltingOreRecipe", "(Ljava/lang/String;Ljava/lang/String;III)V");
			if (methodNode != null)
			{
				fixExtraTiCMelting(methodNode);
			}

			return writeClassToBytes(classNode);
		}

		if (name.equals("hostileworlds.dimension.gen.MapGenSchematics"))
		{
			ModBnBTweaks.Log.info("Patching HostileWorld's MapGenSchematis....");

			ClassNode classNode = readClassFromBytes(bytes);
			MethodNode methodNode = findMethodNodeOfClass(classNode, "genTemple", "(Lnet/minecraft/world/World;II[B)V");
			if (methodNode != null)
			{
				fixHostileWorldsMapGenSchematics(methodNode);
			}

			return writeClassToBytes(classNode);
		}
		
		if (name.equals("net.minecraft.item.ItemBucket") || name.equals("wr"))
		{
			boolean isObfuscated = name.equals("wr");
			
			ModBnBTweaks.Log.info("Patching ItemBucket....");
			
			ClassNode classNode = readClassFromBytes(bytes);
			
		    MethodNode methodNode = findMethodNodeOfClass(classNode, "onItemRightClick", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;");

			MethodNode obfMethodNode = findMethodNodeOfClass(classNode, "a", "(Lye;Labw;Luf;)Lye;");
			
			if (methodNode != null || obfMethodNode != null)
			{
				if (isObfuscated)
				{
					ModBnBTweaks.Log.info("Patching Method: " + obfMethodNode);
					addOnFullItemBucketUseHook(obfMethodNode, Hooks.class, "onFullBucketUse", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;III)Ljava/lang/Boolean;");
				}
				else if (!isObfuscated)
				{
					ModBnBTweaks.Log.info("Patching Method: " + methodNode);
					addOnFullItemBucketUseHook(methodNode, Hooks.class, "onFullBucketUse", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;III)Ljava/lang/Boolean;");
				}
			}
			
			return writeClassToBytes(classNode);
		}
		
		if (name.equals("tconstruct.items.FilledBucket"))
		{
			ModBnBTweaks.Log.info("Patching TiC's FilledBucket....");
			
			ClassNode classNode = readClassFromBytes(bytes);
						
			MethodNode methodNode = findMethodNodeOfClass(classNode, "func_77659_a", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;");
			
			if (methodNode != null)
			{
				ModBnBTweaks.Log.info("Patching Method: " + methodNode);
				addOnFullItemBucketUseHookTiC(methodNode, Hooks.class, "onFullBucketUse", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;III)Ljava/lang/Boolean;");
			}
			
			return writeClassToBytes(classNode);
		}

		return bytes;
	}

	private ClassNode readClassFromBytes(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		return classNode;
	}

	private byte[] writeClassToBytes(ClassNode classNode)
	{
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	private MethodNode findMethodNodeOfClass(ClassNode classNode, String methodName, String methodDesc)
	{
		for (MethodNode method : classNode.methods)
		{
			if (method.name.equals(methodName) && method.desc.equals(methodDesc))
			{
				ModBnBTweaks.Log.info(" Found target method: " + methodName);
				return method;
			}
		}
		return null;
	}
	
	private AbstractInsnNode findChronoInstructionOfType(MethodNode method, int bytecode, int number)
	{
		for (int i = 0; i < number;) 
		{
			for (AbstractInsnNode instruction : method.instructions.toArray())
			{
				if (instruction.getOpcode() == bytecode)
				{
					i++;
					if (i == number)
					{
						return instruction;
					}
				}
			}
		}
		return null;
	}

	private AbstractInsnNode findFirstInstructionOfType(MethodNode method, int bytecode)
	{
		for (AbstractInsnNode instruction : method.instructions.toArray())
		{
			if (instruction.getOpcode() == bytecode)
				return instruction;
		}
		return null;
	}

	public void fixExtraTiCMelting(MethodNode method)
	{
		AbstractInsnNode targetNode = findFirstInstructionOfType(method, ALOAD);

		InsnList toInject = new InsnList();

		/*
		String par1 = "";
		int par3 = 0;
		// equivalent to:
		if (par1.startsWith("ore"))
			par3 = tconstruct.util.config.PHConstruct.ingotsPerOre;
		*/

		toInject.add(new VarInsnNode(ALOAD, 0));
		toInject.add(new LdcInsnNode("ore"));
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, String.class.getName().replace('.', '/'), "startsWith", "(Ljava/lang/String;)Z"));
		LabelNode labelIfNotStartsWith = new LabelNode();
		toInject.add(new JumpInsnNode(IFEQ, labelIfNotStartsWith));
		toInject.add(new FieldInsnNode(GETSTATIC, "tconstruct/util/config/PHConstruct", "ingotsPerOre", "I"));
		toInject.add(new VarInsnNode(ISTORE, 2));
		toInject.add(labelIfNotStartsWith);

		method.instructions.insertBefore(targetNode, toInject);

		ModBnBTweaks.Log.info(" Patched " + method.name);
	}

	public void fixHostileWorldsMapGenSchematics(MethodNode method)
	{
		AbstractInsnNode targetNode = findFirstInstructionOfType(method, ALOAD);
		
		InsnList toInject = new InsnList();
		
		//Add return statement to beginning of method
		
		toInject.add(new InsnNode(RETURN));
		
		method.instructions.insertBefore(targetNode, toInject);
		
		ModBnBTweaks.Log.info(" Patched " + method.name);
	}
	
	public void addOnFullItemBucketUseHook(MethodNode method, Class<?> hookClass, String hookMethod, String hookDesc)
	{
		AbstractInsnNode targetNode = findChronoInstructionOfType(method, ALOAD, 54);
		
		InsnList toInject = new InsnList();
		
		//equivalent to:
		//if (Hooks.onFullBucketUse(itemstack, world, player, x, y, z) == true) {
		//	return new ItemStack(Item.bucketEmpty);
		//}
		
		toInject.add(new VarInsnNode (ALOAD, 1));
		toInject.add(new VarInsnNode (ALOAD, 2));
		toInject.add(new VarInsnNode (ALOAD, 3));
		toInject.add(new VarInsnNode (ILOAD, 7));
		toInject.add(new VarInsnNode (ILOAD, 8));
		toInject.add(new VarInsnNode (ILOAD, 9));
		toInject.add(new MethodInsnNode(INVOKESTATIC, hookClass.getName().replace('.', '/'), hookMethod, hookDesc));
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z"));
		LabelNode labelIfEquals = new LabelNode();
		toInject.add(new JumpInsnNode(IFEQ, labelIfEquals));
		toInject.add(new TypeInsnNode(NEW, "net/minecraft/item/ItemStack"));
		toInject.add(new InsnNode(DUP));
		toInject.add(new FieldInsnNode(GETSTATIC, "yc", "ay", "Lyc;"));
		toInject.add(new MethodInsnNode(INVOKESPECIAL, "net/minecraft/item/ItemStack", "<init>", "(Lnet/minecraft/item/Item;)V"));
		toInject.add(new InsnNode(ARETURN));
		toInject.add(labelIfEquals);
		
		method.instructions.insertBefore(targetNode, toInject);
		
		ModBnBTweaks.Log.info(" Added " + hookMethod + " hook to " + method.name);
	}
	
	public void addOnFullItemBucketUseHookTiC(MethodNode method, Class<?> hookClass, String hookMethod, String hookDesc)
	{
		AbstractInsnNode targetNode = findChronoInstructionOfType(method, ALOAD, 20);
		
		InsnList toInject = new InsnList();
		
		toInject.add(new VarInsnNode (ALOAD, 1));
		toInject.add(new VarInsnNode (ALOAD, 2));
		toInject.add(new VarInsnNode (ALOAD, 3));
		toInject.add(new VarInsnNode (ILOAD, 13));
		toInject.add(new VarInsnNode (ILOAD, 14));
		toInject.add(new VarInsnNode (ILOAD, 15));
		toInject.add(new MethodInsnNode(INVOKESTATIC, hookClass.getName().replace('.', '/'), hookMethod, hookDesc));
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z"));
		LabelNode labelIfEquals = new LabelNode();
		toInject.add(new JumpInsnNode(IFEQ, labelIfEquals));
		toInject.add(new TypeInsnNode(NEW, "net/minecraft/item/ItemStack"));
		toInject.add(new InsnNode(DUP));
		toInject.add(new FieldInsnNode(GETSTATIC, "yc", "ay", "Lyc;"));
		toInject.add(new MethodInsnNode(INVOKESPECIAL, "net/minecraft/item/ItemStack", "<init>", "(Lnet/minecraft/item/Item;)V"));
		toInject.add(new InsnNode(ARETURN));
		toInject.add(labelIfEquals);
		
		method.instructions.insertBefore(targetNode, toInject);
		
		ModBnBTweaks.Log.info(" Added " + hookMethod + " hook to " + method.name);
	}
}