package dev.compactmachines;

import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JvmVendorSpec;

public interface GanderConstants {
	boolean IS_CI = Boolean.getBoolean("CI");

	// java version/vendor to develop against
	JavaLanguageVersion JAVA_VERSION = JavaLanguageVersion.of(17);
	JvmVendorSpec JAVA_VENDOR = IS_CI ? JvmVendorSpec.ADOPTIUM : JvmVendorSpec.JETBRAINS;

	String MINECRAFT_VERSION = "1.20.1";
	String MINECRAFT_VERSION_SHORT = MINECRAFT_VERSION.substring("1.".length());

	String FORGE_VERSION = MINECRAFT_VERSION + "-47.3.12";

	String PARCHMENT_VERSION = MINECRAFT_VERSION;
	String PARCHMENT_MAPPINGS = "2023.09.03";

	String GROUP = "dev.compactmachines";
	String VERSION = MINECRAFT_VERSION_SHORT + ".0";
}
