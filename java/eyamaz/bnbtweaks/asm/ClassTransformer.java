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
		
		if (name.equals("net.minecraft.block.material.MaterialPortal") || name.equals("akf"))
		{
			boolean isObfuscated = name.equals("akf");
			
			ModBnBTweaks.Log.info("Patching Minecraft MaterialPortal");
			
			ClassNode classNode = readClassFromBytes(bytes);
			
			MethodNode methodNode = findMethodNodeOfClass(classNode, "isSolid", "()Z");
			MethodNode obfMethodNode = findMethodNodeOfClass(classNode, "a", "()Z");
			
			if(methodNode != null || obfMethodNode != null)
			{
				if (!isObfuscated)
				{
					fixMinecraftMaterialPortal(methodNode);
				}
				else if (isObfuscated)
				{
					fixMinecraftMaterialPortal(obfMethodNode);
				}
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
				ModBnBTweaks.Log.info("Found target method: " + methodName);
				return method;
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

		ModBnBTweaks.Log.info("Patched " + method.name);
	}

	public void fixHostileWorldsMapGenSchematics(MethodNode method)
	{
		AbstractInsnNode targetNode = findFirstInstructionOfType(method, ALOAD);
		
		InsnList toInject = new InsnList();
		
		//Add return statement to beginning of method
		
		toInject.add(new InsnNode(RETURN));
		
		method.instructions.insertBefore(targetNode, toInject);
		
		ModBnBTweaks.Log.info("Patched " + method.name);
	}
	
	public void fixMinecraftMaterialPortal(MethodNode method)
	{
		AbstractInsnNode targetNode = findFirstInstructionOfType(method, ICONST_0);
		
		InsnList toInject = new InsnList();
		
		//Change portals isSolid to return true, rather than false
		//Causing liquids to no longer break them
		
		toInject.add(new InsnNode(ICONST_1));
		
		method.instructions.insert(targetNode, toInject);
		method.instructions.remove(targetNode);
		
		ModBnBTweaks.Log.info("Patched: " + method.name);
	}
}