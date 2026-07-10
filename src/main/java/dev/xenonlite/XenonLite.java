package dev.xenonlite;

import dev.xenonlite.event.ClientTickHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import xenon.dev.modules.mods.combat.LeftClicker;
import xenon.dev.modules.mods.combat.RightClicker;
import xenon.dev.modules.mods.combat.Refill;
import xenon.dev.modules.mods.combat.ThrowPot;
import xenon.dev.modules.mods.combat.AimAssist;

@Mod(
        modid = XenonLite.MOD_ID,
        name = XenonLite.NAME,
        version = XenonLite.VERSION,
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]"
)
public final class XenonLite {
    public static final String MOD_ID = "xenonlite";
    public static final String NAME = "Xenon Lite";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MOD_ID)
    public static XenonLite instance;

    private LeftClicker leftClicker;
    private RightClicker rightClicker;
    private Refill refill;
    private ThrowPot throwPot;
    private AimAssist aimAssist;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClientTickHandler());
        this.leftClicker = new LeftClicker();
        MinecraftForge.EVENT_BUS.register(this.leftClicker);
        this.rightClicker = new RightClicker();
        MinecraftForge.EVENT_BUS.register(this.rightClicker);
        this.refill = new Refill();
        MinecraftForge.EVENT_BUS.register(this.refill);
        this.throwPot = new ThrowPot();
        this.aimAssist = new AimAssist();
        MinecraftForge.EVENT_BUS.register(this.aimAssist);
    }

    public LeftClicker getLeftClicker() {
        return this.leftClicker;
    }

    public RightClicker getRightClicker() {
        return this.rightClicker;
    }

    public Refill getRefill() {
        return this.refill;
    }

    public ThrowPot getThrowPot() {
        return this.throwPot;
    }

    public AimAssist getAimAssist() {
        return this.aimAssist;
    }
}
