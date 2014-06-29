package eyamaz.bnbtweaks.asm;

import static org.objectweb.asm.Opcodes.*;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
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
		
		if (name.equals("net.minecraft.item.ItemBucket"))
		{
			ModBnBTweaks.Log.info("Patching ItemBucket....");
			
			ClassNode classNode = readClassFromBytes(bytes);
			MethodNode methodNode = findMethodNodeOfClass(classNode, "onItemRightClick", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;");
			if (methodNode != null)
			{
				fixItemBucket(methodNode);
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
	
	public void fixItemBucket(MethodNode method)
	{
		AbstractInsnNode targetNode = findChronoInstructionOfType(method, ALOAD, 54);
		
		InsnList toInject = new InsnList();
		
        	//if (par2World.getBlockId(i, j, k) == 2957) 
        	//{
        	//	return new ItemStack(Item.bucketEmpty);
        	//}
		//Effectively if clicked on a Bloody Cobblestone from Hostile Worlds
		//Removes liquid from bucket without placing
		
		toInject.add(new VarInsnNode(ALOAD, 2)); //par2World
		toInject.add(new VarInsnNode(ILOAD, 7)); //i
		toInject.add(new VarInsnNode(ILOAD, 8)); //j
		toInject.add(new VarInsnNode(ILOAD, 9)); //k
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, "new/minecraft/world/World", "getBlockId", "(III)I")); //getBlockId
		toInject.add(new IntInsnNode(SIPUSH, 2957)); // MyId to be compared to getBlockId
		LabelNode labelIfEqualTo = new LabelNode(); // labelnode if true
		toInject.add(new JumpInsnNode(IF_ICMPNE, labelIfEqualTo)); // if getBlockId == MyId
		toInject.add(new TypeInsnNode(NEW, "net/minecraft/item/ItemStack")); // Load Return Values
		toInject.add(new InsnNode(DUP)); //Load Return Values
		toInject.add(new FieldInsnNode(GETSTATIC, "net/minecraft/item/Item", "bucketEmpty", "Lnet/minecraft/item/Item;")); // Load Return Values
		toInject.add(new MethodInsnNode(INVOKESPECIAL, "net/minecraft/item/ItemStack", "<init>", "(Lnet/minecraft/item/Item;)V")); // Load Return Values
		toInject.add(new InsnNode(ARETURN)); //Return Values
		toInject.add(labelIfEqualTo); // Jump here if true
		
		method.instructions.insertBefore(targetNode, toInject);
		
		ModBnBTweaks.Log.info(" Patched " + method.name);
		
	}
}
