package xenon.dev.modules.mods.combat;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;
import xenon.dev.events.XenonEvent;
import xenon.dev.modules.Mod;
import xenon.dev.modules.Module;
import xenon.dev.modules.Settings;

@Mod(keybind = 0)
public final class PatchCrumbs extends Module {
    private static AxisAlignedBB boundingBox;
    private static long resetBox;
    private static long timeout;
    private static double firstX;
    private static double firstY;
    private static double firstZ;
    private static boolean drawNorthSouth;
    private static boolean drawEastWest;

    private final LinkedHashMap<Vec3, AxisAlignedBB> entityList =
            new LinkedHashMap<Vec3, AxisAlignedBB>();
    private boolean checking;

    public PatchCrumbs() {
        this.sl.add(new Settings("Tracer".toCharArray(), this, false));
        this.sl.add(new Settings("Label".toCharArray(), this, false));
        this.sl.add(new Settings("Dispenser Check".toCharArray(), this, false));
        this.sl.add(new Settings("ABC".toCharArray(), this, false));
        this.sl.add(new Settings("Direction".toCharArray(), this, "Auto",
                "Auto", "Both", "North/South", "East/West"));
        this.sl.add(new Settings("Line Width".toCharArray(), this, 2.0F, 1.0F, 5.0F, 1.0F));
        registerSettings();
        this.name = "Patch Crumbs";
    }

    @Override
    public char getSort() {
        return 'p';
    }

    @Override
    public void onDisable() {
        reset();
    }

    @XenonEvent
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!this.enabled || event.phase != TickEvent.Phase.START
                || this.minecraft.thePlayer == null || this.minecraft.theWorld == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (boundingBox != null && now > resetBox) {
            reset();
        }
        if (timeout > now) {
            return;
        }
        for (Entity entity : this.minecraft.theWorld.loadedEntityList) {
            if (setting(2).getValBoolean() && inDispenserRegion(entity)) {
                continue;
            }
            if (entity instanceof EntityTNTPrimed) {
                if (setting(3).getValBoolean()) {
                    crumbSandCheck(entity);
                } else {
                    crumbNoSandCheck(entity);
                }
            }
        }
    }

    private void crumbSandCheck(Entity entity) {
        boolean stopped = entity.motionX + entity.motionY + entity.motionZ == 0.0D;
        BlockPos below = new BlockPos(entity.posX, entity.posY - 2.0D, entity.posZ);
        boolean sandBelow = this.minecraft.theWorld.getBlockState(below).getBlock() instanceof BlockSand
                || this.minecraft.theWorld.getBlockState(below).getBlock() instanceof BlockGravel;
        if (!this.checking && stopped && sandBelow) {
            Map.Entry<Vec3, AxisAlignedBB> previous = getBoundingBox(entity);
            if (previous == null) {
                return;
            }
            Vec3 position = previous.getKey();
            AxisAlignedBB box = previous.getValue();
            if (same(firstX, position.xCoord, firstZ, position.zCoord)) {
                return;
            }
            checkDirection(new Vec3(entity.posX, entity.posY, entity.posZ));
            boundingBox = new AxisAlignedBB(box.maxX, Math.ceil(box.maxY), box.maxZ,
                    box.minX, Math.ceil(box.minY), box.minZ);
            firstX = position.xCoord;
            firstY = position.yCoord;
            firstZ = position.zCoord;
            resetBox = System.currentTimeMillis() + 6000L;
            this.entityList.clear();
            this.checking = true;
        } else {
            this.checking = false;
            this.entityList.put(new Vec3(entity.posX, entity.posY, entity.posZ),
                    entity.getEntityBoundingBox());
        }
    }

    private void crumbNoSandCheck(Entity entity) {
        long now = System.currentTimeMillis();
        if (now <= timeout || same(firstX, entity.posX, firstZ, entity.posZ)) {
            return;
        }
        checkDirection(new Vec3(entity.posX, entity.posY, entity.posZ));
        firstY = entity.posY;
        firstX = entity.posX;
        firstZ = entity.posZ;
        boundingBox = entity.getEntityBoundingBox();
        resetBox = now + 6000L;
        timeout = now + 1000L;
    }

    private Map.Entry<Vec3, AxisAlignedBB> getBoundingBox(Entity entity) {
        Map.Entry<Vec3, AxisAlignedBB> result = null;
        for (Map.Entry<Vec3, AxisAlignedBB> entry : this.entityList.entrySet()) {
            Vec3 position = entry.getKey();
            if (position.xCoord == entity.posX && position.zCoord == entity.posZ) {
                result = entry;
                break;
            }
        }
        this.entityList.clear();
        return result;
    }

    private static boolean same(double oldX, double newX, double oldZ, double newZ) {
        return Math.ceil(oldX) == Math.ceil(newX) && Math.ceil(oldZ) == Math.ceil(newZ);
    }

    private void checkDirection(Vec3 position) {
        drawEastWest = false;
        drawNorthSouth = false;
        String direction = setting(4).getValString();
        if ("Auto".equals(direction)) {
            if (solid(position.xCoord - 1.0D, position.yCoord, position.zCoord)
                    || solid(position.xCoord + 1.0D, position.yCoord, position.zCoord)) {
                drawEastWest = true;
            }
            if (solid(position.xCoord, position.yCoord, position.zCoord - 1.0D)
                    || solid(position.xCoord, position.yCoord, position.zCoord + 1.0D)) {
                drawNorthSouth = true;
            }
            if (!drawEastWest && !drawNorthSouth) {
                drawEastWest = true;
            }
            drawNorthSouth = true;
        } else if ("North/South".equals(direction)) {
            drawNorthSouth = true;
        } else if ("East/West".equals(direction)) {
            drawEastWest = true;
        } else {
            drawNorthSouth = true;
            drawEastWest = true;
        }
    }

    private boolean solid(double x, double y, double z) {
        return this.minecraft.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock().isFullBlock();
    }

    private boolean inDispenserRegion(Entity entity) {
        for (TileEntity tile : this.minecraft.theWorld.loadedTileEntityList) {
            if (tile instanceof TileEntityDispenser
                    || tile.getBlockType() instanceof BlockDispenser) {
                BlockPos position = tile.getPos();
                if (Math.abs(position.getX() - entity.posX) <= 16.0D
                        && Math.abs(position.getZ() - entity.posZ) <= 16.0D) {
                    return true;
                }
            }
        }
        return false;
    }

    @XenonEvent
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!this.enabled || this.minecraft.theWorld == null || this.minecraft.thePlayer == null
                || boundingBox == null) {
            return;
        }
        if (System.currentTimeMillis() > resetBox) {
            reset();
            return;
        }

        double viewX = this.minecraft.getRenderManager().viewerPosX;
        double viewY = this.minecraft.getRenderManager().viewerPosY;
        double viewZ = this.minecraft.getRenderManager().viewerPosZ;
        float width = setting(5).getValfloat();
        double x = firstX - 0.5D;
        double y = firstY;
        double z = firstZ - 0.5D;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(width);
        GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);

        RenderGlobal.drawSelectionBoundingBox(
                new AxisAlignedBB(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D)
                        .offset(-viewX, -viewY, -viewZ));
        boolean crosshairAligned = isCrosshairAtCrumbHeight();
        if (crosshairAligned) {
            GlStateManager.color(0.0F, 1.0F, 0.0F, 1.0F);
        }
        if (drawEastWest) {
            RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(
                    x - 199.5D, y, z, x + 200.5D, y + 1.0D, z + 1.0D)
                    .offset(-viewX, -viewY, -viewZ));
        }
        if (drawNorthSouth) {
            RenderGlobal.drawSelectionBoundingBox(new AxisAlignedBB(
                    x, y, z - 199.5D, x + 1.0D, y + 1.0D, z + 200.5D)
                    .offset(-viewX, -viewY, -viewZ));
        }
        GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
        drawFilledBox(x - viewX, y - viewY, z - viewZ);

        if (setting(0).getValBoolean()) {
            drawTracer(x + 0.5D - viewX, y + 0.5D - viewY, z + 0.5D - viewZ);
        }
        if (setting(1).getValBoolean()) {
            GlStateManager.enableTexture2D();
            drawLabel(x + 0.5D - viewX, y + 2.5D - viewY, z + 0.5D - viewZ);
            GlStateManager.disableTexture2D();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private boolean isCrosshairAtCrumbHeight() {
        MovingObjectPosition hit = this.minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || hit.getBlockPos() == null || hit.hitVec == null) {
            return false;
        }
        Material material = this.minecraft.theWorld.getBlockState(hit.getBlockPos()).getBlock().getMaterial();
        if (material == Material.air || material.isLiquid()) {
            return false;
        }
        double hitY = hit.hitVec.yCoord;
        return hitY >= firstY && hitY <= firstY + 1.0D;
    }

    private void drawFilledBox(double x, double y, double z) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        GlStateManager.color(1.0F, 0.0F, 0.0F, 0.5F);
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        addBoxVertices(renderer, x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
        tessellator.draw();
    }

    private static void addBoxVertices(WorldRenderer wr, double x1, double y1, double z1,
            double x2, double y2, double z2) {
        wr.pos(x1,y1,z2).endVertex(); wr.pos(x2,y1,z2).endVertex(); wr.pos(x2,y1,z1).endVertex(); wr.pos(x1,y1,z1).endVertex();
        wr.pos(x1,y2,z2).endVertex(); wr.pos(x1,y2,z1).endVertex(); wr.pos(x2,y2,z1).endVertex(); wr.pos(x2,y2,z2).endVertex();
        wr.pos(x1,y1,z2).endVertex(); wr.pos(x1,y2,z2).endVertex(); wr.pos(x2,y2,z2).endVertex(); wr.pos(x2,y1,z2).endVertex();
        wr.pos(x1,y1,z1).endVertex(); wr.pos(x2,y1,z1).endVertex(); wr.pos(x2,y2,z1).endVertex(); wr.pos(x1,y2,z1).endVertex();
        wr.pos(x1,y1,z1).endVertex(); wr.pos(x1,y2,z1).endVertex(); wr.pos(x1,y2,z2).endVertex(); wr.pos(x1,y1,z2).endVertex();
        wr.pos(x2,y1,z1).endVertex(); wr.pos(x2,y1,z2).endVertex(); wr.pos(x2,y2,z2).endVertex(); wr.pos(x2,y2,z1).endVertex();
    }

    private void drawTracer(double x, double y, double z) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(0.0D, this.minecraft.thePlayer.getEyeHeight(), 0.0D);
        GL11.glVertex3d(x, y, z);
        GL11.glEnd();
    }

    private void drawLabel(double x, double y, double z) {
        String label = "X: " + (int)Math.ceil(firstX) + " Y: " + (int)Math.ceil(firstY)
                + " Z: " + (int)Math.ceil(firstZ);
        float scale = Math.max(0.04F, Math.min(1.0F,
                (float)this.minecraft.thePlayer.getDistance(firstX, firstY, firstZ) * 0.003F));
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-this.minecraft.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.minecraft.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        this.minecraft.fontRendererObj.drawString(label,
                -this.minecraft.fontRendererObj.getStringWidth(label) / 2, 0, Color.RED.getRGB());
        GlStateManager.popMatrix();
    }

    private void reset() {
        boundingBox = null;
        this.checking = false;
        drawEastWest = false;
        drawNorthSouth = false;
        this.entityList.clear();
    }

    private Settings setting(int index) {
        return this.sl.get(index);
    }
}
