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
import xenon.dev.modules.mods.combat.Equipo;
import xenon.dev.modules.mods.combat.Reach;
import xenon.dev.modules.mods.combat.PatchClicker;
import xenon.dev.modules.mods.combat.PatchClickerV2;
import xenon.dev.modules.mods.combat.LeftClickerV2;
import xenon.dev.modules.mods.combat.PatchCrumbs;
import xenon.dev.modules.mods.player.Velocity;

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
    private Equipo equipo;
    private Reach reach;
    private PatchClicker patchClicker;
    private PatchClickerV2 patchClickerV2;
    private LeftClickerV2 leftClickerV2;
    private PatchCrumbs patchCrumbs;
    private Velocity velocity;

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
        this.equipo = new Equipo();
        MinecraftForge.EVENT_BUS.register(this.equipo);
        this.aimAssist = new AimAssist(this.equipo);
        MinecraftForge.EVENT_BUS.register(this.aimAssist);
        this.reach = new Reach();
        MinecraftForge.EVENT_BUS.register(this.reach);
        this.patchClicker = new PatchClicker();
        MinecraftForge.EVENT_BUS.register(this.patchClicker);
        this.patchClickerV2 = new PatchClickerV2();
        MinecraftForge.EVENT_BUS.register(this.patchClickerV2);
        this.leftClickerV2 = new LeftClickerV2();
        MinecraftForge.EVENT_BUS.register(this.leftClickerV2);
        this.patchCrumbs = new PatchCrumbs();
        MinecraftForge.EVENT_BUS.register(this.patchCrumbs);
        this.velocity = new Velocity();
        MinecraftForge.EVENT_BUS.register(this.velocity);
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

    public Equipo getEquipo() {
        return this.equipo;
    }

    public Reach getReach() {
        return this.reach;
    }

    public PatchClicker getPatchClicker() {
        return this.patchClicker;
    }

    public PatchClickerV2 getPatchClickerV2() {
        return this.patchClickerV2;
    }

    public LeftClickerV2 getLeftClickerV2() {
        return this.leftClickerV2;
    }

    public PatchCrumbs getPatchCrumbs() {
        return this.patchCrumbs;
    }

    public Velocity getVelocity() {
        return this.velocity;
    }
}
